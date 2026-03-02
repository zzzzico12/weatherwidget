package com.zzzzico12.weatherwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews

class WeatherWidgetProvider : AppWidgetProvider() {

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

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            startUpdateService(context, appWidgetId)
        }
    }

    private fun startUpdateService(context: Context, appWidgetId: Int) {
        val serviceIntent = Intent(context, WeatherUpdateService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        for (id in appWidgetIds) {
            prefs.edit().remove(PREF_TEXT_COLOR_PREFIX + id).apply()
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.zzzzico12.weatherwidget.ACTION_UPDATE_WIDGET"
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

            views.setTextViewText(R.id.tv_temperature, temperature)
            views.setTextColor(R.id.tv_temperature, textColor)
            views.setTextViewText(R.id.tv_updated, updatedTime)
            views.setTextColor(R.id.tv_updated, subtleColor)

            views.setTextColor(R.id.tv_umbrella_icon, textColor)
            views.setTextColor(R.id.tv_umbrella_label, textColor)
            views.setTextViewText(R.id.tv_umbrella_icon, "☂")

            if (needUmbrella) {
                views.setViewVisibility(R.id.tv_no_umbrella_cross, View.GONE)
                views.setTextViewText(R.id.tv_umbrella_label, context.getString(R.string.need_umbrella))
            } else {
                views.setViewVisibility(R.id.tv_no_umbrella_cross, View.VISIBLE)
                views.setTextViewText(R.id.tv_umbrella_label, context.getString(R.string.no_umbrella))
            }

            // 直接Serviceを起動するPendingIntentを作成（タップ時の権限昇格を狙う）
            val intent = Intent(context, WeatherUpdateService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("widget://update/$appWidgetId/${System.currentTimeMillis()}")
            }
            
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    context, appWidgetId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    context, appWidgetId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun showError(context: Context, manager: AppWidgetManager, widgetId: Int, msg: String, isWhiteText: Boolean) {
            val views = RemoteViews(context.packageName, R.layout.weather_widget)
            val textColor = if (isWhiteText) 0xFFFFFFFF.toInt() else 0xFF212121.toInt()
            views.setTextViewText(R.id.tv_temperature, "!")
            views.setTextColor(R.id.tv_temperature, textColor)
            views.setTextViewText(R.id.tv_umbrella_icon, "⚠️")
            views.setViewVisibility(R.id.tv_no_umbrella_cross, View.GONE)
            views.setTextViewText(R.id.tv_umbrella_label, msg)
            views.setTextColor(R.id.tv_umbrella_label, textColor)
            manager.updateAppWidget(widgetId, views)
        }
        
        fun showLoadingImmediate(context: Context, appWidgetId: Int) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isWhiteText = prefs.getBoolean(PREF_TEXT_COLOR_PREFIX + appWidgetId, true)
            
            val views = RemoteViews(context.packageName, R.layout.weather_widget)
            val textColor = if (isWhiteText) 0xFFFFFFFF.toInt() else 0xFF212121.toInt()
            
            views.setTextViewText(R.id.tv_temperature, "…")
            views.setTextColor(R.id.tv_temperature, textColor)
            views.setTextViewText(R.id.tv_umbrella_icon, "⌛")
            views.setViewVisibility(R.id.tv_no_umbrella_cross, View.GONE)
            views.setTextViewText(R.id.tv_umbrella_label, "更新中...")
            views.setTextColor(R.id.tv_umbrella_label, textColor)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
