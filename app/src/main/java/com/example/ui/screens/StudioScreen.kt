package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlusOne
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import com.example.domain.model.StoreWidgetPack
import com.example.domain.model.WeatherSnapshot
import com.example.domain.model.WidgetFontStyle
import com.example.domain.model.WidgetThemeStyle
import com.example.domain.model.WidgetType
import com.example.extension.ExtensionSlotId
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.HWidgetLivePreview
import com.example.ui.components.WidgetVisualSpec
import com.example.ui.viewmodel.WidgetEditorState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    editorState: WidgetEditorState,
    readyMadeCatalog: List<StoreWidgetPack>,
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
    onUpdateEditor: ((WidgetEditorState) -> WidgetEditorState) -> Unit,
    onAddBlock: (BlockType, String?, String?) -> Unit,
    onRemoveBlock: (String) -> Unit,
    onMoveBlock: (Int, Int) -> Unit,
    onToggleBlockWidth: (String) -> Unit,
    onResetNew: (WidgetType) -> Unit,
    onSaveWidget: () -> Unit,
    onInstallReadyMadePack: (StoreWidgetPack) -> Unit,
    onLoadReadyMadeAsCustom: (StoreWidgetPack) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 0 = 自訂編輯 (Custom), 1 = 已經做好的模板 (Ready-Made)
    var studioMode by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var presetQuery by remember { mutableStateOf("") }

    // Drag-and-Drop palette state
    var canvasBounds by remember { mutableStateOf(Rect.Zero) }
    var draggingBlockType by remember { mutableStateOf<BlockType?>(null) }
    var dragWindowPos by remember { mutableStateOf(Offset.Zero) }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }

    // Zero-permission Photo Picker for Background Image
    val bgPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onUpdateEditor {
                it.copy(
                    backgroundPreset = BackgroundPreset.CUSTOM_URI,
                    backgroundImageUri = uri.toString()
                )
            }
        }
    }

    // Zero-permission Photo Picker for Image Block
    val blockImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onAddBlock(BlockType.IMAGE, uri.toString(), null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOffset = it.boundsInWindow().topLeft }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "建立 Widget",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (studioMode == 0) "自由拖放積木與微調視覺參數" else "選擇已經做好的成品直接安裝或改編",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (studioMode == 0) {
                        Button(
                            onClick = onSaveWidget,
                            modifier = Modifier.testTag("studio_save_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("儲存自訂")
                        }
                    }
                }
            }

            // Segmented Switch: 自訂 vs 已經做好的
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = studioMode == 0,
                        onClick = { studioMode = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("studio_mode_custom")
                    ) {
                        Text("完全自訂組合")
                    }
                    SegmentedButton(
                        selected = studioMode == 1,
                        onClick = { studioMode = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("studio_mode_ready_made")
                    ) {
                        Text("已做好成品 (${readyMadeCatalog.size})")
                    }
                }
            }

            if (studioMode == 1) {
                val filteredPresets = readyMadeCatalog.filter { pack ->
                    presetQuery.isBlank() ||
                        pack.title.contains(presetQuery, ignoreCase = true) ||
                        pack.description.contains(presetQuery, ignoreCase = true) ||
                        pack.category.labelZh.contains(presetQuery, ignoreCase = true)
                }
                item {
                    OutlinedTextField(
                        value = presetQuery,
                        onValueChange = { presetQuery = it },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        label = { Text("搜尋 ${readyMadeCatalog.size} 款不重複成品模板...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Ready-Made Quick Install & Clone Gallery
                items(filteredPresets, key = { "preset_${it.id}" }) { pack ->
                    GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pack.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${pack.category.labelZh} · ${pack.themeStyle.labelZh} · ${pack.author}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "已做好",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HWidgetLivePreview(
                                spec = WidgetVisualSpec(
                                    title = pack.title,
                                    themeStyle = pack.themeStyle,
                                    clockStyle = pack.clockStyle,
                                    fontStyle = pack.fontStyle,
                                    opacity = pack.opacity,
                                    blurRadius = pack.blurRadius,
                                    cornerRadius = pack.cornerRadius,
                                    accentColorHex = pack.accentColorHex,
                                    textColorHex = pack.textColorHex,
                                    backgroundPreset = pack.backgroundPreset,
                                    backgroundImageUri = null,
                                    animationsEnabled = true,
                                    blocks = pack.blocks
                                ),
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
                                moodTag = moodTag
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onInstallReadyMadePack(pack) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("直接使用成品")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        onLoadReadyMadeAsCustom(pack)
                                        studioMode = 0
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("套用並自訂")
                                }
                            }
                        }
                    }
                }
            } else {
                // Custom Visual Drag-and-Drop Editor
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editorState.title,
                            onValueChange = { text -> onUpdateEditor { it.copy(title = text, isCustom = true) } },
                            label = { Text("自訂 Widget 名稱") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("studio_title_input")
                        )
                        OutlinedButton(onClick = { onResetNew(WidgetType.COMPOSITE) }) {
                            Text("清空重置")
                        }
                    }
                }

                // Live Drop-Target WYSIWYG Preview Stage
                item {
                    val isHoveringCanvas = draggingBlockType != null && canvasBounds.contains(dragWindowPos)
                    GlassSurfaceCard(
                        cornerRadius = 26.dp,
                        borderColor = if (isHoveringCanvas) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { canvasBounds = it.boundsInWindow() }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHoveringCanvas) "放開即可加入「${draggingBlockType?.labelZh}」積木！" else "自訂畫布即時預覽 (可接收拖放積木)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isHoveringCanvas) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${editorState.blocks.size} 積木",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HWidgetLivePreview(
                                spec = WidgetVisualSpec(
                                    title = editorState.title,
                                    themeStyle = editorState.themeStyle,
                                    clockStyle = editorState.clockStyle,
                                    fontStyle = editorState.fontStyle,
                                    opacity = editorState.opacity,
                                    blurRadius = editorState.blurRadius,
                                    cornerRadius = editorState.cornerRadius,
                                    accentColorHex = editorState.accentColorHex,
                                    textColorHex = editorState.textColorHex,
                                    backgroundPreset = editorState.backgroundPreset,
                                    backgroundImageUri = editorState.backgroundImageUri,
                                    animationsEnabled = editorState.animationsEnabled,
                                    blocks = editorState.blocks
                                ),
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
                                moodTag = moodTag
                            )
                        }
                    }
                }

                // Editor Mode Tabs
                item {
                    val tabs = listOf("積木組合", "主題與時鐘", "外觀設定", "排程與擴充")
                    SecondaryTabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { idx, label ->
                            Tab(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                text = { Text(label, maxLines = 1) }
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        item {
                            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "長按拖曳積木至上方畫布，或點擊直接加入：",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val palette = listOf(
                                        BlockType.TIME to Icons.Rounded.Schedule,
                                        BlockType.WEATHER to Icons.Rounded.WbSunny,
                                        BlockType.TODO to Icons.Rounded.Checklist,
                                        BlockType.POMODORO to Icons.Rounded.Timer,
                                        BlockType.WATER to Icons.Rounded.LocalDrink,
                                        BlockType.HABIT to Icons.Rounded.TaskAlt,
                                        BlockType.CALENDAR_MONTH to Icons.Rounded.CalendarMonth,
                                        BlockType.WORLD_CLOCK to Icons.Rounded.Public,
                                        BlockType.DAY_PROGRESS to Icons.Rounded.Schedule,
                                        BlockType.STOPWATCH to Icons.Rounded.Timer,
                                        BlockType.COUNTER to Icons.Rounded.PlusOne,
                                        BlockType.MOOD to Icons.Rounded.Mood,
                                        BlockType.BREATHING to Icons.Rounded.Air,
                                        BlockType.NETWORK to Icons.Rounded.Wifi,
                                        BlockType.DEVICE_INFO to Icons.Rounded.PhoneAndroid,
                                        BlockType.CALORIE_DISTANCE to Icons.Rounded.LocalFireDepartment,
                                        BlockType.SYSTEM_STATS to Icons.Rounded.Memory,
                                        BlockType.QUOTE to Icons.Rounded.FormatQuote,
                                        BlockType.QUICK_LAUNCH to Icons.Rounded.RocketLaunch,
                                        BlockType.BATTERY to Icons.Rounded.BatteryStd,
                                        BlockType.IMAGE to Icons.Rounded.Image,
                                        BlockType.COUNTDOWN to Icons.Rounded.Event,
                                        BlockType.STEPS to Icons.Rounded.DirectionsWalk,
                                        BlockType.NOTE to Icons.Rounded.StickyNote2,
                                        BlockType.NEWS to Icons.Rounded.Newspaper
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        palette.forEach { (type, icon) ->
                                            DraggablePaletteChip(
                                                type = type,
                                                icon = icon,
                                                onTap = {
                                                    if (type == BlockType.IMAGE) {
                                                        blockImagePicker.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    } else {
                                                        onAddBlock(type, null, null)
                                                    }
                                                },
                                                onDragStart = { startPos ->
                                                    draggingBlockType = type
                                                    dragWindowPos = startPos
                                                },
                                                onDragMove = { delta -> dragWindowPos += delta },
                                                onDragEnd = {
                                                    val dropped = draggingBlockType
                                                    if (dropped != null && canvasBounds.contains(dragWindowPos)) {
                                                        if (dropped == BlockType.IMAGE) {
                                                            blockImagePicker.launch(
                                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                            )
                                                        } else {
                                                            onAddBlock(dropped, null, null)
                                                        }
                                                    }
                                                    draggingBlockType = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "畫布積木順序與寬度配置",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    editorState.blocks.forEachIndexed { idx, block ->
                                        val blockType = runCatching { BlockType.valueOf(block.type) }.getOrDefault(BlockType.TIME)
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Rounded.DragIndicator,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = if (blockType == BlockType.EXTENSION_SLOT) {
                                                        "擴充：${block.extensionId ?: "Slot"}"
                                                    } else {
                                                        blockType.labelZh
                                                    },
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { onToggleBlockWidth(block.id) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.ViewColumn,
                                                        contentDescription = "切換整行或並排",
                                                        tint = if (block.spanFullWidth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onMoveBlock(idx, idx - 1) },
                                                    enabled = idx > 0,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "上移", modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = { onMoveBlock(idx, idx + 1) },
                                                    enabled = idx < editorState.blocks.lastIndex,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "下移", modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = { onRemoveBlock(block.id) },
                                                    enabled = editorState.blocks.size > 1,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Close, contentDescription = "移除積木", modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("外觀主題系統 (6 種風格)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        WidgetThemeStyle.entries.forEach { theme ->
                                            FilterChip(
                                                selected = editorState.themeStyle == theme,
                                                onClick = { onUpdateEditor { it.copy(themeStyle = theme) } },
                                                label = { Text(theme.labelZh) },
                                                leadingIcon = if (editorState.themeStyle == theme) {
                                                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                } else null
                                            )
                                        }
                                    }

                                    Text("時鐘樣式 (數位 / 翻頁 / 像素)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ClockDisplayStyle.entries.forEach { cs ->
                                            FilterChip(
                                                selected = editorState.clockStyle == cs,
                                                onClick = { onUpdateEditor { it.copy(clockStyle = cs) } },
                                                label = { Text(cs.labelZh) }
                                            )
                                        }
                                    }

                                    Text("字型選擇", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        WidgetFontStyle.entries.forEach { fs ->
                                            FilterChip(
                                                selected = editorState.fontStyle == fs,
                                                onClick = { onUpdateEditor { it.copy(fontStyle = fs) } },
                                                label = { Text(fs.labelZh) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        item {
                            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("透明度：${(editorState.opacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                                    Slider(
                                        value = editorState.opacity,
                                        onValueChange = { v -> onUpdateEditor { it.copy(opacity = v) } },
                                        valueRange = 0.2f..1.0f
                                    )

                                    Text("背景模糊效果：${editorState.blurRadius.roundToInt()} dp", style = MaterialTheme.typography.labelMedium)
                                    Slider(
                                        value = editorState.blurRadius,
                                        onValueChange = { v -> onUpdateEditor { it.copy(blurRadius = v) } },
                                        valueRange = 0f..28f
                                    )

                                    Text("圓角半徑：${editorState.cornerRadius.roundToInt()} dp", style = MaterialTheme.typography.labelMedium)
                                    Slider(
                                        value = editorState.cornerRadius,
                                        onValueChange = { v -> onUpdateEditor { it.copy(cornerRadius = v) } },
                                        valueRange = 4f..36f
                                    )

                                    Text("強調色票", style = MaterialTheme.typography.labelMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf(
                                            0xFF4F46E5L,
                                            0xFF22D3EEL,
                                            0xFFF43F5EL,
                                            0xFF10B981L,
                                            0xFFF59E0BL,
                                            0xFF00F5D4L
                                        ).forEach { hex ->
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(hex),
                                                border = if (editorState.accentColorHex == hex) {
                                                    BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface)
                                                } else null,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clickable { onUpdateEditor { it.copy(accentColorHex = hex) } }
                                            ) {}
                                        }
                                    }

                                    Text("背景圖片預設 / 自訂", style = MaterialTheme.typography.labelMedium)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        BackgroundPreset.entries.filter { it != BackgroundPreset.CUSTOM_URI }.forEach { preset ->
                                            FilterChip(
                                                selected = editorState.backgroundPreset == preset,
                                                onClick = { onUpdateEditor { it.copy(backgroundPreset = preset) } },
                                                label = { Text(preset.labelZh) }
                                            )
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                bgPhotoPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                        ) {
                                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("從相簿選背景")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        item {
                            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("翻頁與過場動畫", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            Text("啟用時鐘翻頁彈簧動畫與微互動效果", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = editorState.animationsEnabled,
                                            onCheckedChange = { on -> onUpdateEditor { it.copy(animationsEnabled = on) } }
                                        )
                                    }

                                    Text("背景資料更新頻率", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(15 to "15 分鐘", 30 to "30 分鐘", 60 to "1 小時", 360 to "6 小時").forEach { (mins, label) ->
                                            FilterChip(
                                                selected = editorState.refreshIntervalMinutes == mins,
                                                onClick = { onUpdateEditor { it.copy(refreshIntervalMinutes = mins) } },
                                                label = { Text(label) }
                                            )
                                        }
                                    }

                                    Text("加入預留擴充模組插槽", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ExtensionSlotId.entries.forEach { slot ->
                                            OutlinedButton(
                                                onClick = { onAddBlock(BlockType.EXTENSION_SLOT, null, slot.key) }
                                            ) {
                                                Icon(Icons.Rounded.Extension, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("+ ${slot.displayName}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

        val dragging = draggingBlockType
        if (dragging != null) {
            val localOffset = dragWindowPos - rootOffset
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .offset { IntOffset((localOffset.x - 90).roundToInt(), (localOffset.y - 60).roundToInt()) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = dragging.labelZh,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggablePaletteChip(
    type: BlockType,
    icon: ImageVector,
    onTap: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var chipOrigin by remember { mutableStateOf(Offset.Zero) }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier
            .onGloballyPositioned { chipOrigin = it.boundsInWindow().topLeft }
            .pointerInput(type) {
                detectDragGestures(
                    onDragStart = { localOffset -> onDragStart(chipOrigin + localOffset) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragMove(dragAmount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .clickable(onClick = onTap)
            .testTag("palette_block_${type.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = type.labelZh, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(type.labelZh, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
