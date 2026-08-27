package fjallkartan.fjallkartan.saved

import android.content.Context
import fjallkartan.fjallkartan.measurement.GeoCoordinate
import java.time.Instant
import java.util.UUID
import org.json.JSONObject

data class FeaturedRoute(
    val id: String,
    val name: String,
    val subtitle: String,
    val country: String,
    val route: SavedRoute,
)

object FeaturedRoutes {
    fun load(context: Context): List<FeaturedRoute> {
        return runCatching {
            val root = JSONObject(context.assets.open("featured-routes.json").bufferedReader().use { it.readText() })
            root.getJSONArray("routes").objects().map { entry ->
                val coordinates = entry.getJSONArray("coordinates").let { array ->
                    (0 until array.length()).map { index ->
                        val pair = array.getJSONArray(index)
                        GeoCoordinate(pair.getDouble(0), pair.getDouble(1))
                    }
                }
                val id = entry.getString("id")
                FeaturedRoute(
                    id = id,
                    name = entry.getString("name"),
                    subtitle = entry.getString("subtitle"),
                    country = entry.getString("country"),
                    route = SavedRoute(
                        id = stableUuid(id),
                        createdAt = Instant.EPOCH,
                        meters = entry.getDouble("meters"),
                        coordinates = coordinates,
                        strokeSizes = listOf(coordinates.size),
                        ascent = entry.optDouble("ascent", 0.0),
                        descent = entry.optDouble("descent", 0.0),
                        elevations = entry.optJSONArray("elevations")?.nullableDoubles() ?: emptyList(),
                        name = entry.getString("name"),
                    ),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun stableUuid(id: String): UUID {
        val bytes = id.toByteArray().copyOf(16)
        var most = 0L
        var least = 0L
        for (index in 0 until 8) most = (most shl 8) or (bytes[index].toLong() and 0xFF)
        for (index in 8 until 16) least = (least shl 8) or (bytes[index].toLong() and 0xFF)
        return UUID(most, least)
    }
}
