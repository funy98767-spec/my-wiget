package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.CountdownEntity
import com.example.data.db.HabitEntity
import com.example.data.db.NewsArticleEntity
import com.example.data.db.NoteEntity
import com.example.data.db.RssSourceEntity
import com.example.data.db.TodoEntity
import com.example.data.remote.GeocodingResultDto
import com.example.data.remote.WeatherService
import com.example.domain.model.CountdownCategory
import com.example.domain.model.StickyNoteColor
import com.example.domain.model.WeatherSnapshot
import com.example.ui.theme.NoteBlueBg
import com.example.ui.theme.NoteBlueInk
import com.example.ui.theme.NoteGreenBg
import com.example.ui.theme.NoteGreenInk
import com.example.ui.theme.NotePinkBg
import com.example.ui.theme.NotePinkInk
import com.example.ui.theme.NoteYellowBg
import com.example.ui.theme.NoteYellowInk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class DataHubSheetType {
    TODO, NOTE, COUNTDOWN, WEATHER, NEWS, HABIT_WATER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataHubModalSheet(
    sheetType: DataHubSheetType,
    todos: List<TodoEntity>,
    notes: List<NoteEntity>,
    countdowns: List<CountdownEntity>,
    habits: List<HabitEntity>,
    waterMlToday: Int,
    waterGoalMl: Int,
    quoteText: String,
    quoteAuthor: String,
    weather: WeatherSnapshot,
    cityResults: List<GeocodingResultDto>,
    rssSources: List<RssSourceEntity>,
    articles: List<NewsArticleEntity>,
    isSyncingRss: Boolean,
    onDismiss: () -> Unit,
    onAddTodo: (String, Int?, Boolean, Int) -> Unit,
    onUpdateTodo: (TodoEntity, String, Int?, Boolean, Int) -> Unit,
    onToggleTodo: (TodoEntity) -> Unit,
    onDeleteTodo: (Long) -> Unit,
    onAddNote: (String, String, String) -> Unit,
    onUpdateNoteColor: (NoteEntity, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onAddCountdown: (String, String, Int, Boolean, Long) -> Unit,
    onDeleteCountdown: (Long) -> Unit,
    onSearchCity: (String) -> Unit,
    onSelectCity: (String, Double, Double) -> Unit,
    onRefreshWeather: () -> Unit,
    onAddRssSource: (String, String, Int) -> Unit,
    onDeleteRssSource: (Long) -> Unit,
    onSyncRss: () -> Unit,
    onAddHabit: (String) -> Unit,
    onToggleHabit: (HabitEntity) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    onAddWaterMl: (Int) -> Unit,
    onUpdateQuote: (String, String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            when (sheetType) {
                DataHubSheetType.TODO -> TodoSheetContent(
                    todos = todos,
                    onAddTodo = onAddTodo,
                    onUpdateTodo = onUpdateTodo,
                    onToggleTodo = onToggleTodo,
                    onDeleteTodo = onDeleteTodo
                )
                DataHubSheetType.NOTE -> NoteSheetContent(
                    notes = notes,
                    onAddNote = onAddNote,
                    onUpdateNoteColor = onUpdateNoteColor,
                    onDeleteNote = onDeleteNote
                )
                DataHubSheetType.COUNTDOWN -> CountdownSheetContent(
                    countdowns = countdowns,
                    onAddCountdown = onAddCountdown,
                    onDeleteCountdown = onDeleteCountdown
                )
                DataHubSheetType.WEATHER -> WeatherSheetContent(
                    weather = weather,
                    cityResults = cityResults,
                    onSearchCity = onSearchCity,
                    onSelectCity = onSelectCity,
                    onRefreshWeather = onRefreshWeather
                )
                DataHubSheetType.NEWS -> NewsSheetContent(
                    sources = rssSources,
                    articles = articles,
                    isSyncing = isSyncingRss,
                    onAddSource = onAddRssSource,
                    onDeleteSource = onDeleteRssSource,
                    onSyncRss = onSyncRss
                )
                DataHubSheetType.HABIT_WATER -> HabitWaterQuoteSheetContent(
                    habits = habits,
                    waterMlToday = waterMlToday,
                    waterGoalMl = waterGoalMl,
                    quoteText = quoteText,
                    quoteAuthor = quoteAuthor,
                    onAddHabit = onAddHabit,
                    onToggleHabit = onToggleHabit,
                    onDeleteHabit = onDeleteHabit,
                    onAddWaterMl = onAddWaterMl,
                    onUpdateQuote = onUpdateQuote
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodoSheetContent(
    todos: List<TodoEntity>,
    onAddTodo: (String, Int?, Boolean, Int) -> Unit,
    onUpdateTodo: (TodoEntity, String, Int?, Boolean, Int) -> Unit,
    onToggleTodo: (TodoEntity) -> Unit,
    onDeleteTodo: (Long) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var dueHours by remember { mutableIntStateOf(4) }
    var priority by remember { mutableIntStateOf(1) }
    var reminderOn by remember { mutableStateOf(true) }
    var editingTodo by remember { mutableStateOf<TodoEntity?>(null) }

    Text("待辦事項與到期提醒（點擊 ✎ 可修改）", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("輸入新任務...") },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("todo_input")
        )
        Button(
            onClick = {
                if (input.isNotBlank()) {
                    onAddTodo(input, dueHours, reminderOn, priority)
                    input = ""
                }
            },
            modifier = Modifier.testTag("add_todo_button")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "新增待辦")
        }
    }
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
        listOf(1 to "1小時後", 4 to "4小時後", 24 to "明天").forEach { (hrs, label) ->
            FilterChip(
                selected = dueHours == hrs,
                onClick = { dueHours = hrs },
                label = { Text(label) }
            )
        }
        listOf(0 to "低", 1 to "中", 2 to "高").forEach { (p, label) ->
            FilterChip(
                selected = priority == p,
                onClick = { priority = p },
                label = { Text("優先:$label") }
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("提醒", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(6.dp))
            Switch(checked = reminderOn, onCheckedChange = { reminderOn = it })
        }
    }
    Spacer(Modifier.height(10.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(todos, key = { it.id }) { item ->
            val priLabel = when (item.priority) {
                2 -> "高優先"
                0 -> "低"
                else -> "中"
            }
            val dueStr = item.dueAtMillis?.let {
                SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(it))
            } ?: "不限時間"

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleTodo(item) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = "切換完成狀態",
                        tint = if (item.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$priLabel · 到期 $dueStr" + if (item.reminderEnabled) " · 🔔提醒" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { editingTodo = item },
                        modifier = Modifier.testTag("edit_todo_${item.id}")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "修改待辦")
                    }
                    IconButton(onClick = { onDeleteTodo(item.id) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "刪除任務")
                    }
                }
            }
        }
    }

    editingTodo?.let { target ->
        EditTodoDialog(
            todo = target,
            onDismiss = { editingTodo = null },
            onSave = { newTitle, newDueHours, newReminder, newPriority ->
                onUpdateTodo(target, newTitle, newDueHours, newReminder, newPriority)
                editingTodo = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditTodoDialog(
    todo: TodoEntity,
    onDismiss: () -> Unit,
    onSave: (String, Int?, Boolean, Int) -> Unit
) {
    var title by remember(todo) { mutableStateOf(todo.title) }
    var priority by remember(todo) { mutableIntStateOf(todo.priority) }
    var reminderEnabled by remember(todo) { mutableStateOf(todo.reminderEnabled) }
    var dueHoursChoice by remember(todo) { mutableStateOf<Int?>(null) } // null = 保持原到期時間

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改待辦事項") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("任務內容") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_todo_title_input")
                )
                Text("優先順序", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "低", 1 to "中", 2 to "高").forEach { (p, label) ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(label) }
                        )
                    }
                }
                Text("重新設定到期時間", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = dueHoursChoice == null,
                        onClick = { dueHoursChoice = null },
                        label = { Text("保持原時間") }
                    )
                    listOf(1 to "1小時後", 4 to "4小時後", 24 to "明天").forEach { (hrs, label) ->
                        FilterChip(
                            selected = dueHoursChoice == hrs,
                            onClick = { dueHoursChoice = hrs },
                            label = { Text(label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("到期通知提醒")
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, dueHoursChoice, reminderEnabled, priority)
                    }
                },
                modifier = Modifier.testTag("save_edited_todo_button")
            ) {
                Text("儲存更改")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun HabitWaterQuoteSheetContent(
    habits: List<HabitEntity>,
    waterMlToday: Int,
    waterGoalMl: Int,
    quoteText: String,
    quoteAuthor: String,
    onAddHabit: (String) -> Unit,
    onToggleHabit: (HabitEntity) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    onAddWaterMl: (Int) -> Unit,
    onUpdateQuote: (String, String) -> Unit
) {
    var newHabit by remember { mutableStateOf("") }
    var editQuote by remember(quoteText) { mutableStateOf(quoteText) }
    var editAuthor by remember(quoteAuthor) { mutableStateOf(quoteAuthor) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("喝水追蹤、習慣打卡與語錄", style = MaterialTheme.typography.titleLarge)
        }

        // Water Control
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocalDrink, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("今日喝水：${waterMlToday}ml / ${waterGoalMl}ml", fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { (waterMlToday.toFloat() / waterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onAddWaterMl(250) }) { Text("+250ml") }
                        FilledTonalButton(onClick = { onAddWaterMl(500) }) { Text("+500ml") }
                        OutlinedButton(onClick = { onAddWaterMl(-250) }) { Text("-250ml") }
                    }
                }
            }
        }

        // Habits
        item {
            Text("每日習慣打卡", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newHabit,
                    onValueChange = { newHabit = it },
                    label = { Text("新增每日習慣...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    if (newHabit.isNotBlank()) {
                        onAddHabit(newHabit)
                        newHabit = ""
                    }
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = "新增習慣")
                }
            }
        }

        items(habits, key = { it.id }) { habit ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleHabit(habit) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (habit.checkedToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (habit.checkedToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(habit.title, modifier = Modifier.weight(1f))
                    Text("🔥 ${habit.streakDays} 天", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onDeleteHabit(habit.id) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "刪除習慣")
                    }
                }
            }
        }

        // Daily Quote Editor
        item {
            Text("自訂每日語錄 Widget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = editQuote,
                onValueChange = { editQuote = it },
                label = { Text("語錄內容") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = editAuthor,
                    onValueChange = { editAuthor = it },
                    label = { Text("作者 / 署名") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { onUpdateQuote(editQuote, editAuthor) }) {
                    Text("套用語錄")
                }
            }
        }
    }
}

