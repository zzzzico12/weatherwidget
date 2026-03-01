package com.example.weatherwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isWhiteText = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set RESULT_CANCELED so back press = cancel
        setResult(RESULT_CANCELED)

        // Get widget ID
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.widget_config)

        val btnWhite = findViewById<Button>(R.id.btn_white)
        val btnBlack = findViewById<Button>(R.id.btn_black)
        val btnAdd = findViewById<Button>(R.id.btn_add_widget)

        // Highlight selected button
        fun updateSelection() {
            btnWhite.alpha = if (isWhiteText) 1.0f else 0.5f
            btnBlack.alpha = if (!isWhiteText) 1.0f else 0.5f
        }
        updateSelection()

        btnWhite.setOnClickListener {
            isWhiteText = true
            updateSelection()
        }

        btnBlack.setOnClickListener {
            isWhiteText = false
            updateSelection()
        }

        btnAdd.setOnClickListener {
            savePrefsAndLaunchWidget()
        }
    }

    private fun savePrefsAndLaunchWidget() {
        // Save color preference
        val prefs = getSharedPreferences(WeatherWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(WeatherWidgetProvider.PREF_TEXT_COLOR_PREFIX + appWidgetId, isWhiteText)
            .apply()

        // Trigger first update
        val updateIntent = Intent(this, WeatherUpdateService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        startService(updateIntent)

        // Return OK to home screen
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}
