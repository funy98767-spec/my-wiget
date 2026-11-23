package com.example.data.repository

import android.content.Context
import com.example.data.db.BlockJsonCodec
import com.example.data.db.CountdownEntity
import com.example.data.db.HWidgetDatabase
import com.example.data.db.HabitEntity
import com.example.data.db.NewsArticleEntity
import com.example.data.db.NoteEntity
import com.example.data.db.RssSourceEntity
import com.example.data.db.StoreInstallEntity
import com.example.data.db.TodoEntity
import com.example.data.db.WidgetEntity
import com.example.data.device.ReminderNotifier
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.remote.RssFeedService
import com.example.data.remote.WeatherService
import com.example.domain.model.BackgroundPreset
import com.example.domain.model.BlockType
import com.example.domain.model.ClockDisplayStyle
import com.example.domain.model.StoreWidgetPack
import com.example.domain.model.WeatherSnapshot
import com.example.domain.model.WidgetBlock
import com.example.domain.model.WidgetFontStyle
import com.example.domain.model.WidgetThemeStyle
import com.example.domain.model.WidgetType
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class HWidgetRepository(
    private val context: Context,
    private val database: HWidgetDatabase,
    private val prefs: UserPreferencesRepository,
    private val weatherService: WeatherService = WeatherService(),
    private val rssFeedService: RssFeedService = RssFeedService()
) {
    private val widgetDao = database.widgetDao()
    private val todoDao = database.todoDao()
    private val noteDao = database.noteDao()
    private val countdownDao = database.countdownDao()
    private val newsDao = database.newsDao()
    private val storeInstallDao = database.storeInstallDao()
    private val habitDao = database.habitDao()

    val widgetsFlow: Flow<List<WidgetEntity>> = widgetDao.observeAllWidgets()
    val todosFlow: Flow<List<TodoEntity>> = todoDao.observeAllTodos()
    val notesFlow: Flow<List<NoteEntity>> = noteDao.observeAllNotes()
    val countdownsFlow: Flow<List<CountdownEntity>> = countdownDao.observeAllCountdowns()
    val rssSourcesFlow: Flow<List<RssSourceEntity>> = newsDao.observeRssSources()
    val articlesFlow: Flow<List<NewsArticleEntity>> = newsDao.observeArticles()
    val storeInstallsFlow: Flow<List<StoreInstallEntity>> = storeInstallDao.observeInstalls()
    val habitsFlow: Flow<List<HabitEntity>> = habitDao.observeAllHabits()
    val settingsFlow = prefs.settingsFlow

    suspend fun saveWidget(entity: WidgetEntity): Long {
        val now = System.currentTimeMillis()
        return if (entity.id == 0L) {
            widgetDao.insertWidget(entity.copy(createdAt = now, updatedAt = now))
        } else {
            widgetDao.updateWidget(entity.copy(updatedAt = now))
            entity.id
        }
    }

    suspend fun getWidgetById(id: Long): WidgetEntity? = widgetDao.getWidgetById(id)

    suspend fun deleteWidget(id: Long) {
        widgetDao.deleteWidgetById(id)
        storeInstallDao.deleteByWidgetId(id)
    }

    suspend fun markWidgetPinned(id: Long, appWidgetId: Int) {
        widgetDao.markPinned(id, pinned = true, appWidgetId = appWidgetId)
    }

    suspend fun installStorePack(pack: StoreWidgetPack): Long {
        val entity = WidgetEntity(
            title = pack.title,
            widgetType = pack.category.name,
            themeStyle = pack.themeStyle.name,
            clockStyle = pack.clockStyle.name,
            fontStyle = pack.fontStyle.name,
            opacity = pack.opacity,
            blurRadius = pack.blurRadius,
            cornerRadius = pack.cornerRadius,
            accentColorHex = pack.accentColorHex,
            textColorHex = pack.textColorHex,
            backgroundPreset = pack.backgroundPreset.name,
            animationsEnabled = true,
            refreshIntervalMinutes = 30,
            blocksJson = BlockJsonCodec.encode(pack.blocks),
            sourceStoreId = pack.id,
            isCustom = false
        )
        val newId = widgetDao.insertWidget(entity)
        storeInstallDao.insertInstall(StoreInstallEntity(storeItemId = pack.id, installedWidgetId = newId))
        return newId
    }

    // Todos
    suspend fun addTodo(title: String, dueHours: Int?, reminderEnabled: Boolean, priority: Int = 1) {
        if (title.isBlank()) return
        val dueMillis = dueHours?.let { System.currentTimeMillis() + TimeUnit.HOURS.toMillis(it.toLong()) }
        val id = todoDao.insertTodo(
            TodoEntity(
                title = title.trim(),
                dueAtMillis = dueMillis,
                reminderEnabled = reminderEnabled,
                priority = priority
            )
        )
        if (reminderEnabled && dueMillis != null) {
            ReminderNotifier.scheduleReminder(
                context = context,
                requestCode = (2000 + id).toInt(),
                triggerAtMillis = dueMillis,
                title = "HWidget 待辦到期提醒",
                body = title.trim()
            )
        }
    }

    suspend fun updateTodo(
        existing: TodoEntity,
        newTitle: String,
        dueHoursFromNow: Int?,
        reminderEnabled: Boolean,
        priority: Int
    ) {
        if (newTitle.isBlank()) return
        val updatedDue = if (dueHoursFromNow != null) {
            System.currentTimeMillis() + TimeUnit.HOURS.toMillis(dueHoursFromNow.toLong())
        } else {
            existing.dueAtMillis
        }
        val updated = existing.copy(
            title = newTitle.trim(),
            dueAtMillis = updatedDue,
            reminderEnabled = reminderEnabled,
            priority = priority
        )
        todoDao.updateTodo(updated)
        if (reminderEnabled && updatedDue != null && !updated.isCompleted) {
            ReminderNotifier.scheduleReminder(
                context = context,
                requestCode = (2000 + existing.id).toInt(),
                triggerAtMillis = updatedDue,
                title = "HWidget 待辦到期提醒",
                body = updated.title
            )
        }
    }

    suspend fun toggleTodo(todo: TodoEntity) {
        todoDao.updateTodo(todo.copy(isCompleted = !todo.isCompleted))
    }

    suspend fun deleteTodo(id: Long) {
        todoDao.deleteTodoById(id)
    }

    // Habits
    suspend fun addHabit(title: String) {
        if (title.isBlank()) return
        habitDao.insertHabit(com.example.data.db.HabitEntity(title = title.trim()))
    }

    suspend fun toggleHabit(habit: com.example.data.db.HabitEntity) {
        val nextChecked = !habit.checkedToday
        val nextStreak = if (nextChecked) habit.streakDays + 1 else (habit.streakDays - 1).coerceAtLeast(0)
        habitDao.updateHabit(habit.copy(checkedToday = nextChecked, streakDays = nextStreak))
    }

    suspend fun deleteHabit(id: Long) {
        habitDao.deleteHabitById(id)
    }

    // Sticky Notes
    suspend fun addNote(title: String, content: String, colorKey: String) {
        if (content.isBlank() && title.isBlank()) return
        noteDao.insertNote(
            NoteEntity(
                title = title.ifBlank { "便利貼" }.trim(),
                content = content.trim(),
                colorKey = colorKey,
                isPinned = true,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateNoteColor(note: NoteEntity, colorKey: String) {
        noteDao.updateNote(note.copy(colorKey = colorKey, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(id: Long) {
        noteDao.deleteNoteById(id)
    }

    // Countdowns
    suspend fun addCountdown(
        title: String,
        category: String,
        daysFromNow: Int,
        reminderEnabled: Boolean,
        accentHex: Long
    ) {
        if (title.isBlank()) return
        val targetMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysFromNow.toLong())
        val id = countdownDao.insertCountdown(
            CountdownEntity(
                title = title.trim(),
                category = category,
                targetDateMillis = targetMillis,
                reminderEnabled = reminderEnabled,
                accentHex = accentHex
            )
        )
        if (reminderEnabled && daysFromNow > 0) {
            val trigger = targetMillis - TimeUnit.HOURS.toMillis(12)
            ReminderNotifier.scheduleReminder(
                context = context,
                requestCode = (5000 + id).toInt(),
                triggerAtMillis = trigger,
                title = "HWidget 倒數日提醒",
                body = "「${title.trim()}」即將到來！"
            )
        }
    }

    suspend fun deleteCountdown(id: Long) {
        countdownDao.deleteCountdownById(id)
    }

    // Weather
    suspend fun refreshWeather(cityName: String, lat: Double, lon: Double): WeatherSnapshot {
        val snapshot = weatherService.fetchWeather(cityName, lat, lon)
        prefs.setCachedWeatherSummary(
            "${snapshot.cityName} ${snapshot.temperatureC.toInt()}°C · ${snapshot.conditionText} · 降雨 ${snapshot.precipitationProb}%"
        )
        return snapshot
    }

    suspend fun searchCities(query: String) = weatherService.searchCities(query)

    // RSS News
    suspend fun addRssSource(name: String, url: String, autoUpdateMinutes: Int) {
        if (url.isBlank()) return
        val cleanUrl = if (url.startsWith("http")) url.trim() else "https://${url.trim()}"
        val sourceId = newsDao.insertSource(
            RssSourceEntity(
                name = name.ifBlank { cleanUrl }.trim(),
                feedUrl = cleanUrl,
                autoUpdateMinutes = autoUpdateMinutes,
                isActive = true
            )
        )
        runCatching {
            val articles = rssFeedService.fetchFeed(sourceId, name.ifBlank { cleanUrl }, cleanUrl)
            if (articles.isNotEmpty()) {
                newsDao.insertArticles(articles)
            }
        }
    }

    suspend fun deleteRssSource(id: Long) {
        newsDao.deleteArticlesBySource(id)
        newsDao.deleteSourceById(id)
    }

    suspend fun syncAllRssFeeds(): Int {
        val sources = newsDao.getActiveSourcesOnce()
        var totalInserted = 0
        for (src in sources) {
            val fetched = runCatching {
                rssFeedService.fetchFeed(src.id, src.name, src.feedUrl)
            }.getOrDefault(emptyList())
            if (fetched.isNotEmpty()) {
                newsDao.deleteArticlesBySource(src.id)
                newsDao.insertArticles(fetched)
                newsDao.updateSource(src.copy(lastFetchedAt = System.currentTimeMillis()))
                totalInserted += fetched.size
            }
        }
        return totalInserted
    }

    // Store Catalog (56 Official + Third-party unique widgets)
    fun getStoreCatalog(): List<StoreWidgetPack> = StoreCatalog.allPacks()
}
