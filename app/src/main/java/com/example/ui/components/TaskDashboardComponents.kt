package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.TaskSortOrder
import com.example.data.model.TaskStatistics
import com.example.data.model.TaskStatus
import com.example.data.model.TaskViewTab
import com.example.data.model.categoryEnum
import com.example.data.model.isTaskOverdue
import com.example.data.model.priorityEnum
import com.example.data.model.statusEnum
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningContainerDark
import com.example.ui.theme.AmberWarningContainerLight
import com.example.ui.theme.BlueInfo
import com.example.ui.theme.BlueInfoContainerDark
import com.example.ui.theme.BlueInfoContainerLight
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainerDark
import com.example.ui.theme.EmeraldSuccessContainerLight
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.DashboardFilterState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsOverviewGrid(
    stats: TaskStatistics,
    isWideScreen: Boolean,
    onSelectTab: (TaskViewTab) -> Unit
) {
    val cards = listOf(
        StatCardMeta(
            label = "Total Tasks",
            count = stats.total,
            icon = Icons.Rounded.TaskAlt,
            accent = MaterialTheme.colorScheme.primary,
            container = MaterialTheme.colorScheme.primaryContainer,
            targetTab = TaskViewTab.ALL,
            tag = "stat_card_total"
        ),
        StatCardMeta(
            label = "Pending",
            count = stats.pending,
            icon = Icons.Rounded.PendingActions,
            accent = AmberWarning,
            container = if (isSystemInDarkTheme()) AmberWarningContainerDark else AmberWarningContainerLight,
            targetTab = TaskViewTab.PENDING,
            tag = "stat_card_pending"
        ),
        StatCardMeta(
            label = "In Progress",
            count = stats.inProgress,
            icon = Icons.Rounded.HourglassTop,
            accent = BlueInfo,
            container = if (isSystemInDarkTheme()) BlueInfoContainerDark else BlueInfoContainerLight,
            targetTab = TaskViewTab.IN_PROGRESS,
            tag = "stat_card_in_progress"
        ),
        StatCardMeta(
            label = "Completed",
            count = stats.completed,
            icon = Icons.Rounded.CheckCircle,
            accent = EmeraldSuccess,
            container = if (isSystemInDarkTheme()) EmeraldSuccessContainerDark else EmeraldSuccessContainerLight,
            targetTab = TaskViewTab.COMPLETED,
            tag = "stat_card_completed"
        )
    )

    if (isWideScreen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            cards.forEach { card ->
                StatCardItem(
                    meta = card,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTab(card.targetTab) }
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCardItem(
                    meta = cards[0],
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTab(cards[0].targetTab) }
                )
                StatCardItem(
                    meta = cards[1],
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTab(cards[1].targetTab) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCardItem(
                    meta = cards[2],
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTab(cards[2].targetTab) }
                )
                StatCardItem(
                    meta = cards[3],
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTab(cards[3].targetTab) }
                )
            }
        }
    }
}

private data class StatCardMeta(
    val label: String,
    val count: Int,
    val icon: ImageVector,
    val accent: Color,
    val container: Color,
    val targetTab: TaskViewTab,
    val tag: String
)

