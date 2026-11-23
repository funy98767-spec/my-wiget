package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hwidget_prefs")

data class UserSettings(
    val themeMode: String = "SYSTEM",
    val dynamicColorEnabled: Boolean = true,
    val selectedCityName: String = "Taipei",
    val selectedCityLat: Double = 25.0330,
    val selectedCityLon: Double = 121.5654,
    val dailyStepGoal: Int = 8000,
    val stepSensorBaseline: Int = -1,
    val lastRecordedSteps: Int = 4280,
    val notificationsEnabled: Boolean = true,
    val defaultRefreshMinutes: Int = 30,
    val mountedExtensions: Set<String> = emptySet(),
    val cachedWeatherSummary: String = "Taipei 24°C · 多雲時晴 · 降雨 15%",
    val waterMlToday: Int = 1250,
    val waterGoalMl: Int = 2000,
    val quoteText: String = "把複雜交給工具，把專注留給今天最重要的一件事。",
    val quoteAuthor: String = "HWidget 每日靈感",
    val counterValue: Int = 0,
    val moodTag: String = "專注高效"
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val CITY_NAME = stringPreferencesKey("city_name")
        val CITY_LAT = doublePreferencesKey("city_lat")
        val CITY_LON = doublePreferencesKey("city_lon")
        val STEP_GOAL = intPreferencesKey("daily_step_goal")
        val STEP_BASELINE = intPreferencesKey("step_sensor_baseline")
        val LAST_STEPS = intPreferencesKey("last_recorded_steps")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DEFAULT_REFRESH_MINUTES = intPreferencesKey("default_refresh_minutes")
        val MOUNTED_EXTENSIONS = stringSetPreferencesKey("mounted_extensions")
        val CACHED_WEATHER_SUMMARY = stringPreferencesKey("cached_weather_summary")
        val WATER_ML_TODAY = intPreferencesKey("water_ml_today")
        val WATER_GOAL_ML = intPreferencesKey("water_goal_ml")
        val QUOTE_TEXT = stringPreferencesKey("quote_text")
        val QUOTE_AUTHOR = stringPreferencesKey("quote_author")
        val COUNTER_VALUE = intPreferencesKey("counter_value")
        val MOOD_TAG = stringPreferencesKey("mood_tag")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: true,
            selectedCityName = prefs[Keys.CITY_NAME] ?: "Taipei",
            selectedCityLat = prefs[Keys.CITY_LAT] ?: 25.0330,
            selectedCityLon = prefs[Keys.CITY_LON] ?: 121.5654,
            dailyStepGoal = prefs[Keys.STEP_GOAL] ?: 8000,
            stepSensorBaseline = prefs[Keys.STEP_BASELINE] ?: -1,
            lastRecordedSteps = prefs[Keys.LAST_STEPS] ?: 4280,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            defaultRefreshMinutes = prefs[Keys.DEFAULT_REFRESH_MINUTES] ?: 30,
            mountedExtensions = prefs[Keys.MOUNTED_EXTENSIONS] ?: emptySet(),
            cachedWeatherSummary = prefs[Keys.CACHED_WEATHER_SUMMARY]
                ?: "Taipei 24°C · 多雲時晴 · 降雨 15%",
            waterMlToday = prefs[Keys.WATER_ML_TODAY] ?: 1250,
            waterGoalMl = prefs[Keys.WATER_GOAL_ML] ?: 2000,
            quoteText = prefs[Keys.QUOTE_TEXT] ?: "把複雜交給工具，把專注留給今天最重要的一件事。",
            quoteAuthor = prefs[Keys.QUOTE_AUTHOR] ?: "HWidget 每日靈感",
            counterValue = prefs[Keys.COUNTER_VALUE] ?: 0,
            moodTag = prefs[Keys.MOOD_TAG] ?: "專注高效"
        )
    }

    suspend fun getSnapshot(): UserSettings = settingsFlow.first()

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setSelectedCity(name: String, lat: Double, lon: Double) {
        context.dataStore.edit {
            it[Keys.CITY_NAME] = name
            it[Keys.CITY_LAT] = lat
            it[Keys.CITY_LON] = lon
        }
    }

    suspend fun setDailyStepGoal(goal: Int) {
        context.dataStore.edit { it[Keys.STEP_GOAL] = goal.coerceIn(1000, 50000) }
    }

    suspend fun updateStepReading(baseline: Int, stepsToday: Int) {
        context.dataStore.edit {
            it[Keys.STEP_BASELINE] = baseline
            it[Keys.LAST_STEPS] = stepsToday
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setDefaultRefreshMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_REFRESH_MINUTES] = minutes }
    }

    suspend fun toggleExtensionMount(extensionKey: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.MOUNTED_EXTENSIONS] ?: emptySet()
            prefs[Keys.MOUNTED_EXTENSIONS] = if (extensionKey in current) {
                current - extensionKey
            } else {
                current + extensionKey
            }
        }
    }

    suspend fun setCachedWeatherSummary(summary: String) {
        context.dataStore.edit { it[Keys.CACHED_WEATHER_SUMMARY] = summary }
    }

    suspend fun addWaterMl(deltaMl: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.WATER_ML_TODAY] ?: 1250
            prefs[Keys.WATER_ML_TODAY] = (current + deltaMl).coerceIn(0, 6000)
        }
    }

    suspend fun setWaterGoalMl(goalMl: Int) {
        context.dataStore.edit { it[Keys.WATER_GOAL_ML] = goalMl.coerceIn(500, 5000) }
    }

    suspend fun setCustomQuote(text: String, author: String) {
        context.dataStore.edit {
            it[Keys.QUOTE_TEXT] = text.ifBlank { "保持節奏，穩步向前。" }
            it[Keys.QUOTE_AUTHOR] = author.ifBlank { "我的每日語錄" }
        }
    }

    suspend fun incrementCounter(delta: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.COUNTER_VALUE] ?: 0
            prefs[Keys.COUNTER_VALUE] = (current + delta).coerceIn(0, 99999)
        }
    }

    suspend fun resetCounter() {
        context.dataStore.edit { it[Keys.COUNTER_VALUE] = 0 }
    }

    suspend fun setMoodTag(mood: String) {
        context.dataStore.edit { it[Keys.MOOD_TAG] = mood }
    }
}
