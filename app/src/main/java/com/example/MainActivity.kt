package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AuroraAmbientBackdrop
import com.example.ui.components.DataHubModalSheet
import com.example.ui.components.DataHubSheetType
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StoreScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.HWidgetTheme
import com.example.ui.viewmodel.HWidgetViewModel

const val ROUTE_HOME = "home"
const val ROUTE_STUDIO = "studio"
const val ROUTE_STORE = "store"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: HWidgetViewModel = viewModel()
            HWidgetRootApp(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.widget.HWidgetUpdater.updateAllWidgetsAsync(this)
    }
}

@Composable
fun HWidgetRootApp(viewModel: HWidgetViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val widgets by viewModel.widgets.collectAsStateWithLifecycle()
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val countdowns by viewModel.countdowns.collectAsStateWithLifecycle()
    val rssSources by viewModel.rssSources.collectAsStateWithLifecycle()
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val installedStoreIds by viewModel.installedStoreIds.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val systemStats by viewModel.systemStats.collectAsStateWithLifecycle()
    val pomodoroSeconds by viewModel.pomodoroSeconds.collectAsStateWithLifecycle()
    val pomodoroRunning by viewModel.pomodoroRunning.collectAsStateWithLifecycle()
    val stopwatchSeconds by viewModel.stopwatchSeconds.collectAsStateWithLifecycle()
    val stopwatchRunning by viewModel.stopwatchRunning.collectAsStateWithLifecycle()
    val battery by viewModel.battery.collectAsStateWithLifecycle()
    val steps by viewModel.steps.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val clockMillis by viewModel.liveClockMillis.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val cityResults by viewModel.cityResults.collectAsStateWithLifecycle()
    val isSyncingRss by viewModel.isSyncingRss.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var activeSheet by remember { mutableStateOf<DataHubSheetType?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeStatusMessage()
    }

    val navItems = remember {
        listOf(
            NavDestinationItem(ROUTE_HOME, "已安裝", Icons.Rounded.Widgets, Icons.Outlined.Widgets),
            NavDestinationItem(ROUTE_STUDIO, "建立", Icons.Rounded.AutoAwesomeMosaic, Icons.Outlined.AutoAwesomeMosaic),
            NavDestinationItem(ROUTE_STORE, "商店", Icons.Rounded.Storefront, Icons.Outlined.Storefront),
            NavDestinationItem(ROUTE_SETTINGS, "設定", Icons.Rounded.Settings, Icons.Outlined.Settings)
        )
    }

    HWidgetTheme(
        themeMode = settings.themeMode,
        dynamicColor = settings.dynamicColorEnabled
    ) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

        fun navigateTo(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        AuroraAmbientBackdrop(themeMode = settings.themeMode) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useNavigationRail) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                navItems.forEach { item ->
                                    val selected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTo(item.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = { Text(item.label) },
                                        modifier = Modifier.testTag("nav_${item.route}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavigationRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                navItems.forEach { item ->
                                    val selected = currentRoute == item.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTo(item.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = { Text(item.label) },
                                        modifier = Modifier.testTag("rail_${item.route}")
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_HOME,
                                modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()
                            ) {
                                composable(ROUTE_HOME) {
                                    HomeScreen(
                                        widgets = widgets,
                                        clockMillis = clockMillis,
                                        weather = weather,
                                        battery = battery,
                                        steps = steps,
                                        todos = todos,
                                        notes = notes,
                                        countdowns = countdowns,
                                        articles = articles,
                                        habits = habits,
                                        systemStats = systemStats,
                                        pomodoroSeconds = pomodoroSeconds,
                                        pomodoroRunning = pomodoroRunning,
                                        waterMlToday = settings.waterMlToday,
                                        waterGoalMl = settings.waterGoalMl,
                                        quoteText = settings.quoteText,
                                        quoteAuthor = settings.quoteAuthor,
                                        stopwatchSeconds = stopwatchSeconds,
                                        stopwatchRunning = stopwatchRunning,
                                        counterValue = settings.counterValue,
                                        moodTag = settings.moodTag,
                                        mountedExtensions = settings.mountedExtensions,
                                        onCreateNewWidget = {
                                            viewModel.startNewWidgetInEditor()
                                            navigateTo(ROUTE_STUDIO)
                                        },
                                        onEditWidget = { widget ->
                                            viewModel.loadWidgetIntoEditor(widget, cloneAsCustom = false)
                                            navigateTo(ROUTE_STUDIO)
                                        },
                                        onCloneAsCustom = { widget ->
                                            viewModel.loadWidgetIntoEditor(widget, cloneAsCustom = true)
                                            navigateTo(ROUTE_STUDIO)
                                        },
                                        onPinWidget = { widget -> viewModel.pinWidgetToHomeScreen(widget) },
                                        onDeleteWidget = { id -> viewModel.deleteWidget(id) },
                                        onOpenDataHub = { sheet -> activeSheet = sheet },
                                        onOpenStore = { navigateTo(ROUTE_STORE) },
                                        onToggleTodo = viewModel::toggleTodo,
                                        onTogglePomodoro = viewModel::togglePomodoro,
                                        onResetPomodoro = viewModel::resetPomodoro,
                                        onAddWaterMl = viewModel::addWaterMl,
                                        onToggleHabit = viewModel::toggleHabit,
                                        onQuickAction = { actionKey ->
                                            when (actionKey) {
                                                "TODO" -> activeSheet = DataHubSheetType.TODO
                                                "NOTE" -> activeSheet = DataHubSheetType.NOTE
                                                "HABIT" -> activeSheet = DataHubSheetType.HABIT_WATER
                                                "ALARM" -> runCatching {
                                                    context.startActivity(
                                                        Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                    )
                                                }
                                            }
                                        },
                                        onToggleStopwatch = viewModel::toggleStopwatch,
                                        onResetStopwatch = viewModel::resetStopwatch,
                                        onIncrementCounter = viewModel::incrementCounter,
                                        onResetCounter = viewModel::resetCounter,
                                        onSelectMood = viewModel::setMoodTag
                                    )
                                }

                                composable(ROUTE_STUDIO) {
                                    StudioScreen(
                                        editorState = editorState,
                                        readyMadeCatalog = viewModel.storeCatalog,
                                        clockMillis = clockMillis,
                                        weather = weather,
                                        battery = battery,
                                        steps = steps,
                                        todos = todos,
                                        notes = notes,
                                        countdowns = countdowns,
                                        articles = articles,
                                        habits = habits,
                                        systemStats = systemStats,
                                        pomodoroSeconds = pomodoroSeconds,
                                        pomodoroRunning = pomodoroRunning,
                                        waterMlToday = settings.waterMlToday,
                                        waterGoalMl = settings.waterGoalMl,
                                        quoteText = settings.quoteText,
                                        quoteAuthor = settings.quoteAuthor,
                                        stopwatchSeconds = stopwatchSeconds,
                                        stopwatchRunning = stopwatchRunning,
                                        counterValue = settings.counterValue,
                                        moodTag = settings.moodTag,
                                        mountedExtensions = settings.mountedExtensions,
                                        onUpdateEditor = viewModel::updateEditor,
                                        onAddBlock = viewModel::addBlockToEditor,
                                        onRemoveBlock = viewModel::removeBlockFromEditor,
                                        onMoveBlock = viewModel::moveBlockInEditor,
                                        onToggleBlockWidth = viewModel::toggleBlockWidth,
                                        onResetNew = viewModel::startNewWidgetInEditor,
                                        onSaveWidget = {
                                            viewModel.saveEditorWidget {
                                                navigateTo(ROUTE_HOME)
                                            }
                                        },
                                        onInstallReadyMadePack = { pack ->
                                            viewModel.installStorePack(pack)
                                            navigateTo(ROUTE_HOME)
                                        },
                                        onLoadReadyMadeAsCustom = { pack ->
                                            viewModel.loadStorePackAsCustomInEditor(pack)
                                        }
                                    )
                                }

                                composable(ROUTE_STORE) {
                                    StoreScreen(
                                        catalog = viewModel.storeCatalog,
                                        installedStoreIds = installedStoreIds,
                                        clockMillis = clockMillis,
                                        weather = weather,
                                        battery = battery,
                                        steps = steps,
                                        todos = todos,
                                        notes = notes,
                                        countdowns = countdowns,
                                        articles = articles,
                                        habits = habits,
                                        systemStats = systemStats,
                                        pomodoroSeconds = pomodoroSeconds,
                                        pomodoroRunning = pomodoroRunning,
                                        waterMlToday = settings.waterMlToday,
                                        waterGoalMl = settings.waterGoalMl,
                                        quoteText = settings.quoteText,
                                        quoteAuthor = settings.quoteAuthor,
                                        stopwatchSeconds = stopwatchSeconds,
                                        stopwatchRunning = stopwatchRunning,
                                        counterValue = settings.counterValue,
                                        moodTag = settings.moodTag,
                                        mountedExtensions = settings.mountedExtensions,
                                        onInstallPack = viewModel::installStorePack,
                                        onPinPack = viewModel::installAndPinStorePack,
                                        onToggleExtensionMount = viewModel::toggleExtensionMount
                                    )
                                }

                                composable(ROUTE_SETTINGS) {
                                    SettingsScreen(
                                        settings = settings,
                                        weather = weather,
                                        steps = steps,
                                        widgetCount = widgets.size,
                                        onSetThemeMode = viewModel::setThemeMode,
                                        onSetDynamicColor = viewModel::setDynamicColor,
                                        onSetDailyStepGoal = viewModel::setDailyStepGoal,
                                        onSetNotificationsEnabled = viewModel::setNotificationsEnabled,
                                        onSetDefaultRefreshMinutes = viewModel::setDefaultRefreshMinutes,
                                        onSendTestNotification = viewModel::sendTestNotification,
                                        onOpenWeatherCityPicker = { activeSheet = DataHubSheetType.WEATHER },
                                        onToggleExtensionMount = viewModel::toggleExtensionMount
                                    )
                                }
                            }
                        }
                    }
                }
            }

            activeSheet?.let { sheetType ->
                DataHubModalSheet(
                    sheetType = sheetType,
                    todos = todos,
                    notes = notes,
                    countdowns = countdowns,
                    habits = habits,
                    waterMlToday = settings.waterMlToday,
                    waterGoalMl = settings.waterGoalMl,
                    quoteText = settings.quoteText,
                    quoteAuthor = settings.quoteAuthor,
                    weather = weather,
                    cityResults = cityResults,
                    rssSources = rssSources,
                    articles = articles,
                    isSyncingRss = isSyncingRss,
                    onDismiss = { activeSheet = null },
                    onAddTodo = viewModel::addTodo,
                    onUpdateTodo = viewModel::updateTodo,
                    onToggleTodo = viewModel::toggleTodo,
                    onDeleteTodo = viewModel::deleteTodo,
                    onAddNote = viewModel::addNote,
                    onUpdateNoteColor = viewModel::updateNoteColor,
                    onDeleteNote = viewModel::deleteNote,
                    onAddCountdown = viewModel::addCountdown,
                    onDeleteCountdown = viewModel::deleteCountdown,
                    onSearchCity = viewModel::searchCities,
                    onSelectCity = viewModel::selectCity,
                    onRefreshWeather = viewModel::refreshWeatherFromSettings,
                    onAddRssSource = viewModel::addRssSource,
                    onDeleteRssSource = viewModel::deleteRssSource,
                    onSyncRss = viewModel::syncRssFeeds,
                    onAddHabit = viewModel::addHabit,
                    onToggleHabit = viewModel::toggleHabit,
                    onDeleteHabit = viewModel::deleteHabit,
                    onAddWaterMl = viewModel::addWaterMl,
                    onUpdateQuote = viewModel::updateCustomQuote
                )
            }
        }
    }
}
