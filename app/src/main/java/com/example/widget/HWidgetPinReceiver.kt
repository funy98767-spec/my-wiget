package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.HWidgetDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HWidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != HWidgetUpdater.ACTION_PIN_CONFIRMED) return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val widgetDbId = intent.getLongExtra(HWidgetUpdater.EXTRA_WIDGET_DB_ID, -1L)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || widgetDbId <= 0L) return

        WidgetBindingStore.bind(context, appWidgetId, widgetDbId)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val db = HWidgetDatabase.getInstance(context)
                db.widgetDao().markPinned(widgetDbId, pinned = true, appWidgetId = appWidgetId)
                HWidgetUpdater.updateWidgetInstance(context.applicationContext, appWidgetId)
            }
            pendingResult.finish()
        }
    }
}
