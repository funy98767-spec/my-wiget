package com.example.domain.model

import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.squareup.moshi.JsonClass

enum class WidgetType(val labelZh: String, val iconTag: String) {
    CLOCK("時鐘", "clock"),
    WEATHER("天氣", "weather"),
    TODO("待辦事項", "todo"),
    NEWS("新聞 RSS", "news"),
    NOTE("便利貼筆記", "note"),
    COUNTDOWN("倒數日", "countdown"),
    BATTERY("電量監控", "battery"),
    STEPS("步數健康", "steps"),
    POMODORO("番茄專注", "pomodoro"),
    WATER("喝水追蹤", "water"),
    HABIT("習慣打卡", "habit"),
    SYSTEM_STATS("系統效能", "system"),
    QUOTE("每日語錄", "quote"),
    QUICK_LAUNCH("快捷工具", "quick"),
    CALENDAR_MONTH("月曆看板", "calendar"),
    WORLD_CLOCK("世界時鐘", "world_clock"),
    DAY_PROGRESS("今日進度", "day_progress"),
    STOPWATCH("碼錶計時", "stopwatch"),
    COUNTER("萬用計數器", "counter"),
    MOOD("今日心情", "mood"),
    BREATHING("呼吸引導", "breathing"),
    NETWORK("網路狀態", "network"),
    DEVICE_INFO("裝置資訊", "device_info"),
    CALORIE_DISTANCE("步行消耗", "calorie"),
    COMPOSITE("自由組合", "composite")
}

enum class WidgetThemeStyle(val labelZh: String, val subtitle: String) {
    MATERIAL("Material", "Material You 柔和層次"),
    GLASS("Glass", "毛玻璃擬態與透光邊框"),
    CYBERPUNK("Cyberpunk", "霓虹賽博格線光暈"),
    PIXEL("Pixel", "復古 8-bit 像素點陣"),
    MINIMAL("Minimal", "極簡純淨紙感留白"),
    DARK("Dark", "深邃 OLED 高對比")
}

enum class ClockDisplayStyle(val labelZh: String) {
    DIGITAL("數位時鐘"),
    FLIP("翻頁時鐘"),
    PIXEL("像素時鐘")
}

enum class WidgetFontStyle(val labelZh: String) {
    SANS("Jakarta 現代"),
    MONO("JetBrains 等寬"),
    SPACE("Grotesk 科技"),
    PIXEL("Silkscreen 像素");

    fun toFontFamily(): FontFamily = when (this) {
        SANS -> PlusJakartaFontFamily
        MONO -> JetBrainsMonoFontFamily
        SPACE -> SpaceGroteskFontFamily
        PIXEL -> SilkscreenFontFamily
    }
}

enum class BackgroundPreset(val labelZh: String) {
    NONE("主題預設"),
    AURORA("極光流彩"),
    CYBERPUNK("賽博霓虹"),
    MINIMAL_PAPER("暖陽紙紋"),
    CUSTOM_URI("相簿自訂圖片")
}

enum class BlockType(val labelZh: String) {
    TIME("時間"),
    WEATHER("天氣"),
    TODO("待辦"),
    BATTERY("電量"),
    IMAGE("圖片"),
    COUNTDOWN("倒數日"),
    STEPS("步數"),
    NOTE("筆記"),
    NEWS("新聞"),
    POMODORO("番茄鐘"),
    WATER("喝水"),
    HABIT("習慣打卡"),
    SYSTEM_STATS("系統監控"),
    QUOTE("語錄"),
    QUICK_LAUNCH("快捷列"),
    CALENDAR_MONTH("月曆"),
    WORLD_CLOCK("世界時鐘"),
    DAY_PROGRESS("今日進度"),
    STOPWATCH("碼錶"),
    COUNTER("計數器"),
    MOOD("今日心情"),
    BREATHING("呼吸引導"),
    NETWORK("網路狀態"),
    DEVICE_INFO("裝置資訊"),
    CALORIE_DISTANCE("步行消耗"),
    EXTENSION_SLOT("擴充模組")
}

enum class StickyNoteColor(val labelZh: String) {
    YELLOW("暖黃"),
    BLUE("天藍"),
    PINK("櫻粉"),
    GREEN("薄荷綠")
}

enum class CountdownCategory(val labelZh: String) {
    EXAM("考試 / 高考"),
    BIRTHDAY("生日"),
    TRAVEL("旅行"),
    CUSTOM("自訂")
}

@JsonClass(generateAdapter = true)
data class WidgetBlock(
    val id: String,
    val type: String, // BlockType.name
    val orderIndex: Int = 0,
    val customLabel: String = "",
    val imageUri: String? = null,
    val extensionId: String? = null,
    val spanFullWidth: Boolean = false
)

data class WeatherSnapshot(
    val cityName: String = "Taipei",
    val temperatureC: Double = 24.0,
    val conditionText: String = "多雲時晴",
    val weatherCode: Int = 2,
    val precipitationProb: Int = 15,
    val windKmh: Double = 11.2,
    val isLiveFromApi: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class BatterySnapshot(
    val levelPercent: Int = 85,
    val isCharging: Boolean = false,
    val temperatureC: Float = 31.5f,
    val statusLabel: String = "電池健康良好"
)

data class StepSnapshot(
    val stepsToday: Int = 4280,
    val dailyGoal: Int = 8000,
    val sensorAvailable: Boolean = false,
    val activityPermissionGranted: Boolean = false,
    val healthConnectInstalled: Boolean = false,
    val sourceLabel: String = "裝置計步器 / Health Connect"
)

data class StoreWidgetPack(
    val id: String,
    val title: String,
    val author: String,
    val isOfficial: Boolean,
    val category: WidgetType,
    val themeStyle: WidgetThemeStyle,
    val clockStyle: ClockDisplayStyle = ClockDisplayStyle.DIGITAL,
    val fontStyle: WidgetFontStyle = WidgetFontStyle.SANS,
    val backgroundPreset: BackgroundPreset = BackgroundPreset.NONE,
    val accentColorHex: Long = 0xFF4F46E5L,
    val textColorHex: Long = 0xFFFFFFFFL,
    val cornerRadius: Float = 24f,
    val opacity: Float = 0.88f,
    val blurRadius: Float = 14f,
    val downloadsCount: String,
    val rating: Double,
    val description: String,
    val blocks: List<WidgetBlock>
)
