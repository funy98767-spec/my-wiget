package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.BlockJsonCodec
import com.example.data.db.CountdownEntity
import com.example.data.db.HWidgetDatabase
import com.example.data.db.HabitEntity
import com.example.data.db.NewsArticleEntity
import com.example.data.db.NoteEntity
import com.example.data.db.RssSourceEntity
import com.example.data.db.TodoEntity
import com.example.data.db.WidgetEntity
import com.example.data.device.BatteryMonitor
import com.example.data.device.ReminderNotifier
import com.example.data.device.StepMonitor
import com.example.data.device.SystemStatsMonitor
import com.example.data.device.SystemStatsSnapshot
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.prefs.UserSettings
import com.example.data.remote.GeocodingResultDto
import com.example.data.repository.HWidgetRepository
import com.example.domain.model.BackgroundPreset
import com.example.domain.model.BatterySnapshot
import com.example.domain.model.BlockType
import com.example.domain.model.ClockDisplayStyle
import com.example.domain.model.StepSnapshot
import com.example.domain.model.StoreWidgetPack
import com.example.domain.model.WeatherSnapshot
import com.example.domain.model.WidgetBlock
import com.example.domain.model.WidgetFontStyle
import com.example.domain.model.WidgetThemeStyle
import com.example.domain.model.WidgetType
import com.example.widget.HWidgetUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class WidgetEditorState(
    val editingWidgetId: Long = 0L,
    val title: String = "我的專屬組合 Widget",
    val widgetType: WidgetType = WidgetType.COMPOSITE,
    val themeStyle: WidgetThemeStyle = WidgetThemeStyle.GLASS,
    val clockStyle: ClockDisplayStyle = ClockDisplayStyle.DIGITAL,
    val fontStyle: WidgetFontStyle = WidgetFontStyle.SPACE,
    val opacity: Float = 0.88f,
    val blurRadius: Float = 14f,
    val cornerRadius: Float = 24f,
    val accentColorHex: Long = 0xFF4F46E5L,
    val textColorHex: Long = 0xFFFFFFFFL,
    val backgroundPreset: BackgroundPreset = BackgroundPreset.AURORA,
    val backgroundImageUri: String? = null,
    val animationsEnabled: Boolean = true,
    val refreshIntervalMinutes: Int = 30,
    val isCustom: Boolean = true,
    val blocks: List<WidgetBlock> = listOf(
        WidgetBlock(id = "init_time", type = BlockType.TIME.name, orderIndex = 0, spanFullWidth = true),
        WidgetBlock(id = "init_bat", type = BlockType.BATTERY.name, orderIndex = 1),
        WidgetBlock(id = "init_wea", type = BlockType.WEATHER.name, orderIndex = 2)
    )
) {
    fun toEntity(): WidgetEntity = WidgetEntity(
        id = editingWidgetId,
        title = title.ifBlank { "未命名 Widget" },
        widgetType = if (blocks.size > 1) WidgetType.COMPOSITE.name else widgetType.name,
        themeStyle = themeStyle.name,
        clockStyle = clockStyle.name,
        fontStyle = fontStyle.name,
        opacity = opacity,
        blurRadius = blurRadius,
        cornerRadius = cornerRadius,
        accentColorHex = accentColorHex,
        textColorHex = textColorHex,
        backgroundPreset = backgroundPreset.name,
        backgroundImageUri = backgroundImageUri,
        animationsEnabled = animationsEnabled,
        refreshIntervalMinutes = refreshIntervalMinutes,
        isCustom = isCustom,
        blocksJson = BlockJsonCodec.encode(blocks.mapIndexed { idx, b -> b.copy(orderIndex = idx) })
    )
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HWidgetViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = HWidgetDatabase.getInstance(context)
    private val prefsRepo = UserPreferencesRepository(context)
    private val repository = HWidgetRepository(context, database, prefsRepo)

    val settings: StateFlow<UserSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val widgets: StateFlow<List<WidgetEntity>> = repository.widgetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todos: StateFlow<List<TodoEntity>> = repository.todosFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = repository.notesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val countdowns: StateFlow<List<CountdownEntity>> = repository.countdownsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rssSources: StateFlow<List<RssSourceEntity>> = repository.rssSourcesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val articles: StateFlow<List<NewsArticleEntity>> = repository.articlesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installedStoreIds: StateFlow<Set<String>> = repository.storeInstallsFlow
        .map { list -> list.map { it.storeItemId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val habits: StateFlow<List<HabitEntity>> = repository.habitsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val battery: StateFlow<BatterySnapshot> = BatteryMonitor.observeBattery(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BatterySnapshot())

    val steps: StateFlow<StepSnapshot> = settings
        .flatMapLatest { s -> StepMonitor.observeSteps(context, prefsRepo, s.dailyStepGoal) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StepSnapshot())

    private val _systemStats = MutableStateFlow(SystemStatsMonitor.readStats(context))
    val systemStats: StateFlow<SystemStatsSnapshot> = _systemStats.asStateFlow()

    private val _pomodoroSeconds = MutableStateFlow(25 * 60)
    val pomodoroSeconds: StateFlow<Int> = _pomodoroSeconds.asStateFlow()

    private val _pomodoroRunning = MutableStateFlow(false)
    val pomodoroRunning: StateFlow<Boolean> = _pomodoroRunning.asStateFlow()

    private val _stopwatchSeconds = MutableStateFlow(0)
    val stopwatchSeconds: StateFlow<Int> = _stopwatchSeconds.asStateFlow()

    private val _stopwatchRunning = MutableStateFlow(false)
    val stopwatchRunning: StateFlow<Boolean> = _stopwatchRunning.asStateFlow()

    private val _weather = MutableStateFlow(WeatherSnapshot())
    val weather: StateFlow<WeatherSnapshot> = _weather.asStateFlow()

    private val _liveClockMillis = MutableStateFlow(System.currentTimeMillis())
    val liveClockMillis: StateFlow<Long> = _liveClockMillis.asStateFlow()

    private val _editorState = MutableStateFlow(WidgetEditorState())
    val editorState: StateFlow<WidgetEditorState> = _editorState.asStateFlow()

    private val _cityResults = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val cityResults: StateFlow<List<GeocodingResultDto>> = _cityResults.asStateFlow()

    private val _isSyncingRss = MutableStateFlow(false)
    val isSyncingRss: StateFlow<Boolean> = _isSyncingRss.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val storeCatalog: List<StoreWidgetPack> = repository.getStoreCatalog()

    init {
        ReminderNotifier.ensureChannel(context)
        startClockTicker()
        refreshWeatherFromSettings()
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            var tick = 0
            while (true) {
                _liveClockMillis.value = System.currentTimeMillis()
                if (_pomodoroRunning.value) {
                    val next = _pomodoroSeconds.value - 1
                    if (next <= 0) {
                        _pomodoroSeconds.value = 25 * 60
                        _pomodoroRunning.value = false
                        postMessage("番茄專注 25 分鐘完成！")
                    } else {
                        _pomodoroSeconds.value = next
                    }
                }
                if (_stopwatchRunning.value) {
                    _stopwatchSeconds.value = _stopwatchSeconds.value + 1
                }
                if (tick % 5 == 0) {
                    _systemStats.value = SystemStatsMonitor.readStats(context)
                }
                tick++
                delay(1000L)
            }
        }
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }

    fun postMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun refreshWeatherFromSettings() {
        viewModelScope.launch {
            val s = prefsRepo.getSnapshot()
            runCatching {
                repository.refreshWeather(s.selectedCityName, s.selectedCityLat, s.selectedCityLon)
            }.onSuccess { snap ->
                _weather.value = snap
                HWidgetUpdater.updateAllWidgetsAsync(context)
            }.onFailure {
                // Keep cached fallback if offline
                _weather.value = _weather.value.copy(cityName = s.selectedCityName, isLiveFromApi = false)
            }
        }
    }

    fun selectCity(name: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            prefsRepo.setSelectedCity(name, lat, lon)
            _cityResults.value = emptyList()
            refreshWeatherFromSettings()
            postMessage("已切換天氣城市為 $name")
        }
    }

    fun searchCities(query: String) {
        viewModelScope.launch {
            _cityResults.value = runCatching { repository.searchCities(query) }.getOrDefault(emptyList())
        }
    }

    fun syncRssFeeds() {
        viewModelScope.launch {
            _isSyncingRss.value = true
            val count = runCatching { repository.syncAllRssFeeds() }.getOrDefault(0)
            _isSyncingRss.value = false
            postMessage(if (count > 0) "已更新 $count 則即時新聞" else "新聞來源暫無新文章或離線中")
        }
    }

    fun addRssSource(name: String, url: String, autoUpdateMinutes: Int) {
        viewModelScope.launch {
            _isSyncingRss.value = true
            repository.addRssSource(name, url, autoUpdateMinutes)
            _isSyncingRss.value = false
            postMessage("已新增 RSS 來源：$name")
        }
    }

    fun deleteRssSource(id: Long) {
        viewModelScope.launch { repository.deleteRssSource(id) }
    }

    // Todos
    fun addTodo(title: String, dueHours: Int?, reminderEnabled: Boolean, priority: Int = 1) {
        viewModelScope.launch {
            repository.addTodo(title, dueHours, reminderEnabled, priority)
            HWidgetUpdater.updateAllWidgetsAsync(context)
            postMessage("已新增待辦事項")
        }
    }

    fun updateTodo(
        existing: TodoEntity,
        newTitle: String,
        dueHoursFromNow: Int?,
        reminderEnabled: Boolean,
        priority: Int
    ) {
        viewModelScope.launch {
            repository.updateTodo(existing, newTitle, dueHoursFromNow, reminderEnabled, priority)
            HWidgetUpdater.updateAllWidgetsAsync(context)
            postMessage("已更新待辦：$newTitle")
        }
    }

    fun toggleTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.toggleTodo(todo)
            HWidgetUpdater.updateAllWidgetsAsync(context)
        }
    }

    fun deleteTodo(id: Long) {
        viewModelScope.launch {
            repository.deleteTodo(id)
            HWidgetUpdater.updateAllWidgetsAsync(context)
        }
    }

    // Pomodoro, Water, Habits, Quote
    fun togglePomodoro() {
        _pomodoroRunning.value = !_pomodoroRunning.value
    }

    fun resetPomodoro() {
        _pomodoroRunning.value = false
        _pomodoroSeconds.value = 25 * 60
    }

    fun toggleStopwatch() {
        _stopwatchRunning.value = !_stopwatchRunning.value
    }

    fun resetStopwatch() {
        _stopwatchRunning.value = false
        _stopwatchSeconds.value = 0
    }

    fun incrementCounter(delta: Int) {
        viewModelScope.launch {
            prefsRepo.incrementCounter(delta)
            HWidgetUpdater.updateAllWidgetsAsync(context)
        }
    }

    fun resetCounter() {
        viewModelScope.launch {
            prefsRepo.resetCounter()
            HWidgetUpdater.updateAllWidgetsAsync(context)
        }
    }

    fun setMoodTag(mood: String) {
        viewModelScope.launch {
            prefsRepo.setMoodTag(mood)
            HWidgetUpdater.updateAllWidgetsAsync(context)
            postMessage("已更新今日心情：$mood")
        }
    }

    fun addWaterMl(deltaMl: Int) {
        viewModelScope.launch {
            prefsRepo.addWaterMl(deltaMl)
            HWidgetUpdater.updateAllWidgetsAsync(context)
            postMessage(if (deltaMl > 0) "已記錄喝水 +${deltaMl}ml" else "已調整喝水量")
        }
    }

    fun setWaterGoalMl(goalMl: Int) {
        viewModelScope.launch {
            prefsRepo.setWaterGoalMl(goalMl)
            HWidgetUpdater.updateAllWidgetsAsync(context)
        }
    }

    fun updateCustomQuote(text: String, author: String) {
        viewModelScope.launch {
            prefsRepo.setCustomQuote(text, author)
            HWidgetUpdater.updateAllWidgetsAsync(context)
            postMessage("已更新每日語錄")
        }
    }

    fun addHabit(title: String) {
        viewModelScope.launch {
            repository.addHabit(title)
            HWidgetUpdater.updateAllWidgetsAsync(context)
            postMessage("已新增每日習慣：$title")
        }
    }

    fun toggleHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.toggleHabit(habit)
            HWidgetUpdater.updateAllWidgetsAsync(context)
        }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch {
            repository.deleteHabit(id)
            HWidgetUpdater.updateAllWidgetsAsync(context)
        }
    }

    // Notes
    fun addNote(title: String, content: String, colorKey: String) {
        viewModelScope.launch {
            repository.addNote(title, content, colorKey)
            HWidgetUpdater.updateAllWidgetsAsync(context)
            postMessage("已儲存便利貼筆記")
        }
    }

    fun updateNoteColor(note: NoteEntity, colorKey: String) {
        viewModelScope.launch {
            repository.updateNoteColor(note, colorKey)
            HWidgetUpdater.updateAllWidgetsAsync(context)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
            HWidgetUpdater.updateAllWidgetsAsync(context)
        }
    }

    // Countdowns
    fun addCountdown(title: String, category: String, daysFromNow: Int, reminderEnabled: Boolean, accentHex: Long) {
        viewModelScope.launch {
            repository.addCountdown(title, category, daysFromNow, reminderEnabled, accentHex)
            HWidgetUpdater.updateAllWidgetsAsync(context)
            postMessage("已新增倒數日：$title")
        }
    }

    fun deleteCountdown(id: Long) {
        viewModelScope.launch {
            repository.deleteCountdown(id)
            HWidgetUpdater.updateAllWidgetsAsync(context)
        }
    }

    // Widget Store
    fun installStorePack(pack: StoreWidgetPack) {
        viewModelScope.launch {
            repository.installStorePack(pack)
            HWidgetUpdater.updateAllWidgetsAsync(context)
            postMessage("已安裝「${pack.title}」到我的桌面")
        }
    }

    fun installAndPinStorePack(pack: StoreWidgetPack) {
        viewModelScope.launch {
            val existing = widgets.value.firstOrNull { it.sourceStoreId == pack.id && !it.isCustom }
            val entity = existing ?: run {
                val newId = repository.installStorePack(pack)
                repository.getWidgetById(newId)
            }
            if (entity != null) {
                val launched = HWidgetUpdater.requestPinToHomeScreen(context, entity)
                if (launched) {
                    postMessage("已發送釘選「${entity.title}」到桌面請求，請在系統彈窗確認")
                } else {
                    postMessage("已加入「${entity.title}」；請於手機桌面長按空白處 -> 小工具 -> 選擇「HWidget」挑選該小工具")
                }
            }
        }
    }

    fun deleteWidget(id: Long) {
        viewModelScope.launch {
            repository.deleteWidget(id)
            HWidgetUpdater.updateAllWidgetsAsync(context)
            postMessage("已移除 Widget")
        }
    }

    fun pinWidgetToHomeScreen(widget: WidgetEntity) {
        viewModelScope.launch {
            val launched = HWidgetUpdater.requestPinToHomeScreen(context, widget)
            if (launched) {
                postMessage("已發送釘選「${widget.title}」到桌面請求，請在系統彈窗確認")
            } else {
                postMessage("請於手機桌面長按空白處 -> 小工具 -> 選擇「HWidget」後挑選「${widget.title}」")
            }
        }
    }

    // Studio / Visual Drag-and-Drop Editor
    fun startNewWidgetInEditor(baseType: WidgetType = WidgetType.COMPOSITE) {
        val defaultBlocks = when (baseType) {
            WidgetType.CLOCK -> listOf(WidgetBlock("b_time", BlockType.TIME.name, 0, spanFullWidth = true))
            WidgetType.WEATHER -> listOf(WidgetBlock("b_wea", BlockType.WEATHER.name, 0, spanFullWidth = true))
            WidgetType.TODO -> listOf(WidgetBlock("b_todo", BlockType.TODO.name, 0, spanFullWidth = true))
            WidgetType.NEWS -> listOf(WidgetBlock("b_news", BlockType.NEWS.name, 0, spanFullWidth = true))
            WidgetType.NOTE -> listOf(WidgetBlock("b_note", BlockType.NOTE.name, 0, spanFullWidth = true))
            WidgetType.COUNTDOWN -> listOf(WidgetBlock("b_cd", BlockType.COUNTDOWN.name, 0, spanFullWidth = true))
            WidgetType.BATTERY -> listOf(WidgetBlock("b_bat", BlockType.BATTERY.name, 0, spanFullWidth = true))
            WidgetType.STEPS -> listOf(WidgetBlock("b_stp", BlockType.STEPS.name, 0, spanFullWidth = true))
            WidgetType.POMODORO -> listOf(WidgetBlock("b_pomo", BlockType.POMODORO.name, 0, spanFullWidth = true))
            WidgetType.WATER -> listOf(WidgetBlock("b_wat", BlockType.WATER.name, 0, spanFullWidth = true))
            WidgetType.HABIT -> listOf(WidgetBlock("b_hab", BlockType.HABIT.name, 0, spanFullWidth = true))
            WidgetType.SYSTEM_STATS -> listOf(WidgetBlock("b_sys", BlockType.SYSTEM_STATS.name, 0, spanFullWidth = true))
            WidgetType.QUOTE -> listOf(WidgetBlock("b_quo", BlockType.QUOTE.name, 0, spanFullWidth = true))
            WidgetType.QUICK_LAUNCH -> listOf(WidgetBlock("b_ql", BlockType.QUICK_LAUNCH.name, 0, spanFullWidth = true))
            WidgetType.CALENDAR_MONTH -> listOf(WidgetBlock("b_cal", BlockType.CALENDAR_MONTH.name, 0, spanFullWidth = true))
            WidgetType.WORLD_CLOCK -> listOf(WidgetBlock("b_wc", BlockType.WORLD_CLOCK.name, 0, spanFullWidth = true))
            WidgetType.DAY_PROGRESS -> listOf(WidgetBlock("b_dp", BlockType.DAY_PROGRESS.name, 0, spanFullWidth = true))
            WidgetType.STOPWATCH -> listOf(WidgetBlock("b_sw", BlockType.STOPWATCH.name, 0))
            WidgetType.COUNTER -> listOf(WidgetBlock("b_cnt", BlockType.COUNTER.name, 0))
            WidgetType.MOOD -> listOf(WidgetBlock("b_mood", BlockType.MOOD.name, 0, spanFullWidth = true))
            WidgetType.BREATHING -> listOf(WidgetBlock("b_br", BlockType.BREATHING.name, 0, spanFullWidth = true))
            WidgetType.NETWORK -> listOf(WidgetBlock("b_net", BlockType.NETWORK.name, 0))
            WidgetType.DEVICE_INFO -> listOf(WidgetBlock("b_dev", BlockType.DEVICE_INFO.name, 0, spanFullWidth = true))
            WidgetType.CALORIE_DISTANCE -> listOf(WidgetBlock("b_calor", BlockType.CALORIE_DISTANCE.name, 0))
            WidgetType.COMPOSITE -> listOf(
                WidgetBlock("b_c1", BlockType.TIME.name, 0, spanFullWidth = true),
                WidgetBlock("b_c2", BlockType.BATTERY.name, 1),
                WidgetBlock("b_c3", BlockType.WEATHER.name, 2)
            )
        }
        _editorState.value = WidgetEditorState(
            editingWidgetId = 0L,
            title = if (baseType == WidgetType.COMPOSITE) "我的專屬自訂 Widget" else "自訂${baseType.labelZh} Widget",
            widgetType = baseType,
            isCustom = true,
            blocks = defaultBlocks
        )
    }

    fun loadWidgetIntoEditor(entity: WidgetEntity, cloneAsCustom: Boolean = false) {
        val parsedBlocks = BlockJsonCodec.decode(entity.blocksJson)
        _editorState.value = WidgetEditorState(
            editingWidgetId = if (cloneAsCustom) 0L else entity.id,
            title = if (cloneAsCustom) "${entity.title} (自訂版)" else entity.title,
            widgetType = runCatching { WidgetType.valueOf(entity.widgetType) }.getOrDefault(WidgetType.COMPOSITE),
            themeStyle = runCatching { WidgetThemeStyle.valueOf(entity.themeStyle) }.getOrDefault(WidgetThemeStyle.GLASS),
            clockStyle = runCatching { ClockDisplayStyle.valueOf(entity.clockStyle) }.getOrDefault(ClockDisplayStyle.DIGITAL),
            fontStyle = runCatching { WidgetFontStyle.valueOf(entity.fontStyle) }.getOrDefault(WidgetFontStyle.SANS),
            opacity = entity.opacity,
            blurRadius = entity.blurRadius,
            cornerRadius = entity.cornerRadius,
            accentColorHex = entity.accentColorHex,
            textColorHex = entity.textColorHex,
            backgroundPreset = runCatching { BackgroundPreset.valueOf(entity.backgroundPreset) }.getOrDefault(BackgroundPreset.AURORA),
            backgroundImageUri = entity.backgroundImageUri,
            animationsEnabled = entity.animationsEnabled,
            refreshIntervalMinutes = entity.refreshIntervalMinutes,
            isCustom = if (cloneAsCustom) true else entity.isCustom,
            blocks = if (parsedBlocks.isNotEmpty()) parsedBlocks else listOf(
                WidgetBlock("b_fallback", BlockType.TIME.name, 0, spanFullWidth = true)
            )
        )
    }

    fun loadStorePackAsCustomInEditor(pack: StoreWidgetPack) {
        _editorState.value = WidgetEditorState(
            editingWidgetId = 0L,
            title = "${pack.title} (自訂)",
            widgetType = pack.category,
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
            refreshIntervalMinutes = 30,
            isCustom = true,
            blocks = pack.blocks
        )
    }

    fun updateEditor(transform: (WidgetEditorState) -> WidgetEditorState) {
        _editorState.update(transform)
    }

    fun addBlockToEditor(type: BlockType, imageUri: String? = null, extensionId: String? = null) {
        _editorState.update { state ->
            val newBlock = WidgetBlock(
                id = UUID.randomUUID().toString().take(8),
                type = type.name,
                orderIndex = state.blocks.size,
                imageUri = imageUri,
                extensionId = extensionId,
                spanFullWidth = type in setOf(
                    BlockType.TIME, BlockType.TODO, BlockType.NEWS, BlockType.NOTE,
                    BlockType.HABIT, BlockType.QUOTE, BlockType.QUICK_LAUNCH,
                    BlockType.CALENDAR_MONTH, BlockType.WORLD_CLOCK, BlockType.DAY_PROGRESS,
                    BlockType.MOOD, BlockType.BREATHING, BlockType.DEVICE_INFO, BlockType.SYSTEM_STATS
                )
            )
            state.copy(blocks = state.blocks + newBlock)
        }
    }

    fun removeBlockFromEditor(blockId: String) {
        _editorState.update { state ->
            if (state.blocks.size <= 1) return@update state
            state.copy(blocks = state.blocks.filterNot { it.id == blockId })
        }
    }

    fun moveBlockInEditor(fromIndex: Int, toIndex: Int) {
        _editorState.update { state ->
            val list = state.blocks.toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) {
                return@update state
            }
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            state.copy(blocks = list.mapIndexed { idx, b -> b.copy(orderIndex = idx) })
        }
    }

    fun toggleBlockWidth(blockId: String) {
        _editorState.update { state ->
            state.copy(
                blocks = state.blocks.map {
                    if (it.id == blockId) it.copy(spanFullWidth = !it.spanFullWidth) else it
                }
            )
        }
    }

    fun saveEditorWidget(onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val savedId = repository.saveWidget(_editorState.value.toEntity())
            _editorState.update { it.copy(editingWidgetId = savedId) }
            HWidgetUpdater.updateAllWidgetsAsync(context)
            postMessage("已儲存「${_editorState.value.title}」並同步桌面 Widget")
            onSaved(savedId)
        }
    }

    // Settings
    fun setThemeMode(mode: String) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDynamicColorEnabled(enabled) }
    }

    fun setDailyStepGoal(goal: Int) {
        viewModelScope.launch { prefsRepo.setDailyStepGoal(goal) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setNotificationsEnabled(enabled) }
    }

    fun setDefaultRefreshMinutes(minutes: Int) {
        viewModelScope.launch { prefsRepo.setDefaultRefreshMinutes(minutes) }
    }

    fun toggleExtensionMount(extensionKey: String) {
        viewModelScope.launch {
            prefsRepo.toggleExtensionMount(extensionKey)
            postMessage("已更新擴充模組掛載狀態")
        }
    }

    fun sendTestNotification() {
        ReminderNotifier.showNotificationNow(
            context = context,
            id = 9901,
            title = "HWidget 提醒測試成功",
            body = "待辦到期與倒數日通知通道已就緒！"
        )
        postMessage("已發送測試通知")
    }
}
