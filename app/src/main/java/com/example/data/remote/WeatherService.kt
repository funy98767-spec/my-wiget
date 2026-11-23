package com.example.data.remote

import com.example.domain.model.WeatherSnapshot
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    @Json(name = "current") val current: CurrentWeatherDto? = null,
    @Json(name = "hourly") val hourly: HourlyWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @Json(name = "temperature_2m") val temperature2m: Double = 24.0,
    @Json(name = "weather_code") val weatherCode: Int = 0,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double = 8.0
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    @Json(name = "results") val results: List<GeocodingResultDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResultDto(
    @Json(name = "name") val name: String,
    @Json(name = "country") val country: String? = null,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double
)

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code,wind_speed_10m",
        @Query("hourly") hourly: String = "precipitation_probability",
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "zh"
    ): GeocodingResponse
}

class WeatherService {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val forecastApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenMeteoForecastApi::class.java)

    private val geocodingApi = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenMeteoGeocodingApi::class.java)

    suspend fun fetchWeather(cityName: String, lat: Double, lon: Double): WeatherSnapshot {
        val response = forecastApi.getForecast(latitude = lat, longitude = lon)
        val current = response.current
        val code = current?.weatherCode ?: 1
        val rainProb = response.hourly?.precipitationProbability?.firstOrNull() ?: 10
        return WeatherSnapshot(
            cityName = cityName,
            temperatureC = current?.temperature2m ?: 24.0,
            conditionText = mapWmoCode(code),
            weatherCode = code,
            precipitationProb = rainProb,
            windKmh = current?.windSpeed10m ?: 8.0,
            isLiveFromApi = true,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    suspend fun searchCities(query: String): List<GeocodingResultDto> {
        if (query.isBlank()) return emptyList()
        return geocodingApi.searchCity(query.trim()).results.orEmpty()
    }

    private fun mapWmoCode(code: Int): String = when (code) {
        0 -> "晴朗無雲"
        1, 2 -> "多雲時晴"
        3 -> "陰天多雲"
        45, 48 -> "起霧微濛"
        51, 53, 55 -> "細雨綿綿"
        61, 63, 65 -> "陣雨降雨"
        71, 73, 75 -> "降雪"
        80, 81, 82 -> "局部陣雨"
        95, 96, 99 -> "雷雨"
        else -> "多雲"
    }

    companion object {
        val presetCities = listOf(
            Triple("Taipei 台北", 25.0330, 121.5654),
            Triple("Taichung 台中", 24.1477, 120.6736),
            Triple("Kaohsiung 高雄", 22.6273, 120.3014),
            Triple("Tokyo 東京", 35.6762, 139.6503),
            Triple("Seoul 首爾", 37.5665, 126.9780),
            Triple("Singapore 新加坡", 1.3521, 103.8198),
            Triple("San Francisco 舊金山", 37.7749, -122.4194),
            Triple("London 倫敦", 51.5072, -0.1276)
        )
    }
}
