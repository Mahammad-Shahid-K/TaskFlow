package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TaskRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAuthenticatedUid(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google to sync tasks.")
        check(uid == expectedUserId) { "Signed-in account changed; refusing cross-user operation." }
        return uid
    }

    fun observeTasks(userId: String): Flow<List<TaskItem>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val uid = requireAuthenticatedUid(userId)
        val path = "users/$uid/tasks"
        emitAll(
            db.collection("users").document(uid).collection("tasks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TaskItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createTask(
        userId: String,
        title: String,
        description: String,
        priority: TaskPriority,
        category: TaskCategory,
        status: TaskStatus,
        dueDateMillis: Long?
    ): Result<Unit> {
        var docPath = "users/$userId/tasks"
        return try {
            val uid = requireAuthenticatedUid(userId)
            val cleanTitle = title.trim()
            val cleanDescription = description.trim()
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 120) {
                "Task title must be between 1 and 120 characters."
            }
            require(cleanDescription.length <= 1000) {
                "Description cannot exceed 1000 characters."
            }
            if (dueDateMillis != null) {
                require(dueDateMillis >= 0L) { "Due date must be valid." }
            }

            val docRef = db.collection("users").document(uid).collection("tasks").document()
            docPath = docRef.path

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to cleanTitle,
                "description" to cleanDescription,
                "priority" to priority.name,
                "category" to category.name,
                "status" to status.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (dueDateMillis != null) {
                payload["dueDateMillis"] = dueDateMillis
            }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTask(
        userId: String,
        taskId: String,
        title: String,
        description: String,
        priority: TaskPriority,
        category: TaskCategory,
        status: TaskStatus,
        dueDateMillis: Long?
    ): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireAuthenticatedUid(userId)
            require(taskId.isNotBlank()) { "Task ID must not be blank." }
            val cleanTitle = title.trim()
            val cleanDescription = description.trim()
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 120) {
                "Task title must be between 1 and 120 characters."
            }
            require(cleanDescription.length <= 1000) {
                "Description cannot exceed 1000 characters."
            }
            if (dueDateMillis != null) {
                require(dueDateMillis >= 0L) { "Due date must be valid." }
            }

            val updates = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "description" to cleanDescription,
                "priority" to priority.name,
                "category" to category.name,
                "status" to status.name,
                "dueDateMillis" to (dueDateMillis ?: FieldValue.delete()),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("users").document(uid).collection("tasks").document(taskId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTaskStatus(
        userId: String,
        taskId: String,
        status: TaskStatus
    ): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireAuthenticatedUid(userId)
            require(taskId.isNotBlank()) { "Task ID must not be blank." }
            db.collection("users").document(uid).collection("tasks").document(taskId)
                .update(
                    mapOf(
                        "status" to status.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireAuthenticatedUid(userId)
            require(taskId.isNotBlank()) { "Task ID must not be blank." }
            db.collection("users").document(uid).collection("tasks").document(taskId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
