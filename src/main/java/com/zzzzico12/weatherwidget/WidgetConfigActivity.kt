package com.zzzzico12.weatherwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isWhiteText = true // デフォルトを白に設定

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

        val btnWhite = findViewById<Button>(R.id.btn_white)
        val btnBlack = findViewById<Button>(R.id.btn_black)
        val btnAdd = findViewById<Button>(R.id.btn_add_widget)

        btnWhite.setOnClickListener {
            isWhiteText = true
            // 視覚的なフィードバックが必要な場合はここで背景色などを変更する処理を追加
        }

        btnBlack.setOnClickListener {
            isWhiteText = false
        }

        btnAdd.setOnClickListener {
            saveColorPref(this, appWidgetId, isWhiteText)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            
            // 手動で更新サービスを起動
            val serviceIntent = Intent(this, WeatherUpdateService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            startService(serviceIntent)

            finish()
        }
    }

    private fun saveColorPref(context: Context, widgetId: Int, isWhite: Boolean) {
        val prefs = context.getSharedPreferences(WeatherWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(WeatherWidgetProvider.PREF_TEXT_COLOR_PREFIX + widgetId, isWhite).apply()
    }
}
