package com.zzzzico12.weatherwidget

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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

        // 1. フォアグラウンドサービスとして登録を試みる（Android 14+ の SecurityException 対策）
        try {
            startForeground(NOTIFICATION_ID, createNotification())
            // 2. 即座にUIを「更新中」に変更
            WeatherWidgetProvider.showLoadingImmediate(this, appWidgetId)
        } catch (e: Exception) {
            Log.e("WeatherService", "Could not start foreground service", e)
            // 権限不足などでフォアグラウンド化できない場合は、エラーを表示して終了する
            val manager = AppWidgetManager.getInstance(this)
            val isWhiteText = getSharedPreferences(WeatherWidgetProvider.PREFS_NAME, MODE_PRIVATE)
                .getBoolean(WeatherWidgetProvider.PREF_TEXT_COLOR_PREFIX + appWidgetId, true)
            WeatherWidgetProvider.showError(this, manager, appWidgetId, getString(R.string.error_location), isWhiteText)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        scope.launch {
            try {
                withTimeout(20000) {
                    updateWeather(appWidgetId)
                }
            } catch (e: Exception) {
                Log.e("WeatherService", "Update failed", e)
                val manager = AppWidgetManager.getInstance(this@WeatherUpdateService)
                val prefs = getSharedPreferences(WeatherWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
                val isWhiteText = prefs.getBoolean(WeatherWidgetProvider.PREF_TEXT_COLOR_PREFIX + appWidgetId, true)
                WeatherWidgetProvider.showError(this@WeatherUpdateService, manager, appWidgetId, getString(R.string.error_network), isWhiteText)
            } finally {
                stopSelf(startId)
            }
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

        val location = getCurrentLocation()
        val lat: Double
        val lon: Double
        
        if (location != null) {
            lat = location.latitude
            lon = location.longitude
            prefs.edit().putString("last_lat", lat.toString()).putString("last_lon", lon.toString()).apply()
        } else {
            val savedLat = prefs.getString("last_lat", null)
            val savedLon = prefs.getString("last_lon", null)
            if (savedLat != null && savedLon != null) {
                lat = savedLat.toDouble()
                lon = savedLon.toDouble()
            } else {
                WeatherWidgetProvider.showError(this, appWidgetManager, appWidgetId, getString(R.string.error_location), isWhiteText)
                return
            }
        }

        val weatherInfo = repository.fetchWeather(lat, lon)
        val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))

        WeatherWidgetProvider.updateWidget(
            this, appWidgetManager, appWidgetId,
            weatherInfo.temperature, weatherInfo.needUmbrella, timeStr, isWhiteText
        )
    }

    private suspend fun getCurrentLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        return try {
            val last = fusedLocationClient.lastLocation.await()
            if (last != null && (System.currentTimeMillis() - last.time) < 1000 * 60 * 60) {
                last
            } else {
                withTimeoutOrNull(5000) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createNotification(): Notification {
        val channelId = "weather_update_channel"
        val channel = NotificationChannel(channelId, "天気情報の更新", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("天気を更新しています")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1
    }
}
