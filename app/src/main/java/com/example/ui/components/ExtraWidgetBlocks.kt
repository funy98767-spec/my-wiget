package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.device.SystemStatsSnapshot
import com.example.domain.model.StepSnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun CalendarMonthBlockView(
    clockMillis: Long,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val cal = Calendar.getInstance().apply { timeInMillis = clockMillis }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayCal = Calendar.getInstance().apply {
        set(year, month - 1, 1)
    }
    val startOffset = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text("${year}年 ${month}月", color = ink, fontFamily = fontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        val headers = listOf("日", "一", "二", "三", "四", "五", "六")
        Row(modifier = Modifier.fillMaxWidth()) {
            headers.forEach { d ->
                Text(
                    text = d,
                    color = subInk,
                    fontFamily = fontFamily,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        val cells = List(startOffset) { 0 } + (1..daysInMonth).toList()
        val rows = cells.chunked(7)
        rows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                for (i in 0 until 7) {
                    val day = week.getOrNull(i) ?: 0
                    Box(
                        modifier = Modifier.weight(1f).height(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day > 0) {
                            val isToday = day == today
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) accent else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$day",
                                    color = if (isToday) Color.White else ink,
                                    fontFamily = fontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
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
fun WorldClockBlockView(
    clockMillis: Long,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val zones = listOf(
        "東京 JST" to "Asia/Tokyo",
        "首爾 KST" to "Asia/Seoul",
        "倫敦 UTC" to "Europe/London",
        "紐約 EST" to "America/New_York"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.Public, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text("世界時鐘", color = subInk, fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            zones.forEach { (city, tzId) ->
                val fmt = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone(tzId)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ink.copy(alpha = 0.07f))
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(city, color = subInk, fontFamily = fontFamily, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(fmt.format(Date(clockMillis)), color = ink, fontFamily = fontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DayProgressBlockView(
    clockMillis: Long,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val cal = Calendar.getInstance().apply { timeInMillis = clockMillis }
    val minutesToday = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val dayProgress = (minutesToday / 1440f).coerceIn(0f, 1f)
    val remainHours = ((1440 - minutesToday) / 60f)

    val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
    val maxDaysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR).coerceAtLeast(365)
    val yearProgress = (dayOfYear.toFloat() / maxDaysInYear).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text("今日與年度進度", color = subInk, fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("今日已過 ${(dayProgress * 100).toInt()}%", color = ink, fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("剩 ${String.format("%.1f", remainHours)} 小時", color = subInk, fontFamily = fontFamily, fontSize = 11.sp)
        }
        LinearProgressIndicator(
            progress = { dayProgress },
            color = accent,
            trackColor = ink.copy(alpha = 0.16f),
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("今年進度 ${(yearProgress * 100).toInt()}%", color = ink, fontFamily = fontFamily, fontSize = 11.sp)
            Text("第 $dayOfYear/$maxDaysInYear 天", color = subInk, fontFamily = fontFamily, fontSize = 11.sp)
        }
        LinearProgressIndicator(
            progress = { yearProgress },
            color = accent.copy(alpha = 0.8f),
            trackColor = ink.copy(alpha = 0.16f),
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun StopwatchBlockView(
    stopwatchSeconds: Int,
    stopwatchRunning: Boolean,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    onToggle: (() -> Unit)?,
    onReset: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val hrs = stopwatchSeconds / 3600
    val mins = (stopwatchSeconds % 3600) / 60
    val secs = stopwatchSeconds % 60
    val formatted = if (hrs > 0) String.format("%d:%02d:%02d", hrs, mins, secs) else String.format("%02d:%02d", mins, secs)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text("碼錶計時", color = subInk, fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatted, color = ink, fontFamily = fontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.25f),
                    modifier = Modifier.size(28.dp).clickable(enabled = onToggle != null) { onToggle?.invoke() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (stopwatchRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "碼錶開始暫停",
                            tint = ink,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = ink.copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp).clickable(enabled = onReset != null) { onReset?.invoke() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Replay,
                            contentDescription = "歸零碼錶",
                            tint = ink,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CounterBlockView(
    counterValue: Int,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    onIncrement: ((Int) -> Unit)?,
    onReset: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("萬用計數器", color = subInk, fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            if (onReset != null) {
                Text(
                    text = "歸零",
                    color = accent,
                    fontFamily = fontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onReset() }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$counterValue", color = ink, fontFamily = fontFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = CircleShape,
                    color = ink.copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp).clickable(enabled = onIncrement != null) { onIncrement?.invoke(-1) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Remove, contentDescription = "減一", tint = ink, modifier = Modifier.size(16.dp))
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.28f),
                    modifier = Modifier.size(28.dp).clickable(enabled = onIncrement != null) { onIncrement?.invoke(1) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, contentDescription = "加一", tint = ink, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodBlockView(
    selectedMood: String,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    onSelectMood: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val moods = listOf("開心滿足", "專注高效", "平靜放鬆", "需要充電", "靈感爆發")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.Mood, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text("今日心情：$selectedMood", color = ink, fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            moods.forEach { mood ->
                val isSelected = mood == selectedMood
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) accent.copy(alpha = 0.30f) else ink.copy(alpha = 0.09f),
                    border = if (isSelected) BorderStroke(1.dp, accent) else null,
                    modifier = Modifier.clickable(enabled = onSelectMood != null) { onSelectMood?.invoke(mood) }
                ) {
                    Text(
                        text = mood,
                        color = ink,
                        fontFamily = fontFamily,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BreathingBlockView(
    clockMillis: Long,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val cycleSec = ((clockMillis / 1000L) % 12L).toInt()
    val (phaseLabel, phaseProgress, stepSec) = when {
        cycleSec < 4 -> Triple("深呼吸 · 緩慢吸氣", (cycleSec + 1) / 4f, cycleSec + 1)
        cycleSec < 8 -> Triple("保持靜心 · 屏息 4 秒", (cycleSec - 3) / 4f, cycleSec - 3)
        else -> Triple("放鬆肩膀 · 緩慢吐氣", (cycleSec - 7) / 4f, cycleSec - 7)
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Air, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Text(phaseLabel, color = ink, fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text("${stepSec}/4s", color = accent, fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { phaseProgress },
            color = accent,
            trackColor = ink.copy(alpha = 0.16f),
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
fun NetworkBlockView(
    stats: SystemStatsSnapshot,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(
                imageVector = if (stats.isOnline) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Text("網路連線", color = subInk, fontFamily = fontFamily, fontSize = 11.sp)
        }
        Text(stats.networkLabel, color = ink, fontFamily = fontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(
            text = if (stats.isOnline) "即時同步就緒" else "離線模式運行中",
            color = subInk,
            fontFamily = fontFamily,
            fontSize = 11.sp
        )
    }
}

@Composable
fun DeviceInfoBlockView(
    stats: SystemStatsSnapshot,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val hrs = stats.uptimeMinutes / 60
    val mins = stats.uptimeMinutes % 60
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text("裝置與系統規格", color = subInk, fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stats.deviceModel, color = ink, fontFamily = fontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stats.androidVersion, color = accent, fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = "已連續運行 ${hrs}小時 ${mins}分 · 儲存剩 ${stats.freeStorageGb} GB",
            color = subInk,
            fontFamily = fontFamily,
            fontSize = 11.sp
        )
    }
}

@Composable
fun CalorieDistanceBlockView(
    steps: StepSnapshot,
    fontFamily: FontFamily,
    ink: Color,
    subInk: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val distanceKm = steps.stepsToday * 0.00075f
    val kcal = (steps.stepsToday * 0.04f).toInt()
    val remaining = (steps.dailyGoal - steps.stepsToday).coerceAtLeast(0)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text("步行消耗", color = subInk, fontFamily = fontFamily, fontSize = 11.sp)
        }
        Text("${kcal} kcal", color = ink, fontFamily = fontFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "${String.format("%.2f", distanceKm)} km · 距目標 $remaining 步",
            color = subInk,
            fontFamily = fontFamily,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
