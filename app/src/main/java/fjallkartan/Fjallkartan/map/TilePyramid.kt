package fjallkartan.fjallkartan.map

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

object TilePyramid {
    const val MIN_ZOOM = 7
    const val MAX_ZOOM = 14
    private const val ELEVATION_ZOOM = 12
    private const val SLOPE_COVERAGE_FACTOR = 0.45
    private const val MEASURED_ELEVATION_BYTES = 18_400.0
    private const val ELEVATION_COVERAGE_FACTOR = 0.8
    const val MAX_DOWNLOAD_BYTES = 1_500_000_000L

    private val measuredBaseBytesPerPosition = mapOf(
        11 to 84_000.0,
        12 to 61_000.0,
        13 to 55_000.0,
        14 to 50_000.0,
    )

    private val measuredSlopeBytesPerPosition = mapOf(
        7 to 34_000.0,
        8 to 31_000.0,
        9 to 37_000.0,
        10 to 38_000.0,
        11 to 25_000.0,
        12 to 21_000.0,
        13 to 15_000.0,
        14 to 10_000.0,
    )

    fun estimateBytes(
        north: Double,
        east: Double,
        south: Double,
        west: Double,
    ): Long {
        var bytes = 0.0
        for (zoom in MIN_ZOOM..MAX_ZOOM) {
            val positions = tileRange(north, east, south, west, zoom)?.count ?: 0
            bytes += positions * (
                baseBytesPerPosition(zoom) +
                    slopeBytesPerPosition(zoom) +
                    elevationBytesPerPosition(zoom)
                )
        }
        return bytes.toLong()
    }

    private fun baseBytesPerPosition(zoom: Int): Double {
        measuredBaseBytesPerPosition[zoom]?.let { return it }
        if (zoom > MAX_ZOOM) {
            measuredBaseBytesPerPosition[MAX_ZOOM]?.let { return it }
        }
        val lowestMeasured = measuredBaseBytesPerPosition.keys.min()
        if (zoom < lowestMeasured) {
            val base = measuredBaseBytesPerPosition.getValue(lowestMeasured)
            val levels = lowestMeasured - zoom
            return base / 2.0.pow(levels.toDouble())
        }
        return measuredBaseBytesPerPosition.values.min()
    }

    private fun slopeBytesPerPosition(zoom: Int): Double {
        val measured = measuredSlopeBytesPerPosition[zoom]
            ?: measuredSlopeBytesPerPosition.getValue(MAX_ZOOM)
        return measured * SLOPE_COVERAGE_FACTOR
    }

    private fun elevationBytesPerPosition(zoom: Int): Double {
        if (zoom != ELEVATION_ZOOM) return 0.0
        return MEASURED_ELEVATION_BYTES * ELEVATION_COVERAGE_FACTOR
    }

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

    data class TileRange(val minX: Int, val maxX: Int, val minY: Int, val maxY: Int) {
        val count: Int get() = (maxX - minX + 1) * (maxY - minY + 1)
    }

    fun tileRange(
        north: Double,
        east: Double,
        south: Double,
        west: Double,
        zoom: Int,
    ): TileRange? {
        val northwest = tileCoordinate(north, west, zoom) ?: return null
        val southeast = tileCoordinate(south, east, zoom) ?: return null
        return TileRange(
            minX = minOf(northwest.first, southeast.first),
            maxX = maxOf(northwest.first, southeast.first),
            minY = minOf(northwest.second, southeast.second),
            maxY = maxOf(northwest.second, southeast.second),
        )
    }

    fun tilePositionCount(
        north: Double,
        east: Double,
        south: Double,
        west: Double,
    ): Int = (MIN_ZOOM..MAX_ZOOM).sumOf { zoom ->
        tileRange(north, east, south, west, zoom)?.count ?: 0
    }
}
