package com.zzzzico12.weatherwidget

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isWhiteText = true

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            completeConfiguration()
        } else {
            showPermissionGuidanceDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.widget_config)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        findViewById<Button>(R.id.btn_white).setOnClickListener { isWhiteText = true }
        findViewById<Button>(R.id.btn_black).setOnClickListener { isWhiteText = false }
        findViewById<Button>(R.id.btn_add_widget).setOnClickListener { checkPermissionsAndSave() }
    }

    private fun checkPermissionsAndSave() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val isGranted = permissions.any {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (isGranted) {
            completeConfiguration()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun showPermissionGuidanceDialog() {
        AlertDialog.Builder(this)
            .setTitle("位置情報の許可が必要です")
            .setMessage("天気を表示するために位置情報の権限が必要です。「アプリの使用中のみ」または「常に許可」を選択してください。")
            .setPositiveButton("設定を開く") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun completeConfiguration() {
        saveColorPref(appWidgetId, isWhiteText)

        setResult(RESULT_OK, Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        })

        val serviceIntent = Intent(this, WeatherUpdateService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        
        // Target SDK 26+ (this project is 35) always uses startForegroundService
        startForegroundService(serviceIntent)
        finish()
    }

    private fun saveColorPref(widgetId: Int, isWhite: Boolean) {
        getSharedPreferences(WeatherWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(WeatherWidgetProvider.PREF_TEXT_COLOR_PREFIX + widgetId, isWhite)
            .apply()
    }
}
