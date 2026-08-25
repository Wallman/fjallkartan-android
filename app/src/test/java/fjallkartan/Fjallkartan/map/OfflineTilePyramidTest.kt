package fjallkartan.fjallkartan.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTilePyramidTest {
    @Test
    fun computesInclusiveTileRange() {
        val range = TilePyramid.tileRange(
            north = 68.4,
            east = 18.9,
            south = 68.3,
            west = 18.7,
            zoom = 12,
        )!!
        assertEquals((range.maxX - range.minX + 1) * (range.maxY - range.minY + 1), range.count)
        assertTrue(range.count > 0)
    }

    @Test
    fun smallViewportStaysBelowDownloadGuard() {
        val count = TilePyramid.tilePositionCount(68.4, 18.9, 68.3, 18.7)
        assertTrue(count in 1 until 50_000)
    }
}
