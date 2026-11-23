package com.example.widget

import android.content.Context

object WidgetBindingStore {
    private const val PREFS_NAME = "hwidget_appwidget_bindings"
    private const val KEY_PREFIX = "binding_"
    private const val KEY_PENDING_WIDGET_ID = "pending_pin_widget_id"
    private const val KEY_PENDING_TIME = "pending_pin_time"
    private const val PENDING_TTL_MS = 90_000L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun bind(context: Context, appWidgetId: Int, widgetDbId: Long) {
        if (appWidgetId <= 0 || widgetDbId <= 0L) return
        prefs(context).edit()
            .putLong("$KEY_PREFIX$appWidgetId", widgetDbId)
            .remove(KEY_PENDING_WIDGET_ID)
            .remove(KEY_PENDING_TIME)
            .apply()
    }

    fun getWidgetDbId(context: Context, appWidgetId: Int): Long? {
        if (appWidgetId <= 0) return null
        val id = prefs(context).getLong("$KEY_PREFIX$appWidgetId", -1L)
        return if (id > 0L) id else null
    }

    fun unbind(context: Context, appWidgetId: Int) {
        if (appWidgetId <= 0) return
        prefs(context).edit()
            .remove("$KEY_PREFIX$appWidgetId")
            .apply()
    }

    fun setPendingPinWidgetId(context: Context, widgetDbId: Long) {
        prefs(context).edit()
            .putLong(KEY_PENDING_WIDGET_ID, widgetDbId)
            .putLong(KEY_PENDING_TIME, System.currentTimeMillis())
            .apply()
    }

    fun consumePendingPinIfRecent(context: Context): Long? {
        val p = prefs(context)
        val widgetId = p.getLong(KEY_PENDING_WIDGET_ID, -1L)
        val ts = p.getLong(KEY_PENDING_TIME, 0L)
        if (widgetId > 0L && System.currentTimeMillis() - ts <= PENDING_TTL_MS) {
            p.edit()
                .remove(KEY_PENDING_WIDGET_ID)
                .remove(KEY_PENDING_TIME)
                .apply()
            return widgetId
        }
        return null
    }
}