@Composable
private fun NoteSheetContent(
    notes: List<NoteEntity>,
    onAddNote: (String, String, String) -> Unit,
    onUpdateNoteColor: (NoteEntity, String) -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(StickyNoteColor.YELLOW) }

    Text("桌面便利貼（黃 / 藍 / 粉 / 綠）", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("標題（選填）") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = content,
        onValueChange = { content = it },
        label = { Text("便籤內容...") },
        minLines = 2,
        maxLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StickyNoteColor.entries.forEach { c ->
                val bg = noteBgColor(c.name)
                Surface(
                    shape = CircleShape,
                    color = bg,
                    border = if (selectedColor == c) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { selectedColor = c }
                ) {}
            }
        }
        Button(
            onClick = {
                if (content.isNotBlank() || title.isNotBlank()) {
                    onAddNote(title, content, selectedColor.name)
                    title = ""
                    content = ""
                }
            },
            modifier = Modifier.testTag("add_note_button")
        ) {
            Text("釘選便籤")
        }
    }
    Spacer(Modifier.height(10.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(notes, key = { it.id }) { note ->
            val bg = noteBgColor(note.colorKey)
            val ink = noteInkColor(note.colorKey)
            Surface(shape = RoundedCornerShape(16.dp), color = bg) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(note.title, color = ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        StickyNoteColor.entries.forEach { c ->
                            Surface(
                                shape = CircleShape,
                                color = noteBgColor(c.name),
                                border = BorderStroke(1.dp, ink.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(18.dp)
                                    .clickable { onUpdateNoteColor(note, c.name) }
                            ) {}
                        }
                        IconButton(onClick = { onDeleteNote(note.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "刪除便籤", tint = ink)
                        }
                    }
                    Text(note.content, color = ink.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountdownSheetContent(
    countdowns: List<CountdownEntity>,
    onAddCountdown: (String, String, Int, Boolean, Long) -> Unit,
    onDeleteCountdown: (Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var daysStr by remember { mutableStateOf("30") }
    var category by remember { mutableStateOf(CountdownCategory.EXAM) }
    var reminder by remember { mutableStateOf(true) }

    Text("倒數日管理（支援多筆）", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("事件名稱（如高考/旅行）") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = daysStr,
            onValueChange = { daysStr = it.filter { ch -> ch.isDigit() }.take(4) },
            label = { Text("天數") },
            singleLine = true,
            modifier = Modifier.width(84.dp)
        )
    }
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CountdownCategory.entries.forEach { cat ->
            FilterChip(
                selected = category == cat,
                onClick = { category = cat },
                label = { Text(cat.labelZh) }
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("到期前提醒", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(8.dp))
            Switch(checked = reminder, onCheckedChange = { reminder = it })
        }
        Button(onClick = {
            val days = daysStr.toIntOrNull() ?: 10
            if (title.isNotBlank()) {
                val color = when (category) {
                    CountdownCategory.EXAM -> 0xFFF43F5EL
                    CountdownCategory.BIRTHDAY -> 0xFFF59E0BL
                    CountdownCategory.TRAVEL -> 0xFF06B6D4L
                    CountdownCategory.CUSTOM -> 0xFF818CF8L
                }
                onAddCountdown(title, category.name, days, reminder, color)
                title = ""
            }
        }) {
            Text("新增倒數日")
        }
    }
    Spacer(Modifier.height(10.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(countdowns, key = { it.id }) { item ->
            val days = TimeUnit.MILLISECONDS.toDays(item.targetDateMillis - System.currentTimeMillis()).coerceAtLeast(0)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${item.category} · 還有 $days 天",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(item.accentHex)
                        )
                    }
                    IconButton(onClick = { onDeleteCountdown(item.id) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "刪除倒數日")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeatherSheetContent(
    weather: WeatherSnapshot,
    cityResults: List<GeocodingResultDto>,
    onSearchCity: (String) -> Unit,
    onSelectCity: (String, Double, Double) -> Unit,
    onRefreshWeather: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("即時天氣設定", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${weather.cityName} ${weather.temperatureC.toInt()}°C · ${weather.conditionText} · 降雨機率 ${weather.precipitationProb}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onRefreshWeather) {
            Icon(Icons.Rounded.Refresh, contentDescription = "重新整理天氣")
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜尋全球城市 (Open-Meteo)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        FilledTonalButton(onClick = { onSearchCity(query) }) {
            Icon(Icons.Rounded.Search, contentDescription = "搜尋城市")
        }
    }
    if (cityResults.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        cityResults.forEach { res ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { onSelectCity(res.name, res.latitude, res.longitude) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Text(
                    text = "${res.name} (${res.country.orEmpty()})",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Text("快速切換熱門城市", style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WeatherService.presetCities.forEach { (name, lat, lon) ->
            FilterChip(
                selected = weather.cityName == name,
                onClick = { onSelectCity(name, lat, lon) },
                label = { Text(name) }
            )
        }
    }
}

@Composable
private fun NewsSheetContent(
    sources: List<RssSourceEntity>,
    articles: List<NewsArticleEntity>,
    isSyncing: Boolean,
    onAddSource: (String, String, Int) -> Unit,
    onDeleteSource: (Long) -> Unit,
    onSyncRss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("RSS 新聞來源與頭條", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        FilledTonalButton(onClick = onSyncRss) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("立即更新")
        }
    }
    if (isSyncing) {
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("來源名稱") },
            singleLine = true,
            modifier = Modifier.weight(0.4f)
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("RSS Feed URL") },
            singleLine = true,
            modifier = Modifier.weight(0.6f)
        )
        Button(onClick = {
            if (url.isNotBlank()) {
                onAddSource(name, url, 30)
                name = ""
                url = ""
            }
        }) {
            Icon(Icons.Rounded.Add, contentDescription = "新增 RSS")
        }
    }
    Spacer(Modifier.height(8.dp))
    sources.forEach { src ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "• ${src.name} (${src.autoUpdateMinutes}m)",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onDeleteSource(src.id) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "移除來源", modifier = Modifier.size(16.dp))
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(articles, key = { it.id }) { art ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.clickable {
                    if (art.link.startsWith("http")) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(art.link)))
                        }
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(art.sourceName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(art.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(art.summary, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (art.link.startsWith("http")) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = "開啟連結", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun noteBgColor(key: String): Color = when (key) {
    "BLUE" -> NoteBlueBg
    "PINK" -> NotePinkBg
    "GREEN" -> NoteGreenBg
    else -> NoteYellowBg
}

private fun noteInkColor(key: String): Color = when (key) {
    "BLUE" -> NoteBlueInk
    "PINK" -> NotePinkInk
    "GREEN" -> NoteGreenInk
    else -> NoteYellowInk
}
