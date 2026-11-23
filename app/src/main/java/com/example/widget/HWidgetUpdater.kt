package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.BlockJsonCodec
import com.example.data.db.HWidgetDatabase
import com.example.data.db.WidgetEntity
import com.example.data.device.BatteryMonitor
import com.example.data.device.SystemStatsMonitor
import com.example.data.prefs.UserPreferencesRepository
import com.example.domain.model.BlockType
import com.example.domain.model.WidgetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object HWidgetUpdater {

    const val ACTION_REFRESH = "com.example.HWIDGET_ACTION_REFRESH"
    const val ACTION_PIN_CONFIRMED = "com.example.HWIDGET_ACTION_PIN_CONFIRMED"
    const val EXTRA_WIDGET_DB_ID = "extra_widget_db_id"

    fun updateAllWidgetsAsync(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            updateAllWidgets(context.applicationContext)
        }
    }

    fun updateWidgetIdsAsync(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            val appCtx = context.applicationContext
            val snapshot = loadLiveSnapshot(appCtx)
            val manager = AppWidgetManager.getInstance(appCtx)
            for (id in appWidgetIds) {
                updateSingleWidgetInternal(appCtx, manager, id, snapshot)
            }
        }
    }

    suspend fun updateWidgetInstance(context: Context, appWidgetId: Int) {
        val appCtx = context.applicationContext
        val manager = AppWidgetManager.getInstance(appCtx)
        val snapshot = loadLiveSnapshot(appCtx)
        updateSingleWidgetInternal(appCtx, manager, appWidgetId, snapshot)
    }

    suspend fun updateAllWidgets(context: Context) {
        val appCtx = context.applicationContext
        val manager = AppWidgetManager.getInstance(appCtx)
        val compositeIds = manager.getAppWidgetIds(ComponentName(appCtx, HWidgetProvider::class.java))
        val clockIds = manager.getAppWidgetIds(ComponentName(appCtx, HWidgetClockProvider::class.java))
        if (compositeIds.isEmpty() && clockIds.isEmpty()) return

        val snapshot = loadLiveSnapshot(appCtx)
        for (id in compositeIds) {
            updateSingleWidgetInternal(appCtx, manager, id, snapshot, forceClockLayout = false)
        }
        for (id in clockIds) {
            updateSingleWidgetInternal(appCtx, manager, id, snapshot, forceClockLayout = true)
        }
    }

    private suspend fun loadLiveSnapshot(context: Context): LiveWidgetDataSnapshot {
        val db = HWidgetDatabase.getInstance(context)
        val settings = UserPreferencesRepository(context).getSnapshot()
        return LiveWidgetDataSnapshot(
            settings = settings,
            battery = BatteryMonitor.readCurrentSnapshot(context),
            systemStats = SystemStatsMonitor.readStats(context),
            todos = db.todoDao().getActiveTodosOnce(),
            topNote = db.noteDao().getTopNoteOnce(),
            countdowns = db.countdownDao().getUpcomingCountdownsOnce(),
            articles = db.newsDao().getTopArticlesOnce(),
            habits = db.habitDao().getAllHabitsOnce()
        )
    }

    private suspend fun resolveWidgetForAppWidgetId(context: Context, appWidgetId: Int): WidgetEntity? {
        val dao = HWidgetDatabase.getInstance(context).widgetDao()
        val boundId = WidgetBindingStore.getWidgetDbId(context, appWidgetId)
        if (boundId != null) {
            dao.getWidgetById(boundId)?.let { return it }
        }
        dao.getWidgetByAppWidgetId(appWidgetId)?.let { entity ->
            WidgetBindingStore.bind(context, appWidgetId, entity.id)
            return entity
        }
        val pendingId = WidgetBindingStore.consumePendingPinIfRecent(context)
        if (pendingId != null) {
            dao.getWidgetById(pendingId)?.let { entity ->
                WidgetBindingStore.bind(context, appWidgetId, entity.id)
                dao.markPinned(entity.id, pinned = true, appWidgetId = appWidgetId)
                return entity
            }
        }
        return dao.getAllWidgetsOnce().firstOrNull()
    }

    private suspend fun updateSingleWidgetInternal(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        snapshot: LiveWidgetDataSnapshot,
        forceClockLayout: Boolean? = null
    ) {
        val widget = resolveWidgetForAppWidgetId(context, appWidgetId)
        val isClockProvider = forceClockLayout ?: run {
            val info = manager.getAppWidgetInfo(appWidgetId)
            info?.provider?.className == HWidgetClockProvider::class.java.name
        }
        val options = manager.getAppWidgetOptions(appWidgetId)
        val targetWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            .takeIf { it > 0 }?.toFloat()
            ?: if (isClockProvider) 280f else 340f

        val views = buildRemoteViewsForWidget(
            context = context,
            widget = widget,
            appWidgetId = appWidgetId,
            isClockProvider = isClockProvider,
            targetWidthDp = targetWidthDp,
            snapshot = snapshot
        )
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun buildRemoteViewsForWidget(
        context: Context,
        widget: WidgetEntity?,
        appWidgetId: Int,
        isClockProvider: Boolean,
        targetWidthDp: Float,
        snapshot: LiveWidgetDataSnapshot
    ): RemoteViews {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (widget != null) {
                putExtra(EXTRA_WIDGET_DB_ID, widget.id)
            }
        }
        val openPending = PendingIntent.getActivity(
            context,
            (appWidgetId.coerceAtLeast(1) * 10) + 1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rendered = WidgetBitmapRenderer.renderWidget(
            context = context,
            widget = widget,
            snapshot = snapshot,
            targetWidthDp = targetWidthDp,
            renderScale = 2.5f
        )

        val layoutId = if (isClockProvider) {
            R.layout.widget_hwidget_clock
        } else {
            R.layout.widget_hwidget_composite
        }
        val rootId = if (isClockProvider) {
            R.id.clock_widget_root
        } else {
            R.id.widget_root
        }

        return RemoteViews(context.packageName, layoutId).apply {
            if (appWidgetId > 0) {
                val uri = HWidgetSurfaceProvider.saveSurfaceAndGetUri(context, appWidgetId, rendered)
                rendered.recycle()
                setImageViewUri(R.id.widget_surface_image, uri)
            } else {
                val safePreview = WidgetBitmapRenderer.scaleForBinderPreview(rendered)
                if (safePreview !== rendered) rendered.recycle()
                setImageViewBitmap(R.id.widget_surface_image, safePreview)
            }
            setOnClickPendingIntent(rootId, openPending)
            setOnClickPendingIntent(R.id.widget_surface_image, openPending)
        }
    }

    suspend fun requestPinToHomeScreen(context: Context, widget: WidgetEntity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false

        val isClockOnly = widget.widgetType == WidgetType.CLOCK.name &&
            BlockJsonCodec.decode(widget.blocksJson).all { it.type == BlockType.TIME.name }

        val provider = ComponentName(
            context,
            if (isClockOnly) HWidgetClockProvider::class.java else HWidgetProvider::class.java
        )

        WidgetBindingStore.setPendingPinWidgetId(context, widget.id)

        val callbackIntent = Intent(context, HWidgetPinReceiver::class.java).apply {
            action = ACTION_PIN_CONFIRMED
            putExtra(EXTRA_WIDGET_DB_ID, widget.id)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val successCallback = PendingIntent.getBroadcast(
            context,
            widget.id.toInt().coerceAtLeast(1),
            callbackIntent,
            flags
        )

        val snapshot = loadLiveSnapshot(context)
        val previewViews = buildRemoteViewsForWidget(
            context = context,
            widget = widget,
            appWidgetId = 0,
            isClockProvider = isClockOnly,
            targetWidthDp = if (isClockOnly) 280f else 340f,
            snapshot = snapshot
        )
        val extras = Bundle().apply {
            putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewViews)
        }

        return manager.requestPinAppWidget(provider, extras, successCallback)
    }
}
