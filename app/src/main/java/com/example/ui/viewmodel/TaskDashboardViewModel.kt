package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.TaskSortOrder
import com.example.data.model.TaskStatistics
import com.example.data.model.TaskStatus
import com.example.data.model.TaskViewTab
import com.example.data.model.UiState
import com.example.data.model.categoryEnum
import com.example.data.model.isTaskDueToday
import com.example.data.model.isTaskOverdue
import com.example.data.model.isTaskUpcoming
import com.example.data.model.priorityEnum
import com.example.data.model.statusEnum
import com.example.data.repository.TaskRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardFilterState(
    val selectedTab: TaskViewTab = TaskViewTab.ALL,
    val searchQuery: String = "",
    val priorityFilter: TaskPriority? = null,
    val categoryFilter: TaskCategory? = null,
    val statusFilter: TaskStatus? = null,
    val sortOrder: TaskSortOrder = TaskSortOrder.DUE_DATE_ASC
)

data class DashboardUiModel(
    val allTasks: List<TaskItem> = emptyList(),
    val filteredTasks: List<TaskItem> = emptyList(),
    val statistics: TaskStatistics = TaskStatistics(),
    val filterState: DashboardFilterState = DashboardFilterState()
)

sealed interface SnackbarNotice {
    data class Success(val message: String) : SnackbarNotice
    data class Error(val message: String) : SnackbarNotice
}

