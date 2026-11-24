package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.signOut
import com.example.core.ThemeMode
import com.example.data.model.TaskItem
import com.example.data.model.TaskStatus
import com.example.data.model.UiState
import com.example.ui.components.AiAssistantSheet
import com.example.ui.components.EmptyTasksState
import com.example.ui.components.SearchAndFilterControls
import com.example.ui.components.SettingsSheet
import com.example.ui.components.StatisticsOverviewGrid
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskEditorSheet
import com.example.ui.viewmodel.AiAssistantViewModel
import com.example.ui.viewmodel.SnackbarNotice
import com.example.ui.viewmodel.TaskDashboardViewModel
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentUser: FirebaseUser,
    dashboardViewModel: TaskDashboardViewModel,
    aiViewModel: AiAssistantViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val dashboardState by dashboardViewModel.dashboardState.collectAsStateWithLifecycle()
    val filterState by dashboardViewModel.filterState.collectAsStateWithLifecycle()
    val isMutating by dashboardViewModel.isMutating.collectAsStateWithLifecycle()
    val aiUiState by aiViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var showTaskEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskItem?>(null) }
    var showAiSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(dashboardViewModel) {
        dashboardViewModel.notices.collect { notice ->
            val text = when (notice) {
                is SnackbarNotice.Success -> notice.message
                is SnackbarNotice.Error -> notice.message
            }
            snackbarHostState.showSnackbar(text)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.navigationBars,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.taskflow_icon_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.dashboard_greeting),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            val subtitle = currentUser.displayName?.takeIf { it.isNotBlank() }
                                ?: currentUser.email
                                ?: "Personal Workspace"
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { showAiSheet = true }
                            .testTag("open_ai_button"),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Open AI Coach",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.ask_ai),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Open Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingTask = null
                    aiViewModel.clearError()
                    showTaskEditor = true
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(R.string.add_task)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task_fab")
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 600.dp

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                when (val state = dashboardState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Syncing your workspace…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.widthIn(max = 480.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Could not sync tasks",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    is UiState.Success -> {
                        val model = state.data
                        val hasActiveFilter = filterState.searchQuery.isNotBlank() ||
                            filterState.priorityFilter != null ||
                            filterState.categoryFilter != null ||
                            filterState.statusFilter != null

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 880.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item(key = "stats_grid") {
                                StatisticsOverviewGrid(
                                    stats = model.statistics,
                                    isWideScreen = isWideScreen,
                                    onSelectTab = dashboardViewModel::selectTab
                                )
                            }

                            item(key = "search_filters") {
                                SearchAndFilterControls(
                                    filterState = model.filterState,
                                    onSearchChange = dashboardViewModel::updateSearchQuery,
                                    onSelectTab = dashboardViewModel::selectTab,
                                    onTogglePriority = dashboardViewModel::togglePriorityFilter,
                                    onToggleCategory = dashboardViewModel::toggleCategoryFilter,
                                    onToggleStatus = dashboardViewModel::toggleStatusFilter,
                                    onSelectSortOrder = dashboardViewModel::setSortOrder,
                                    onClearFilters = dashboardViewModel::clearAllFilters
                                )
                            }

                            item(key = "section_header") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${model.filterState.selectedTab.label} (${model.filteredTasks.size})",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (model.statistics.overdue > 0) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${model.statistics.overdue} overdue",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (model.filteredTasks.isEmpty()) {
                                item(key = "empty_state") {
                                    EmptyTasksState(
                                        hasActiveFilter = hasActiveFilter,
                                        onCreateTask = {
                                            editingTask = null
                                            aiViewModel.clearError()
                                            showTaskEditor = true
                                        },
                                        onOpenAi = { showAiSheet = true },
                                        onClearFilters = dashboardViewModel::clearAllFilters
                                    )
                                }
                            } else {
                                items(model.filteredTasks, key = { it.id }) { task ->
                                    TaskCard(
                                        task = task,
                                        onToggleComplete = {
                                            dashboardViewModel.toggleTaskCompletion(task)
                                        },
                                        onChangeStatus = { newStatus ->
                                            dashboardViewModel.updateStatus(task.id, newStatus)
                                        },
                                        onEdit = {
                                            editingTask = task
                                            aiViewModel.clearError()
                                            showTaskEditor = true
                                        },
                                        onDelete = {
                                            taskToDelete = task
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTaskEditor) {
        TaskEditorSheet(
            editingTask = editingTask,
            isSaving = isMutating,
            isBreakingDownWithAi = aiUiState.isBreakingDownTask,
            aiBreakdownError = aiUiState.breakdownError,
            onDismiss = {
                showTaskEditor = false
                editingTask = null
            },
            onRequestAiBreakdown = { title, desc, onApplied ->
                aiViewModel.requestSmartBreakdown(title, desc) { result ->
                    onApplied(
                        result.refinedDescription,
                        result.suggestedPriority,
                        result.suggestedCategory
                    )
                }
            },
            onSave = { title, desc, priority, category, status, dueDateMillis ->
                val current = editingTask
                if (current == null) {
                    dashboardViewModel.createTask(
                        title = title,
                        description = desc,
                        priority = priority,
                        category = category,
                        status = status,
                        dueDateMillis = dueDateMillis,
                        onDone = { showTaskEditor = false }
                    )
                } else {
                    dashboardViewModel.updateTask(
                        taskId = current.id,
                        title = title,
                        description = desc,
                        priority = priority,
                        category = category,
                        status = status,
                        dueDateMillis = dueDateMillis,
                        onDone = {
                            showTaskEditor = false
                            editingTask = null
                        }
                    )
                }
            }
        )
    }

    val pendingDelete = taskToDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text(stringResource(R.string.delete_task_title)) },
            text = {
                Text(
                    text = "Delete \"${pendingDelete.title}\"? ${stringResource(R.string.delete_task_message)}"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        dashboardViewModel.deleteTask(pendingDelete.id)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(stringResource(R.string.confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAiSheet) {
        val currentTasks = (dashboardState as? UiState.Success)?.data?.allTasks.orEmpty()
        AiAssistantSheet(
            uiState = aiUiState,
            activeTaskCount = currentTasks.size,
            onDismiss = { showAiSheet = false },
            onToggleSearchGrounding = aiViewModel::toggleSearchGrounding,
            onToggleHighThinking = aiViewModel::toggleHighThinking,
            onSendMessage = { prompt ->
                aiViewModel.sendMessage(prompt, currentTasks)
            },
            onClearConversation = aiViewModel::clearConversation,
            onAddSuggestedTask = { suggested ->
                dashboardViewModel.createTask(
                    title = suggested.title,
                    description = suggested.description,
                    priority = suggested.priority,
                    category = suggested.category,
                    status = TaskStatus.PENDING,
                    dueDateMillis = null
                )
            }
        )
    }

    if (showSettingsSheet) {
        SettingsSheet(
            userEmail = currentUser.email,
            userName = currentUser.displayName,
            currentThemeMode = themeMode,
            onSelectThemeMode = onThemeModeChange,
            onSignOut = {
                showSettingsSheet = false
                signOut(
                    credentialManager = credentialManager,
                    scope = scope
                )
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}
