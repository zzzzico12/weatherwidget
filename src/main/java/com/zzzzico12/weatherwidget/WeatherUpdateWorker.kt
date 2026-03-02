package com.zzzzico12.weatherwidget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.location.Location
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeatherUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val repository = WeatherRepository()

    override suspend fun doWork(): Result {
        val appWidgetId = inputData.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return Result.failure()

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val prefs = context.getSharedPreferences(WeatherWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        val isWhiteText = prefs.getBoolean(WeatherWidgetProvider.PREF_TEXT_COLOR_PREFIX + appWidgetId, true)

        return try {
            // 1. 位置情報の取得（キャッシュ優先、失敗時は保存された座標を使用）
            val location = getCurrentLocation()
            
            val lat: Double
            val lon: Double
            
            if (location != null) {
                lat = location.latitude
                lon = location.longitude
                // 座標を保存
                prefs.edit().putString("last_lat", lat.toString()).putString("last_lon", lon.toString()).apply()
            } else {
                // 保存された座標を読み出す
                val savedLat = prefs.getString("last_lat", null)
                val savedLon = prefs.getString("last_lon", null)
                if (savedLat != null && savedLon != null) {
                    lat = savedLat.toDouble()
                    lon = savedLon.toDouble()
                } else {
                    showError(appWidgetManager, appWidgetId, context.getString(R.string.error_location), isWhiteText)
                    return Result.failure()
                }
            }

            // 2. 天気取得
            val weatherInfo = repository.fetchWeather(lat, lon)
            
            // 3. ウィジェット更新
            val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
            WeatherWidgetProvider.updateWidget(
                context, appWidgetManager, appWidgetId,
                weatherInfo.temperature, weatherInfo.needUmbrella, timeStr, isWhiteText
            )

            Result.success()
        } catch (e: Exception) {
            Log.e("WeatherWorker", "Update failed", e)
            showError(appWidgetManager, appWidgetId, context.getString(R.string.error_network), isWhiteText)
            Result.failure()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        return try {
            // まずキャッシュ
            val last = fusedLocationClient.lastLocation.await()
            if (last != null && (System.currentTimeMillis() - last.time) < 1000 * 60 * 60) {
                last
            } else {
                // ダメなら測位（WorkManagerならバックグラウンドでも比較的安定）
                withTimeoutOrNull(5000) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun showError(manager: AppWidgetManager, widgetId: Int, msg: String, isWhiteText: Boolean) {
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
}