class TaskDashboardViewModel(
    private val repository: TaskRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _filterState = MutableStateFlow(DashboardFilterState())
    val filterState: StateFlow<DashboardFilterState> = _filterState.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _notices = MutableSharedFlow<SnackbarNotice>()
    val notices: SharedFlow<SnackbarNotice> = _notices.asSharedFlow()

    private val rawTasksState: StateFlow<UiState<List<TaskItem>>> =
        repository.observeTasks(currentUserId)
            .map<List<TaskItem>, UiState<List<TaskItem>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing tasks", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load tasks."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val dashboardState: StateFlow<UiState<DashboardUiModel>> =
        combine(rawTasksState, _filterState) { tasksUi, filter ->
            when (tasksUi) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> UiState.Error(tasksUi.message)
                is UiState.Success -> {
                    val tasks = tasksUi.data
                    val now = System.currentTimeMillis()
                    val stats = TaskStatistics(
                        total = tasks.size,
                        pending = tasks.count { it.statusEnum() == TaskStatus.PENDING },
                        inProgress = tasks.count { it.statusEnum() == TaskStatus.IN_PROGRESS },
                        completed = tasks.count { it.statusEnum() == TaskStatus.COMPLETED },
                        overdue = tasks.count { it.isTaskOverdue(now) }
                    )
                    val filtered = applyFiltersAndSort(tasks, filter, now)
                    UiState.Success(
                        DashboardUiModel(
                            allTasks = tasks,
                            filteredTasks = filtered,
                            statistics = stats,
                            filterState = filter
                        )
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun selectTab(tab: TaskViewTab) {
        _filterState.value = _filterState.value.copy(selectedTab = tab)
    }

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun togglePriorityFilter(priority: TaskPriority?) {
        val next = if (_filterState.value.priorityFilter == priority) null else priority
        _filterState.value = _filterState.value.copy(priorityFilter = next)
    }

    fun toggleCategoryFilter(category: TaskCategory?) {
        val next = if (_filterState.value.categoryFilter == category) null else category
        _filterState.value = _filterState.value.copy(categoryFilter = next)
    }

    fun toggleStatusFilter(status: TaskStatus?) {
        val next = if (_filterState.value.statusFilter == status) null else status
        _filterState.value = _filterState.value.copy(statusFilter = next)
    }

    fun setSortOrder(order: TaskSortOrder) {
        _filterState.value = _filterState.value.copy(sortOrder = order)
    }

    fun clearAllFilters() {
        _filterState.value = DashboardFilterState(selectedTab = _filterState.value.selectedTab)
    }

    fun createTask(
        title: String,
        description: String,
        priority: TaskPriority,
        category: TaskCategory,
        status: TaskStatus,
        dueDateMillis: Long?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.createTask(
                userId = currentUserId,
                title = title,
                description = description,
                priority = priority,
                category = category,
                status = status,
                dueDateMillis = dueDateMillis
            )
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    _notices.emit(SnackbarNotice.Success("Task created"))
                    onDone()
                },
                onFailure = { err ->
                    _notices.emit(SnackbarNotice.Error(err.localizedMessage ?: "Could not create task"))
                }
            )
        }
    }

    fun updateTask(
        taskId: String,
        title: String,
        description: String,
        priority: TaskPriority,
        category: TaskCategory,
        status: TaskStatus,
        dueDateMillis: Long?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateTask(
                userId = currentUserId,
                taskId = taskId,
                title = title,
                description = description,
                priority = priority,
                category = category,
                status = status,
                dueDateMillis = dueDateMillis
            )
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    _notices.emit(SnackbarNotice.Success("Task updated"))
                    onDone()
                },
                onFailure = { err ->
                    _notices.emit(SnackbarNotice.Error(err.localizedMessage ?: "Could not update task"))
                }
            )
        }
    }

    fun toggleTaskCompletion(task: TaskItem) {
        val nextStatus = if (task.statusEnum() == TaskStatus.COMPLETED) {
            TaskStatus.PENDING
        } else {
            TaskStatus.COMPLETED
        }
        updateStatus(task.id, nextStatus)
    }

    fun updateStatus(taskId: String, newStatus: TaskStatus) {
        viewModelScope.launch {
            val result = repository.updateTaskStatus(currentUserId, taskId, newStatus)
            result.onFailure { err ->
                _notices.emit(SnackbarNotice.Error(err.localizedMessage ?: "Could not update status"))
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteTask(currentUserId, taskId)
            _isMutating.value = false
            result.fold(
                onSuccess = { _notices.emit(SnackbarNotice.Success("Task deleted")) },
                onFailure = { err ->
                    _notices.emit(SnackbarNotice.Error(err.localizedMessage ?: "Could not delete task"))
                }
            )
        }
    }

    private fun applyFiltersAndSort(
        tasks: List<TaskItem>,
        filter: DashboardFilterState,
        nowMillis: Long
    ): List<TaskItem> {
        val query = filter.searchQuery.trim().lowercase()

        val filtered = tasks.filter { task ->
            val matchesTab = when (filter.selectedTab) {
                TaskViewTab.ALL -> true
                TaskViewTab.MY_DAY -> task.isTaskDueToday(nowMillis)
                TaskViewTab.UPCOMING -> task.isTaskUpcoming(nowMillis)
                TaskViewTab.PENDING -> task.statusEnum() == TaskStatus.PENDING
                TaskViewTab.IN_PROGRESS -> task.statusEnum() == TaskStatus.IN_PROGRESS
                TaskViewTab.COMPLETED -> task.statusEnum() == TaskStatus.COMPLETED
            }
            val matchesQuery = query.isEmpty() ||
                task.title.lowercase().contains(query) ||
                task.description.lowercase().contains(query)
            val matchesPriority = filter.priorityFilter == null || task.priorityEnum() == filter.priorityFilter
            val matchesCategory = filter.categoryFilter == null || task.categoryEnum() == filter.categoryFilter
            val matchesStatus = filter.statusFilter == null || task.statusEnum() == filter.statusFilter

            matchesTab && matchesQuery && matchesPriority && matchesCategory && matchesStatus
        }

        return when (filter.sortOrder) {
            TaskSortOrder.DUE_DATE_ASC -> filtered.sortedWith(
                compareBy<TaskItem> { it.statusEnum() == TaskStatus.COMPLETED }
                    .thenBy { it.dueDateMillis ?: Long.MAX_VALUE }
                    .thenByDescending { it.priorityEnum().weight }
            )
            TaskSortOrder.DUE_DATE_DESC -> filtered.sortedWith(
                compareBy<TaskItem> { it.statusEnum() == TaskStatus.COMPLETED }
                    .thenByDescending { it.dueDateMillis ?: Long.MIN_VALUE }
            )
            TaskSortOrder.PRIORITY_HIGH_FIRST -> filtered.sortedWith(
                compareBy<TaskItem> { it.statusEnum() == TaskStatus.COMPLETED }
                    .thenByDescending { it.priorityEnum().weight }
                    .thenBy { it.dueDateMillis ?: Long.MAX_VALUE }
            )
            TaskSortOrder.NEWEST_FIRST -> filtered.sortedByDescending {
                it.createdAt ?: Timestamp(0, 0)
            }
        }
    }

    private companion object {
        const val TAG = "TaskDashboardVM"
    }
}
