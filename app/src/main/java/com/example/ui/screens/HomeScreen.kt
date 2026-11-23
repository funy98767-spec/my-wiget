package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.BlockJsonCodec
import com.example.data.db.CountdownEntity
import com.example.data.db.HabitEntity
import com.example.data.db.NewsArticleEntity
import com.example.data.db.NoteEntity
import com.example.data.db.TodoEntity
import com.example.data.db.WidgetEntity
import com.example.data.device.SystemStatsSnapshot
import com.example.domain.model.BackgroundPreset
import com.example.domain.model.BatterySnapshot
import com.example.domain.model.ClockDisplayStyle
import com.example.domain.model.StepSnapshot
import com.example.domain.model.WeatherSnapshot
import com.example.domain.model.WidgetFontStyle
import com.example.domain.model.WidgetThemeStyle
import com.example.extension.HWidgetPluginRegistry
import com.example.ui.components.DataHubSheetType
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.HWidgetLivePreview
import com.example.ui.components.WidgetVisualSpec

private enum class HomeWidgetFilter {
    ALL, READY_MADE, CUSTOM
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    widgets: List<WidgetEntity>,
    clockMillis: Long,
    weather: WeatherSnapshot,
    battery: BatterySnapshot,
    steps: StepSnapshot,
    todos: List<TodoEntity>,
    notes: List<NoteEntity>,
    countdowns: List<CountdownEntity>,
    articles: List<NewsArticleEntity>,
    habits: List<HabitEntity>,
    systemStats: SystemStatsSnapshot,
    pomodoroSeconds: Int,
    pomodoroRunning: Boolean,
    waterMlToday: Int,
    waterGoalMl: Int,
    quoteText: String,
    quoteAuthor: String,
    stopwatchSeconds: Int,
    stopwatchRunning: Boolean,
    counterValue: Int,
    moodTag: String,
    mountedExtensions: Set<String>,
    onCreateNewWidget: () -> Unit,
    onEditWidget: (WidgetEntity) -> Unit,
    onCloneAsCustom: (WidgetEntity) -> Unit,
    onPinWidget: (WidgetEntity) -> Unit,
    onDeleteWidget: (Long) -> Unit,
    onOpenDataHub: (DataHubSheetType) -> Unit,
    onOpenStore: () -> Unit,
    onToggleTodo: (TodoEntity) -> Unit,
    onTogglePomodoro: () -> Unit,
    onResetPomodoro: () -> Unit,
    onAddWaterMl: (Int) -> Unit,
    onToggleHabit: (HabitEntity) -> Unit,
    onQuickAction: (String) -> Unit,
    onToggleStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    onIncrementCounter: (Int) -> Unit,
    onResetCounter: () -> Unit,
    onSelectMood: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(HomeWidgetFilter.ALL) }
    val readyMadeWidgets = remember(widgets) { widgets.filter { !it.isCustom } }
    val customWidgets = remember(widgets) { widgets.filter { it.isCustom } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Hero Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HWidget",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "成品 ${readyMadeWidgets.size} 個 · 自訂 ${customWidgets.size} 個",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onCreateNewWidget,
                    modifier = Modifier.testTag("home_create_widget_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("自訂建立")
                }
            }
        }

        // Live Device & Cloud Telemetry Bar
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    TelemetryPill(
                        icon = Icons.Rounded.WbSunny,
                        label = "${weather.cityName} ${weather.temperatureC.toInt()}°C",
                        sub = "降雨 ${weather.precipitationProb}%",
                        onClick = { onOpenDataHub(DataHubSheetType.WEATHER) }
                    )
                }
                item {
                    TelemetryPill(
                        icon = if (battery.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                        label = "電量 ${battery.levelPercent}%",
                        sub = if (battery.isCharging) "充電中" else "${battery.temperatureC}°C",
                        onClick = {}
                    )
                }
                item {
                    TelemetryPill(
                        icon = Icons.Rounded.DirectionsWalk,
                        label = "${steps.stepsToday} 步",
                        sub = "目標 ${steps.dailyGoal}",
                        onClick = {}
                    )
                }
                item {
                    TelemetryPill(
                        icon = Icons.Rounded.Memory,
                        label = "RAM ${systemStats.ramUsagePercent}%",
                        sub = "可用空間 ${systemStats.freeStorageGb} GB",
                        onClick = {}
                    )
                }
                item {
                    val mins = pomodoroSeconds / 60
                    val secs = pomodoroSeconds % 60
                    TelemetryPill(
                        icon = Icons.Rounded.Timer,
                        label = String.format("%02d:%02d", mins, secs),
                        sub = if (pomodoroRunning) "專注計時中" else "番茄鐘就緒",
                        onClick = onTogglePomodoro
                    )
                }
            }
        }

        // Quick Data Hub Cards (Todo, Notes, Countdowns, RSS News)
        item {
            Text(
                text = "小工具即時資料中心",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    QuickHubTile(
                        icon = Icons.Rounded.Checklist,
                        title = "待辦 (${todos.count { !it.isCompleted }})",
                        subtitle = todos.firstOrNull { !it.isCompleted }?.title ?: "全部完成",
                        onClick = { onOpenDataHub(DataHubSheetType.TODO) },
                        testTag = "hub_todo_card"
                    )
                }
                item {
                    QuickHubTile(
                        icon = Icons.Rounded.StickyNote2,
                        title = "便利貼 (${notes.size})",
                        subtitle = notes.firstOrNull()?.title ?: "新增筆記",
                        onClick = { onOpenDataHub(DataHubSheetType.NOTE) },
                        testTag = "hub_note_card"
                    )
                }
                item {
                    QuickHubTile(
                        icon = Icons.Rounded.Event,
                        title = "倒數日 (${countdowns.size})",
                        subtitle = countdowns.firstOrNull()?.title ?: "設定倒數",
                        onClick = { onOpenDataHub(DataHubSheetType.COUNTDOWN) },
                        testTag = "hub_countdown_card"
                    )
                }
                item {
                    QuickHubTile(
                        icon = Icons.Rounded.Newspaper,
                        title = "RSS 新聞 (${articles.size})",
                        subtitle = articles.firstOrNull()?.title ?: "同步訂閱源",
                        onClick = { onOpenDataHub(DataHubSheetType.NEWS) },
                        testTag = "hub_news_card"
                    )
                }
                item {
                    QuickHubTile(
                        icon = Icons.Rounded.LocalDrink,
                        title = "喝水·習慣·語錄",
                        subtitle = "${waterMlToday}/${waterGoalMl}ml · ${habits.count { it.checkedToday }}/${habits.size} 習慣",
                        onClick = { onOpenDataHub(DataHubSheetType.HABIT_WATER) },
                        testTag = "hub_habit_water_card"
                    )
                }
            }
        }

        // Ready-Made vs Custom Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "我的桌面 Widget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    AssistChip(
                        onClick = onOpenStore,
                        label = { Text("商店找更多") },
                        leadingIcon = { Icon(Icons.Rounded.Widgets, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filter == HomeWidgetFilter.ALL,
                        onClick = { filter = HomeWidgetFilter.ALL },
                        label = { Text("全部 (${widgets.size})") },
                        modifier = Modifier.testTag("filter_all")
                    )
                    FilterChip(
                        selected = filter == HomeWidgetFilter.READY_MADE,
                        onClick = { filter = HomeWidgetFilter.READY_MADE },
                        label = { Text("已經做好的 (${readyMadeWidgets.size})") },
                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("filter_ready_made")
                    )
                    FilterChip(
                        selected = filter == HomeWidgetFilter.CUSTOM,
                        onClick = { filter = HomeWidgetFilter.CUSTOM },
                        label = { Text("自訂的 (${customWidgets.size})") },
                        leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("filter_custom")
                    )
                }
            }
        }

        // Custom Widgets Section
        if (filter == HomeWidgetFilter.ALL || filter == HomeWidgetFilter.CUSTOM) {
            item {
                SectionHeaderRow(
                    icon = Icons.Rounded.Tune,
                    title = "自訂 Widget",
                    subtitle = "你在編輯器自由拖放與微調的專屬元件",
                    actionLabel = "+ 新增自訂",
                    onAction = onCreateNewWidget
                )
            }
            if (customWidgets.isEmpty()) {
                item {
                    EmptyCategoryCard(
                        title = "還沒有自訂 Widget",
                        description = "使用可視化拖放編輯器，自由組合時間、電量、天氣與照片，打造獨一無二的小工具。",
                        buttonLabel = "開始建立自訂 Widget",
                        onClick = onCreateNewWidget
                    )
                }
            } else {
                items(customWidgets, key = { "custom_${it.id}" }) { widget ->
                    InstalledWidgetCard(
                        widget = widget,
                        clockMillis = clockMillis,
                        weather = weather,
                        battery = battery,
                        steps = steps,
                        todos = todos,
                        notes = notes,
                        countdowns = countdowns,
                        articles = articles,
                        habits = habits,
                        systemStats = systemStats,
                        pomodoroSeconds = pomodoroSeconds,
                        pomodoroRunning = pomodoroRunning,
                        waterMlToday = waterMlToday,
                        waterGoalMl = waterGoalMl,
                        quoteText = quoteText,
                        quoteAuthor = quoteAuthor,
                        stopwatchSeconds = stopwatchSeconds,
                        stopwatchRunning = stopwatchRunning,
                        counterValue = counterValue,
                        moodTag = moodTag,
                        mountedExtensions = mountedExtensions,
                        onPin = { onPinWidget(widget) },
                        onEdit = { onEditWidget(widget) },
                        onCloneAsCustom = { onCloneAsCustom(widget) },
                        onDelete = { onDeleteWidget(widget.id) },
                        onToggleTodo = onToggleTodo,
                        onEditTodosClick = { onOpenDataHub(DataHubSheetType.TODO) },
                        onTogglePomodoro = onTogglePomodoro,
                        onResetPomodoro = onResetPomodoro,
                        onAddWaterMl = onAddWaterMl,
                        onToggleHabit = onToggleHabit,
                        onQuickAction = onQuickAction,
                        onToggleStopwatch = onToggleStopwatch,
                        onResetStopwatch = onResetStopwatch,
                        onIncrementCounter = onIncrementCounter,
                        onResetCounter = onResetCounter,
                        onSelectMood = onSelectMood
                    )
                }
            }
        }

        // Ready-Made Widgets Section
        if (filter == HomeWidgetFilter.ALL || filter == HomeWidgetFilter.READY_MADE) {
            item {
                SectionHeaderRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "已經做好的 Widget",
                    subtitle = "官方預設與商店下載的成品，可直接釘選或複製成自訂版",
                    actionLabel = "逛商店",
                    onAction = onOpenStore
                )
            }
            if (readyMadeWidgets.isEmpty()) {
                item {
                    EmptyCategoryCard(
                        title = "尚未安裝已經做好的 Widget",
                        description = "前往 Widget Store 一鍵下載官方或第三方創作者做好的精美小工具。",
                        buttonLabel = "前往 Widget Store",
                        onClick = onOpenStore
                    )
                }
            } else {
                items(readyMadeWidgets, key = { "ready_${it.id}" }) { widget ->
                    InstalledWidgetCard(
                        widget = widget,
                        clockMillis = clockMillis,
                        weather = weather,
                        battery = battery,
                        steps = steps,
                        todos = todos,
                        notes = notes,
                        countdowns = countdowns,
                        articles = articles,
                        habits = habits,
                        systemStats = systemStats,
                        pomodoroSeconds = pomodoroSeconds,
                        pomodoroRunning = pomodoroRunning,
                        waterMlToday = waterMlToday,
                        waterGoalMl = waterGoalMl,
                        quoteText = quoteText,
                        quoteAuthor = quoteAuthor,
                        stopwatchSeconds = stopwatchSeconds,
                        stopwatchRunning = stopwatchRunning,
                        counterValue = counterValue,
                        moodTag = moodTag,
                        mountedExtensions = mountedExtensions,
                        onPin = { onPinWidget(widget) },
                        onEdit = { onEditWidget(widget) },
                        onCloneAsCustom = { onCloneAsCustom(widget) },
                        onDelete = { onDeleteWidget(widget.id) },
                        onToggleTodo = onToggleTodo,
                        onEditTodosClick = { onOpenDataHub(DataHubSheetType.TODO) },
                        onTogglePomodoro = onTogglePomodoro,
                        onResetPomodoro = onResetPomodoro,
                        onAddWaterMl = onAddWaterMl,
                        onToggleHabit = onToggleHabit,
                        onQuickAction = onQuickAction,
                        onToggleStopwatch = onToggleStopwatch,
                        onResetStopwatch = onResetStopwatch,
                        onIncrementCounter = onIncrementCounter,
                        onResetCounter = onResetCounter,
                        onSelectMood = onSelectMood
                    )
                }
            }
        }

        // Future Expansion Slots Dock (HeChat, HTask, HNews, HAI)
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "預留擴充模組插槽 (H-Ecosystem)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(HWidgetPluginRegistry.allPlugins(), key = { it.slotId.key }) { plugin ->
                    val isMounted = plugin.slotId.key in mountedExtensions
                    val preview = plugin.createPreview(isMounted)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        border = BorderStroke(
                            1.dp,
                            if (isMounted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.width(210.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Extension,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = preview.slotId.displayName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = preview.statusBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMounted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = preview.slotId.categoryLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = preview.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionHeaderRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AssistChip(onClick = onAction, label = { Text(actionLabel) })
    }
}

@Composable
private fun EmptyCategoryCard(
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onClick) { Text(buttonLabel) }
        }
    }
}

