package fjallkartan.fjallkartan.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TilePyramidTest {
    @Test
    fun returnsKnownTileCoordinate() {
        assertEquals(35_680 to 16_159, TilePyramid.tileCoordinate(67.0, 16.0, 16))
    }

    @Test
    fun rejectsInvalidCoordinates() {
        assertNull(TilePyramid.tileCoordinate(91.0, 0.0, 12))
        assertNull(TilePyramid.tileCoordinate(0.0, 181.0, 12))
        assertNull(TilePyramid.tileCoordinate(0.0, 0.0, 31))
    }

    @Test
    fun estimateBytesGrowsWithArea() {
        val small = TilePyramid.estimateBytes(60.1, 10.1, 60.0, 10.0)
        val large = TilePyramid.estimateBytes(61.0, 11.0, 60.0, 10.0)
        assertTrue(small > 0)
        assertTrue(large > small)
    }

    @Test
    fun estimateBytesExceedsGuardForLargeArea() {
        val bytes = TilePyramid.estimateBytes(70.0, 30.0, 58.0, 5.0)
        assertTrue(bytes > TilePyramid.MAX_DOWNLOAD_BYTES)
    }
}
