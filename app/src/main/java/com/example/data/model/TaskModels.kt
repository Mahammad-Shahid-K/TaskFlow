package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Calendar

enum class TaskPriority(val label: String, val weight: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3);

    companion object {
        fun fromRaw(raw: String?): TaskPriority =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MEDIUM
    }
}

enum class TaskCategory(val label: String) {
    PERSONAL("Personal"),
    WORK("Work"),
    STUDY("Study"),
    OTHER("Other");

    companion object {
        fun fromRaw(raw: String?): TaskCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PERSONAL
    }
}

enum class TaskStatus(val label: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    companion object {
        fun fromRaw(raw: String?): TaskStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PENDING
    }
}

enum class TaskViewTab(val label: String) {
    ALL("All Tasks"),
    MY_DAY("My Day"),
    UPCOMING("Upcoming"),
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

enum class TaskSortOrder(val label: String) {
    DUE_DATE_ASC("Due Date (Soonest)"),
    DUE_DATE_DESC("Due Date (Latest)"),
    PRIORITY_HIGH_FIRST("Priority (High First)"),
    NEWEST_FIRST("Recently Created")
}

@Keep
@IgnoreExtraProperties
data class TaskItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val priority: String = TaskPriority.MEDIUM.name,
    val category: String = TaskCategory.PERSONAL.name,
    val status: String = TaskStatus.PENDING.name,
    val dueDateMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun TaskItem.priorityEnum(): TaskPriority = TaskPriority.fromRaw(priority)
fun TaskItem.categoryEnum(): TaskCategory = TaskCategory.fromRaw(category)
fun TaskItem.statusEnum(): TaskStatus = TaskStatus.fromRaw(status)

fun TaskItem.isTaskOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
    val due = dueDateMillis ?: return false
    return statusEnum() != TaskStatus.COMPLETED && due < nowMillis
}

fun TaskItem.isTaskDueToday(nowMillis: Long = System.currentTimeMillis()): Boolean {
    val due = dueDateMillis ?: return false
    val calNow = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val calDue = Calendar.getInstance().apply { timeInMillis = due }
    return calNow.get(Calendar.YEAR) == calDue.get(Calendar.YEAR) &&
        calNow.get(Calendar.DAY_OF_YEAR) == calDue.get(Calendar.DAY_OF_YEAR)
}

fun TaskItem.isTaskUpcoming(nowMillis: Long = System.currentTimeMillis()): Boolean {
    val due = dueDateMillis ?: return false
    if (statusEnum() == TaskStatus.COMPLETED) return false
    val endOfToday = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
    return due > endOfToday
}

data class TaskStatistics(
    val total: Int = 0,
    val pending: Int = 0,
    val inProgress: Int = 0,
    val completed: Int = 0,
    val overdue: Int = 0
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class GroundingSource(
    val title: String,
    val url: String
)

data class SuggestedActionTask(
    val title: String,
    val description: String,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: TaskCategory = TaskCategory.PERSONAL
)

data class AiChatMessage(
    val id: String,
    val isFromUser: Boolean,
    val text: String,
    val usedSearchGrounding: Boolean = false,
    val usedHighThinking: Boolean = false,
    val sources: List<GroundingSource> = emptyList(),
    val suggestedTask: SuggestedActionTask? = null
)

data class AiBreakdownResult(
    val refinedDescription: String,
    val suggestedPriority: TaskPriority,
    val suggestedCategory: TaskCategory
)