@Composable
private fun InstalledWidgetCard(
    widget: WidgetEntity,
    clockMillis: Long,
    weather: WeatherSnapshot,
    battery: BatterySnapshot,
    steps: StepSnapshot,
    todos: List<TodoEntity>,
    notes: List<NoteEntity>,
    countdowns: List<CountdownEntity>,
    articles: List<NewsArticleEntity>,
    habits: List<HabitEntity>,
    systemStats: SystemStatsSnapshot,
    pomodoroSeconds: Int,
    pomodoroRunning: Boolean,
    waterMlToday: Int,
    waterGoalMl: Int,
    quoteText: String,
    quoteAuthor: String,
    stopwatchSeconds: Int,
    stopwatchRunning: Boolean,
    counterValue: Int,
    moodTag: String,
    mountedExtensions: Set<String>,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onCloneAsCustom: () -> Unit,
    onDelete: () -> Unit,
    onToggleTodo: (TodoEntity) -> Unit,
    onEditTodosClick: () -> Unit,
    onTogglePomodoro: () -> Unit,
    onResetPomodoro: () -> Unit,
    onAddWaterMl: (Int) -> Unit,
    onToggleHabit: (HabitEntity) -> Unit,
    onQuickAction: (String) -> Unit,
    onToggleStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    onIncrementCounter: (Int) -> Unit,
    onResetCounter: () -> Unit,
    onSelectMood: (String) -> Unit
) {
    val spec = widget.toVisualSpec()
    GlassSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("installed_widget_card_${widget.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = widget.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (widget.isCustom) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = if (widget.isCustom) "自訂" else "已做好",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (widget.isCustom) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "${spec.themeStyle.labelZh} · ${spec.blocks.size} 個積木 · 更新 ${widget.refreshIntervalMinutes} 分鐘",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (widget.isPinnedToHome) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "已釘選",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HWidgetLivePreview(
                spec = spec,
                clockMillis = clockMillis,
                weather = weather,
                battery = battery,
                steps = steps,
                todos = todos,
                notes = notes,
                countdowns = countdowns,
                articles = articles,
                mountedExtensions = mountedExtensions,
                habits = habits,
                systemStats = systemStats,
                pomodoroSeconds = pomodoroSeconds,
                pomodoroRunning = pomodoroRunning,
                waterMlToday = waterMlToday,
                waterGoalMl = waterGoalMl,
                quoteText = quoteText,
                quoteAuthor = quoteAuthor,
                stopwatchSeconds = stopwatchSeconds,
                stopwatchRunning = stopwatchRunning,
                counterValue = counterValue,
                moodTag = moodTag,
                onToggleTodo = onToggleTodo,
                onEditTodosClick = onEditTodosClick,
                onTogglePomodoro = onTogglePomodoro,
                onResetPomodoro = onResetPomodoro,
                onAddWaterMl = onAddWaterMl,
                onToggleHabit = onToggleHabit,
                onQuickAction = onQuickAction,
                onToggleStopwatch = onToggleStopwatch,
                onResetStopwatch = onResetStopwatch,
                onIncrementCounter = onIncrementCounter,
                onResetCounter = onResetCounter,
                onSelectMood = onSelectMood
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onPin,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("釘選", maxLines = 1)
                }
                if (!widget.isCustom) {
                    FilledTonalButton(
                        onClick = onCloneAsCustom,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("改為自訂", maxLines = 1)
                    }
                } else {
                    FilledTonalButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("編輯", maxLines = 1)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "刪除 Widget")
                }
            }
        }
    }
}

