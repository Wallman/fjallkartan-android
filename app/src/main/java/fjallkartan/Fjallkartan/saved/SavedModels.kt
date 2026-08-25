package fjallkartan.fjallkartan.saved

import fjallkartan.fjallkartan.elevation.ElevationProfileState
import fjallkartan.fjallkartan.measurement.GeoCoordinate
import fjallkartan.fjallkartan.measurement.MeasurementState
import java.text.DateFormat
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class SavedRoute(
    val id: UUID = UUID.randomUUID(),
    val createdAt: Instant = Instant.now(),
    val meters: Double,
    val coordinates: List<GeoCoordinate>,
    val strokeSizes: List<Int>,
    val ascent: Double = 0.0,
    val descent: Double = 0.0,
    val elevations: List<Double?> = emptyList(),
    val name: String? = null,
    val schemaVersion: Int = 1,
) {
    val displayName: String
        get() = name?.takeIf(String::isNotBlank)
            ?: DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date.from(createdAt))

    fun renamed(name: String): SavedRoute = copy(name = name.trim().ifBlank { null })

    fun toJson(): JSONObject = JSONObject()
        .put("id", id.toString())
        .put("createdAt", createdAt.toString())
        .put("meters", meters)
        .put(
            "coordinates",
            JSONArray(
                coordinates.map {
                    JSONObject().put("latitude", it.latitude).put("longitude", it.longitude)
                },
            ),
        )
        .put("strokeSizes", JSONArray(strokeSizes))
        .put("ascent", ascent)
        .put("descent", descent)
        .put("elevations", JSONArray(elevations.map { it ?: JSONObject.NULL }))
        .put("name", name ?: JSONObject.NULL)
        .put("schemaVersion", schemaVersion)

    companion object {
        fun snapshot(
            measurement: MeasurementState,
            elevation: ElevationProfileState,
            name: String?,
        ): SavedRoute = SavedRoute(
            meters = measurement.committedMeters,
            coordinates = measurement.coordinates,
            strokeSizes = measurement.strokeSizes,
            ascent = elevation.ascent,
            descent = elevation.descent,
            elevations = elevation.points.map { it.elevation },
            name = name?.trim()?.ifBlank { null },
        )

        fun fromJson(json: JSONObject): SavedRoute = SavedRoute(
            id = UUID.fromString(json.getString("id")),
            createdAt = Instant.parse(json.getString("createdAt")),
            meters = json.getDouble("meters"),
            coordinates = json.getJSONArray("coordinates").objects().map {
                GeoCoordinate(
                    it.optDouble("latitude", it.optDouble("lat")),
                    it.optDouble("longitude", it.optDouble("lon")),
                )
            },
            strokeSizes = json.getJSONArray("strokeSizes").ints(),
            ascent = json.optDouble("ascent", 0.0),
            descent = json.optDouble("descent", 0.0),
            elevations = json.optJSONArray("elevations")?.nullableDoubles() ?: emptyList(),
            name = json.optString("name").takeIf { it.isNotBlank() && it != "null" },
            schemaVersion = json.optInt("schemaVersion", 1),
        )
    }
}

data class SavedPin(
    val id: UUID = UUID.randomUUID(),
    val createdAt: Instant = Instant.now(),
    val coordinate: GeoCoordinate,
    val name: String? = null,
    val notes: String? = null,
    val schemaVersion: Int = 1,
) {
    val displayName: String get() = name.orEmpty()

    fun renamed(name: String): SavedPin = copy(name = name.trim().ifBlank { null })

    fun toJson(): JSONObject = JSONObject()
        .put("id", id.toString())
        .put("createdAt", createdAt.toString())
        .put(
            "coordinate",
            JSONObject().put("latitude", coordinate.latitude).put("longitude", coordinate.longitude),
        )
        .put("name", name ?: JSONObject.NULL)
        .put("notes", notes ?: JSONObject.NULL)
        .put("schemaVersion", schemaVersion)

    companion object {
        fun fromJson(json: JSONObject): SavedPin {
            val coordinate = json.getJSONObject("coordinate")
            return SavedPin(
                id = UUID.fromString(json.getString("id")),
                createdAt = Instant.parse(json.getString("createdAt")),
                coordinate = GeoCoordinate(
                    coordinate.optDouble("latitude", coordinate.optDouble("lat")),
                    coordinate.optDouble("longitude", coordinate.optDouble("lon")),
                ),
                name = json.optString("name").takeIf { it.isNotBlank() && it != "null" },
                notes = json.optString("notes").takeIf { it.isNotBlank() && it != "null" },
                schemaVersion = json.optInt("schemaVersion", 1),
            )
        }
    }
}

internal fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
internal fun JSONArray.ints(): List<Int> = (0 until length()).map(::getInt)
internal fun JSONArray.nullableDoubles(): List<Double?> = (0 until length()).map {
    if (isNull(it)) null else getDouble(it)
}
