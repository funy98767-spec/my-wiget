package com.example.data.device

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock

data class SystemStatsSnapshot(
    val usedRamMb: Long = 3200L,
    val totalRamMb: Long = 8192L,
    val ramUsagePercent: Int = 39,
    val freeStorageGb: Int = 48,
    val totalStorageGb: Int = 128,
    val storageUsagePercent: Int = 62,
    val networkLabel: String = "Wi-Fi 已連線",
    val isOnline: Boolean = true,
    val deviceModel: String = Build.MODEL ?: "Android",
    val androidVersion: String = "Android ${Build.VERSION.RELEASE}",
    val uptimeMinutes: Long = 120L
)

object SystemStatsMonitor {
    fun readStats(context: Context): SystemStatsSnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val availRamMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L)
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(0L)
        val ramPct = ((usedRamMb * 100f) / totalRamMb).toInt().coerceIn(0, 100)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
        val availBytes = statFs.availableBlocksLong * statFs.blockSizeLong
        val totalGb = (totalBytes / (1024L * 1024L * 1024L)).toInt().coerceAtLeast(1)
        val freeGb = (availBytes / (1024L * 1024L * 1024L)).toInt().coerceAtLeast(0)
        val storagePct = (((totalBytes - availBytes) * 100f) / totalBytes.coerceAtLeast(1L)).toInt().coerceIn(0, 100)

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val (networkLabel, isOnline) = when {
            caps == null -> "未連線" to false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi 已連線" to true
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "行動網路連線中" to true
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "乙太網路已連線" to true
            else -> "網路已連線" to true
        }

        val uptimeMinutes = (SystemClock.elapsedRealtime() / 60_000L).coerceAtLeast(1L)

        return SystemStatsSnapshot(
            usedRamMb = usedRamMb,
            totalRamMb = totalRamMb,
            ramUsagePercent = ramPct,
            freeStorageGb = freeGb,
            totalStorageGb = totalGb,
            storageUsagePercent = storagePct,
            networkLabel = networkLabel,
            isOnline = isOnline,
            deviceModel = Build.MODEL ?: "Android",
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            uptimeMinutes = uptimeMinutes
        )
    }
}