@Composable
private fun TelemetryPill(
    icon: ImageVector,
    label: String,
    sub: String,
    onClick: () -> Unit
) {
    GlassSurfaceCard(
        cornerRadius = 18.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuickHubTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    GlassSurfaceCard(
        cornerRadius = 20.dp,
        modifier = Modifier
            .width(164.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun WidgetEntity.toVisualSpec(): WidgetVisualSpec = WidgetVisualSpec(
    title = title,
    themeStyle = runCatching { WidgetThemeStyle.valueOf(themeStyle) }.getOrDefault(WidgetThemeStyle.GLASS),
    clockStyle = runCatching { ClockDisplayStyle.valueOf(clockStyle) }.getOrDefault(ClockDisplayStyle.DIGITAL),
    fontStyle = runCatching { WidgetFontStyle.valueOf(fontStyle) }.getOrDefault(WidgetFontStyle.SANS),
    opacity = opacity,
    blurRadius = blurRadius,
    cornerRadius = cornerRadius,
    accentColorHex = accentColorHex,
    textColorHex = textColorHex,
    backgroundPreset = runCatching { BackgroundPreset.valueOf(backgroundPreset) }.getOrDefault(BackgroundPreset.NONE),
    backgroundImageUri = backgroundImageUri,
    animationsEnabled = animationsEnabled,
    blocks = BlockJsonCodec.decode(blocksJson)
)
