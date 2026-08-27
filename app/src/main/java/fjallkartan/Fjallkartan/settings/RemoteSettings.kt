package fjallkartan.fjallkartan.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import fjallkartan.fjallkartan.map.TileServer
import java.net.URI
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class TileSettings(
    val minAppVersion: String,
    val lantmaterietUrl: String,
    val kartverketUrl: String,
    val norwaySlopeUrl: String,
    val swedenSlopeUrl: String,
    val elevationUrl: String,
) {
    fun isUsable(): Boolean {
        return VERSION_PATTERN.matches(minAppVersion) &&
            listOf(
                lantmaterietUrl,
                kartverketUrl,
                norwaySlopeUrl,
                swedenSlopeUrl,
                elevationUrl,
            ).all(::isUsableTemplate)
    }

    companion object {
        private val VERSION_PATTERN = Regex("""[0-9]+\.[0-9]+(\.[0-9]+)?""")

        fun substitute(template: String, z: Int, x: Int, y: Int): String {
            return template
                .replace("{z}", z.toString())
                .replace("{x}", x.toString())
                .replace("{y}", y.toString())
        }

        private fun isUsableTemplate(template: String): Boolean {
            if (!template.contains("{z}") || !template.contains("{x}") || !template.contains("{y}")) {
                return false
            }
            return runCatching {
                val uri = URI(substitute(template, 0, 0, 0))
                (uri.scheme == "https" || uri.scheme == "http") && !uri.host.isNullOrBlank()
            }.getOrDefault(false)
        }
    }
}

object RemoteSettings {
    private const val TAG = "RemoteSettings"
    private const val SETTINGS_URL = "https://tiles.wallman.dev/settings.json"
    private const val REFRESH_INTERVAL_MS = 6 * 60 * 60 * 1_000L
    private const val KEY_PAYLOAD = "payload"
    private const val KEY_FETCHED_AT = "fetchedAt"

    val builtIn = TileSettings(
        minAppVersion = "1.0",
        lantmaterietUrl = "https://tiles.wallman.dev/v1/{z}/{y}/{x}.png",
        kartverketUrl = "https://cache.kartverket.no/v1/wmts/1.0.0/topo/default/webmercator/{z}/{y}/{x}.png",
        norwaySlopeUrl = "https://gis3.nve.no/arcgis/rest/services/wmts/Bratthet_med_utlop_2024/MapServer/tile/{z}/{y}/{x}",
        swedenSlopeUrl = "https://tiles.wallman.dev/slope/v1/{z}/{y}/{x}.png",
        elevationUrl = "https://tiles.wallman.dev/elevation/v1/{z}/{y}/{x}.png",
    )

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    @Volatile
    var settings: TileSettings = builtIn
        private set

    private lateinit var preferences: SharedPreferences

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences("remote-settings", Context.MODE_PRIVATE)
        settings = decode(preferences.getString(KEY_PAYLOAD, null)) ?: builtIn
    }

    fun refresh(force: Boolean = false) {
        if (!::preferences.isInitialized) return
        val elapsed = System.currentTimeMillis() - preferences.getLong(KEY_FETCHED_AT, 0)
        if (!force && elapsed < REFRESH_INTERVAL_MS) return

        client.newCall(Request.Builder().url(SETTINGS_URL).build()).enqueue(
            object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    Log.e(TAG, "Settings fetch failed", e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            Log.e(TAG, "Settings fetch failed: HTTP ${response.code}")
                            return
                        }
                        val payload = response.body.string()
                        val decoded = decode(payload)
                        if (decoded == null) {
                            Log.e(TAG, "Discarding unusable settings payload")
                            return
                        }
                        settings = decoded
                        preferences.edit {
                            putString(KEY_PAYLOAD, payload)
                            putLong(KEY_FETCHED_AT, System.currentTimeMillis())
                        }
                    }
                }
            },
        )
    }

    fun tileUrl(server: TileServer, z: Int, x: Int, y: Int): String {
        val current = settings
        val template = when (server) {
            TileServer.Kartverket -> current.kartverketUrl
            TileServer.Lantmateriet -> current.lantmaterietUrl
            TileServer.NorwaySlope -> current.norwaySlopeUrl
            TileServer.SwedenSlope -> current.swedenSlopeUrl
            TileServer.Elevation -> current.elevationUrl
        }
        return TileSettings.substitute(template, z, x, y)
    }

    private fun decode(payload: String?): TileSettings? {
        if (payload.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(payload)
            TileSettings(
                minAppVersion = json.getString("minAppVersion"),
                lantmaterietUrl = json.getString("lantmaterietUrl"),
                kartverketUrl = json.getString("kartverketUrl"),
                norwaySlopeUrl = json.getString("norwaySlopeUrl"),
                swedenSlopeUrl = json.getString("swedenSlopeUrl"),
                elevationUrl = json.getString("elevationUrl"),
            )
        }.getOrNull()?.takeIf(TileSettings::isUsable)
    }
}
