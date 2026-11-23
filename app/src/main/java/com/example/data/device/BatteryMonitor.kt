package com.example.data.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.domain.model.BatterySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object BatteryMonitor {

    fun readCurrentSnapshot(context: Context): BatterySnapshot {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return parseIntent(context, intent)
    }

    fun observeBattery(context: Context): Flow<BatterySnapshot> = callbackFlow {
        trySend(readCurrentSnapshot(context))
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                trySend(parseIntent(ctx, intent))
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    private fun parseIntent(context: Context, intent: Intent?): BatterySnapshot {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val fallbackLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 } ?: 85
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else fallbackLevel

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL ||
            (bm?.isCharging == true)

        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 315) ?: 315
        val tempC = tempTenths / 10f

        val healthInt = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
            ?: BatteryManager.BATTERY_HEALTH_GOOD
        val healthText = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> if (isCharging) "充電中 · 健康良好" else "放電中 · 健康良好"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "電池溫度偏高"
            else -> "狀態正常"
        }

        return BatterySnapshot(
            levelPercent = pct,
            isCharging = isCharging,
            temperatureC = tempC,
            statusLabel = healthText
        )
    }
}
