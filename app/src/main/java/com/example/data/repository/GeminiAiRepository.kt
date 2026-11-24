package com.example.data.repository

import android.util.Log
import com.example.data.model.AiBreakdownResult
import com.example.data.model.GroundingSource
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.categoryEnum
import com.example.data.model.priorityEnum
import com.example.data.model.statusEnum
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "GeminiAiRepository"
private const val DEFAULT_MODEL = "gemini-3.8-flash"

data class AiStreamChunk(
    val accumulatedText: String,
    val sources: List<GroundingSource> = emptyList(),
    val isFinal: Boolean = false
)

class GeminiAiRepository {

    private val breakdownSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "summary" to Schema.string(description = "A concise 1-sentence goal summary."),
                "steps" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 5 crisp, actionable subtasks."
                ),
                "priority" to Schema.enumeration(
                    values = listOf("LOW", "MEDIUM", "HIGH"),
                    description = "Recommended priority level."
                ),
                "category" to Schema.enumeration(
                    values = listOf("PERSONAL", "WORK", "STUDY", "OTHER"),
                    description = "Most fitting task category."
                )
            )
        )
    }

    private val breakdownModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = breakdownSchema
            },
            systemInstruction = content {
                text(
                    "You are TaskFlow's task decomposition engine. Break a user's task title and notes into 3-5 clear, concrete checklist steps and suggest the best priority and category."
                )
            }
        )
    }

    suspend fun generateSmartBreakdown(
        title: String,
        currentDescription: String
    ): Result<AiBreakdownResult> {
        return try {
            val response = withTimeout(60_000L) {
                breakdownModel.generateContent(
                    "Task title: $title\nExisting notes: ${currentDescription.ifBlank { "None" }}"
                )
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))
            val json = JSONObject(rawJson)
            val summary = json.optString("summary", "").trim()
            val stepsJson = json.optJSONArray("steps")
            val steps = buildList {
                if (stepsJson != null) {
                    for (i in 0 until stepsJson.length()) {
                        val step = stepsJson.optString(i, "").trim()
                        if (step.isNotEmpty()) add("• $step")
                    }
                }
            }
            val combined = buildString {
                if (summary.isNotEmpty()) {
                    append(summary)
                    if (steps.isNotEmpty()) append("\n\n")
                }
                append(steps.joinToString("\n"))
            }.take(980)

            val priority = TaskPriority.fromRaw(json.optString("priority"))
            val category = TaskCategory.fromRaw(json.optString("category"))
            Result.success(
                AiBreakdownResult(
                    refinedDescription = combined,
                    suggestedPriority = priority,
                    suggestedCategory = category
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Smart breakdown failed", e)
            Result.failure(e)
        }
    }

    fun streamAssistantResponse(
        userPrompt: String,
        conversationHistory: List<Content>,
        tasksSnapshot: List<TaskItem>,
        enableSearchGrounding: Boolean,
        enableHighThinking: Boolean
    ): Flow<AiStreamChunk> = flow {
        val taskContext = buildTaskContext(tasksSnapshot)
        val systemPrompt = content {
            text(
                """
                You are TaskFlow AI Coach, an expert productivity strategist and research assistant inside the TaskFlow Android app.
                Help the user prioritize work, break down complex goals, answer questions, and plan realistic schedules.
                Be concise, structured, and actionable.
                When suggesting a concrete new task the user might want to add, clearly title it so they can save it.

                Current user task workspace snapshot:
                $taskContext
                """.trimIndent()
            )
        }

        val config = if (enableHighThinking) {
            // Do not set maxOutputTokens when ThinkingLevel.HIGH is active
            generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            }
        } else {
            generationConfig { }
        }

        val tools = if (enableSearchGrounding) {
            listOf(Tool.googleSearch())
        } else {
            emptyList()
        }

        val model = Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = config,
            tools = tools,
            systemInstruction = systemPrompt
        )

        val chat = model.startChat(history = conversationHistory)
        val accumulated = StringBuilder()
        val collectedSources = linkedMapOf<String, GroundingSource>()

        withTimeout(60_000L) {
            chat.sendMessageStream(userPrompt).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                }
                val groundingChunks = chunk.candidates.firstOrNull()?.groundingMetadata?.groundingChunks
                groundingChunks?.forEach { gChunk ->
                    val web = gChunk.web
                    val uri = web?.uri
                    if (!uri.isNullOrBlank()) {
                        val title = web.title?.takeIf { it.isNotBlank() } ?: uri
                        collectedSources[uri] = GroundingSource(title = title, url = uri)
                    }
                }
                emit(
                    AiStreamChunk(
                        accumulatedText = accumulated.toString(),
                        sources = collectedSources.values.toList(),
                        isFinal = false
                    )
                )
            }
        }

        if (accumulated.isBlank()) {
            throw IllegalStateException("Gemini returned an empty response.")
        }

        emit(
            AiStreamChunk(
                accumulatedText = accumulated.toString(),
                sources = collectedSources.values.toList(),
                isFinal = true
            )
        )
    }

    private fun buildTaskContext(tasks: List<TaskItem>): String {
        if (tasks.isEmpty()) return "No tasks currently saved."
        val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        return tasks.take(25).joinToString("\n") { task ->
            val due = task.dueDateMillis?.let { dateFormat.format(Date(it)) } ?: "No due date"
            "- [${task.statusEnum().label}] (${task.priorityEnum().label} | ${task.categoryEnum().label}) ${task.title} — Due: $due"
        }
    }
}
