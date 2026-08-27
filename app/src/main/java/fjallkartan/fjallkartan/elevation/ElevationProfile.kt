package fjallkartan.fjallkartan.elevation

import fjallkartan.fjallkartan.measurement.DistanceMeasurement
import fjallkartan.fjallkartan.measurement.GeoCoordinate

data class ElevationPoint(val distance: Double, val elevation: Double?)

data class ElevationProfileState(
    val points: List<ElevationPoint> = emptyList(),
    val ascent: Double = 0.0,
    val descent: Double = 0.0,
    val minimum: Double? = null,
    val maximum: Double? = null,
    val coverage: Double = 0.0,
    val isLoading: Boolean = false,
) {
    val hasData: Boolean get() = coverage > 0 && points.size >= 2
    val isPartial: Boolean get() = hasData && coverage < 0.95
}

object ElevationProfile {
    const val SAMPLE_SPACING_METERS = 25.0
    const val MAX_SAMPLES = 500
    const val HYSTERESIS_METERS = 4.0

    data class Sample(val coordinate: GeoCoordinate, val distance: Double)

    fun apply(points: List<ElevationPoint>): ElevationProfileState {
        val known = points.mapNotNull(ElevationPoint::elevation)
        val (ascent, descent) = gain(points.map(ElevationPoint::elevation))
        return ElevationProfileState(
            points = points,
            ascent = ascent,
            descent = descent,
            minimum = known.minOrNull(),
            maximum = known.maxOrNull(),
            coverage = if (points.isEmpty()) 0.0 else known.size.toDouble() / points.size,
        )
    }

    fun resample(
        coordinates: List<GeoCoordinate>,
        spacing: Double = SAMPLE_SPACING_METERS,
        maxSamples: Int = MAX_SAMPLES,
    ): List<Sample> {
        if (coordinates.size < 2 || maxSamples < 2) {
            return coordinates.map { Sample(it, 0.0) }
        }
        val total = DistanceMeasurement.length(coordinates)
        if (total <= 0) return listOf(Sample(coordinates.first(), 0.0))
        val step = maxOf(spacing, total / (maxSamples - 1))

        val samples = mutableListOf(Sample(coordinates.first(), 0.0))
        var target = step
        var travelled = 0.0
        for ((start, end) in coordinates.zipWithNext()) {
            val segment = DistanceMeasurement.meters(start, end)
            if (segment <= 0) continue
            while (target <= travelled + segment && samples.size < maxSamples) {
                val fraction = (target - travelled) / segment
                samples += Sample(
                    GeoCoordinate(
                        start.latitude + (end.latitude - start.latitude) * fraction,
                        start.longitude + (end.longitude - start.longitude) * fraction,
                    ),
                    target,
                )
                target += step
            }
            travelled += segment
        }
        if (
            samples.size < maxSamples &&
            total - samples.last().distance > 0.5
        ) {
            samples += Sample(coordinates.last(), total)
        }
        return samples
    }

    fun gain(
        elevations: List<Double?>,
        hysteresis: Double = HYSTERESIS_METERS,
    ): Pair<Double, Double> {
        var ascent = 0.0
        var descent = 0.0
        var reference: Double? = null
        for (elevation in elevations) {
            if (elevation == null) {
                reference = null
                continue
            }
            val previous = reference
            if (previous == null) {
                reference = elevation
                continue
            }
            val delta = elevation - previous
            if (delta >= hysteresis) {
                ascent += delta
                reference = elevation
            } else if (delta <= -hysteresis) {
                descent -= delta
                reference = elevation
            }
        }
        return ascent to descent
    }
}
