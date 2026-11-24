package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.categoryEnum
import com.example.data.model.priorityEnum
import com.example.data.model.statusEnum
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    editingTask: TaskItem?,
    isSaving: Boolean,
    isBreakingDownWithAi: Boolean,
    aiBreakdownError: String?,
    onDismiss: () -> Unit,
    onRequestAiBreakdown: (
        title: String,
        description: String,
        onApplied: (refinedDesc: String, priority: TaskPriority, category: TaskCategory) -> Unit
    ) -> Unit,
    onSave: (
        title: String,
        description: String,
        priority: TaskPriority,
        category: TaskCategory,
        status: TaskStatus,
        dueDateMillis: Long?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(editingTask) { mutableStateOf(editingTask?.title ?: "") }
    var description by remember(editingTask) { mutableStateOf(editingTask?.description ?: "") }
    var priority by remember(editingTask) { mutableStateOf(editingTask?.priorityEnum() ?: TaskPriority.MEDIUM) }
    var category by remember(editingTask) { mutableStateOf(editingTask?.categoryEnum() ?: TaskCategory.PERSONAL) }
    var status by remember(editingTask) { mutableStateOf(editingTask?.statusEnum() ?: TaskStatus.PENDING) }
    var dueDateMillis by remember(editingTask) { mutableStateOf(editingTask?.dueDateMillis) }
    var titleError by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTask == null) stringResource(R.string.add_task) else stringResource(R.string.edit_task),
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close editor")
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it.length <= 120) {
                        title = it
                        titleError = null
                    }
                },
                label = { Text("Task Title *") },
                placeholder = { Text("e.g., Prepare sprint demo or study chapter 5") },
                isError = titleError != null,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(titleError ?: "")
                        Text("${title.length}/120")
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 1000) description = it },
                label = { Text("Description & Subtasks") },
                placeholder = { Text("Add context, links, or click Smart Breakdown below…") },
                minLines = 3,
                maxLines = 6,
                supportingText = {
                    Text("${description.length}/1000")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_description_input")
            )

            // AI Smart Breakdown Button
            OutlinedButton(
                onClick = {
                    if (title.trim().isEmpty()) {
                        titleError = "Enter a task title first"
                    } else {
                        onRequestAiBreakdown(title, description) { refinedDesc, suggestedPriority, suggestedCategory ->
                            description = refinedDesc
                            priority = suggestedPriority
                            category = suggestedCategory
                        }
                    }
                },
                enabled = !isBreakingDownWithAi && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_breakdown_button")
            ) {
                if (isBreakingDownWithAi) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating AI breakdown…")
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.ai_breakdown_action))
                }
            }

            if (!aiBreakdownError.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aiBreakdownError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Due Date & Time Row
            Text("Due Date & Time", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateLabel = dueDateMillis?.let {
                    SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(it))
                } ?: "Set Date"
                val timeLabel = dueDateMillis?.let {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it))
                } ?: "Set Time"

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_due_date_button")
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(dateLabel, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier
                        .weight(0.85f)
                        .testTag("task_due_time_button")
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(timeLabel, maxLines = 1)
                }

                if (dueDateMillis != null) {
                    IconButton(onClick = { dueDateMillis = null }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear due date")
                    }
                }
            }

            // Priority Chips
            Text("Priority", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.entries.forEach { item ->
                    val selected = priority == item
                    FilterChip(
                        selected = selected,
                        onClick = { priority = item },
                        label = { Text(item.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Category Chips
            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskCategory.entries.forEach { item ->
                    val selected = category == item
                    FilterChip(
                        selected = selected,
                        onClick = { category = item },
                        label = { Text(item.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Status Chips
            Text("Status", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskStatus.entries.forEach { item ->
                    val selected = status == item
                    FilterChip(
                        selected = selected,
                        onClick = { status = item },
                        label = { Text(item.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    if (trimmedTitle.isEmpty()) {
                        titleError = "Task title is required"
                    } else {
                        onSave(trimmedTitle, description.trim(), priority, category, status, dueDateMillis)
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_button"),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Saving…")
                } else {
                    Text(
                        text = if (editingTask == null) stringResource(R.string.create_task) else stringResource(R.string.save_task),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val initialUtcMillis = remember(dueDateMillis) {
            val base = dueDateMillis ?: System.currentTimeMillis()
            val localCal = Calendar.getInstance().apply { timeInMillis = base }
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(
                    localCal.get(Calendar.YEAR),
                    localCal.get(Calendar.MONTH),
                    localCal.get(Calendar.DAY_OF_MONTH),
                    0, 0, 0
                )
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedUtc = datePickerState.selectedDateMillis
                        if (selectedUtc != null) {
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = selectedUtc
                            }
                            val targetCal = Calendar.getInstance().apply {
                                timeInMillis = dueDateMillis ?: System.currentTimeMillis()
                                if (dueDateMillis == null) {
                                    set(Calendar.HOUR_OF_DAY, 17)
                                    set(Calendar.MINUTE, 0)
                                }
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            dueDateMillis = targetCal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = remember(dueDateMillis) {
            Calendar.getInstance().apply {
                timeInMillis = dueDateMillis ?: System.currentTimeMillis()
            }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Due Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = Calendar.getInstance().apply {
                            timeInMillis = dueDateMillis ?: System.currentTimeMillis()
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        dueDateMillis = updated.timeInMillis
                        showTimePicker = false
                    }
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
