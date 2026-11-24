package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiBreakdownResult
import com.example.data.model.AiChatMessage
import com.example.data.model.SuggestedActionTask
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.repository.GeminiAiRepository
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AiAssistantUiState(
    val messages: List<AiChatMessage> = listOf(
        AiChatMessage(
            id = "welcome",
            isFromUser = false,
            text = "Hi! I'm your TaskFlow AI Coach. Ask me to prioritize your day, break down a big project, or enable Google Search grounding for live research."
        )
    ),
    val streamingText: String = "",
    val isGenerating: Boolean = false,
    val useSearchGrounding: Boolean = false,
    val useHighThinking: Boolean = false,
    val errorMessage: String? = null,
    val isBreakingDownTask: Boolean = false,
    val breakdownError: String? = null
)

class AiAssistantViewModel(
    private val aiRepository: GeminiAiRepository = GeminiAiRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<Content>()

    fun toggleSearchGrounding() {
        _uiState.value = _uiState.value.copy(useSearchGrounding = !_uiState.value.useSearchGrounding)
    }

    fun toggleHighThinking() {
        _uiState.value = _uiState.value.copy(useHighThinking = !_uiState.value.useHighThinking)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null, breakdownError = null)
    }

    fun clearConversation() {
        conversationHistory.clear()
        _uiState.value = _uiState.value.copy(
            messages = listOf(
                AiChatMessage(
                    id = "welcome_reset",
                    isFromUser = false,
                    text = "Fresh conversation started. How can I help organize your tasks today?"
                )
            ),
            streamingText = "",
            errorMessage = null
        )
    }

    fun sendMessage(prompt: String, currentTasks: List<TaskItem>) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty() || _uiState.value.isGenerating) return

        val searchEnabled = _uiState.value.useSearchGrounding
        val thinkingEnabled = _uiState.value.useHighThinking
        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            isFromUser = true,
            text = cleanPrompt,
            usedSearchGrounding = searchEnabled,
            usedHighThinking = thinkingEnabled
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg,
            streamingText = "",
            isGenerating = true,
            errorMessage = null
        )

        viewModelScope.launch {
            var finalText = ""
            var finalSources = emptyList<com.example.data.model.GroundingSource>()
            try {
                aiRepository.streamAssistantResponse(
                    userPrompt = cleanPrompt,
                    conversationHistory = conversationHistory.toList(),
                    tasksSnapshot = currentTasks,
                    enableSearchGrounding = searchEnabled,
                    enableHighThinking = thinkingEnabled
                ).collect { chunk ->
                    finalText = chunk.accumulatedText
                    finalSources = chunk.sources
                    _uiState.value = _uiState.value.copy(streamingText = finalText)
                }

                conversationHistory.add(content("user") { text(cleanPrompt) })
                conversationHistory.add(content("model") { text(finalText) })

                val suggestedTask = extractSuggestedTask(cleanPrompt, finalText)
                val assistantMsg = AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    isFromUser = false,
                    text = finalText,
                    usedSearchGrounding = searchEnabled,
                    usedHighThinking = thinkingEnabled,
                    sources = finalSources,
                    suggestedTask = suggestedTask
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + assistantMsg,
                    streamingText = "",
                    isGenerating = false
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini stream timed out", e)
                _uiState.value = _uiState.value.copy(
                    streamingText = "",
                    isGenerating = false,
                    errorMessage = "Gemini timed out after 60 seconds. Please retry or shorten your prompt."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini stream failed", e)
                _uiState.value = _uiState.value.copy(
                    streamingText = "",
                    isGenerating = false,
                    errorMessage = e.localizedMessage ?: "AI request failed."
                )
            }
        }
    }

    fun requestSmartBreakdown(
        title: String,
        description: String,
        onResult: (AiBreakdownResult) -> Unit
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            _uiState.value = _uiState.value.copy(breakdownError = "Enter a task title first for AI breakdown.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBreakingDownTask = true, breakdownError = null)
            val result = aiRepository.generateSmartBreakdown(cleanTitle, description)
            _uiState.value = _uiState.value.copy(isBreakingDownTask = false)
            result.fold(
                onSuccess = { onResult(it) },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        breakdownError = err.localizedMessage ?: "Smart breakdown failed."
                    )
                }
            )
        }
    }

    private fun extractSuggestedTask(prompt: String, reply: String): SuggestedActionTask {
        val titleCandidate = prompt
            .removePrefix("Break down")
            .removePrefix("Help me plan")
            .trim()
            .take(80)
            .ifBlank { "AI Action Plan" }
        val cleanDesc = reply.trim().take(600)
        val priority = if (reply.contains("urgent", ignoreCase = true) || reply.contains("high priority", ignoreCase = true)) {
            TaskPriority.HIGH
        } else {
            TaskPriority.MEDIUM
        }
        val category = when {
            prompt.contains("study", ignoreCase = true) || prompt.contains("exam", ignoreCase = true) -> TaskCategory.STUDY
            prompt.contains("work", ignoreCase = true) || prompt.contains("meeting", ignoreCase = true) -> TaskCategory.WORK
            else -> TaskCategory.PERSONAL
        }
        return SuggestedActionTask(
            title = titleCandidate,
            description = cleanDesc,
            priority = priority,
            category = category
        )
    }

    private companion object {
        const val TAG = "AiAssistantVM"
    }
}
