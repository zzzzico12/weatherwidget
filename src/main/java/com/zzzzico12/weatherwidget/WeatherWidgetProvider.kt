package com.zzzzico12.weatherwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            startUpdateService(context, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                startUpdateService(context, appWidgetId)
            }
        }
    }

    private fun startUpdateService(context: Context, appWidgetId: Int) {
        val serviceIntent = Intent(context, WeatherUpdateService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        try {
            context.startService(serviceIntent)
        } catch (e: Exception) {
            // Background restrictions on Android 12+ handled by getService in updateWidget
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (appWidgetId in appWidgetIds) {
            editor.remove(PREF_TEXT_COLOR_PREFIX + appWidgetId)
        }
        editor.apply()
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.zzzzico12.weatherwidget.UPDATE_WIDGET"
        const val PREFS_NAME = "WeatherWidgetPrefs"
        const val PREF_TEXT_COLOR_PREFIX = "text_color_"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            temperature: String,
            needUmbrella: Boolean,
            updatedTime: String,
            isWhiteText: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.weather_widget)
            val textColor = if (isWhiteText) 0xFFFFFFFF.toInt() else 0xFF212121.toInt()
            val subtleColor = if (isWhiteText) 0xBBFFFFFF.toInt() else 0x88000000.toInt()

            // Left side: Temperature and time
            views.setTextViewText(R.id.tv_temperature, temperature)
            views.setTextColor(R.id.tv_temperature, textColor)
            views.setTextViewText(R.id.tv_updated, updatedTime)
            views.setTextColor(R.id.tv_updated, subtleColor)

            // Right side: Umbrella Icon + Label
            views.setTextColor(R.id.tv_umbrella_icon, textColor)
            views.setTextColor(R.id.tv_umbrella_label, textColor)
            views.setTextViewText(R.id.tv_umbrella_icon, "☂")

            if (needUmbrella) {
                views.setViewVisibility(R.id.tv_no_umbrella_cross, View.GONE)
                views.setTextViewText(R.id.tv_umbrella_label, context.getString(R.string.need_umbrella))
            } else {
                // Show X over the umbrella icon
                views.setViewVisibility(R.id.tv_no_umbrella_cross, View.VISIBLE)
                views.setTextViewText(R.id.tv_umbrella_label, context.getString(R.string.no_umbrella))
            }

            // Tap to refresh
            val serviceIntent = Intent(context, WeatherUpdateService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val pendingIntent = PendingIntent.getService(
                context,
                appWidgetId,
                serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
