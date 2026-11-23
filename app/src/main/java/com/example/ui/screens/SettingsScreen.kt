package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.device.ReminderNotifier
import com.example.data.device.StepMonitor
import com.example.data.prefs.UserSettings
import com.example.domain.model.StepSnapshot
import com.example.domain.model.WeatherSnapshot
import com.example.extension.HWidgetPluginRegistry
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    weather: WeatherSnapshot,
    steps: StepSnapshot,
    widgetCount: Int,
    onSetThemeMode: (String) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetDailyStepGoal: (Int) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onSetDefaultRefreshMinutes: (Int) -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenWeatherCityPicker: () -> Unit,
    onToggleExtensionMount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showArchitectureDialog by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onSendTestNotification()
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Flow updates automatically on resume */ }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "平台設定與擴充中樞",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Material You 外觀、提醒通知、Health Connect 與系統架構",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Appearance & Material You
        item {
            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("外觀與深色模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM" to "跟隨系統", "LIGHT" to "淺色模式", "DARK" to "深色模式").forEach { (key, label) ->
                            FilterChip(
                                selected = settings.themeMode == key,
                                onClick = { onSetThemeMode(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Material You 動態取色", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Android 12+ 自動跟隨桌布色彩調色盤",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.dynamicColorEnabled,
                            onCheckedChange = onSetDynamicColor
                        )
                    }
                }
            }
        }

        // Notifications
        item {
            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("待辦與倒數日通知", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("啟用到期與里程碑推播提醒", modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = onSetNotificationsEnabled
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !ReminderNotifier.hasNotificationPermission(context)
                                ) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSendTestNotification()
                                }
                            },
                            modifier = Modifier.testTag("settings_test_notification_button")
                        ) {
                            Text("發送測試提醒通知")
                        }
                    }
                }
            }
        }

        // Steps & Health Connect
        item {
            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DirectionsWalk, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("步數感測與 Health Connect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "狀態：${steps.sourceLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("每日步數目標：${settings.dailyStepGoal} 步", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = settings.dailyStepGoal.toFloat(),
                        onValueChange = { onSetDailyStepGoal((it / 500f).roundToInt() * 500) },
                        valueRange = 2000f..20000f
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !steps.activityPermissionGranted) {
                            Button(onClick = {
                                activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            }) {
                                Text("授權體能活動感測器")
                            }
                        }
                        OutlinedButton(onClick = {
                            runCatching { context.startActivity(StepMonitor.openHealthConnectIntent(context)) }
                        }) {
                            Text("開啟 Health Connect")
                        }
                    }
                }
            }
        }

        // Weather & Default Refresh
        item {
            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("天氣與背景更新頻率", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("目前城市：${weather.cityName}")
                        FilledTonalButton(onClick = onOpenWeatherCityPicker) {
                            Text("切換城市")
                        }
                    }
                    Text("預設 Widget 更新間隔", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 60, 360).forEach { mins ->
                            FilterChip(
                                selected = settings.defaultRefreshMinutes == mins,
                                onClick = { onSetDefaultRefreshMinutes(mins) },
                                label = { Text("${mins} 分鐘") }
                            )
                        }
                    }
                }
            }
        }

        // Reserved Plugin Extensions
        item {
            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("預留模組掛載管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    HWidgetPluginRegistry.allPlugins().forEach { plugin ->
                        val mounted = plugin.slotId.key in settings.mountedExtensions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plugin.slotId.displayName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    plugin.contractUri,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = mounted,
                                onCheckedChange = { onToggleExtensionMount(plugin.slotId.key) }
                            )
                        }
                    }
                }
            }
        }

        // Technical Architecture, SQLite Schema, WidgetProvider & APK Build Specs
        item {
            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("系統架構與 SQLite / APK 規格", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "資料庫：Room SQLite (hwidget_platform.db) · 已儲存 $widgetCount 個 Widget\n偏好設定：Jetpack DataStore Preferences\n桌面整合：HWidgetProvider & HWidgetClockProvider (AppWidgetProvider)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showArchitectureDialog = true },
                        modifier = Modifier.testTag("settings_view_schema_button")
                    ) {
                        Text("檢視 SQLite Schema 與 APK 建置說明")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showArchitectureDialog) {
        AlertDialog(
            onDismissRequest = { showArchitectureDialog = false },
            title = { Text("HWidget 技術規格與建置說明") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = """
                        1. SQLite Schema (Room / hwidget_platform.db):
                        • installed_widgets (id, title, widgetType, themeStyle, clockStyle, fontStyle, opacity, blurRadius, cornerRadius, accentColorHex, textColorHex, backgroundPreset, backgroundImageUri, animationsEnabled, refreshIntervalMinutes, blocksJson, isPinnedToHome, appWidgetId, sourceStoreId, isCustom, createdAt, updatedAt)
                        • todos (id, title, isCompleted, dueAtMillis, reminderEnabled, priority, createdAt)
                        • notes (id, title, content, colorKey, isPinned, updatedAt)
                        • countdowns (id, title, category, targetDateMillis, reminderEnabled, accentHex, createdAt)
                        • habits (id, title, streakDays, checkedToday, createdAt)
                        • rss_sources (id, name, feedUrl, autoUpdateMinutes, isActive, lastFetchedAt)
                        • news_articles (id, sourceId, sourceName, title, summary, link, publishedAtText, fetchedAt)
                        • store_installs (storeItemId, installedWidgetId, installedAt)

                        2. Android Widget Providers & XML:
                        • com.example.widget.HWidgetProvider -> @xml/hwidget_composite_info -> @layout/widget_hwidget_composite
                        • com.example.widget.HWidgetClockProvider -> @xml/hwidget_clock_info -> @layout/widget_hwidget_clock

                        3. APK 建置指令:
                        • Debug APK: gradle :app:assembleDebug
                        • Release APK: gradle :app:assembleRelease
                        • App Bundle: gradle :app:bundleRelease
                        """.trimIndent(),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showArchitectureDialog = false }) {
                    Text("關閉")
                }
            }
        )
    }
}
