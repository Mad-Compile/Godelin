package com.godelin.browser.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private enum class NewsCategory(val label: String, val query: String) {
    TOP("Top", "(world OR politics OR economy OR business OR technology OR science)"),
    WORLD("World", "(international OR diplomacy OR conflict OR government)"),
    TECHNOLOGY("Technology", "(technology OR artificial intelligence OR software OR cybersecurity)"),
    SCIENCE("Science", "(science OR research OR space OR climate)")
}

private data class NewsStory(
    val title: String,
    val domain: String,
    val url: String,
    val publishedAt: String
)

private data class WeatherPlace(
    val label: String,
    val latitude: Double,
    val longitude: Double
)

private data class WeatherSnapshot(
    val temperature: Double,
    val feelsLike: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val high: Double?,
    val low: Double?,
    val rainChance: Int?
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GodelinHomeFeed(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("godelin_home", Context.MODE_PRIVATE)
    }
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var city by remember { mutableStateOf(preferences.getString("weather_city", "").orEmpty()) }
    var latitude by remember { mutableStateOf(preferences.getString("weather_lat", null)?.toDoubleOrNull()) }
    var longitude by remember { mutableStateOf(preferences.getString("weather_lon", null)?.toDoubleOrNull()) }

    var weather by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var weatherLoading by remember { mutableStateOf(false) }
    var weatherError by remember { mutableStateOf(false) }
    var weatherRefresh by remember { mutableIntStateOf(0) }

    var category by remember { mutableStateOf(NewsCategory.TOP) }
    var stories by remember { mutableStateOf<List<NewsStory>>(emptyList()) }
    var newsLoading by remember { mutableStateOf(true) }
    var newsError by remember { mutableStateOf(false) }
    var newsRefresh by remember { mutableIntStateOf(0) }

    var showCityPicker by remember { mutableStateOf(false) }
    var cityQuery by remember { mutableStateOf("") }
    var cityMatches by remember { mutableStateOf<List<WeatherPlace>>(emptyList()) }
    var citySearchLoading by remember { mutableStateOf(false) }
    var citySearchError by remember { mutableStateOf(false) }

    LaunchedEffect(category, newsRefresh) {
        newsLoading = true
        newsError = false
        val result = withContext(Dispatchers.IO) {
            runCatching { fetchNews(category) }
        }
        stories = result.getOrDefault(emptyList())
        newsError = result.isFailure
        newsLoading = false
    }

    LaunchedEffect(latitude, longitude, weatherRefresh) {
        if (latitude == null || longitude == null) {
            weather = null
            weatherLoading = false
            weatherError = false
        } else {
            weatherLoading = true
            weatherError = false
            val result = withContext(Dispatchers.IO) {
                runCatching { fetchWeather(latitude!!, longitude!!) }
            }
            weather = result.getOrNull()
            weatherError = result.isFailure
            weatherLoading = false
        }
    }

    fun searchCities() {
        val query = cityQuery.trim()
        if (query.length < 2) return
        scope.launch {
            citySearchLoading = true
            citySearchError = false
            val result = withContext(Dispatchers.IO) {
                runCatching { fetchPlaces(query) }
            }
            cityMatches = result.getOrDefault(emptyList())
            citySearchError = result.isFailure || cityMatches.isEmpty()
            citySearchLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = remember {
                        java.time.LocalDate.now().format(
                            DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
                        )
                    },
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Today",
                    color = colors.onSurface,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.8).sp
                )
            }
            IconButton(onClick = { newsRefresh++; weatherRefresh++ }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh today's briefing",
                    tint = colors.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        WeatherPanel(
            city = city,
            weather = weather,
            loading = weatherLoading,
            error = weatherError,
            onChooseCity = {
                cityQuery = ""
                cityMatches = emptyList()
                citySearchError = false
                showCityPicker = true
            },
            onRetry = { weatherRefresh++ }
        )

        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Latest reporting",
                    color = colors.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Recent headlines from across the web",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = { newsRefresh++ }) {
                Text("Refresh")
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NewsCategory.entries.forEach { option ->
                FilterChip(
                    selected = category == option,
                    onClick = { category = option },
                    label = { Text(option.label) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        when {
            newsLoading && stories.isEmpty() -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colors.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Loading headlines", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
            newsError && stories.isEmpty() -> {
                FeedMessage(
                    title = "Headlines couldn't load",
                    detail = "Check your connection and try again.",
                    action = "Retry",
                    onAction = { newsRefresh++ }
                )
            }
            stories.isEmpty() -> {
                FeedMessage(
                    title = "No recent headlines",
                    detail = "There are no matching stories in this feed right now.",
                    action = "Refresh",
                    onAction = { newsRefresh++ }
                )
            }
            else -> {
                stories.forEachIndexed { index, story ->
                    NewsRow(story = story, onClick = { onNavigate(story.url) })
                    if (index != stories.lastIndex) {
                        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.65f))
                    }
                }
                Text(
                    text = "Headlines provided by GDELT. Stories open at the publisher.",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = "Shortcuts",
            color = colors.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShortcutChip("Google", Icons.Default.Search, Modifier.weight(1f)) { onNavigate("google.com") }
            ShortcutChip("YouTube", Icons.Default.PlayCircleOutline, Modifier.weight(1f)) { onNavigate("youtube.com") }
            ShortcutChip("Wikipedia", Icons.Default.MenuBook, Modifier.weight(1f)) { onNavigate("wikipedia.org") }
        }
    }

    if (showCityPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCityPicker = false },
            containerColor = colors.surface,
            contentColor = colors.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Choose a city",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Weather is looked up for the city you select. Godelin does not need your device location.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp, bottom = 14.dp)
                )
                OutlinedTextField(
                    value = cityQuery,
                    onValueChange = { cityQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("City or postal code") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchCities() })
                )
                Button(
                    onClick = { searchCities() },
                    enabled = cityQuery.trim().length >= 2 && !citySearchLoading,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 48.dp)
                ) {
                    if (citySearchLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Find city")
                }
                if (citySearchError) {
                    Text(
                        text = if (cityMatches.isEmpty()) "No matching city found. Try adding a country." else "City search failed. Try again.",
                        color = colors.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                cityMatches.forEach { place ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable {
                                city = place.label
                                latitude = place.latitude
                                longitude = place.longitude
                                preferences.edit()
                                    .putString("weather_city", place.label)
                                    .putString("weather_lat", place.latitude.toString())
                                    .putString("weather_lon", place.longitude.toString())
                                    .apply()
                                showCityPicker = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(place.label, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                            Text("Use this location for weather", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                    }
                    HorizontalDivider(color = colors.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun WeatherPanel(
    city: String,
    weather: WeatherSnapshot?,
    loading: Boolean,
    error: Boolean,
    onChooseCity: () -> Unit,
    onRetry: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Weather", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    Text(
                        text = city.ifBlank { "No city selected" },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                TextButton(onClick = onChooseCity) {
                    Text(if (city.isBlank()) "Choose city" else "Change")
                }
            }
            when {
                loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Getting the latest forecast", color = colors.onSurfaceVariant)
                    }
                }
                error -> {
                    Text(
                        text = "Weather is unavailable right now.",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = onRetry) { Text("Try again") }
                }
                weather == null -> {
                    Text(
                        text = "Choose a city to see current conditions and today's forecast.",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = weather.temperature.roundToInt().toString() + "°",
                                color = colors.onSurface,
                                fontSize = 48.sp,
                                lineHeight = 52.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-1.8).sp
                            )
                            Text(
                                text = weatherDescription(weather.weatherCode),
                                color = colors.onSurface,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Feels like " + weather.feelsLike.roundToInt() + "°",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(
                            imageVector = weatherIcon(weather.weatherCode),
                            contentDescription = weatherDescription(weather.weatherCode),
                            tint = colors.onSurface,
                            modifier = Modifier.size(54.dp).padding(end = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = colors.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WeatherMetric("High", weather.high?.let { it.roundToInt().toString() + "°" } ?: "—")
                        WeatherMetric("Low", weather.low?.let { it.roundToInt().toString() + "°" } ?: "—")
                        WeatherMetric("Wind", weather.windSpeed.roundToInt().toString() + " km/h")
                        WeatherMetric("Rain", weather.rainChance?.let { it.toString() + "%" } ?: "—")
                    }
                    Text(
                        text = "Forecast by Open-Meteo",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherMetric(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(label, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text(value, color = colors.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun NewsRow(story: NewsStory, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = story.domain + "  ·  " + story.publishedAt,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = story.title,
                color = colors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                lineHeight = 23.sp,
                modifier = Modifier.padding(top = 5.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            imageVector = Icons.Default.ArrowOutward,
            contentDescription = "Open story",
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun FeedMessage(
    title: String,
    detail: String,
    action: String,
    onAction: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
    ) {
        Text(title, color = colors.onSurface, style = MaterialTheme.typography.titleMedium)
        Text(detail, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        TextButton(onClick = onAction, modifier = Modifier.padding(start = 0.dp, top = 4.dp)) {
            Text(action)
        }
    }
}

@Composable
private fun ShortcutChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.heightIn(min = 48.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = colors.onSurface, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

private fun fetchNews(category: NewsCategory): List<NewsStory> {
    val query = URLEncoder.encode(category.query, "UTF-8")
    val endpoint = "https://api.gdeltproject.org/api/v2/doc/doc?query=" + query +
        "&mode=ArtList&format=json&maxrecords=8&timespan=24h&sort=DateDesc"
    val response = requestJson(endpoint)
    val articles = response.optJSONArray("articles") ?: return emptyList()
    val seenTitles = mutableSetOf<String>()
    val results = mutableListOf<NewsStory>()

    for (index in 0 until articles.length()) {
        val article = articles.optJSONObject(index) ?: continue
        val title = article.optString("title").trim()
        val articleUrl = article.optString("url").trim()
        if (title.isBlank()) continue
        if (!articleUrl.startsWith("https://", true) && !articleUrl.startsWith("http://", true)) continue
        val normalized = title.lowercase(Locale.ROOT)
        if (!seenTitles.add(normalized)) continue
        val domain = article.optString("domain").ifBlank {
            runCatching { URL(articleUrl).host.removePrefix("www.") }.getOrDefault("Publisher")
        }
        results.add(
            NewsStory(
                title = title,
                domain = domain,
                url = articleUrl,
                publishedAt = formatPublishedAt(article.optString("seendate"))
            )
        )
        if (results.size == 7) break
    }
    return results
}

private fun fetchPlaces(query: String): List<WeatherPlace> {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val endpoint = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=6&language=en&format=json"
    val results = requestJson(endpoint).optJSONArray("results") ?: return emptyList()
    val places = mutableListOf<WeatherPlace>()

    for (index in 0 until results.length()) {
        val place = results.optJSONObject(index) ?: continue
        val name = place.optString("name")
        val admin = place.optString("admin1")
        val country = place.optString("country")
        val label = listOf(name, admin, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
        val lat = place.optDouble("latitude", Double.NaN)
        val lon = place.optDouble("longitude", Double.NaN)
        if (name.isNotBlank() && lat.isFinite() && lon.isFinite()) {
            places.add(WeatherPlace(label, lat, lon))
        }
    }
    return places
}

private fun fetchWeather(latitude: Double, longitude: Double): WeatherSnapshot {
    val endpoint = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude +
        "&longitude=" + longitude +
        "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m" +
        "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
        "&forecast_days=1&timezone=auto"
    val response = requestJson(endpoint)
    val current = response.getJSONObject("current")
    val daily = response.optJSONObject("daily")
    val highs = daily?.optJSONArray("temperature_2m_max")
    val lows = daily?.optJSONArray("temperature_2m_min")
    val rain = daily?.optJSONArray("precipitation_probability_max")

    return WeatherSnapshot(
        temperature = current.getDouble("temperature_2m"),
        feelsLike = current.optDouble("apparent_temperature", current.getDouble("temperature_2m")),
        weatherCode = current.optInt("weather_code", -1),
        windSpeed = current.optDouble("wind_speed_10m", 0.0),
        high = if (highs != null && highs.length() > 0 && !highs.isNull(0)) highs.optDouble(0) else null,
        low = if (lows != null && lows.length() > 0 && !lows.isNull(0)) lows.optDouble(0) else null,
        rainChance = if (rain != null && rain.length() > 0 && !rain.isNull(0)) rain.optInt(0) else null
    )
}

private fun requestJson(endpoint: String): JSONObject {
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 10_000
    connection.readTimeout = 12_000
    connection.setRequestProperty("Accept", "application/json")
    connection.setRequestProperty("User-Agent", "Godelin-Android/0.1")
    try {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (status !in 200..299) throw IllegalStateException("Service returned HTTP " + status)
        return JSONObject(body)
    } finally {
        connection.disconnect()
    }
}

private fun formatPublishedAt(value: String): String {
    if (value.isBlank()) return "Recently"
    return runCatching {
        val parser = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX", Locale.US)
        val instant = Instant.from(parser.parse(value))
        DateTimeFormatter.ofPattern("MMM d · h:mm a", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }.getOrDefault("Recently")
}

private fun weatherDescription(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    56, 57 -> "Freezing drizzle"
    61, 63, 65 -> "Rain"
    66, 67 -> "Freezing rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95, 96, 99 -> "Thunderstorms"
    else -> "Current conditions"
}

private fun weatherIcon(code: Int): ImageVector = when (code) {
    0 -> Icons.Default.WbSunny
    1, 2, 3, 45, 48 -> Icons.Default.Cloud
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Default.Grain
    71, 73, 75, 77, 85, 86 -> Icons.Default.AcUnit
    95, 96, 99 -> Icons.Default.Thunderstorm
    else -> Icons.Default.Cloud
}
