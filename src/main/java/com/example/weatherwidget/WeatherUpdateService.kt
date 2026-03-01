package com.example.weatherwidget

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherUpdateService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val repository = WeatherRepository()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        scope.launch {
            updateWeather(appWidgetId)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private suspend fun updateWeather(appWidgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val prefs = getSharedPreferences(WeatherWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        val isWhiteText = prefs.getBoolean(
            WeatherWidgetProvider.PREF_TEXT_COLOR_PREFIX + appWidgetId,
            true
        )

        showLoading(appWidgetManager, appWidgetId, isWhiteText)

        try {
            val location = getLastKnownLocation()
            if (location == null) {
                showError(appWidgetManager, appWidgetId, getString(R.string.error_location), isWhiteText)
                return
            }

            // 共通リポジトリを使用して天気を取得
            val weatherInfo = repository.fetchWeather(location.latitude, location.longitude)
            val timeStr = SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date())

            WeatherWidgetProvider.updateWidget(
                this,
                appWidgetManager,
                appWidgetId,
                weatherInfo.temperature,
                weatherInfo.needUmbrella,
                timeStr,
                isWhiteText
            )

        } catch (e: Exception) {
            showError(appWidgetManager, appWidgetId, getString(R.string.error_network), isWhiteText)
        }
    }

    private fun getLastKnownLocation(): Location? {
        return try {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            var bestLocation: Location? = null
            for (provider in providers) {
                try {
                    val loc = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                } catch (_: SecurityException) {}
            }
            bestLocation
        } catch (_: Exception) { null }
    }

    private fun showLoading(manager: AppWidgetManager, widgetId: Int, isWhiteText: Boolean) {
        val views = RemoteViews(packageName, R.layout.weather_widget)
        val textColor = if (isWhiteText) 0xFFFFFFFF.toInt() else 0xFF212121.toInt()
        views.setTextViewText(R.id.tv_temperature, "…")
        views.setTextColor(R.id.tv_temperature, textColor)
        views.setTextViewText(R.id.tv_umbrella_icon, "⌛")
        views.setViewVisibility(R.id.tv_no_umbrella_cross, View.GONE)
        views.setTextViewText(R.id.tv_umbrella_label, getString(R.string.loading))
        views.setTextColor(R.id.tv_umbrella_label, textColor)
        manager.updateAppWidget(widgetId, views)
    }

    private fun showError(manager: AppWidgetManager, widgetId: Int, msg: String, isWhiteText: Boolean) {
        val views = RemoteViews(packageName, R.layout.weather_widget)
        val textColor = if (isWhiteText) 0xFFFFFFFF.toInt() else 0xFF212121.toInt()
        views.setTextViewText(R.id.tv_temperature, "!")
        views.setTextColor(R.id.tv_temperature, textColor)
        views.setTextViewText(R.id.tv_umbrella_icon, "⚠️")
        views.setViewVisibility(R.id.tv_no_umbrella_cross, View.GONE)
        views.setTextViewText(R.id.tv_umbrella_label, msg)
        views.setTextColor(R.id.tv_umbrella_label, textColor)
        manager.updateAppWidget(widgetId, views)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
