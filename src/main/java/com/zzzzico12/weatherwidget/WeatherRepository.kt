package com.zzzzico12.weatherwidget

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

@Serializable
data class WeatherResponse(
    val current: CurrentWeather,
    val daily: DailyWeather
)

@Serializable
data class CurrentWeather(
    @SerialName("temperature_2m") val temperature: Double,
    val precipitation: Double,
    val weathercode: Int
)

@Serializable
data class DailyWeather(
    @SerialName("precipitation_sum") val precipitationSum: List<Double>,
    val weathercode: List<Int>
)

data class WeatherInfo(
    val temperature: String,
    val needUmbrella: Boolean
)

class WeatherRepository {
    companion object {
        private val client = HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(15, TimeUnit.SECONDS)
                    readTimeout(15, TimeUnit.SECONDS)
                    writeTimeout(15, TimeUnit.SECONDS)
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 15000
            }
        }
        
        private val RAIN_CODES = setOf(
            51, 53, 55, 61, 63, 65, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99
        )
    }

    suspend fun fetchWeather(lat: Double, lon: Double): WeatherInfo {
        val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,precipitation,weathercode" +
                "&daily=precipitation_sum,weathercode" +
                "&timezone=auto" +
                "&forecast_days=1"

        return try {
            val response: WeatherResponse = client.get(url).body()
            
            val tempStr = "${response.current.temperature.toInt()}°"
            val precipSum = response.daily.precipitationSum.firstOrNull() ?: 0.0
            val weatherCode = response.daily.weathercode.firstOrNull() ?: 0
            val needUmbrella = precipSum > 0.5 || weatherCode in RAIN_CODES

            WeatherInfo(tempStr, needUmbrella)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
