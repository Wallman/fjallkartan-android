package fjallkartan.fjallkartan.map

import org.junit.Assert.assertEquals
import org.junit.Test

class ScaleBarTest {
    @Test
    fun usesLargestNiceDistanceWithinMaximumWidth() {
        assertEquals(ScaleBarScale(100f, "2 km"), calculateScaleBar(metersPerDp = 20.0))
    }

    @Test
    fun formatsDistancesBelowOneKilometerInMeters() {
        assertEquals(ScaleBarScale(100f, "500 m"), calculateScaleBar(metersPerDp = 5.0))
    }
}
