package fjallkartan.fjallkartan.map

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

object TilePyramid {
    const val MIN_ZOOM = 7
    const val MAX_ZOOM = 14

    fun tileCoordinate(latitude: Double, longitude: Double, zoom: Int): Pair<Int, Int>? {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0 || zoom !in 0..30) {
            return null
        }
        val clampedLatitude = latitude.coerceIn(-85.0511, 85.0511)
        val n = 1 shl zoom
        val x = floor((longitude + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
        val latitudeRadians = Math.toRadians(clampedLatitude)
        val y = floor(
            (1.0 - ln(tan(latitudeRadians) + 1.0 / cos(latitudeRadians)) / PI) / 2.0 * n,
        ).toInt().coerceIn(0, n - 1)
        return x to y
    }
}
