package fjallkartan.fjallkartan.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
