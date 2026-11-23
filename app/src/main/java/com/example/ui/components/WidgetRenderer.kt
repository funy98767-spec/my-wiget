package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.db.CountdownEntity
import com.example.data.db.HabitEntity
import com.example.data.db.NewsArticleEntity
import com.example.data.db.NoteEntity
import com.example.data.db.TodoEntity
import com.example.data.device.SystemStatsSnapshot
import com.example.domain.model.BackgroundPreset
import com.example.domain.model.BatterySnapshot
import com.example.domain.model.BlockType
import com.example.domain.model.ClockDisplayStyle
import com.example.domain.model.StepSnapshot
import com.example.domain.model.WeatherSnapshot
import com.example.domain.model.WidgetBlock
import com.example.domain.model.WidgetFontStyle
import com.example.domain.model.WidgetThemeStyle
import com.example.extension.HWidgetPluginRegistry
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberNeonPink
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.NoteBlueBg
import com.example.ui.theme.NoteBlueInk
import com.example.ui.theme.NoteGreenBg
import com.example.ui.theme.NoteGreenInk
import com.example.ui.theme.NotePinkBg
import com.example.ui.theme.NotePinkInk
import com.example.ui.theme.NoteYellowBg
import com.example.ui.theme.NoteYellowInk
import com.example.ui.theme.SilkscreenFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class WidgetVisualSpec(
    val title: String,
    val themeStyle: WidgetThemeStyle,
    val clockStyle: ClockDisplayStyle,
    val fontStyle: WidgetFontStyle,
    val opacity: Float,
    val blurRadius: Float,
    val cornerRadius: Float,
    val accentColorHex: Long,
    val textColorHex: Long,
    val backgroundPreset: BackgroundPreset,
    val backgroundImageUri: String?,
    val animationsEnabled: Boolean,
    val blocks: List<WidgetBlock>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HWidgetLivePreview(
    spec: WidgetVisualSpec,
    clockMillis: Long,
    weather: WeatherSnapshot,
    battery: BatterySnapshot,
    steps: StepSnapshot,
    todos: List<TodoEntity>,
    notes: List<NoteEntity>,
    countdowns: List<CountdownEntity>,
    articles: List<NewsArticleEntity>,
    mountedExtensions: Set<String>,
    modifier: Modifier = Modifier,
    habits: List<HabitEntity> = emptyList(),
    systemStats: SystemStatsSnapshot = SystemStatsSnapshot(),
    pomodoroSeconds: Int = 25 * 60,
    pomodoroRunning: Boolean = false,
    waterMlToday: Int = 1250,
    waterGoalMl: Int = 2000,
    quoteText: String = "把複雜交給工具，把專注留給今天最重要的一件事。",
    quoteAuthor: String = "HWidget 每日靈感",
    stopwatchSeconds: Int = 0,
    stopwatchRunning: Boolean = false,
    counterValue: Int = 0,
    moodTag: String = "專注高效",
    onToggleTodo: ((TodoEntity) -> Unit)? = null,
    onEditTodosClick: (() -> Unit)? = null,
    onTogglePomodoro: (() -> Unit)? = null,
    onResetPomodoro: (() -> Unit)? = null,
    onAddWaterMl: ((Int) -> Unit)? = null,
    onToggleHabit: ((HabitEntity) -> Unit)? = null,
    onQuickAction: ((String) -> Unit)? = null,
    onToggleStopwatch: (() -> Unit)? = null,
    onResetStopwatch: (() -> Unit)? = null,
    onIncrementCounter: ((Int) -> Unit)? = null,
    onResetCounter: (() -> Unit)? = null,
    onSelectMood: ((String) -> Unit)? = null
) {
    val shape = RoundedCornerShape(spec.cornerRadius.dp)
    val accent = Color(spec.accentColorHex)
    val themeColors = resolveThemeColors(spec.themeStyle, spec.textColorHex, accent)
    val fontFamily = if (spec.themeStyle == WidgetThemeStyle.PIXEL) {
        SilkscreenFontFamily
    } else {
        spec.fontStyle.toFontFamily()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(width = themeColors.borderWidth.dp, color = themeColors.borderColor, shape = shape)
    ) {
        // Background Layer (with optional blur & opacity)
        Box(
            modifier = Modifier
                .matchParentSize()
                .alpha(spec.opacity.coerceIn(0.15f, 1f))
                .then(
                    if (spec.blurRadius > 1f) Modifier.blur((spec.blurRadius * 0.45f).dp) else Modifier
                )
        ) {
            when (spec.backgroundPreset) {
                BackgroundPreset.AURORA -> Image(
                    painter = painterResource(id = R.drawable.img_bg_aurora),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                BackgroundPreset.CYBERPUNK -> Image(
                    painter = painterResource(id = R.drawable.img_bg_cyberpunk),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                BackgroundPreset.MINIMAL_PAPER -> Image(
                    painter = painterResource(id = R.drawable.img_bg_minimal_paper),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                BackgroundPreset.CUSTOM_URI -> {
                    if (!spec.backgroundImageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = spec.backgroundImageUri,
                            contentDescription = "Widget custom background",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(themeColors.bgBrush))
                    }
                }
                BackgroundPreset.NONE -> {
                    Box(modifier = Modifier.fillMaxSize().background(themeColors.bgBrush))
                }
            }

            // Glassmorphism frost scrim for legibility over photos
            if (spec.backgroundPreset != BackgroundPreset.NONE) {
                val scrimAlpha = if (spec.themeStyle == WidgetThemeStyle.MINIMAL) 0.25f else 0.36f
                val scrimColor = if (spec.themeStyle == WidgetThemeStyle.MINIMAL) Color.White else Color.Black
                Box(modifier = Modifier.fillMaxSize().background(scrimColor.copy(alpha = scrimAlpha)))
            }
        }

        // Foreground Content Blocks
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val fullBlocks = spec.blocks.filter {
                it.spanFullWidth ||
                    it.type == BlockType.TIME.name ||
                    it.type == BlockType.TODO.name ||
                    it.type == BlockType.NEWS.name ||
                    it.type == BlockType.NOTE.name ||
                    it.type == BlockType.QUOTE.name ||
                    it.type == BlockType.QUICK_LAUNCH.name ||
                    it.type == BlockType.SYSTEM_STATS.name ||
                    it.type == BlockType.HABIT.name ||
                    it.type == BlockType.CALENDAR_MONTH.name ||
                    it.type == BlockType.WORLD_CLOCK.name ||
                    it.type == BlockType.DAY_PROGRESS.name ||
                    it.type == BlockType.MOOD.name ||
                    it.type == BlockType.BREATHING.name ||
                    it.type == BlockType.DEVICE_INFO.name
            }
            val compactBlocks = spec.blocks.filterNot { it in fullBlocks }

            // Render blocks in original order, grouping consecutive compact blocks in 2-up rows
            var index = 0
            while (index < spec.blocks.size) {
                val block = spec.blocks[index]
                val isFull = block in fullBlocks
                if (isFull) {
                    RenderSingleBlock(
                        block = block,
                        spec = spec,
                        fontFamily = fontFamily,
                        ink = themeColors.ink,
                        subInk = themeColors.subInk,
                        accent = themeColors.accent,
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
                        onSelectMood = onSelectMood,
                        modifier = Modifier.fillMaxWidth()
                    )
                    index++
                } else {
                    val next = spec.blocks.getOrNull(index + 1)
                    if (next != null && next in compactBlocks) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RenderSingleBlock(
                                block = block,
                                spec = spec,
                                fontFamily = fontFamily,
                                ink = themeColors.ink,
                                subInk = themeColors.subInk,
                                accent = themeColors.accent,
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
                                onSelectMood = onSelectMood,
                                modifier = Modifier.weight(1f)
                            )
                            RenderSingleBlock(
                                block = next,
                                spec = spec,
                                fontFamily = fontFamily,
                                ink = themeColors.ink,
                                subInk = themeColors.subInk,
                                accent = themeColors.accent,
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
                                onSelectMood = onSelectMood,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        index += 2
                    } else {
                        RenderSingleBlock(
                            block = block,
                            spec = spec,
                            fontFamily = fontFamily,
                            ink = themeColors.ink,
                            subInk = themeColors.subInk,
                            accent = themeColors.accent,
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
                            onSelectMood = onSelectMood,
                            modifier = Modifier.fillMaxWidth()
                        )
                        index++
                    }
                }
            }

            if (spec.blocks.isEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "拖曳積木到此畫布即可即時預覽",
                        color = themeColors.subInk,
                        fontFamily = fontFamily,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderSingleBlock(
    block: WidgetBlock,
    spec: WidgetVisualSpec,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    clockMillis: Long,
    weather: WeatherSnapshot,
    battery: BatterySnapshot,
    steps: StepSnapshot,
    todos: List<TodoEntity>,
    notes: List<NoteEntity>,
    countdowns: List<CountdownEntity>,
    articles: List<NewsArticleEntity>,
    mountedExtensions: Set<String>,
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
    onToggleTodo: ((TodoEntity) -> Unit)?,
    onEditTodosClick: (() -> Unit)?,
    onTogglePomodoro: (() -> Unit)?,
    onResetPomodoro: (() -> Unit)?,
    onAddWaterMl: ((Int) -> Unit)?,
    onToggleHabit: ((HabitEntity) -> Unit)?,
    onQuickAction: ((String) -> Unit)?,
    onToggleStopwatch: (() -> Unit)?,
    onResetStopwatch: (() -> Unit)?,
    onIncrementCounter: ((Int) -> Unit)?,
    onResetCounter: (() -> Unit)?,
    onSelectMood: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val blockType = runCatching { BlockType.valueOf(block.type) }.getOrDefault(BlockType.TIME)
    when (blockType) {
        BlockType.TIME -> ClockBlockView(
            clockMillis = clockMillis,
            clockStyle = spec.clockStyle,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            animated = spec.animationsEnabled,
            modifier = modifier
        )
        BlockType.WEATHER -> WeatherBlockView(
            weather = weather,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.BATTERY -> BatteryBlockView(
            battery = battery,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.STEPS -> StepsBlockView(
            steps = steps,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.TODO -> TodoBlockView(
            todos = todos,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            onToggleTodo = onToggleTodo,
            onEditTodosClick = onEditTodosClick,
            modifier = modifier
        )
        BlockType.NOTE -> NoteBlockView(
            note = notes.firstOrNull(),
            fontFamily = fontFamily,
            modifier = modifier
        )
        BlockType.COUNTDOWN -> CountdownBlockView(
            countdowns = countdowns,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.NEWS -> NewsBlockView(
            articles = articles,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.IMAGE -> ImageBlockView(
            imageUri = block.imageUri,
            label = block.customLabel,
            ink = ink,
            subInk = subInk,
            modifier = modifier
        )
        BlockType.POMODORO -> PomodoroBlockView(
            secondsLeft = pomodoroSeconds,
            isRunning = pomodoroRunning,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            onToggle = onTogglePomodoro,
            onReset = onResetPomodoro,
            modifier = modifier
        )
        BlockType.WATER -> WaterBlockView(
            waterMlToday = waterMlToday,
            waterGoalMl = waterGoalMl,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            onAddWater = onAddWaterMl,
            modifier = modifier
        )
        BlockType.HABIT -> HabitBlockView(
            habits = habits,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            onToggleHabit = onToggleHabit,
            modifier = modifier
        )
        BlockType.SYSTEM_STATS -> SystemStatsBlockView(
            stats = systemStats,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.QUOTE -> QuoteBlockView(
            quoteText = quoteText,
            quoteAuthor = quoteAuthor,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.QUICK_LAUNCH -> QuickLaunchBlockView(
            fontFamily = fontFamily,
            ink = ink,
            accent = accent,
            onQuickAction = onQuickAction,
            modifier = modifier
        )
        BlockType.CALENDAR_MONTH -> CalendarMonthBlockView(
            clockMillis = clockMillis,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.WORLD_CLOCK -> WorldClockBlockView(
            clockMillis = clockMillis,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.DAY_PROGRESS -> DayProgressBlockView(
            clockMillis = clockMillis,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.STOPWATCH -> StopwatchBlockView(
            stopwatchSeconds = stopwatchSeconds,
            stopwatchRunning = stopwatchRunning,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            onToggle = onToggleStopwatch,
            onReset = onResetStopwatch,
            modifier = modifier
        )
        BlockType.COUNTER -> CounterBlockView(
            counterValue = counterValue,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            onIncrement = onIncrementCounter,
            onReset = onResetCounter,
            modifier = modifier
        )
        BlockType.MOOD -> MoodBlockView(
            selectedMood = moodTag,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            onSelectMood = onSelectMood,
            modifier = modifier
        )
        BlockType.BREATHING -> BreathingBlockView(
            clockMillis = clockMillis,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.NETWORK -> NetworkBlockView(
            stats = systemStats,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.DEVICE_INFO -> DeviceInfoBlockView(
            stats = systemStats,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.CALORIE_DISTANCE -> CalorieDistanceBlockView(
            steps = steps,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
        BlockType.EXTENSION_SLOT -> ExtensionSlotBlockView(
            extensionKey = block.extensionId,
            mountedExtensions = mountedExtensions,
            fontFamily = fontFamily,
            ink = ink,
            subInk = subInk,
            accent = accent,
            modifier = modifier
        )
    }
}

@Composable
private fun ClockBlockView(
    clockMillis: Long,
    clockStyle: ClockDisplayStyle,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    animated: Boolean,
    modifier: Modifier = Modifier
) {
    val date = Date(clockMillis)
    val hourStr = SimpleDateFormat("HH", Locale.getDefault()).format(date)
    val minStr = SimpleDateFormat("mm", Locale.getDefault()).format(date)
    val secStr = SimpleDateFormat("ss", Locale.getDefault()).format(date)
    val dateStr = SimpleDateFormat("yyyy/MM/dd EEEE", Locale.TRADITIONAL_CHINESE).format(date)

    when (clockStyle) {
        ClockDisplayStyle.DIGITAL -> {
            Column(modifier = modifier) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$hourStr:$minStr",
                        color = ink,
                        fontFamily = fontFamily,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = secStr,
                        color = accent,
                        fontFamily = fontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text = dateStr,
                    color = subInk,
                    fontFamily = fontFamily,
                    fontSize = 12.sp
                )
            }
        }
        ClockDisplayStyle.FLIP -> {
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlipCardTile(value = hourStr, fontFamily = fontFamily, ink = ink, animated = animated)
                    Text(":", color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    FlipCardTile(value = minStr, fontFamily = fontFamily, ink = ink, animated = animated)
                    FlipCardTile(
                        value = secStr,
                        fontFamily = fontFamily,
                        ink = accent,
                        animated = animated,
                        compact = true
                    )
                }
                Text(text = dateStr, color = subInk, fontFamily = fontFamily, fontSize = 11.sp)
            }
        }
        ClockDisplayStyle.PIXEL -> {
            Column(
                modifier = modifier
                    .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                    .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "$hourStr:$minStr:$secStr",
                    color = accent,
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 26.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = SimpleDateFormat("MM-dd EEE", Locale.ENGLISH).format(date).uppercase(),
                    color = subInk,
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun FlipCardTile(
    value: String,
    fontFamily: FontFamily,
    ink: Color,
    animated: Boolean,
    compact: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 6.dp else 8.dp
            ),
            contentAlignment = Alignment.Center
        ) {
            if (animated) {
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            initialOffsetY = { -it / 2 }
                        ) + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut())
                    },
                    label = "flip_digit"
                ) { digit ->
                    Text(
                        text = digit,
                        color = ink,
                        fontFamily = fontFamily,
                        fontSize = if (compact) 18.sp else 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = value,
                    color = ink,
                    fontFamily = fontFamily,
                    fontSize = if (compact) 18.sp else 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Split flap horizontal hinge line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.14f))
            )
        }
    }
}

@Composable
private fun WeatherBlockView(
    weather: WeatherSnapshot,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WbSunny,
                    contentDescription = "天氣",
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = weather.cityName,
                    color = subInk,
                    fontFamily = fontFamily,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${weather.temperatureC.toInt()}°C · ${weather.conditionText}",
                color = ink,
                fontFamily = fontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "降雨機率 ${weather.precipitationProb}%",
                color = subInk,
                fontFamily = fontFamily,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun BatteryBlockView(
    battery: BatterySnapshot,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (battery.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                    contentDescription = "電池電量",
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${battery.levelPercent}%",
                    color = ink,
                    fontFamily = fontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { (battery.levelPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = accent,
                trackColor = ink.copy(alpha = 0.16f)
            )
            Text(
                text = if (battery.isCharging) "充電中" else "${battery.temperatureC}°C",
                color = subInk,
                fontFamily = fontFamily,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StepsBlockView(
    steps: StepSnapshot,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val pct = (steps.stepsToday.toFloat() / steps.dailyGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.DirectionsWalk,
                    contentDescription = "步數",
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${steps.stepsToday} 步",
                    color = ink,
                    fontFamily = fontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = accent,
                trackColor = ink.copy(alpha = 0.16f)
            )
            Text(
                text = "目標 ${steps.dailyGoal} · ${(pct * 100).toInt()}%",
                color = subInk,
                fontFamily = fontFamily,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TodoBlockView(
    todos: List<TodoEntity>,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    onToggleTodo: ((TodoEntity) -> Unit)? = null,
    onEditTodosClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.09f)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "待辦事項",
                    color = accent,
                    fontFamily = fontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (onEditTodosClick != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onEditTodosClick)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "更改待辦",
                            tint = accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text("更改", color = accent, fontFamily = fontFamily, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (todos.isEmpty()) {
                Text("尚無待辦任務", color = subInk, fontFamily = fontFamily, fontSize = 12.sp)
            } else {
                todos.take(3).forEach { todo ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (onToggleTodo != null) Modifier.clickable { onToggleTodo(todo) } else Modifier
                            )
                            .padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (todo.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (todo.isCompleted) accent else subInk,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = todo.title,
                            color = if (todo.isCompleted) subInk else ink,
                            fontFamily = fontFamily,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteBlockView(
    note: NoteEntity?,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier
) {
    val (bg, ink) = when (note?.colorKey) {
        "BLUE" -> NoteBlueBg to NoteBlueInk
        "PINK" -> NotePinkBg to NotePinkInk
        "GREEN" -> NoteGreenBg to NoteGreenInk
        else -> NoteYellowBg to NoteYellowInk
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bg
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.StickyNote2,
                    contentDescription = "便利貼",
                    tint = ink,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = note?.title ?: "桌面便利貼",
                    color = ink,
                    fontFamily = fontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = note?.content ?: "點擊首頁便利貼卡片即可新增黃/藍/粉/綠彩色便籤。",
                color = ink.copy(alpha = 0.88f),
                fontFamily = fontFamily,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CountdownBlockView(
    countdowns: List<CountdownEntity>,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val item = countdowns.firstOrNull()
    val daysLeft = if (item != null) {
        TimeUnit.MILLISECONDS.toDays(item.targetDateMillis - System.currentTimeMillis()).coerceAtLeast(0)
    } else 0L

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Event,
                    contentDescription = "倒數日",
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = item?.title ?: "重要倒數日",
                    color = subInk,
                    fontFamily = fontFamily,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (item != null) "還有 $daysLeft 天" else "未設定",
                color = ink,
                fontFamily = fontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NewsBlockView(
    articles: List<NewsArticleEntity>,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.09f)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Newspaper,
                    contentDescription = "RSS 新聞",
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = articles.firstOrNull()?.sourceName ?: "RSS 即時新聞",
                    color = accent,
                    fontFamily = fontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = articles.firstOrNull()?.title ?: "尚未同步新聞，點擊刷新 RSS 來源",
                color = ink,
                fontFamily = fontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            articles.getOrNull(1)?.let { second ->
                Text(
                    text = "• ${second.title}",
                    color = subInk,
                    fontFamily = fontFamily,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ImageBlockView(
    imageUri: String?,
    label: String,
    ink: Color,
    subInk: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.12f)
    ) {
        if (!imageUri.isNullOrBlank()) {
            AsyncImage(
                model = imageUri,
                contentDescription = label.ifBlank { "自訂圖片積木" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        tint = ink,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = label.ifBlank { "自訂圖片積木" },
                        color = subInk,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtensionSlotBlockView(
    extensionKey: String?,
    mountedExtensions: Set<String>,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val plugin = HWidgetPluginRegistry.getPlugin(extensionKey)
    val isMounted = extensionKey != null && extensionKey in mountedExtensions
    val preview = plugin?.createPreview(isMounted)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Extension,
                    contentDescription = "擴充插槽",
                    tint = accent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = preview?.slotId?.displayName ?: "擴充模組插槽",
                    color = ink,
                    fontFamily = fontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = preview?.headline ?: "等待掛載擴充模組",
                color = subInk,
                fontFamily = fontFamily,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PomodoroBlockView(
    secondsLeft: Int,
    isRunning: Boolean,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    onToggle: (() -> Unit)?,
    onReset: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val mins = secondsLeft / 60
    val secs = secondsLeft % 60
    val timeText = String.format(Locale.US, "%02d:%02d", mins, secs)
    val progress = ((25 * 60 - secondsLeft).toFloat() / (25 * 60)).coerceIn(0f, 1f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, contentDescription = "番茄鐘", tint = accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isRunning) "番茄專注進行中" else "25 分鐘番茄鐘",
                        color = subInk,
                        fontFamily = fontFamily,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = timeText,
                    color = ink,
                    fontFamily = fontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = accent,
                    trackColor = ink.copy(alpha = 0.15f)
                )
            }
            if (onToggle != null) {
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.24f),
                        modifier = Modifier.size(34.dp).clickable(onClick = onToggle)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isRunning) "暫停" else "開始",
                                tint = ink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (onReset != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.size(34.dp).clickable(onClick = onReset)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Replay,
                                    contentDescription = "重置",
                                    tint = subInk,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterBlockView(
    waterMlToday: Int,
    waterGoalMl: Int,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    onAddWater: ((Int) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val pct = (waterMlToday.toFloat() / waterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalDrink, contentDescription = "喝水", tint = accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${waterMlToday}ml", color = ink, fontFamily = fontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (onAddWater != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.25f),
                        modifier = Modifier.clickable { onAddWater(250) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "喝水250ml", tint = ink, modifier = Modifier.size(12.dp))
                            Text("250", color = ink, fontFamily = fontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = accent,
                trackColor = ink.copy(alpha = 0.16f)
            )
            Text("飲水目標 ${waterGoalMl}ml", color = subInk, fontFamily = fontFamily, fontSize = 10.sp)
        }
    }
}

@Composable
private fun HabitBlockView(
    habits: List<HabitEntity>,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    onToggleHabit: ((HabitEntity) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.09f)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = "習慣", tint = accent, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("每日習慣打卡", color = accent, fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (habits.isEmpty()) {
                Text("尚未設定習慣", color = subInk, fontFamily = fontFamily, fontSize = 12.sp)
            } else {
                habits.take(3).forEach { habit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (onToggleHabit != null) Modifier.clickable { onToggleHabit(habit) } else Modifier)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (habit.checkedToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (habit.checkedToday) accent else subInk,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = habit.title,
                            color = ink,
                            fontFamily = fontFamily,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "🔥${habit.streakDays}天",
                            color = accent,
                            fontFamily = fontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemStatsBlockView(
    stats: SystemStatsSnapshot,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Memory, contentDescription = "系統監控", tint = accent, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("系統記憶體與儲存空間", color = accent, fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("RAM ${stats.ramUsagePercent}%", color = ink, fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { stats.ramUsagePercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = accent,
                        trackColor = ink.copy(alpha = 0.15f)
                    )
                    Text("${stats.usedRamMb}/${stats.totalRamMb} MB", color = subInk, fontFamily = fontFamily, fontSize = 10.sp)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("空間剩 ${stats.freeStorageGb}GB", color = ink, fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { stats.storageUsagePercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = accent,
                        trackColor = ink.copy(alpha = 0.15f)
                    )
                    Text("已用 ${stats.storageUsagePercent}% / ${stats.totalStorageGb}GB", color = subInk, fontFamily = fontFamily, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun QuoteBlockView(
    quoteText: String,
    quoteAuthor: String,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.09f)
    ) {
        Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Rounded.FormatQuote, contentDescription = "語錄", tint = accent, modifier = Modifier.size(18.dp))
            Text(
                text = "「$quoteText」",
                color = ink,
                fontFamily = fontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "— $quoteAuthor",
                color = subInk,
                fontFamily = fontFamily,
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickLaunchBlockView(
    fontFamily: FontFamily,
    ink: Color,
    accent: Color,
    onQuickAction: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.09f)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bolt, contentDescription = "快捷工具", tint = accent, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("快速啟動列", color = accent, fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("TODO" to "記待辦", "NOTE" to "便利貼", "HABIT" to "喝水/打卡", "ALARM" to "系統鬧鐘").forEach { (key, label) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accent.copy(alpha = 0.22f),
                        modifier = Modifier.then(
                            if (onQuickAction != null) Modifier.clickable { onQuickAction(key) } else Modifier
                        )
                    ) {
                        Text(
                            text = label,
                            color = ink,
                            fontFamily = fontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class ResolvedThemeColors(
    val bgBrush: Brush,
    val ink: Color,
    val subInk: Color,
    val accent: Color,
    val borderColor: Color,
    val borderWidth: Float
)

@Composable
private fun resolveThemeColors(
    style: WidgetThemeStyle,
    customTextHex: Long,
    accent: Color
): ResolvedThemeColors {
    return when (style) {
        WidgetThemeStyle.MATERIAL -> ResolvedThemeColors(
            bgBrush = Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer
                )
            ),
            ink = MaterialTheme.colorScheme.onPrimaryContainer,
            subInk = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            accent = MaterialTheme.colorScheme.primary,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
            borderWidth = 1f
        )
        WidgetThemeStyle.GLASS -> ResolvedThemeColors(
            bgBrush = Brush.linearGradient(
                listOf(
                    Color(0xFF1E293B).copy(alpha = 0.82f),
                    Color(0xFF312E81).copy(alpha = 0.72f),
                    Color(0xFF0F172A).copy(alpha = 0.88f)
                )
            ),
            ink = Color(customTextHex),
            subInk = Color(customTextHex).copy(alpha = 0.72f),
            accent = accent,
            borderColor = Color.White.copy(alpha = 0.34f),
            borderWidth = 1.2f
        )
        WidgetThemeStyle.CYBERPUNK -> ResolvedThemeColors(
            bgBrush = Brush.linearGradient(listOf(CyberVoid, Color(0xFF1A103D))),
            ink = CyberNeonCyan,
            subInk = Color.White.copy(alpha = 0.78f),
            accent = CyberNeonPink,
            borderColor = CyberNeonCyan,
            borderWidth = 1.6f
        )
        WidgetThemeStyle.PIXEL -> ResolvedThemeColors(
            bgBrush = Brush.verticalGradient(listOf(Color(0xFF111827), Color(0xFF1F2937))),
            ink = Color(0xFF4ADE80),
            subInk = Color(0xFFA7F3D0),
            accent = Color(0xFFFACC15),
            borderColor = Color(0xFF4ADE80),
            borderWidth = 2f
        )
        WidgetThemeStyle.MINIMAL -> ResolvedThemeColors(
            bgBrush = Brush.linearGradient(listOf(Color(0xFFFAF9F6), Color(0xFFF1F5F9))),
            ink = Color(0xFF0F172A),
            subInk = Color(0xFF64748B),
            accent = accent,
            borderColor = Color(0xFFCBD5E1),
            borderWidth = 1f
        )
        WidgetThemeStyle.DARK -> ResolvedThemeColors(
            bgBrush = Brush.linearGradient(listOf(Color(0xFF05070B), Color(0xFF111827))),
            ink = Color(0xFFF8FAFC),
            subInk = Color(0xFF94A3B8),
            accent = accent,
            borderColor = Color.White.copy(alpha = 0.14f),
            borderWidth = 1f
        )
    }
}
