package fjallkartan.fjallkartan.elevation

import fjallkartan.fjallkartan.measurement.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevationProfileTest {
    @Test
    fun decodesPackedHeightPixels() {
        assertEquals(1234.0, HeightTile.decodePixel(0xFF84D200.toInt())!!, 0.0)
        assertNull(HeightTile.decodePixel(0x0084D200))
    }

    @Test
    fun resamplesAtFixedSpacingAndIncludesEnd() {
        val samples = ElevationProfile.resample(
            listOf(GeoCoordinate(60.0, 10.0), GeoCoordinate(60.0, 10.01)),
            spacing = 100.0,
        )
        assertTrue(samples.size in 6..7)
        assertEquals(0.0, samples.first().distance, 0.0)
        assertEquals(GeoCoordinate(60.0, 10.01), samples.last().coordinate)
    }

    @Test
    fun ignoresNoiseAndBreaksAtGaps() {
        val gain = ElevationProfile.gain(listOf(100.0, 102.0, 105.0, null, 200.0, 194.0))
        assertEquals(5.0, gain.first, 0.0)
        assertEquals(6.0, gain.second, 0.0)
    }

    @Test
    fun computesCoverageAndExtremes() {
        val state = ElevationProfile.apply(
            listOf(
                ElevationPoint(0.0, 100.0),
                ElevationPoint(25.0, null),
                ElevationPoint(50.0, 120.0),
            ),
        )
        assertEquals(2.0 / 3.0, state.coverage, 0.0001)
        assertEquals(100.0, state.minimum!!, 0.0)
        assertEquals(120.0, state.maximum!!, 0.0)
    }

    @Test
    fun mapsCoordinatesToElevationTiles() {
        val address = ElevationService.pixelAddress(GeoCoordinate(67.0, 16.0))!!
        assertEquals(ElevationService.TileKey(2_230, 1_009), address.key)
    }
}
