package fjallkartan.fjallkartan.measurement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceMeasurementTest {
    private val oslo = GeoCoordinate(59.9139, 10.7522)
    private val bergen = GeoCoordinate(60.3913, 5.3221)

    @Test
    fun calculatesGeodesicDistance() {
        assertEquals(304_000.0, DistanceMeasurement.meters(oslo, bergen), 2_000.0)
    }

    @Test
    fun appendsAndUndoesWholeStrokes() {
        var state = MeasurementState(isMeasuring = true)
        state = DistanceMeasurement.appendStroke(
            state,
            listOf(oslo, GeoCoordinate(60.0, 8.0), bergen),
        )
        assertEquals(3, state.coordinates.size)
        assertTrue(state.committedMeters > 0)

        state = DistanceMeasurement.undo(state)
        assertTrue(state.coordinates.isEmpty())
        assertEquals(0.0, state.committedMeters, 0.0)
    }

    @Test
    fun emitsDistanceMarkersInsideSegments() {
        val points = listOf(GeoCoordinate(60.0, 10.0), GeoCoordinate(60.0, 10.1))
        val markers = DistanceMeasurement.markers(points, spacing = 1_000.0)
        assertEquals(5, markers.size)
        assertEquals(1_000.0, markers.first().meters, 0.0)
    }

    @Test
    fun simplifiesScreenSpaceJitter() {
        val simplified = LineSimplifier.simplify(
            listOf(
                ScreenPoint(0.0, 0.0),
                ScreenPoint(1.0, 0.2),
                ScreenPoint(2.0, -0.1),
                ScreenPoint(3.0, 0.0),
            ),
            tolerance = 0.5,
        )
        assertEquals(listOf(ScreenPoint(0.0, 0.0), ScreenPoint(3.0, 0.0)), simplified)
    }

    @Test
    fun formatsDistancesAndMarkers() {
        assertEquals("999 m", DistanceMeasurement.formatDistance(999.0))
        assertEquals("1.50 km", DistanceMeasurement.formatDistance(1_500.0))
        assertEquals("0.5 km", DistanceMeasurement.markerLabel(500.0))
        assertEquals("2 km", DistanceMeasurement.markerLabel(2_000.0))
    }
}
