package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.data.db.HWidgetDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        HWidgetUpdater.updateWidgetIdsAsync(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        HWidgetUpdater.updateWidgetIdsAsync(context, intArrayOf(appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        cleanupDeletedWidgets(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            HWidgetUpdater.ACTION_REFRESH,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val targetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (targetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    HWidgetUpdater.updateWidgetIdsAsync(context, intArrayOf(targetId))
                } else {
                    HWidgetUpdater.updateAllWidgetsAsync(context)
                }
            }
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                HWidgetUpdater.updateAllWidgetsAsync(context)
            }
        }
    }
}

class HWidgetClockProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        HWidgetUpdater.updateWidgetIdsAsync(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        HWidgetUpdater.updateWidgetIdsAsync(context, intArrayOf(appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        cleanupDeletedWidgets(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_DATE_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            HWidgetUpdater.updateAllWidgetsAsync(context)
        }
    }
}

private fun cleanupDeletedWidgets(context: Context, appWidgetIds: IntArray) {
    for (id in appWidgetIds) {
        WidgetBindingStore.unbind(context, id)
        HWidgetSurfaceProvider.deleteSurfacesForWidget(context, id)
    }
    CoroutineScope(Dispatchers.IO).launch {
        val dao = HWidgetDatabase.getInstance(context.applicationContext).widgetDao()
        for (id in appWidgetIds) {
            runCatching { dao.clearPinnedByAppWidgetId(id) }
        }
    }
}