@Composable
private fun StatCardItem(
    meta: StatCardMeta,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(meta.tag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = meta.count.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = meta.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                modifier = Modifier.size(36.dp),
                color = meta.container,
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = meta.icon,
                        contentDescription = meta.label,
                        tint = meta.accent,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchAndFilterControls(
    filterState: DashboardFilterState,
    onSearchChange: (String) -> Unit,
    onSelectTab: (TaskViewTab) -> Unit,
    onTogglePriority: (TaskPriority) -> Unit,
    onToggleCategory: (TaskCategory) -> Unit,
    onToggleStatus: (TaskStatus) -> Unit,
    onSelectSortOrder: (TaskSortOrder) -> Unit,
    onClearFilters: () -> Unit
) {
    var filtersExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val activeFilterCount = listOfNotNull(
        filterState.priorityFilter,
        filterState.categoryFilter,
        filterState.statusFilter
    ).size

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Horizontal View Tabs (All, My Day, Upcoming, Pending, In Progress, Completed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskViewTab.entries.forEach { tab ->
                val selected = filterState.selectedTab == tab
                FilterChip(
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    modifier = Modifier.testTag("view_tab_${tab.name.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Search + Filter Toggle + Sort Dropdown Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search tasks",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("task_search_input")
            )

            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(MaterialTheme.shapes.medium)
                    .border(
                        width = 1.dp,
                        color = if (filtersExpanded || activeFilterCount > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = MaterialTheme.shapes.medium
                    )
                    .clickable { filtersExpanded = !filtersExpanded }
                    .testTag("toggle_filters_button"),
                color = if (filtersExpanded || activeFilterCount > 0) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Filter tasks",
                        tint = if (filtersExpanded || activeFilterCount > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    if (activeFilterCount > 0) {
                        Text(
                            text = activeFilterCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Box {
                Surface(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(MaterialTheme.shapes.medium)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable { sortMenuExpanded = true }
                        .testTag("sort_tasks_button"),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "Sort tasks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    TaskSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = order.label,
                                    fontWeight = if (filterState.sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                    color = if (filterState.sortOrder == order) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            onClick = {
                                onSelectSortOrder(order)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Expandable Priority / Category / Status Filter Chips
        AnimatedVisibility(visible = filtersExpanded) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter by attributes",
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (activeFilterCount > 0) {
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onClearFilters)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text("Priority", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskPriority.entries.forEach { p ->
                            FilterChip(
                                selected = filterState.priorityFilter == p,
                                onClick = { onTogglePriority(p) },
                                label = { Text(p.label) }
                            )
                        }
                    }

                    Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskCategory.entries.forEach { c ->
                            FilterChip(
                                selected = filterState.categoryFilter == c,
                                onClick = { onToggleCategory(c) },
                                label = { Text(c.label) }
                            )
                        }
                    }

                    Text("Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskStatus.entries.forEach { s ->
                            FilterChip(
                                selected = filterState.statusFilter == s,
                                onClick = { onToggleStatus(s) },
                                label = { Text(s.label) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCard(
    task: TaskItem,
    onToggleComplete: () -> Unit,
    onChangeStatus: (TaskStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val status = task.statusEnum()
    val priority = task.priorityEnum()
    val category = task.categoryEnum()
    val isCompleted = status == TaskStatus.COMPLETED
    val isOverdue = task.isTaskOverdue()

    var statusMenuExpanded by remember { mutableStateOf(false) }

    val priorityColor = when (priority) {
        TaskPriority.HIGH -> MaterialTheme.colorScheme.primary
        TaskPriority.MEDIUM -> AmberWarning
        TaskPriority.LOW -> EmeraldSuccess
    }

    val statusBg = when (status) {
        TaskStatus.COMPLETED -> if (isSystemInDarkTheme()) EmeraldSuccessContainerDark else EmeraldSuccessContainerLight
        TaskStatus.IN_PROGRESS -> if (isSystemInDarkTheme()) BlueInfoContainerDark else BlueInfoContainerLight
        TaskStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusFg = when (status) {
        TaskStatus.COMPLETED -> EmeraldSuccess
        TaskStatus.IN_PROGRESS -> BlueInfo
        TaskStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onEdit)
            .testTag("task_card_${task.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isOverdue) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    shape = MaterialTheme.shapes.medium
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("task_checkbox_${task.id}")
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (isCompleted) "Mark pending" else "Mark completed",
                        tint = if (isCompleted) EmeraldSuccess else priorityColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_task_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit task",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_task_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete task",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.padding(start = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status selector pill
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { statusMenuExpanded = true },
                        color = statusBg
                    ) {
                        Text(
                            text = status.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        TaskStatus.entries.forEach { targetStatus ->
                            DropdownMenuItem(
                                text = { Text(targetStatus.label) },
                                onClick = {
                                    onChangeStatus(targetStatus)
                                    statusMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Priority pill
                Surface(
                    color = priorityColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                        Text(
                            text = priority.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = priorityColor
                        )
                    }
                }

                // Category pill
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Due date & Overdue pill
                val dueMillis = task.dueDateMillis
                if (dueMillis != null) {
                    val formatted = remember(dueMillis) {
                        SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()).format(Date(dueMillis))
                    }
                    Surface(
                        color = if (isOverdue) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isOverdue) Icons.Rounded.WarningAmber else Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = if (isOverdue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isOverdue) "Overdue • $formatted" else formatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTasksState(
    hasActiveFilter: Boolean,
    onCreateTask: () -> Unit,
    onOpenAi: () -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_tasks),
                contentDescription = "No tasks found",
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(170.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (hasActiveFilter) "No matching tasks" else stringResource(R.string.empty_tasks_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasActiveFilter) {
                    "Try clearing your search or attribute filters to see all tasks."
                } else {
                    stringResource(R.string.empty_tasks_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasActiveFilter) {
                    OutlinedButton(onClick = onClearFilters) {
                        Text("Clear Filters")
                    }
                } else {
                    OutlinedButton(onClick = onOpenAi) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Plan with AI")
                    }
                }
                Button(onClick = onCreateTask) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.add_task))
                }
            }
        }
    }
}
