package fjallkartan.fjallkartan.measurement

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoCoordinate(val latitude: Double, val longitude: Double)

data class DistanceMarker(val coordinate: GeoCoordinate, val meters: Double)

data class MeasurementState(
    val isMeasuring: Boolean = false,
    val coordinates: List<GeoCoordinate> = emptyList(),
    val strokeSizes: List<Int> = emptyList(),
    val committedMeters: Double = 0.0,
    val previewMeters: Double? = null,
    val version: Int = 0,
) {
    val totalMeters: Double get() = committedMeters + (previewMeters ?: 0.0)
    val canUndo: Boolean get() = strokeSizes.isNotEmpty()
    val isEmpty: Boolean get() = coordinates.isEmpty()
}

object DistanceMeasurement {
    private const val EARTH_RADIUS_METERS = 6_371_008.8
    private val markerSpacings = listOf(500.0, 1_000.0, 2_000.0, 5_000.0, 10_000.0, 25_000.0, 50_000.0, 100_000.0)
    private const val MAXIMUM_MARKERS = 60

    fun appendStroke(state: MeasurementState, stroke: List<GeoCoordinate>): MeasurementState {
        if (stroke.size < 2) return state.copy(previewMeters = null)
        val coordinates = state.coordinates + stroke
        return state.copy(
            coordinates = coordinates,
            strokeSizes = state.strokeSizes + stroke.size,
            committedMeters = length(coordinates),
            previewMeters = null,
            version = state.version + 1,
        )
    }

    fun undo(state: MeasurementState): MeasurementState {
        val size = state.strokeSizes.lastOrNull() ?: return state
        val coordinates = state.coordinates.dropLast(size)
        return state.copy(
            coordinates = coordinates,
            strokeSizes = state.strokeSizes.dropLast(1),
            committedMeters = length(coordinates),
            previewMeters = null,
            version = state.version + 1,
        )
    }

    fun clear(state: MeasurementState): MeasurementState {
        if (state.coordinates.isEmpty() && state.previewMeters == null) return state
        return MeasurementState(
            isMeasuring = state.isMeasuring,
            version = state.version + 1,
        )
    }

    fun load(
        state: MeasurementState,
        coordinates: List<GeoCoordinate>,
        strokeSizes: List<Int>,
        meters: Double,
    ): MeasurementState {
        return MeasurementState(
            isMeasuring = false,
            coordinates = coordinates,
            strokeSizes = strokeSizes,
            committedMeters = meters,
            version = state.version + 1,
        )
    }

    fun meters(from: GeoCoordinate, to: GeoCoordinate): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val deltaLat = lat2 - lat1
        val deltaLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun length(coordinates: List<GeoCoordinate>): Double {
        return coordinates.zipWithNext().sumOf { (start, end) -> meters(start, end) }
    }

    fun markerSpacing(routeLengthMeters: Double, zoom: Double): Double {
        val byZoom = when {
            zoom < 6 -> 100_000.0
            zoom < 7 -> 50_000.0
            zoom < 8 -> 25_000.0
            zoom < 10 -> 10_000.0
            zoom < 11 -> 5_000.0
            zoom < 12 -> 2_000.0
            zoom < 13 -> 1_000.0
            else -> 500.0
        }
        val byLength = markerSpacings.firstOrNull {
            routeLengthMeters / it <= MAXIMUM_MARKERS
        } ?: markerSpacings.last()
        return maxOf(byZoom, byLength)
    }

    fun markers(
        coordinates: List<GeoCoordinate>,
        spacing: Double,
    ): List<DistanceMarker> {
        if (coordinates.size < 2 || spacing <= 0) return emptyList()
        val total = length(coordinates)
        if (total < spacing) return emptyList()

        val markers = mutableListOf<DistanceMarker>()
        var travelled = 0.0
        var next = spacing
        for ((start, end) in coordinates.zipWithNext()) {
            val segment = meters(start, end)
            if (segment <= 0) continue
            while (next <= travelled + segment) {
                val fraction = (next - travelled) / segment
                markers += DistanceMarker(interpolate(start, end, fraction), next)
                next += spacing
            }
            travelled += segment
        }
        return markers
    }

    fun formatDistance(meters: Double): String {
        return if (meters < 1_000) {
            "${meters.roundToInt()} m"
        } else {
            String.format("%.2f km", meters / 1_000)
        }
    }

    fun markerLabel(meters: Double): String {
        val kilometers = meters / 1_000
        val rounded = (kilometers * 10).roundToInt() / 10.0
        return if (rounded == rounded.toInt().toDouble()) {
            "${rounded.toInt()} km"
        } else {
            String.format("%.1f km", rounded)
        }
    }

    private fun interpolate(
        start: GeoCoordinate,
        end: GeoCoordinate,
        fraction: Double,
    ): GeoCoordinate {
        var deltaLongitude = end.longitude - start.longitude
        if (deltaLongitude > 180) deltaLongitude -= 360
        if (deltaLongitude < -180) deltaLongitude += 360
        var longitude = start.longitude + deltaLongitude * fraction
        if (longitude > 180) longitude -= 360
        if (longitude < -180) longitude += 360
        return GeoCoordinate(
            latitude = start.latitude + (end.latitude - start.latitude) * fraction,
            longitude = longitude,
        )
    }
}

data class ScreenPoint(val x: Double, val y: Double)

object LineSimplifier {
    fun simplify(points: List<ScreenPoint>, tolerance: Double): List<ScreenPoint> {
        if (points.size <= 2 || tolerance <= 0) return points
        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true
        simplifyRange(points, 0, points.lastIndex, tolerance * tolerance, keep)
        return points.filterIndexed { index, _ -> keep[index] }
    }

    private fun simplifyRange(
        points: List<ScreenPoint>,
        start: Int,
        end: Int,
        toleranceSquared: Double,
        keep: BooleanArray,
    ) {
        if (end <= start + 1) return
        var maximumDistance = 0.0
        var maximumIndex = start
        for (index in start + 1 until end) {
            val distance = segmentDistanceSquared(points[index], points[start], points[end])
            if (distance > maximumDistance) {
                maximumDistance = distance
                maximumIndex = index
            }
        }
        if (maximumDistance > toleranceSquared) {
            keep[maximumIndex] = true
            simplifyRange(points, start, maximumIndex, toleranceSquared, keep)
            simplifyRange(points, maximumIndex, end, toleranceSquared, keep)
        }
    }

    private fun segmentDistanceSquared(
        point: ScreenPoint,
        start: ScreenPoint,
        end: ScreenPoint,
    ): Double {
        val dx = end.x - start.x
        val dy = end.y - start.y
        if (dx == 0.0 && dy == 0.0) {
            return squared(point.x - start.x) + squared(point.y - start.y)
        }
        val t = (((point.x - start.x) * dx + (point.y - start.y) * dy) /
            (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
        val projectedX = start.x + t * dx
        val projectedY = start.y + t * dy
        return squared(point.x - projectedX) + squared(point.y - projectedY)
    }

    private fun squared(value: Double): Double = value * value
}
