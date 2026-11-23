package com.example.data.device

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.prefs.UserPreferencesRepository
import com.example.domain.model.StepSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

object StepMonitor {

    fun hasActivityPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isHealthConnectAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true // System Health Connect integrated in Android 14+
        }
        return runCatching {
            context.packageManager.getPackageInfo("com.google.android.apps.healthdata", 0)
            true
        }.getOrDefault(false)
    }

    fun openHealthConnectIntent(context: Context): Intent {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            "android.health.connect.action.HEALTH_HOME_SETTINGS"
        } else {
            "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
        }
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            intent
        } else {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun observeSteps(
        context: Context,
        prefsRepo: UserPreferencesRepository,
        dailyGoal: Int
    ): Flow<StepSnapshot> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val hasPerm = hasActivityPermission(context)
        val healthConnect = isHealthConnectAvailable(context)

        val initialSnapshot = prefsRepo.getSnapshot()
        trySend(
            StepSnapshot(
                stepsToday = initialSnapshot.lastRecordedSteps,
                dailyGoal = dailyGoal,
                sensorAvailable = stepSensor != null,
                activityPermissionGranted = hasPerm,
                healthConnectInstalled = healthConnect,
                sourceLabel = buildSourceLabel(stepSensor != null, hasPerm, healthConnect)
            )
        )

        var listener: SensorEventListener? = null
        if (stepSensor != null && hasPerm) {
            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val totalSinceBoot = event.values.firstOrNull()?.toInt() ?: return
                    CoroutineScope(Dispatchers.IO).launch {
                        val currentPrefs = prefsRepo.getSnapshot()
                        val baseline = if (currentPrefs.stepSensorBaseline < 0 || totalSinceBoot < currentPrefs.stepSensorBaseline) {
                            totalSinceBoot
                        } else {
                            currentPrefs.stepSensorBaseline
                        }
                        val delta = (totalSinceBoot - baseline).coerceAtLeast(0)
                        val combinedSteps = if (delta > 0) delta else currentPrefs.lastRecordedSteps
                        prefsRepo.updateStepReading(baseline, combinedSteps)
                        trySend(
                            StepSnapshot(
                                stepsToday = combinedSteps,
                                dailyGoal = currentPrefs.dailyStepGoal,
                                sensorAvailable = true,
                                activityPermissionGranted = true,
                                healthConnectInstalled = healthConnect,
                                sourceLabel = buildSourceLabel(true, true, healthConnect)
                            )
                        )
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            if (listener != null) {
                sensorManager?.unregisterListener(listener)
            }
        }
    }

    private fun buildSourceLabel(
        sensorAvailable: Boolean,
        hasPerm: Boolean,
        healthConnect: Boolean
    ): String = when {
        sensorAvailable && hasPerm && healthConnect -> "裝置計步器 · Health Connect 就緒"
        sensorAvailable && hasPerm -> "裝置硬體計步器即時追蹤"
        healthConnect -> "Health Connect 已連結（待授權感測器）"
        else -> "本機步數目標追蹤"
    }
}
