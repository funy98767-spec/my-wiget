package com.example.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.db.BlockJsonCodec
import com.example.data.db.CountdownEntity
import com.example.data.db.HabitEntity
import com.example.data.db.NewsArticleEntity
import com.example.data.db.NoteEntity
import com.example.data.db.TodoEntity
import com.example.data.db.WidgetEntity
import com.example.data.device.SystemStatsSnapshot
import com.example.data.prefs.UserSettings
import com.example.domain.model.BackgroundPreset
import com.example.domain.model.BatterySnapshot
import com.example.domain.model.BlockType
import com.example.domain.model.ClockDisplayStyle
import com.example.domain.model.WeatherSnapshot
import com.example.domain.model.WidgetBlock
import com.example.domain.model.WidgetFontStyle
import com.example.domain.model.WidgetThemeStyle
import com.example.extension.HWidgetPluginRegistry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal data class LiveWidgetDataSnapshot(
    val settings: UserSettings,
    val battery: BatterySnapshot,
    val systemStats: SystemStatsSnapshot,
    val todos: List<TodoEntity>,
    val topNote: NoteEntity?,
    val countdowns: List<CountdownEntity>,
    val articles: List<NewsArticleEntity>,
    val habits: List<HabitEntity>
)

internal object WidgetBitmapRenderer {

    private data class Palette(
        val bgColors: IntArray,
        val ink: Int,
        val subInk: Int,
        val accent: Int,
        val borderColor: Int,
        val borderWidthDp: Float
    )

    private sealed class LayoutRow {
        data class Full(val block: WidgetBlock, val heightDp: Float) : LayoutRow()
        data class Pair(
            val left: WidgetBlock,
            val right: WidgetBlock,
            val heightDp: Float
        ) : LayoutRow()
    }

    private enum class IconKind {
        SUN, BATTERY, BATTERY_CHARGING, WALK, CHECK_CIRCLE, UNCHECK_CIRCLE,
        NOTE, EVENT, NEWS, IMAGE, EXTENSION, TIMER, DRINK, FIRE, MEMORY,
        QUOTE, BOLT, CALENDAR, GLOBE, SCHEDULE, MOOD, AIR, WIFI, WIFI_OFF,
        PHONE, PLAY, REPLAY, PLUS, MINUS
    }

    fun renderWidget(
        context: Context,
        widget: WidgetEntity?,
        snapshot: LiveWidgetDataSnapshot,
        targetWidthDp: Float = 340f,
        renderScale: Float = 2.5f
    ): Bitmap {
        val widthDp = targetWidthDp.coerceIn(200f, 420f)
        val themeStyle = runCatching {
            WidgetThemeStyle.valueOf(widget?.themeStyle ?: WidgetThemeStyle.GLASS.name)
        }.getOrDefault(WidgetThemeStyle.GLASS)
        val clockStyle = runCatching {
            ClockDisplayStyle.valueOf(widget?.clockStyle ?: ClockDisplayStyle.DIGITAL.name)
        }.getOrDefault(ClockDisplayStyle.DIGITAL)
        val fontStyle = runCatching {
            WidgetFontStyle.valueOf(widget?.fontStyle ?: WidgetFontStyle.SPACE.name)
        }.getOrDefault(WidgetFontStyle.SPACE)
        val bgPreset = runCatching {
            BackgroundPreset.valueOf(widget?.backgroundPreset ?: BackgroundPreset.AURORA.name)
        }.getOrDefault(BackgroundPreset.AURORA)

        val opacity = (widget?.opacity ?: 0.88f).coerceIn(0.15f, 1f)
        val blurRadius = widget?.blurRadius ?: 14f
        val cornerRadiusDp = (widget?.cornerRadius ?: 24f).coerceIn(4f, 40f)
        val customAccent = (widget?.accentColorHex ?: 0xFF4F46E5L).toInt()
        val customTextColor = (widget?.textColorHex ?: 0xFFFFFFFFL).toInt()
        val bgImageUri = widget?.backgroundImageUri

        val blocks = widget?.let { BlockJsonCodec.decode(it.blocksJson) }.orEmpty().ifEmpty {
            listOf(
                WidgetBlock(id = "f_time", type = BlockType.TIME.name, orderIndex = 0, spanFullWidth = true),
                WidgetBlock(id = "f_bat", type = BlockType.BATTERY.name, orderIndex = 1),
                WidgetBlock(id = "f_wea", type = BlockType.WEATHER.name, orderIndex = 2)
            )
        }

        val palette = resolvePalette(context, themeStyle, customTextColor, customAccent)
        val regularTypeface = loadTypeface(context, if (themeStyle == WidgetThemeStyle.PIXEL) WidgetFontStyle.PIXEL else fontStyle)
        val boldTypeface = Typeface.create(regularTypeface, Typeface.BOLD)
        val pixelTypeface = loadTypeface(context, WidgetFontStyle.PIXEL)

        val clockMillis = System.currentTimeMillis()
        val outerPadDp = 16f
        val blockGapDp = 10f
        val innerWidthDp = widthDp - outerPadDp * 2f
        val halfWidthDp = (innerWidthDp - blockGapDp) / 2f

        val fullBlockTypes = setOf(
            BlockType.TIME.name, BlockType.TODO.name, BlockType.NEWS.name, BlockType.NOTE.name,
            BlockType.QUOTE.name, BlockType.QUICK_LAUNCH.name, BlockType.SYSTEM_STATS.name,
            BlockType.HABIT.name, BlockType.CALENDAR_MONTH.name, BlockType.WORLD_CLOCK.name,
            BlockType.DAY_PROGRESS.name, BlockType.MOOD.name, BlockType.BREATHING.name,
            BlockType.DEVICE_INFO.name
        )

        fun isFull(b: WidgetBlock): Boolean = b.spanFullWidth || b.type in fullBlockTypes

        val rows = mutableListOf<LayoutRow>()
        var idx = 0
        while (idx < blocks.size) {
            val curr = blocks[idx]
            if (isFull(curr)) {
                val h = measureBlockHeightDp(
                    block = curr,
                    widthDp = innerWidthDp,
                    clockStyle = clockStyle,
                    regularTf = regularTypeface,
                    boldTf = boldTypeface,
                    snapshot = snapshot,
                    clockMillis = clockMillis
                )
                rows += LayoutRow.Full(curr, h)
                idx++
            } else {
                val next = blocks.getOrNull(idx + 1)
                if (next != null && !isFull(next)) {
                    val hLeft = measureBlockHeightDp(
                        block = curr,
                        widthDp = halfWidthDp,
                        clockStyle = clockStyle,
                        regularTf = regularTypeface,
                        boldTf = boldTypeface,
                        snapshot = snapshot,
                        clockMillis = clockMillis
                    )
                    val hRight = measureBlockHeightDp(
                        block = next,
                        widthDp = halfWidthDp,
                        clockStyle = clockStyle,
                        regularTf = regularTypeface,
                        boldTf = boldTypeface,
                        snapshot = snapshot,
                        clockMillis = clockMillis
                    )
                    rows += LayoutRow.Pair(curr, next, max(hLeft, hRight))
                    idx += 2
                } else {
                    val h = measureBlockHeightDp(
                        block = curr,
                        widthDp = innerWidthDp,
                        clockStyle = clockStyle,
                        regularTf = regularTypeface,
                        boldTf = boldTypeface,
                        snapshot = snapshot,
                        clockMillis = clockMillis
                    )
                    rows += LayoutRow.Full(curr, h)
                    idx++
                }
            }
        }

        val contentHeightDp = rows.sumOf { row ->
            when (row) {
                is LayoutRow.Full -> row.heightDp.toDouble()
                is LayoutRow.Pair -> row.heightDp.toDouble()
            }
        }.toFloat() + (rows.size - 1).coerceAtLeast(0) * blockGapDp
        val totalHeightDp = outerPadDp * 2f + contentHeightDp.coerceAtLeast(48f)

        val pxWidth = (widthDp * renderScale).roundToInt().coerceAtLeast(120)
        val pxHeight = (totalHeightDp * renderScale).roundToInt().coerceAtLeast(80)

        val output = Bitmap.createBitmap(pxWidth, pxHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.scale(renderScale, renderScale)

        val cardRect = RectF(0f, 0f, widthDp, totalHeightDp)
        val clipPath = Path().apply {
            addRoundRect(cardRect, cornerRadiusDp, cornerRadiusDp, Path.Direction.CW)
        }

        val saveCount = canvas.save()
        canvas.clipPath(clipPath)

        // Background Layer (matching opacity + preset/custom image + blur + frost scrim)
        drawBackgroundLayer(
            context = context,
            canvas = canvas,
            rect = cardRect,
            themeStyle = themeStyle,
            bgPreset = bgPreset,
            bgImageUri = bgImageUri,
            palette = palette,
            opacity = opacity,
            blurRadius = blurRadius
        )

        // Foreground Blocks
        var yDp = outerPadDp
        for (row in rows) {
            when (row) {
                is LayoutRow.Full -> {
                    val rect = RectF(outerPadDp, yDp, outerPadDp + innerWidthDp, yDp + row.heightDp)
                    drawBlock(
                        context = context,
                        canvas = canvas,
                        rect = rect,
                        block = row.block,
                        clockStyle = clockStyle,
                        regularTf = regularTypeface,
                        boldTf = boldTypeface,
                        pixelTf = pixelTypeface,
                        palette = palette,
                        snapshot = snapshot,
                        clockMillis = clockMillis
                    )
                    yDp += row.heightDp + blockGapDp
                }
                is LayoutRow.Pair -> {
                    val leftRect = RectF(outerPadDp, yDp, outerPadDp + halfWidthDp, yDp + row.heightDp)
                    val rightRect = RectF(outerPadDp + halfWidthDp + blockGapDp, yDp, outerPadDp + innerWidthDp, yDp + row.heightDp)
                    drawBlock(
                        context = context,
                        canvas = canvas,
                        rect = leftRect,
                        block = row.left,
                        clockStyle = clockStyle,
                        regularTf = regularTypeface,
                        boldTf = boldTypeface,
                        pixelTf = pixelTypeface,
                        palette = palette,
                        snapshot = snapshot,
                        clockMillis = clockMillis
                    )
                    drawBlock(
                        context = context,
                        canvas = canvas,
                        rect = rightRect,
                        block = row.right,
                        clockStyle = clockStyle,
                        regularTf = regularTypeface,
                        boldTf = boldTypeface,
                        pixelTf = pixelTypeface,
                        palette = palette,
                        snapshot = snapshot,
                        clockMillis = clockMillis
                    )
                    yDp += row.heightDp + blockGapDp
                }
            }
        }

        canvas.restoreToCount(saveCount)

        // Outer Border (exact borderWidth + borderColor)
        val halfBorder = palette.borderWidthDp / 2f
        val borderRect = RectF(halfBorder, halfBorder, widthDp - halfBorder, totalHeightDp - halfBorder)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = palette.borderWidthDp
            color = palette.borderColor
        }
        canvas.drawRoundRect(
            borderRect,
            (cornerRadiusDp - halfBorder).coerceAtLeast(1f),
            (cornerRadiusDp - halfBorder).coerceAtLeast(1f),
            borderPaint
        )

        return output
    }

