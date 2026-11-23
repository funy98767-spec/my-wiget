package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.CountdownEntity
import com.example.data.db.HabitEntity
import com.example.data.db.NewsArticleEntity
import com.example.data.db.NoteEntity
import com.example.data.db.TodoEntity
import com.example.data.device.SystemStatsSnapshot
import com.example.domain.model.BatterySnapshot
import com.example.domain.model.StepSnapshot
import com.example.domain.model.StoreWidgetPack
import com.example.domain.model.WeatherSnapshot
import com.example.domain.model.WidgetType
import com.example.extension.HWidgetPluginRegistry
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.HWidgetLivePreview
import com.example.ui.components.WidgetVisualSpec

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreScreen(
    catalog: List<StoreWidgetPack>,
    installedStoreIds: Set<String>,
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
    onInstallPack: (StoreWidgetPack) -> Unit,
    onPinPack: (StoreWidgetPack) -> Unit,
    onToggleExtensionMount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("ALL") } // ALL, OFFICIAL, THIRD_PARTY
    var categoryGroup by remember { mutableStateOf("ALL") }

    val categoryGroups = remember {
        listOf(
            "ALL" to "全部分類",
            "CLOCK" to "時鐘/世界/碼錶",
            "WEATHER" to "天氣氣象",
            "PRODUCTIVITY" to "待辦/番茄/倒數",
            "HEALTH" to "健康/喝水/運動",
            "NOTES_CAL" to "筆記/月曆/語錄",
            "SYSTEM" to "系統/電量/網路",
            "NEWS" to "RSS 新聞",
            "COMPOSITE" to "全能複合"
        )
    }

    val filtered = remember(catalog, searchQuery, filterMode, categoryGroup) {
        catalog.filter { pack ->
            val matchesFilter = when (filterMode) {
                "OFFICIAL" -> pack.isOfficial
                "THIRD_PARTY" -> !pack.isOfficial
                else -> true
            }
            val matchesCategory = when (categoryGroup) {
                "CLOCK" -> pack.category in setOf(WidgetType.CLOCK, WidgetType.WORLD_CLOCK, WidgetType.STOPWATCH)
                "WEATHER" -> pack.category == WidgetType.WEATHER
                "PRODUCTIVITY" -> pack.category in setOf(WidgetType.TODO, WidgetType.POMODORO, WidgetType.COUNTDOWN, WidgetType.QUICK_LAUNCH, WidgetType.DAY_PROGRESS)
                "HEALTH" -> pack.category in setOf(WidgetType.WATER, WidgetType.STEPS, WidgetType.HABIT, WidgetType.CALORIE_DISTANCE, WidgetType.BREATHING, WidgetType.COUNTER, WidgetType.MOOD)
                "NOTES_CAL" -> pack.category in setOf(WidgetType.NOTE, WidgetType.CALENDAR_MONTH, WidgetType.QUOTE)
                "SYSTEM" -> pack.category in setOf(WidgetType.SYSTEM_STATS, WidgetType.BATTERY, WidgetType.NETWORK, WidgetType.DEVICE_INFO)
                "NEWS" -> pack.category == WidgetType.NEWS
                "COMPOSITE" -> pack.category == WidgetType.COMPOSITE
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                pack.title.contains(searchQuery, ignoreCase = true) ||
                pack.description.contains(searchQuery, ignoreCase = true) ||
                pack.author.contains(searchQuery, ignoreCase = true) ||
                pack.category.labelZh.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesCategory && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Store Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(158.dp)
                    .clip(RoundedCornerShape(26.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_store_hero),
                    contentDescription = "HWidget Store Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.72f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Text(
                        text = "HWidget Store (${catalog.size} 款不重複精選)",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "匯集 ${catalog.size} 款官方與第三方創作者獨家設計，一鍵下載安裝",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }

        // Search + Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    label = { Text("搜尋 ${catalog.size} 款不重複 Widget...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filterMode == "ALL",
                        onClick = { filterMode = "ALL" },
                        label = { Text("全部 (${catalog.size})") }
                    )
                    FilterChip(
                        selected = filterMode == "OFFICIAL",
                        onClick = { filterMode = "OFFICIAL" },
                        label = { Text("官方 (${catalog.count { it.isOfficial }})") }
                    )
                    FilterChip(
                        selected = filterMode == "THIRD_PARTY",
                        onClick = { filterMode = "THIRD_PARTY" },
                        label = { Text("第三方 (${catalog.count { !it.isOfficial }})") }
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categoryGroups, key = { it.first }) { (key, label) ->
                        FilterChip(
                            selected = categoryGroup == key,
                            onClick = { categoryGroup = key },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // Store Items
        items(filtered, key = { it.id }) { pack ->
            val isInstalled = pack.id in installedStoreIds
            GlassSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_item_${pack.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = pack.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (pack.isOfficial) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Verified,
                                        contentDescription = "官方認證",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${pack.author} · ${pack.themeStyle.labelZh} · ⬇ ${pack.downloadsCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("${pack.rating}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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

                    Text(
                        text = pack.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { onPinPack(pack) },
                            modifier = Modifier.testTag("store_pin_${pack.id}")
                        ) {
                            Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("釘選桌面")
                        }
                        if (isInstalled) {
                            FilledTonalButton(
                                onClick = { onInstallPack(pack) },
                                modifier = Modifier.testTag("store_reinstall_${pack.id}")
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("已安裝（再新增）")
                            }
                        } else {
                            Button(
                                onClick = { onInstallPack(pack) },
                                modifier = Modifier.testTag("store_install_${pack.id}")
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("下載安裝")
                            }
                        }
                    }
                }
            }
        }

        // Future Expansion Modules in Store
        item {
            Text(
                text = "未來系統擴充預留模組 (Plugins)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
        }

        items(HWidgetPluginRegistry.allPlugins(), key = { it.slotId.key }) { plugin ->
            val isMounted = plugin.slotId.key in mountedExtensions
            val preview = plugin.createPreview(isMounted)
            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(preview.slotId.displayName, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${preview.slotId.categoryLabel} · ${plugin.contractUri}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = preview.slotId.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = { onToggleExtensionMount(plugin.slotId.key) }) {
                        Text(if (isMounted) "已掛載" else "預先掛載")
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}