    fun scaleForBinderPreview(bitmap: Bitmap, maxPixels: Int = 140_000): Bitmap {
        val currentPixels = bitmap.width * bitmap.height
        if (currentPixels <= maxPixels) return bitmap
        val ratio = sqrt(maxPixels.toDouble() / currentPixels.toDouble()).toFloat()
        val newW = (bitmap.width * ratio).roundToInt().coerceAtLeast(100)
        val newH = (bitmap.height * ratio).roundToInt().coerceAtLeast(60)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun drawBackgroundLayer(
        context: Context,
        canvas: Canvas,
        rect: RectF,
        themeStyle: WidgetThemeStyle,
        bgPreset: BackgroundPreset,
        bgImageUri: String?,
        palette: Palette,
        opacity: Float,
        blurRadius: Float
    ) {
        val alphaInt = (opacity * 255f).roundToInt().coerceIn(20, 255)
        val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = alphaInt
        }
        val bgSave = canvas.saveLayer(rect, layerPaint)

        val bgBitmap: Bitmap? = when (bgPreset) {
            BackgroundPreset.AURORA -> decodeSampledResource(context, R.drawable.img_bg_aurora, 420, 320)
            BackgroundPreset.CYBERPUNK -> decodeSampledResource(context, R.drawable.img_bg_cyberpunk, 420, 320)
            BackgroundPreset.MINIMAL_PAPER -> decodeSampledResource(context, R.drawable.img_bg_minimal_paper, 420, 320)
            BackgroundPreset.CUSTOM_URI -> bgImageUri?.let { decodeSampledUri(context, it, 420, 320) }
            BackgroundPreset.NONE -> null
        }

        if (bgBitmap != null) {
            val processed = if (blurRadius > 1f) applySoftBlur(bgBitmap, (blurRadius * 0.45f).coerceIn(1f, 16f)) else bgBitmap
            drawCropBitmap(canvas, processed, rect)
            if (processed !== bgBitmap) processed.recycle()
            bgBitmap.recycle()

            val scrimColor = if (themeStyle == WidgetThemeStyle.MINIMAL) {
                withAlpha(Color.WHITE, 0.25f)
            } else {
                withAlpha(Color.BLACK, 0.36f)
            }
            val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = scrimColor }
            canvas.drawRect(rect, scrimPaint)
        } else {
            val grad = LinearGradient(
                0f, 0f, rect.width(), rect.height(),
                palette.bgColors,
                null,
                Shader.TileMode.CLAMP
            )
            val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = grad }
            canvas.drawRect(rect, gradPaint)
        }

        canvas.restoreToCount(bgSave)
    }

    private fun resolvePalette(
        context: Context,
        style: WidgetThemeStyle,
        customTextColor: Int,
        customAccent: Int
    ): Palette {
        val nightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return when (style) {
            WidgetThemeStyle.MATERIAL -> {
                val pContainer = getSystemColorOrFallback(
                    context,
                    if (nightMode) android.R.color.system_accent1_800 else android.R.color.system_accent1_100,
                    if (nightMode) 0xFF312E81.toInt() else 0xFFE0E7FF.toInt()
                )
                val sContainer = getSystemColorOrFallback(
                    context,
                    if (nightMode) android.R.color.system_accent2_800 else android.R.color.system_accent2_100,
                    if (nightMode) 0xFF1E293B.toInt() else 0xFFDCE4FF.toInt()
                )
                val onContainer = getSystemColorOrFallback(
                    context,
                    if (nightMode) android.R.color.system_accent1_100 else android.R.color.system_accent1_900,
                    if (nightMode) 0xFFE0E7FF.toInt() else 0xFF1E1B4B.toInt()
                )
                val primary = getSystemColorOrFallback(
                    context,
                    if (nightMode) android.R.color.system_accent1_200 else android.R.color.system_accent1_600,
                    if (nightMode) 0xFFA5B4FC.toInt() else 0xFF4F46E5.toInt()
                )
                Palette(
                    bgColors = intArrayOf(pContainer, sContainer),
                    ink = onContainer,
                    subInk = withAlpha(onContainer, 0.72f),
                    accent = primary,
                    borderColor = withAlpha(onContainer, 0.25f),
                    borderWidthDp = 1f
                )
            }
            WidgetThemeStyle.GLASS -> Palette(
                bgColors = intArrayOf(
                    withAlpha(0xFF1E293B.toInt(), 0.82f),
                    withAlpha(0xFF312E81.toInt(), 0.72f),
                    withAlpha(0xFF0F172A.toInt(), 0.88f)
                ),
                ink = customTextColor,
                subInk = withAlpha(customTextColor, 0.72f),
                accent = customAccent,
                borderColor = withAlpha(Color.WHITE, 0.34f),
                borderWidthDp = 1.2f
            )
            WidgetThemeStyle.CYBERPUNK -> Palette(
                bgColors = intArrayOf(0xFF070812.toInt(), 0xFF1A103D.toInt()),
                ink = 0xFF00F5D4.toInt(),
                subInk = withAlpha(Color.WHITE, 0.78f),
                accent = 0xFFFF2A85.toInt(),
                borderColor = 0xFF00F5D4.toInt(),
                borderWidthDp = 1.6f
            )
            WidgetThemeStyle.PIXEL -> Palette(
                bgColors = intArrayOf(0xFF111827.toInt(), 0xFF1F2937.toInt()),
                ink = 0xFF4ADE80.toInt(),
                subInk = 0xFFA7F3D0.toInt(),
                accent = 0xFFFACC15.toInt(),
                borderColor = 0xFF4ADE80.toInt(),
                borderWidthDp = 2f
            )
            WidgetThemeStyle.MINIMAL -> Palette(
                bgColors = intArrayOf(0xFFFAF9F6.toInt(), 0xFFF1F5F9.toInt()),
                ink = 0xFF0F172A.toInt(),
                subInk = 0xFF64748B.toInt(),
                accent = customAccent,
                borderColor = 0xFFCBD5E1.toInt(),
                borderWidthDp = 1f
            )
            WidgetThemeStyle.DARK -> Palette(
                bgColors = intArrayOf(0xFF05070B.toInt(), 0xFF111827.toInt()),
                ink = 0xFFF8FAFC.toInt(),
                subInk = 0xFF94A3B8.toInt(),
                accent = customAccent,
                borderColor = withAlpha(Color.WHITE, 0.14f),
                borderWidthDp = 1f
            )
        }
    }

    private fun getSystemColorOrFallback(context: Context, resId: Int, fallback: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { ContextCompat.getColor(context, resId) }.getOrDefault(fallback)
        } else {
            fallback
        }
    }

    private fun loadTypeface(context: Context, style: WidgetFontStyle): Typeface {
        val resId = when (style) {
            WidgetFontStyle.SANS -> R.font.plus_jakarta_sans
            WidgetFontStyle.MONO -> R.font.jetbrains_mono
            WidgetFontStyle.SPACE -> R.font.space_grotesk
            WidgetFontStyle.PIXEL -> R.font.silkscreen
        }
        return runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull() ?: Typeface.DEFAULT
    }

    private fun measureBlockHeightDp(
        block: WidgetBlock,
        widthDp: Float,
        clockStyle: ClockDisplayStyle,
        regularTf: Typeface,
        boldTf: Typeface,
        snapshot: LiveWidgetDataSnapshot,
        clockMillis: Long
    ): Float {
        val type = runCatching { BlockType.valueOf(block.type) }.getOrDefault(BlockType.TIME)
        return when (type) {
            BlockType.TIME -> when (clockStyle) {
                ClockDisplayStyle.DIGITAL -> 68f
                ClockDisplayStyle.FLIP -> 78f
                ClockDisplayStyle.PIXEL -> 72f
            }
            BlockType.WEATHER, BlockType.BATTERY, BlockType.STEPS, BlockType.WATER,
            BlockType.NETWORK, BlockType.CALORIE_DISTANCE -> 74f
            BlockType.COUNTDOWN, BlockType.STOPWATCH, BlockType.COUNTER -> 68f
            BlockType.IMAGE -> 78f
            BlockType.EXTENSION_SLOT -> 58f
            BlockType.POMODORO -> 78f
            BlockType.SYSTEM_STATS -> 86f
            BlockType.BREATHING -> 52f
            BlockType.DEVICE_INFO -> 76f
            BlockType.WORLD_CLOCK -> 90f
            BlockType.DAY_PROGRESS -> 108f
            BlockType.TODO -> {
                val count = snapshot.todos.take(3).size.coerceAtLeast(1)
                42f + count * 22f
            }
            BlockType.HABIT -> {
                val count = snapshot.habits.take(3).size.coerceAtLeast(1)
                42f + count * 22f
            }
            BlockType.NOTE -> {
                val content = snapshot.topNote?.content ?: "點擊首頁便利貼卡片即可新增黃/藍/粉/綠彩色便籤。"
                val textH = measureTextHeight(content, widthDp - 24f, 12f, regularTf, 3)
                46f + textH
            }
            BlockType.NEWS -> {
                val headline = snapshot.articles.firstOrNull()?.title ?: "尚未同步新聞，點擊刷新 RSS 來源"
                val h1 = measureTextHeight(headline, widthDp - 20f, 13f, boldTf, 2)
                val extra = if (snapshot.articles.size >= 2) 18f else 0f
                44f + h1 + extra
            }
            BlockType.QUOTE -> {
                val quote = "「${snapshot.settings.quoteText}」"
                val qH = measureTextHeight(quote, widthDp - 22f, 13f, regularTf, 3)
                58f + qH
            }
            BlockType.QUICK_LAUNCH -> {
                val labels = listOf("記待辦", "便利貼", "喝水/打卡", "系統鬧鐘")
                val rowsNeeded = countChipRows(labels, widthDp - 20f, 11f, boldTf, 20f, 8f)
                42f + rowsNeeded * 28f + (rowsNeeded - 1).coerceAtLeast(0) * 6f
            }
            BlockType.MOOD -> {
                val moods = listOf("開心滿足", "專注高效", "平靜放鬆", "需要充電", "靈感爆發")
                val rowsNeeded = countChipRows(moods, widthDp - 20f, 11f, regularTf, 16f, 6f)
                42f + rowsNeeded * 26f + (rowsNeeded - 1).coerceAtLeast(0) * 6f
            }
            BlockType.CALENDAR_MONTH -> {
                val cal = Calendar.getInstance().apply { timeInMillis = clockMillis }
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDay = Calendar.getInstance().apply {
                    timeInMillis = clockMillis
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val startOffset = firstDay.get(Calendar.DAY_OF_WEEK) - 1
                val weekRows = ceil((startOffset + daysInMonth) / 7.0).toInt().coerceAtLeast(4)
                58f + weekRows * 24f
            }
        }
    }

    private fun drawBlock(
        context: Context,
        canvas: Canvas,
        rect: RectF,
        block: WidgetBlock,
        clockStyle: ClockDisplayStyle,
        regularTf: Typeface,
        boldTf: Typeface,
        pixelTf: Typeface,
        palette: Palette,
        snapshot: LiveWidgetDataSnapshot,
        clockMillis: Long
    ) {
        val type = runCatching { BlockType.valueOf(block.type) }.getOrDefault(BlockType.TIME)
        when (type) {
            BlockType.TIME -> drawClockBlock(canvas, rect, clockStyle, regularTf, boldTf, pixelTf, palette, clockMillis)
            BlockType.WEATHER -> drawWeatherBlock(canvas, rect, regularTf, boldTf, palette, snapshot.settings)
            BlockType.BATTERY -> drawBatteryBlock(canvas, rect, regularTf, boldTf, palette, snapshot.battery)
            BlockType.STEPS -> drawStepsBlock(canvas, rect, regularTf, boldTf, palette, snapshot.settings)
            BlockType.TODO -> drawTodoBlock(canvas, rect, regularTf, boldTf, palette, snapshot.todos)
            BlockType.NOTE -> drawNoteBlock(canvas, rect, regularTf, boldTf, snapshot.topNote)
            BlockType.COUNTDOWN -> drawCountdownBlock(canvas, rect, regularTf, boldTf, palette, snapshot.countdowns)
            BlockType.NEWS -> drawNewsBlock(canvas, rect, regularTf, boldTf, palette, snapshot.articles)
            BlockType.IMAGE -> drawImageBlock(context, canvas, rect, block, regularTf, palette)
            BlockType.POMODORO -> drawPomodoroBlock(canvas, rect, regularTf, boldTf, palette)
            BlockType.WATER -> drawWaterBlock(canvas, rect, regularTf, boldTf, palette, snapshot.settings)
            BlockType.HABIT -> drawHabitBlock(canvas, rect, regularTf, boldTf, palette, snapshot.habits)
            BlockType.SYSTEM_STATS -> drawSystemStatsBlock(canvas, rect, regularTf, boldTf, palette, snapshot.systemStats)
            BlockType.QUOTE -> drawQuoteBlock(canvas, rect, regularTf, boldTf, palette, snapshot.settings)
            BlockType.QUICK_LAUNCH -> drawQuickLaunchBlock(canvas, rect, boldTf, palette)
            BlockType.CALENDAR_MONTH -> drawCalendarMonthBlock(canvas, rect, regularTf, boldTf, palette, clockMillis)
            BlockType.WORLD_CLOCK -> drawWorldClockBlock(canvas, rect, regularTf, boldTf, palette, clockMillis)
            BlockType.DAY_PROGRESS -> drawDayProgressBlock(canvas, rect, regularTf, boldTf, palette, clockMillis)
            BlockType.STOPWATCH -> drawStopwatchBlock(canvas, rect, regularTf, boldTf, palette)
            BlockType.COUNTER -> drawCounterBlock(canvas, rect, regularTf, boldTf, palette, snapshot.settings.counterValue)
            BlockType.MOOD -> drawMoodBlock(canvas, rect, regularTf, boldTf, palette, snapshot.settings.moodTag)
            BlockType.BREATHING -> drawBreathingBlock(canvas, rect, regularTf, boldTf, palette, clockMillis)
            BlockType.NETWORK -> drawNetworkBlock(canvas, rect, regularTf, boldTf, palette, snapshot.systemStats)
            BlockType.DEVICE_INFO -> drawDeviceInfoBlock(canvas, rect, regularTf, boldTf, palette, snapshot.systemStats)
            BlockType.CALORIE_DISTANCE -> drawCalorieDistanceBlock(canvas, rect, regularTf, boldTf, palette, snapshot.settings)
            BlockType.EXTENSION_SLOT -> drawExtensionSlotBlock(canvas, rect, block, regularTf, boldTf, palette, snapshot.settings.mountedExtensions)
        }
    }

    private fun drawClockBlock(
        canvas: Canvas,
        rect: RectF,
        clockStyle: ClockDisplayStyle,
        regularTf: Typeface,
        boldTf: Typeface,
        pixelTf: Typeface,
        palette: Palette,
        clockMillis: Long
    ) {
        val date = Date(clockMillis)
        val hourStr = SimpleDateFormat("HH", Locale.getDefault()).format(date)
        val minStr = SimpleDateFormat("mm", Locale.getDefault()).format(date)
        val secStr = SimpleDateFormat("ss", Locale.getDefault()).format(date)
        val dateStr = SimpleDateFormat("yyyy/MM/dd EEEE", Locale.TRADITIONAL_CHINESE).format(date)

        when (clockStyle) {
            ClockDisplayStyle.DIGITAL -> {
                val mainPaint = textPaint(40f, palette.ink, boldTf).apply { letterSpacing = -0.025f }
                val mainText = "$hourStr:$minStr"
                val secPaint = textPaint(18f, palette.accent, boldTf)
                val mainY = drawTextBaseline(canvas, mainText, rect.left, rect.top + 38f, mainPaint)
                val mainW = mainPaint.measureText(mainText)
                drawTextBaseline(canvas, secStr, rect.left + mainW + 6f, mainY - 4f, secPaint)
                val datePaint = textPaint(12f, palette.subInk, regularTf)
                drawTextBaseline(canvas, dateStr, rect.left, rect.top + 58f, datePaint)
            }
            ClockDisplayStyle.FLIP -> {
                var x = rect.left
                x += drawFlipTile(canvas, x, rect.top, hourStr, boldTf, palette.ink, compact = false) + 8f
                val colonPaint = textPaint(26f, palette.accent, boldTf)
                drawTextBaseline(canvas, ":", x, rect.top + 34f, colonPaint)
                x += colonPaint.measureText(":") + 8f
                x += drawFlipTile(canvas, x, rect.top, minStr, boldTf, palette.ink, compact = false) + 8f
                drawFlipTile(canvas, x, rect.top + 6f, secStr, boldTf, palette.accent, compact = true)
                val datePaint = textPaint(11f, palette.subInk, regularTf)
                drawTextBaseline(canvas, dateStr, rect.left, rect.top + 70f, datePaint)
            }
            ClockDisplayStyle.PIXEL -> {
                drawSubCard(canvas, rect, 12f, withAlpha(Color.BLACK, 0.28f), withAlpha(palette.accent, 0.5f), 1f)
                val timePaint = textPaint(26f, palette.accent, pixelTf)
                drawTextBaseline(canvas, "$hourStr:$minStr:$secStr", rect.left + 12f, rect.top + 36f, timePaint)
                val engDate = SimpleDateFormat("MM-dd EEE", Locale.ENGLISH).format(date).uppercase()
                val subPaint = textPaint(11f, palette.subInk, pixelTf)
                drawTextBaseline(canvas, engDate, rect.left + 12f, rect.top + 56f, subPaint)
            }
        }
    }

    private fun drawFlipTile(
        canvas: Canvas,
        x: Float,
        y: Float,
        value: String,
        boldTf: Typeface,
        ink: Int,
        compact: Boolean
    ): Float {
        val textSize = if (compact) 18f else 28f
        val padH = if (compact) 10f else 14f
        val padV = if (compact) 6f else 8f
        val tp = textPaint(textSize, ink, boldTf)
        val textW = tp.measureText(value)
        val w = textW + padH * 2f
        val h = textSize * 1.15f + padV * 2f
        val tileRect = RectF(x, y, x + w, y + h)
        drawSubCard(canvas, tileRect, 12f, withAlpha(Color.BLACK, 0.42f), withAlpha(Color.WHITE, 0.16f), 1f)
        val centerY = y + h / 2f - (tp.descent() + tp.ascent()) / 2f
        canvas.drawText(value, x + padH, centerY, tp)
        val hingePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.WHITE, 0.14f)
            strokeWidth = 1f
        }
        canvas.drawLine(x + w * 0.05f, y + h / 2f, x + w * 0.95f, y + h / 2f, hingePaint)
        return w
    }

    private fun drawWeatherBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        settings: UserSettings
    ) {
        drawSubCard(canvas, rect, 14f, withAlpha(Color.WHITE, 0.10f))
        val p = 10f
        drawVectorIcon(canvas, IconKind.SUN, rect.left + p, rect.top + p, 18f, palette.accent)
        val city = settings.selectedCityName
        drawSingleLine(canvas, city, rect.left + p + 24f, rect.top + p + 13f, rect.width() - p * 2f - 24f, textPaint(11f, palette.subInk, regularTf))
        val raw = settings.cachedWeatherSummary.ifBlank { "$city 24°C · 多雲時晴 · 降雨 15%" }
        val withoutCity = raw.removePrefix("$city ").removePrefix("Taipei ").trim()
        val parts = withoutCity.split(" · 降雨 ")
        val tempAndCond = parts.firstOrNull().orEmpty().ifBlank { "24°C · 多雲時晴" }
        val rainProb = parts.getOrNull(1)?.let { "降雨機率 $it" } ?: "降雨機率 15%"
        drawSingleLine(canvas, tempAndCond, rect.left + p, rect.top + p + 36f, rect.width() - p * 2f, textPaint(15f, palette.ink, boldTf))
        drawSingleLine(canvas, rainProb, rect.left + p, rect.top + p + 54f, rect.width() - p * 2f, textPaint(11f, palette.subInk, regularTf))
    }

    private fun drawBatteryBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        battery: BatterySnapshot
    ) {
        drawSubCard(canvas, rect, 14f, withAlpha(Color.WHITE, 0.10f))
        val p = 10f
        drawVectorIcon(
            canvas,
            if (battery.isCharging) IconKind.BATTERY_CHARGING else IconKind.BATTERY,
            rect.left + p, rect.top + p, 18f, palette.accent
        )
        drawSingleLine(canvas, "${battery.levelPercent}%", rect.left + p + 22f, rect.top + p + 14f, rect.width() - p * 2f - 22f, textPaint(16f, palette.ink, boldTf))
        drawProgressBar(
            canvas,
            RectF(rect.left + p, rect.top + p + 26f, rect.right - p, rect.top + p + 31f),
            battery.levelPercent / 100f,
            palette.accent,
            withAlpha(palette.ink, 0.16f)
        )
        val sub = if (battery.isCharging) "充電中" else "${battery.temperatureC}°C"
        drawSingleLine(canvas, sub, rect.left + p, rect.top + p + 48f, rect.width() - p * 2f, textPaint(11f, palette.subInk, regularTf))
    }

    private fun drawStepsBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        settings: UserSettings
    ) {
        drawSubCard(canvas, rect, 14f, withAlpha(Color.WHITE, 0.10f))
        val p = 10f
        val pct = (settings.lastRecordedSteps.toFloat() / settings.dailyStepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
        drawVectorIcon(canvas, IconKind.WALK, rect.left + p, rect.top + p, 18f, palette.accent)
        drawSingleLine(canvas, "${settings.lastRecordedSteps} 步", rect.left + p + 22f, rect.top + p + 14f, rect.width() - p * 2f - 22f, textPaint(15f, palette.ink, boldTf))
        drawProgressBar(
            canvas,
            RectF(rect.left + p, rect.top + p + 26f, rect.right - p, rect.top + p + 31f),
            pct,
            palette.accent,
            withAlpha(palette.ink, 0.16f)
        )
        drawSingleLine(canvas, "目標 ${settings.dailyStepGoal} · ${(pct * 100).toInt()}%", rect.left + p, rect.top + p + 48f, rect.width() - p * 2f, textPaint(11f, palette.subInk, regularTf))
    }

    private fun drawTodoBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        todos: List<TodoEntity>
    ) {
        drawSubCard(canvas, rect, 14f, withAlpha(Color.WHITE, 0.09f))
        val p = 10f
        drawTextBaseline(canvas, "待辦事項", rect.left + p, rect.top + p + 11f, textPaint(11f, palette.accent, boldTf))
        if (todos.isEmpty()) {
            drawTextBaseline(canvas, "尚無待辦任務", rect.left + p, rect.top + p + 32f, textPaint(12f, palette.subInk, regularTf))
        } else {
            var y = rect.top + p + 20f
            todos.take(3).forEach { todo ->
                drawVectorIcon(
                    canvas,
                    if (todo.isCompleted) IconKind.CHECK_CIRCLE else IconKind.UNCHECK_CIRCLE,
                    rect.left + p, y, 15f,
                    if (todo.isCompleted) palette.accent else palette.subInk
                )
                val tp = textPaint(12f, if (todo.isCompleted) palette.subInk else palette.ink, regularTf).apply {
                    isStrikeThruText = todo.isCompleted
                }
                drawSingleLine(canvas, todo.title, rect.left + p + 21f, y + 12f, rect.width() - p * 2f - 21f, tp)
                y += 22f
            }
        }
    }

    private fun drawNoteBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        note: NoteEntity?
    ) {
        val (bg, ink) = when (note?.colorKey) {
            "BLUE" -> 0xFFBAE6FD.toInt() to 0xFF0C4A6E.toInt()
            "PINK" -> 0xFFFBCFE8.toInt() to 0xFF831843.toInt()
            "GREEN" -> 0xFFBBF7D0.toInt() to 0xFF14532D.toInt()
            else -> 0xFFFEF08A.toInt() to 0xFF713F12.toInt()
        }
        drawSubCard(canvas, rect, 14f, bg)
        val p = 12f
        drawVectorIcon(canvas, IconKind.NOTE, rect.left + p, rect.top + p, 16f, ink)
        drawSingleLine(canvas, note?.title ?: "桌面便利貼", rect.left + p + 22f, rect.top + p + 13f, rect.width() - p * 2f - 22f, textPaint(13f, ink, boldTf))
        val body = note?.content ?: "點擊首頁便利貼卡片即可新增黃/藍/粉/綠彩色便籤。"
        drawMultilineText(canvas, body, rect.left + p, rect.top + p + 24f, rect.width() - p * 2f, textPaint(12f, withAlpha(ink, 0.88f), regularTf), 3)
    }

    private fun drawCountdownBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        countdowns: List<CountdownEntity>
    ) {
        drawSubCard(canvas, rect, 14f, withAlpha(Color.WHITE, 0.10f))
        val p = 10f
        val item = countdowns.firstOrNull()
        val daysLeft = if (item != null) {
            TimeUnit.MILLISECONDS.toDays(item.targetDateMillis - System.currentTimeMillis()).coerceAtLeast(0)
        } else 0L
        drawVectorIcon(canvas, IconKind.EVENT, rect.left + p, rect.top + p, 16f, palette.accent)
        drawSingleLine(canvas, item?.title ?: "重要倒數日", rect.left + p + 20f, rect.top + p + 12f, rect.width() - p * 2f - 20f, textPaint(11f, palette.subInk, regularTf))
        drawSingleLine(canvas, if (item != null) "還有 $daysLeft 天" else "未設定", rect.left + p, rect.top + p + 38f, rect.width() - p * 2f, textPaint(16f, palette.ink, boldTf))
    }

    private fun drawNewsBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        articles: List<NewsArticleEntity>
    ) {
        drawSubCard(canvas, rect, 14f, withAlpha(Color.WHITE, 0.09f))
        val p = 10f
        val first = articles.firstOrNull()
        drawVectorIcon(canvas, IconKind.NEWS, rect.left + p, rect.top + p, 16f, palette.accent)
        drawSingleLine(canvas, first?.sourceName ?: "RSS 即時新聞", rect.left + p + 22f, rect.top + p + 12f, rect.width() - p * 2f - 22f, textPaint(11f, palette.accent, boldTf))
        val headline = first?.title ?: "尚未同步新聞，點擊刷新 RSS 來源"
        val hUsed = drawMultilineText(canvas, headline, rect.left + p, rect.top + p + 22f, rect.width() - p * 2f, textPaint(13f, palette.ink, boldTf), 2)
        articles.getOrNull(1)?.let { second ->
            drawSingleLine(canvas, "• ${second.title}", rect.left + p, rect.top + p + 22f + hUsed + 14f, rect.width() - p * 2f, textPaint(11f, palette.subInk, regularTf))
        }
    }

    private fun drawImageBlock(
        context: Context,
        canvas: Canvas,
        rect: RectF,
        block: WidgetBlock,
        regularTf: Typeface,
        palette: Palette
    ) {
        val clip = Path().apply { addRoundRect(rect, 14f, 14f, Path.Direction.CW) }
        val save = canvas.save()
        canvas.clipPath(clip)
        drawSubCard(canvas, rect, 14f, withAlpha(Color.WHITE, 0.12f))
        val bmp = block.imageUri?.let { decodeSampledUri(context, it, 400, 220) }
        if (bmp != null) {
            drawCropBitmap(canvas, bmp, rect)
            bmp.recycle()
        } else {
            val label = block.customLabel.ifBlank { "自訂圖片積木" }
            val tp = textPaint(12f, palette.subInk, regularTf)
            val tw = tp.measureText(label)
            val totalW = 18f + 6f + tw
            val sx = rect.centerX() - totalW / 2f
            drawVectorIcon(canvas, IconKind.IMAGE, sx, rect.centerY() - 9f, 18f, palette.ink)
            drawTextBaseline(canvas, label, sx + 24f, rect.centerY() + 4f, tp)
        }
        canvas.restoreToCount(save)
    }

    private fun drawPomodoroBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette
    ) {
        drawSubCard(canvas, rect, 14f, withAlpha(Color.WHITE, 0.10f))
        val p = 10f
        drawVectorIcon(canvas, IconKind.TIMER, rect.left + p, rect.top + p, 15f, palette.accent)
        drawSingleLine(canvas, "25 分鐘番茄鐘", rect.left + p + 20f, rect.top + p + 12f, rect.width() - 96f, textPaint(11f, palette.subInk, regularTf))
        drawTextBaseline(canvas, "25:00", rect.left + p, rect.top + p + 38f, textPaint(22f, palette.ink, boldTf))
        drawProgressBar(
            canvas,
            RectF(rect.left + p, rect.top + p + 48f, rect.right - 90f, rect.top + p + 52f),
            0.12f,
            palette.accent,
            withAlpha(palette.ink, 0.15f)
        )
        drawCircleIconButton(canvas, rect.right - 68f, rect.centerY(), 17f, withAlpha(palette.accent, 0.24f), IconKind.PLAY, palette.ink)
        drawCircleIconButton(canvas, rect.right - 28f, rect.centerY(), 17f, withAlpha(Color.WHITE, 0.12f), IconKind.REPLAY, palette.subInk)
    }

    private fun drawWaterBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        settings: UserSettings
    ) {
        drawSubCard(canvas, rect, 14f, withAlpha(Color.WHITE, 0.10f))
        val p = 10f
        val pct = (settings.waterMlToday.toFloat() / settings.waterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
        drawVectorIcon(canvas, IconKind.DRINK, rect.left + p, rect.top + p, 16f, palette.accent)
        drawSingleLine(canvas, "${settings.waterMlToday}ml", rect.left + p + 20f, rect.top + p + 13f, rect.width() - 78f, textPaint(15f, palette.ink, boldTf))
        val pillW = 44f
        val pillRect = RectF(rect.right - p - pillW, rect.top + p - 1f, rect.right - p, rect.top + p + 17f)
        drawSubCard(canvas, pillRect, 9f, withAlpha(palette.accent, 0.25f))
        drawTextBaseline(canvas, "+250", pillRect.left + 9f, pillRect.top + 12f, textPaint(10f, palette.ink, boldTf))
        drawProgressBar(
            canvas,
            RectF(rect.left + p, rect.top + p + 25f, rect.right - p, rect.top + p + 30f),
            pct,
            palette.accent,
            withAlpha(palette.ink, 0.16f)
        )
        drawSingleLine(canvas, "飲水目標 ${settings.waterGoalMl}ml", rect.left + p, rect.top + p + 47f, rect.width() - p * 2f, textPaint(10f, palette.subInk, regularTf))
    }

    private fun drawHabitBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        habits: List<HabitEntity>
    ) {
        drawSubCard(canvas, rect, 14f, withAlpha(Color.WHITE, 0.09f))
        val p = 10f
        drawVectorIcon(canvas, IconKind.FIRE, rect.left + p, rect.top + p, 15f, palette.accent)
        drawTextBaseline(canvas, "每日習慣打卡", rect.left + p + 19f, rect.top + p + 11f, textPaint(11f, palette.accent, boldTf))
        if (habits.isEmpty()) {
            drawTextBaseline(canvas, "尚未設定習慣", rect.left + p, rect.top + p + 32f, textPaint(12f, palette.subInk, regularTf))
        } else {
            var y = rect.top + p + 20f
            habits.take(3).forEach { habit ->
                drawVectorIcon(
                    canvas,
                    if (habit.checkedToday) IconKind.CHECK_CIRCLE else IconKind.UNCHECK_CIRCLE,
                    rect.left + p, y, 15f,
                    if (habit.checkedToday) palette.accent else palette.subInk
                )
                val streak = "🔥${habit.streakDays}天"
                val streakPaint = textPaint(11f, palette.accent, boldTf)
                val streakW = streakPaint.measureText(streak)
                drawSingleLine(canvas, habit.title, rect.left + p + 21f, y + 12f, rect.width() - p * 2f - 28f - streakW, textPaint(12f, palette.ink, regularTf))
                drawTextBaseline(canvas, streak, rect.right - p - streakW, y + 12f, streakPaint)
                y += 22f
            }
        }
    }

    private fun drawSystemStatsBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        stats: SystemStatsSnapshot
    ) {
        drawSubCard(canvas, rect, 14f, withAlpha(Color.WHITE, 0.10f))
        val p = 10f
        drawVectorIcon(canvas, IconKind.MEMORY, rect.left + p, rect.top + p, 15f, palette.accent)
        drawTextBaseline(canvas, "系統記憶體與儲存空間", rect.left + p + 20f, rect.top + p + 11f, textPaint(11f, palette.accent, boldTf))

        val colW = (rect.width() - p * 2f - 10f) / 2f
        val leftX = rect.left + p
        val rightX = leftX + colW + 10f

        drawSingleLine(canvas, "RAM ${stats.ramUsagePercent}%", leftX, rect.top + p + 32f, colW, textPaint(12f, palette.ink, boldTf))
        drawProgressBar(canvas, RectF(leftX, rect.top + p + 39f, leftX + colW, rect.top + p + 43f), stats.ramUsagePercent / 100f, palette.accent, withAlpha(palette.ink, 0.15f))
        drawSingleLine(canvas, "${stats.usedRamMb}/${stats.totalRamMb} MB", leftX, rect.top + p + 58f, colW, textPaint(10f, palette.subInk, regularTf))

        drawSingleLine(canvas, "空間剩 ${stats.freeStorageGb}GB", rightX, rect.top + p + 32f, colW, textPaint(12f, palette.ink, boldTf))
        drawProgressBar(canvas, RectF(rightX, rect.top + p + 39f, rightX + colW, rect.top + p + 43f), stats.storageUsagePercent / 100f, palette.accent, withAlpha(palette.ink, 0.15f))
        drawSingleLine(canvas, "已用 ${stats.storageUsagePercent}% / ${stats.totalStorageGb}GB", rightX, rect.top + p + 58f, colW, textPaint(10f, palette.subInk, regularTf))
    }

    private fun drawQuoteBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        settings: UserSettings
    ) {
        drawSubCard(canvas, rect, 14f, withAlpha(Color.WHITE, 0.09f))
        val p = 11f
        drawVectorIcon(canvas, IconKind.QUOTE, rect.left + p, rect.top + p, 18f, palette.accent)
        val qH = drawMultilineText(canvas, "「${settings.quoteText}」", rect.left + p, rect.top + p + 22f, rect.width() - p * 2f, textPaint(13f, palette.ink, boldTf), 3)
        drawSingleLine(canvas, "— ${settings.quoteAuthor}", rect.left + p, rect.top + p + 26f + qH + 10f, rect.width() - p * 2f, textPaint(11f, palette.subInk, regularTf))
    }

    private fun drawQuickLaunchBlock(
        canvas: Canvas,
        rect: RectF,
        boldTf: Typeface,
        palette: Palette
    ) {
        drawSubCard(canvas, rect, 14f, withAlpha(Color.WHITE, 0.09f))
        val p = 10f
        drawVectorIcon(canvas, IconKind.BOLT, rect.left + p, rect.top + p, 15f, palette.accent)
        drawTextBaseline(canvas, "快速啟動列", rect.left + p + 19f, rect.top + p + 11f, textPaint(11f, palette.accent, boldTf))
        val labels = listOf("記待辦", "便利貼", "喝水/打卡", "系統鬧鐘")
        drawChipFlow(
            canvas = canvas,
            startX = rect.left + p,
            startY = rect.top + p + 22f,
            maxW = rect.width() - p * 2f,
            labels = labels,
            selected = null,
            textSizeSp = 11f,
            tf = boldTf,
            padH = 10f,
            padV = 6f,
            gap = 8f,
            ink = palette.ink,
            fillColor = withAlpha(palette.accent, 0.22f),
            selectedFillColor = palette.accent,
            selectedBorderColor = null
        )
    }

    private fun drawCalendarMonthBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        clockMillis: Long
    ) {
        drawSubCard(canvas, rect, 16f, withAlpha(palette.ink, 0.08f))
        val p = 10f
        val cal = Calendar.getInstance().apply { timeInMillis = clockMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstCal = Calendar.getInstance().apply {
            timeInMillis = clockMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val startOffset = firstCal.get(Calendar.DAY_OF_WEEK) - 1

        drawVectorIcon(canvas, IconKind.CALENDAR, rect.left + p, rect.top + p, 16f, palette.accent)
        drawTextBaseline(canvas, "${year}年 ${month}月", rect.left + p + 22f, rect.top + p + 13f, textPaint(13f, palette.ink, boldTf))

        val colW = (rect.width() - p * 2f) / 7f
        val headers = listOf("日", "一", "二", "三", "四", "五", "六")
        val hdrPaint = textPaint(10f, palette.subInk, regularTf).apply { textAlign = Paint.Align.CENTER }
        headers.forEachIndexed { idx, label ->
            canvas.drawText(label, rect.left + p + colW * (idx + 0.5f), rect.top + p + 32f, hdrPaint)
        }

        val cells = List(startOffset) { 0 } + (1..daysInMonth).toList()
        val weekRows = cells.chunked(7)
        var rowY = rect.top + p + 40f
        weekRows.forEach { week ->
            for (c in 0 until 7) {
                val day = week.getOrNull(c) ?: 0
                if (day > 0) {
                    val cx = rect.left + p + colW * (c + 0.5f)
                    val cy = rowY + 11f
                    val isToday = day == today
                    if (isToday) {
                        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent }
                        canvas.drawCircle(cx, cy, 10f, circlePaint)
                    }
                    val dpPaint = textPaint(
                        11f,
                        if (isToday) Color.WHITE else palette.ink,
                        if (isToday) boldTf else regularTf
                    ).apply { textAlign = Paint.Align.CENTER }
                    val baseline = cy - (dpPaint.descent() + dpPaint.ascent()) / 2f
                    canvas.drawText("$day", cx, baseline, dpPaint)
                }
            }
            rowY += 24f
        }
    }

    private fun drawWorldClockBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        clockMillis: Long
    ) {
        drawSubCard(canvas, rect, 16f, withAlpha(palette.ink, 0.08f))
        val p = 10f
        drawVectorIcon(canvas, IconKind.GLOBE, rect.left + p, rect.top + p, 16f, palette.accent)
        drawTextBaseline(canvas, "世界時鐘", rect.left + p + 22f, rect.top + p + 12f, textPaint(11f, palette.subInk, boldTf))

        val zones = listOf(
            "東京 JST" to "Asia/Tokyo",
            "首爾 KST" to "Asia/Seoul",
            "倫敦 UTC" to "Europe/London",
            "紐約 EST" to "America/New_York"
        )
        val gap = 8f
        val tileW = (rect.width() - p * 2f - gap * 3f) / 4f
        val tileTop = rect.top + p + 22f
        val tileBottom = rect.bottom - p
        zones.forEachIndexed { idx, (city, tzId) ->
            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone(tzId)
            }
            val left = rect.left + p + idx * (tileW + gap)
            val tileRect = RectF(left, tileTop, left + tileW, tileBottom)
            drawSubCard(canvas, tileRect, 10f, withAlpha(palette.ink, 0.07f))
            val cityP = textPaint(9f, palette.subInk, regularTf).apply { textAlign = Paint.Align.CENTER }
            val timeP = textPaint(14f, palette.ink, boldTf).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText(city, tileRect.centerX(), tileTop + 16f, cityP)
            canvas.drawText(fmt.format(Date(clockMillis)), tileRect.centerX(), tileTop + 36f, timeP)
        }
    }

    private fun drawDayProgressBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        clockMillis: Long
    ) {
        drawSubCard(canvas, rect, 16f, withAlpha(palette.ink, 0.08f))
        val p = 10f
        val cal = Calendar.getInstance().apply { timeInMillis = clockMillis }
        val minutesToday = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val dayProgress = (minutesToday / 1440f).coerceIn(0f, 1f)
        val remainHours = (1440 - minutesToday) / 60f
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_YEAR).coerceAtLeast(365)
        val yearProgress = (dayOfYear.toFloat() / maxDays).coerceIn(0f, 1f)

        drawVectorIcon(canvas, IconKind.SCHEDULE, rect.left + p, rect.top + p, 16f, palette.accent)
        drawTextBaseline(canvas, "今日與年度進度", rect.left + p + 22f, rect.top + p + 12f, textPaint(11f, palette.subInk, boldTf))

        drawTextBaseline(canvas, "今日已過 ${(dayProgress * 100).toInt()}%", rect.left + p, rect.top + p + 32f, textPaint(12f, palette.ink, boldTf))
        val remText = "剩 ${String.format("%.1f", remainHours)} 小時"
        val remP = textPaint(11f, palette.subInk, regularTf)
        drawTextBaseline(canvas, remText, rect.right - p - remP.measureText(remText), rect.top + p + 32f, remP)
        drawProgressBar(canvas, RectF(rect.left + p, rect.top + p + 38f, rect.right - p, rect.top + p + 43f), dayProgress, palette.accent, withAlpha(palette.ink, 0.16f))

        drawTextBaseline(canvas, "今年進度 ${(yearProgress * 100).toInt()}%", rect.left + p, rect.top + p + 64f, textPaint(11f, palette.ink, regularTf))
        val dayCountText = "第 $dayOfYear/$maxDays 天"
        drawTextBaseline(canvas, dayCountText, rect.right - p - remP.measureText(dayCountText), rect.top + p + 64f, remP)
        drawProgressBar(canvas, RectF(rect.left + p, rect.top + p + 71f, rect.right - p, rect.top + p + 75f), yearProgress, withAlpha(palette.accent, 0.8f), withAlpha(palette.ink, 0.16f))
    }

    private fun drawStopwatchBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette
    ) {
        drawSubCard(canvas, rect, 16f, withAlpha(palette.ink, 0.08f))
        val p = 10f
        drawVectorIcon(canvas, IconKind.TIMER, rect.left + p, rect.top + p, 16f, palette.accent)
        drawTextBaseline(canvas, "碼錶計時", rect.left + p + 21f, rect.top + p + 12f, textPaint(11f, palette.subInk, boldTf))
        drawTextBaseline(canvas, "00:00", rect.left + p, rect.top + p + 42f, textPaint(20f, palette.ink, boldTf))
        drawCircleIconButton(canvas, rect.right - p - 46f, rect.top + p + 34f, 14f, withAlpha(palette.accent, 0.25f), IconKind.PLAY, palette.ink)
        drawCircleIconButton(canvas, rect.right - p - 14f, rect.top + p + 34f, 14f, withAlpha(palette.ink, 0.12f), IconKind.REPLAY, palette.ink)
    }

    private fun drawCounterBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        counterValue: Int
    ) {
        drawSubCard(canvas, rect, 16f, withAlpha(palette.ink, 0.08f))
        val p = 10f
        drawTextBaseline(canvas, "萬用計數器", rect.left + p, rect.top + p + 12f, textPaint(11f, palette.subInk, boldTf))
        val resetP = textPaint(11f, palette.accent, boldTf)
        drawTextBaseline(canvas, "歸零", rect.right - p - resetP.measureText("歸零"), rect.top + p + 12f, resetP)
        drawTextBaseline(canvas, "$counterValue", rect.left + p, rect.top + p + 44f, textPaint(22f, palette.ink, boldTf))
        drawCircleIconButton(canvas, rect.right - p - 46f, rect.top + p + 34f, 14f, withAlpha(palette.ink, 0.12f), IconKind.MINUS, palette.ink)
        drawCircleIconButton(canvas, rect.right - p - 14f, rect.top + p + 34f, 14f, withAlpha(palette.accent, 0.28f), IconKind.PLUS, palette.ink)
    }

    private fun drawMoodBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        selectedMood: String
    ) {
        drawSubCard(canvas, rect, 16f, withAlpha(palette.ink, 0.08f))
        val p = 10f
        drawVectorIcon(canvas, IconKind.MOOD, rect.left + p, rect.top + p, 16f, palette.accent)
        drawSingleLine(canvas, "今日心情：$selectedMood", rect.left + p + 22f, rect.top + p + 12f, rect.width() - p * 2f - 22f, textPaint(12f, palette.ink, boldTf))
        val moods = listOf("開心滿足", "專注高效", "平靜放鬆", "需要充電", "靈感爆發")
        drawChipFlow(
            canvas = canvas,
            startX = rect.left + p,
            startY = rect.top + p + 22f,
            maxW = rect.width() - p * 2f,
            labels = moods,
            selected = selectedMood,
            textSizeSp = 11f,
            tf = regularTf,
            padH = 8f,
            padV = 4f,
            gap = 6f,
            ink = palette.ink,
            fillColor = withAlpha(palette.ink, 0.09f),
            selectedFillColor = withAlpha(palette.accent, 0.30f),
            selectedBorderColor = palette.accent
        )
    }

    private fun drawBreathingBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        clockMillis: Long
    ) {
        drawSubCard(canvas, rect, 16f, withAlpha(palette.ink, 0.08f))
        val p = 10f
        val cycleSec = ((clockMillis / 1000L) % 12L).toInt()
        val (phaseLabel, phaseProgress, stepSec) = when {
            cycleSec < 4 -> Triple("深呼吸 · 緩慢吸氣", (cycleSec + 1) / 4f, cycleSec + 1)
            cycleSec < 8 -> Triple("保持靜心 · 屏息 4 秒", (cycleSec - 3) / 4f, cycleSec - 3)
            else -> Triple("放鬆肩膀 · 緩慢吐氣", (cycleSec - 7) / 4f, cycleSec - 7)
        }
        drawVectorIcon(canvas, IconKind.AIR, rect.left + p, rect.top + p, 16f, palette.accent)
        val stepStr = "$stepSec/4s"
        val stepP = textPaint(11f, palette.accent, boldTf)
        val stepW = stepP.measureText(stepStr)
        drawSingleLine(canvas, phaseLabel, rect.left + p + 22f, rect.top + p + 12f, rect.width() - p * 2f - 28f - stepW, textPaint(12f, palette.ink, boldTf))
        drawTextBaseline(canvas, stepStr, rect.right - p - stepW, rect.top + p + 12f, stepP)
        drawProgressBar(canvas, RectF(rect.left + p, rect.top + p + 24f, rect.right - p, rect.top + p + 29f), phaseProgress, palette.accent, withAlpha(palette.ink, 0.16f))
    }

    private fun drawNetworkBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        stats: SystemStatsSnapshot
    ) {
        drawSubCard(canvas, rect, 16f, withAlpha(palette.ink, 0.08f))
        val p = 10f
        drawVectorIcon(canvas, if (stats.isOnline) IconKind.WIFI else IconKind.WIFI_OFF, rect.left + p, rect.top + p, 16f, palette.accent)
        drawSingleLine(canvas, "網路連線", rect.left + p + 21f, rect.top + p + 12f, rect.width() - p * 2f - 21f, textPaint(11f, palette.subInk, regularTf))
        drawSingleLine(canvas, stats.networkLabel, rect.left + p, rect.top + p + 34f, rect.width() - p * 2f, textPaint(14f, palette.ink, boldTf))
        drawSingleLine(canvas, if (stats.isOnline) "即時同步就緒" else "離線模式運行中", rect.left + p, rect.top + p + 50f, rect.width() - p * 2f, textPaint(11f, palette.subInk, regularTf))
    }

    private fun drawDeviceInfoBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        stats: SystemStatsSnapshot
    ) {
        drawSubCard(canvas, rect, 16f, withAlpha(palette.ink, 0.08f))
        val p = 10f
        drawVectorIcon(canvas, IconKind.PHONE, rect.left + p, rect.top + p, 16f, palette.accent)
        drawTextBaseline(canvas, "裝置與系統規格", rect.left + p + 22f, rect.top + p + 12f, textPaint(11f, palette.subInk, boldTf))
        val verP = textPaint(12f, palette.accent, boldTf)
        val verW = verP.measureText(stats.androidVersion)
        drawSingleLine(canvas, stats.deviceModel, rect.left + p, rect.top + p + 33f, rect.width() - p * 2f - verW - 8f, textPaint(13f, palette.ink, boldTf))
        drawTextBaseline(canvas, stats.androidVersion, rect.right - p - verW, rect.top + p + 33f, verP)
        val hrs = stats.uptimeMinutes / 60
        val mins = stats.uptimeMinutes % 60
        drawSingleLine(canvas, "已連續運行 ${hrs}小時 ${mins}分 · 儲存剩 ${stats.freeStorageGb} GB", rect.left + p, rect.top + p + 50f, rect.width() - p * 2f, textPaint(11f, palette.subInk, regularTf))
    }

    private fun drawCalorieDistanceBlock(
        canvas: Canvas,
        rect: RectF,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        settings: UserSettings
    ) {
        drawSubCard(canvas, rect, 16f, withAlpha(palette.ink, 0.08f))
        val p = 10f
        val distanceKm = settings.lastRecordedSteps * 0.00075f
        val kcal = (settings.lastRecordedSteps * 0.04f).toInt()
        val remaining = (settings.dailyStepGoal - settings.lastRecordedSteps).coerceAtLeast(0)
        drawVectorIcon(canvas, IconKind.FIRE, rect.left + p, rect.top + p, 16f, palette.accent)
        drawSingleLine(canvas, "步行消耗", rect.left + p + 21f, rect.top + p + 12f, rect.width() - p * 2f - 21f, textPaint(11f, palette.subInk, regularTf))
        drawSingleLine(canvas, "$kcal kcal", rect.left + p, rect.top + p + 36f, rect.width() - p * 2f, textPaint(18f, palette.ink, boldTf))
        drawSingleLine(canvas, "${String.format("%.2f", distanceKm)} km · 距目標 $remaining 步", rect.left + p, rect.top + p + 52f, rect.width() - p * 2f, textPaint(11f, palette.subInk, regularTf))
    }

    private fun drawExtensionSlotBlock(
        canvas: Canvas,
        rect: RectF,
        block: WidgetBlock,
        regularTf: Typeface,
        boldTf: Typeface,
        palette: Palette,
        mountedExtensions: Set<String>
    ) {
        drawSubCard(canvas, rect, 14f, withAlpha(palette.accent, 0.16f), withAlpha(palette.accent, 0.45f), 1f)
        val p = 10f
        val plugin = HWidgetPluginRegistry.getPlugin(block.extensionId)
        val isMounted = block.extensionId != null && block.extensionId in mountedExtensions
        val preview = plugin?.createPreview(isMounted)
        drawVectorIcon(canvas, IconKind.EXTENSION, rect.left + p, rect.top + p, 15f, palette.accent)
        drawSingleLine(canvas, preview?.slotId?.displayName ?: "擴充模組插槽", rect.left + p + 21f, rect.top + p + 12f, rect.width() - p * 2f - 21f, textPaint(12f, palette.ink, boldTf))
        drawSingleLine(canvas, preview?.headline ?: "等待掛載擴充模組", rect.left + p, rect.top + p + 32f, rect.width() - p * 2f, textPaint(11f, palette.subInk, regularTf))
    }

    // ---- Drawing Primitives ----

    private fun drawSubCard(
        canvas: Canvas,
        rect: RectF,
        cornerDp: Float,
        fillColor: Int,
        borderColor: Int? = null,
        borderWidthDp: Float = 1f
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
        canvas.drawRoundRect(rect, cornerDp, cornerDp, fillPaint)
        if (borderColor != null) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = borderWidthDp
                color = borderColor
            }
            canvas.drawRoundRect(rect, cornerDp, cornerDp, strokePaint)
        }
    }

    private fun drawProgressBar(
        canvas: Canvas,
        rect: RectF,
        progress: Float,
        fillColor: Int,
        trackColor: Int
    ) {
        val r = rect.height() / 2f
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColor }
        canvas.drawRoundRect(rect, r, r, trackPaint)
        val clamped = progress.coerceIn(0f, 1f)
        if (clamped > 0f) {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
            val w = (rect.width() * clamped).coerceAtLeast(rect.height())
            canvas.drawRoundRect(RectF(rect.left, rect.top, min(rect.right, rect.left + w), rect.bottom), r, r, fillPaint)
        }
    }

    private fun drawCircleIconButton(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        bgColor: Int,
        icon: IconKind,
        iconColor: Int
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas.drawCircle(cx, cy, radius, bgPaint)
        val iconSize = radius * 1.05f
        drawVectorIcon(canvas, icon, cx - iconSize / 2f, cy - iconSize / 2f, iconSize, iconColor)
    }

    private fun drawChipFlow(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        maxW: Float,
        labels: List<String>,
        selected: String?,
        textSizeSp: Float,
        tf: Typeface,
        padH: Float,
        padV: Float,
        gap: Float,
        ink: Int,
        fillColor: Int,
        selectedFillColor: Int,
        selectedBorderColor: Int?
    ) {
        var curX = startX
        var curY = startY
        val tp = textPaint(textSizeSp, ink, tf)
        val chipH = textSizeSp * 1.25f + padV * 2f
        labels.forEach { label ->
            val tw = tp.measureText(label)
            val chipW = tw + padH * 2f
            if (curX > startX && (curX - startX) + chipW > maxW) {
                curX = startX
                curY += chipH + gap
            }
            val isSel = label == selected
            val chipRect = RectF(curX, curY, curX + chipW, curY + chipH)
            drawSubCard(
                canvas,
                chipRect,
                10f,
                if (isSel) selectedFillColor else fillColor,
                if (isSel) selectedBorderColor else null,
                1f
            )
            val baseline = chipRect.centerY() - (tp.descent() + tp.ascent()) / 2f
            canvas.drawText(label, curX + padH, baseline, tp)
            curX += chipW + gap
        }
    }

    private fun countChipRows(
        labels: List<String>,
        maxW: Float,
        textSizeSp: Float,
        tf: Typeface,
        padHTotal: Float,
        gap: Float
    ): Int {
        val tp = textPaint(textSizeSp, Color.WHITE, tf)
        var curX = 0f
        var rows = 1
        labels.forEach { label ->
            val chipW = tp.measureText(label) + padHTotal
            if (curX > 0f && curX + chipW > maxW) {
                rows++
                curX = chipW + gap
            } else {
                curX += chipW + gap
            }
        }
        return rows
    }

    private fun drawVectorIcon(
        canvas: Canvas,
        kind: IconKind,
        x: Float,
        y: Float,
        size: Float,
        color: Int
    ) {
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = (size * 0.11f).coerceAtLeast(1.2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val cx = x + size / 2f
        val cy = y + size / 2f
        val r = size * 0.42f

        when (kind) {
            IconKind.SUN -> {
                canvas.drawCircle(cx, cy, size * 0.21f, fill)
                for (a in 0 until 8) {
                    val rad = Math.toRadians((a * 45).toDouble())
                    val x1 = cx + (cos(rad) * size * 0.30f).toFloat()
                    val y1 = cy + (sin(rad) * size * 0.30f).toFloat()
                    val x2 = cx + (cos(rad) * size * 0.44f).toFloat()
                    val y2 = cy + (sin(rad) * size * 0.44f).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, stroke)
                }
            }
            IconKind.BATTERY, IconKind.BATTERY_CHARGING -> {
                val tipW = size * 0.28f
                canvas.drawRoundRect(RectF(cx - tipW / 2f, y + size * 0.08f, cx + tipW / 2f, y + size * 0.19f), 1.5f, 1.5f, fill)
                val body = RectF(cx - size * 0.24f, y + size * 0.19f, cx + size * 0.24f, y + size * 0.90f)
                canvas.drawRoundRect(body, size * 0.09f, size * 0.09f, stroke)
                if (kind == IconKind.BATTERY_CHARGING) {
                    val bolt = Path().apply {
                        moveTo(cx + size * 0.04f, y + size * 0.30f)
                        lineTo(cx - size * 0.09f, cy + size * 0.04f)
                        lineTo(cx + size * 0.02f, cy + size * 0.04f)
                        lineTo(cx - size * 0.05f, y + size * 0.78f)
                        lineTo(cx + size * 0.10f, cy + size * 0.00f)
                        lineTo(cx - size * 0.01f, cy + size * 0.00f)
                        close()
                    }
                    canvas.drawPath(bolt, fill)
                } else {
                    canvas.drawRoundRect(RectF(body.left + size * 0.07f, y + size * 0.36f, body.right - size * 0.07f, body.bottom - size * 0.07f), 1.5f, 1.5f, fill)
                }
            }
            IconKind.WALK -> {
                canvas.drawCircle(cx + size * 0.05f, y + size * 0.20f, size * 0.10f, fill)
                val p = Path().apply {
                    moveTo(cx, y + size * 0.34f)
                    lineTo(cx - size * 0.06f, y + size * 0.58f)
                    lineTo(cx - size * 0.20f, y + size * 0.86f)
                    moveTo(cx - size * 0.05f, y + size * 0.56f)
                    lineTo(cx + size * 0.16f, y + size * 0.86f)
                    moveTo(cx - size * 0.16f, y + size * 0.46f)
                    lineTo(cx + size * 0.18f, y + size * 0.44f)
                }
                canvas.drawPath(p, stroke)
            }
            IconKind.CHECK_CIRCLE -> {
                canvas.drawCircle(cx, cy, r, fill)
                val check = Paint(stroke).apply { this.color = Color.WHITE }
                val p = Path().apply {
                    moveTo(cx - r * 0.45f, cy + r * 0.05f)
                    lineTo(cx - r * 0.10f, cy + r * 0.40f)
                    lineTo(cx + r * 0.48f, cy - r * 0.34f)
                }
                canvas.drawPath(p, check)
            }
            IconKind.UNCHECK_CIRCLE -> {
                canvas.drawCircle(cx, cy, r, stroke)
            }
            IconKind.NOTE -> {
                val rect = RectF(x + size * 0.15f, y + size * 0.15f, x + size * 0.85f, y + size * 0.85f)
                canvas.drawRoundRect(rect, size * 0.14f, size * 0.14f, stroke)
                canvas.drawLine(rect.left + size * 0.14f, cy - size * 0.08f, rect.right - size * 0.14f, cy - size * 0.08f, stroke)
                canvas.drawLine(rect.left + size * 0.14f, cy + size * 0.10f, rect.centerX() + size * 0.08f, cy + size * 0.10f, stroke)
            }
            IconKind.EVENT, IconKind.CALENDAR -> {
                val rect = RectF(x + size * 0.14f, y + size * 0.20f, x + size * 0.86f, y + size * 0.86f)
                canvas.drawRoundRect(rect, size * 0.12f, size * 0.12f, stroke)
                canvas.drawLine(rect.left, y + size * 0.40f, rect.right, y + size * 0.40f, stroke)
                canvas.drawLine(x + size * 0.34f, y + size * 0.10f, x + size * 0.34f, y + size * 0.24f, stroke)
                canvas.drawLine(x + size * 0.66f, y + size * 0.10f, x + size * 0.66f, y + size * 0.24f, stroke)
            }
            IconKind.NEWS -> {
                val rect = RectF(x + size * 0.12f, y + size * 0.18f, x + size * 0.88f, y + size * 0.82f)
                canvas.drawRoundRect(rect, size * 0.10f, size * 0.10f, stroke)
                canvas.drawLine(rect.left + size * 0.14f, cy - size * 0.06f, rect.right - size * 0.14f, cy - size * 0.06f, stroke)
                canvas.drawLine(rect.left + size * 0.14f, cy + size * 0.14f, rect.centerX() + size * 0.10f, cy + size * 0.14f, stroke)
            }
            IconKind.IMAGE -> {
                val rect = RectF(x + size * 0.14f, y + size * 0.16f, x + size * 0.86f, y + size * 0.84f)
                canvas.drawRoundRect(rect, size * 0.12f, size * 0.12f, stroke)
                canvas.drawCircle(x + size * 0.34f, y + size * 0.36f, size * 0.07f, fill)
                val hill = Path().apply {
                    moveTo(rect.left + size * 0.10f, rect.bottom - size * 0.12f)
                    lineTo(cx - size * 0.04f, cy + size * 0.04f)
                    lineTo(cx + size * 0.10f, cy + size * 0.16f)
                    lineTo(rect.right - size * 0.10f, rect.bottom - size * 0.12f)
                }
                canvas.drawPath(hill, stroke)
            }
            IconKind.EXTENSION -> {
                val rect = RectF(x + size * 0.18f, y + size * 0.22f, x + size * 0.78f, y + size * 0.82f)
                canvas.drawRoundRect(rect, size * 0.10f, size * 0.10f, stroke)
                canvas.drawCircle(cx, y + size * 0.16f, size * 0.09f, fill)
                canvas.drawCircle(x + size * 0.84f, cy, size * 0.09f, fill)
            }
            IconKind.TIMER, IconKind.SCHEDULE -> {
                canvas.drawCircle(cx, cy + size * 0.04f, size * 0.36f, stroke)
                canvas.drawLine(cx, cy + size * 0.04f, cx, cy - size * 0.16f, stroke)
                canvas.drawLine(cx, cy + size * 0.04f, cx + size * 0.16f, cy + size * 0.12f, stroke)
            }
            IconKind.DRINK -> {
                val drop = Path().apply {
                    moveTo(cx, y + size * 0.12f)
                    cubicTo(x + size * 0.84f, cy + size * 0.06f, x + size * 0.82f, y + size * 0.86f, cx, y + size * 0.86f)
                    cubicTo(x + size * 0.18f, y + size * 0.86f, x + size * 0.16f, cy + size * 0.06f, cx, y + size * 0.12f)
                    close()
                }
                canvas.drawPath(drop, fill)
            }
            IconKind.FIRE -> {
                val flame = Path().apply {
                    moveTo(cx, y + size * 0.12f)
                    cubicTo(x + size * 0.82f, y + size * 0.42f, x + size * 0.80f, y + size * 0.88f, cx, y + size * 0.88f)
                    cubicTo(x + size * 0.20f, y + size * 0.88f, x + size * 0.18f, y + size * 0.42f, cx, y + size * 0.12f)
                    close()
                }
                canvas.drawPath(flame, fill)
            }
            IconKind.MEMORY -> {
                val chip = RectF(x + size * 0.24f, y + size * 0.24f, x + size * 0.76f, y + size * 0.76f)
                canvas.drawRoundRect(chip, size * 0.08f, size * 0.08f, stroke)
                canvas.drawRect(RectF(cx - size * 0.10f, cy - size * 0.10f, cx + size * 0.10f, cy + size * 0.10f), fill)
            }
            IconKind.QUOTE -> {
                val p = textPaint(size * 0.9f, color, Typeface.SERIF).apply { isFakeBoldText = true }
                drawTextBaseline(canvas, "❝", x + size * 0.05f, y + size * 0.80f, p)
            }
            IconKind.BOLT -> {
                val bolt = Path().apply {
                    moveTo(cx + size * 0.06f, y + size * 0.10f)
                    lineTo(x + size * 0.24f, cy + size * 0.04f)
                    lineTo(cx + size * 0.02f, cy + size * 0.04f)
                    lineTo(cx - size * 0.06f, y + size * 0.90f)
                    lineTo(x + size * 0.76f, cy - size * 0.02f)
                    lineTo(cx, cy - size * 0.02f)
                    close()
                }
                canvas.drawPath(bolt, fill)
            }
            IconKind.GLOBE -> {
                canvas.drawCircle(cx, cy, r, stroke)
                canvas.drawOval(RectF(cx - r * 0.48f, cy - r, cx + r * 0.48f, cy + r), stroke)
                canvas.drawLine(cx - r, cy, cx + r, cy, stroke)
            }
            IconKind.MOOD -> {
                canvas.drawCircle(cx, cy, r, stroke)
                canvas.drawCircle(cx - r * 0.35f, cy - r * 0.15f, size * 0.05f, fill)
                canvas.drawCircle(cx + r * 0.35f, cy - r * 0.15f, size * 0.05f, fill)
                canvas.drawArc(RectF(cx - r * 0.45f, cy - r * 0.10f, cx + r * 0.45f, cy + r * 0.55f), 15f, 150f, false, stroke)
            }
            IconKind.AIR -> {
                canvas.drawLine(x + size * 0.14f, cy - size * 0.16f, x + size * 0.76f, cy - size * 0.16f, stroke)
                canvas.drawLine(x + size * 0.14f, cy, x + size * 0.86f, cy, stroke)
                canvas.drawLine(x + size * 0.14f, cy + size * 0.16f, x + size * 0.66f, cy + size * 0.16f, stroke)
            }
            IconKind.WIFI, IconKind.WIFI_OFF -> {
                canvas.drawCircle(cx, y + size * 0.78f, size * 0.07f, fill)
                canvas.drawArc(RectF(cx - size * 0.24f, cy - size * 0.02f, cx + size * 0.24f, y + size * 0.84f), -135f, 90f, false, stroke)
                canvas.drawArc(RectF(cx - size * 0.42f, y + size * 0.24f, cx + size * 0.42f, y + size * 0.94f), -135f, 90f, false, stroke)
                if (kind == IconKind.WIFI_OFF) {
                    canvas.drawLine(x + size * 0.18f, y + size * 0.18f, x + size * 0.82f, y + size * 0.82f, stroke)
                }
            }
            IconKind.PHONE -> {
                val rect = RectF(cx - size * 0.24f, y + size * 0.12f, cx + size * 0.24f, y + size * 0.88f)
                canvas.drawRoundRect(rect, size * 0.08f, size * 0.08f, stroke)
                canvas.drawCircle(cx, rect.bottom - size * 0.08f, size * 0.04f, fill)
            }
            IconKind.PLAY -> {
                val tri = Path().apply {
                    moveTo(cx - size * 0.18f, cy - size * 0.26f)
                    lineTo(cx + size * 0.26f, cy)
                    lineTo(cx - size * 0.18f, cy + size * 0.26f)
                    close()
                }
                canvas.drawPath(tri, fill)
            }
            IconKind.REPLAY -> {
                canvas.drawArc(RectF(cx - r * 0.72f, cy - r * 0.72f, cx + r * 0.72f, cy + r * 0.72f), -60f, 270f, false, stroke)
            }
            IconKind.PLUS -> {
                canvas.drawLine(cx - r * 0.6f, cy, cx + r * 0.6f, cy, stroke)
                canvas.drawLine(cx, cy - r * 0.6f, cx, cy + r * 0.6f, stroke)
            }
            IconKind.MINUS -> {
                canvas.drawLine(cx - r * 0.6f, cy, cx + r * 0.6f, cy, stroke)
            }
        }
    }

    // ---- Text & Bitmap Utilities ----

    private fun textPaint(sizeSp: Float, color: Int, tf: Typeface): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            textSize = sizeSp
            this.color = color
            typeface = tf
        }
    }

    private fun drawTextBaseline(canvas: Canvas, text: String, x: Float, baselineY: Float, paint: TextPaint): Float {
        canvas.drawText(text, x, baselineY, paint)
        return baselineY
    }

    private fun drawSingleLine(canvas: Canvas, text: String, x: Float, baselineY: Float, maxWidth: Float, paint: TextPaint) {
        val clipped = TextUtils.ellipsize(text, paint, maxWidth.coerceAtLeast(10f), TextUtils.TruncateAt.END).toString()
        canvas.drawText(clipped, x, baselineY, paint)
    }

    private fun buildStaticLayout(text: String, widthDp: Float, paint: TextPaint, maxLines: Int): StaticLayout {
        val w = widthDp.roundToInt().coerceAtLeast(24)
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, w)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .build()
    }

    private fun measureTextHeight(text: String, widthDp: Float, sizeSp: Float, tf: Typeface, maxLines: Int): Float {
        val sl = buildStaticLayout(text, widthDp, textPaint(sizeSp, Color.WHITE, tf), maxLines)
        return sl.height.toFloat()
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        topY: Float,
        widthDp: Float,
        paint: TextPaint,
        maxLines: Int
    ): Float {
        val sl = buildStaticLayout(text, widthDp, paint, maxLines)
        val save = canvas.save()
        canvas.translate(x, topY)
        sl.draw(canvas)
        canvas.restoreToCount(save)
        return sl.height.toFloat()
    }

    private fun drawCropBitmap(canvas: Canvas, src: Bitmap, dest: RectF) {
        val scale = max(dest.width() / src.width.toFloat(), dest.height() / src.height.toFloat())
        val dx = dest.centerX() - (src.width * scale) / 2f
        val dy = dest.centerY() - (src.height * scale) / 2f
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(src, matrix, paint)
    }

    private fun decodeSampledResource(context: Context, resId: Int, reqW: Int, reqH: Int): Bitmap? {
        return runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resId, opts)
            opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, reqW, reqH)
            opts.inJustDecodeBounds = false
            BitmapFactory.decodeResource(context.resources, resId, opts)
        }.getOrNull()
    }

    private fun decodeSampledUri(context: Context, uriStr: String, reqW: Int, reqH: Int): Bitmap? {
        return runCatching {
            val uri = Uri.parse(uriStr)
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, reqW, reqH)
            opts.inJustDecodeBounds = false
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        }.getOrNull()
    }

    private fun calculateInSampleSize(rawW: Int, rawH: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (rawH > reqH || rawW > reqW) {
            val halfH = rawH / 2
            val halfW = rawW / 2
            while (halfH / inSampleSize >= reqH && halfW / inSampleSize >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun applySoftBlur(src: Bitmap, radius: Float): Bitmap {
        val factor = (radius / 3f).coerceIn(1.5f, 6f)
        val smallW = (src.width / factor).roundToInt().coerceAtLeast(24)
        val smallH = (src.height / factor).roundToInt().coerceAtLeast(24)
        val down = Bitmap.createScaledBitmap(src, smallW, smallH, true)
        val up = Bitmap.createScaledBitmap(down, src.width, src.height, true)
        if (down !== up) down.recycle()
        return up
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (Color.alpha(color) * alpha.coerceIn(0f, 1f)).roundToInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }
}
