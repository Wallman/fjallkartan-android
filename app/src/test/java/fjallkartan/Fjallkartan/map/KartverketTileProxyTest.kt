package fjallkartan.fjallkartan.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KartverketTileProxyTest {
    @Test
    fun parsesTilePaths() {
        assertEquals(
            KartverketTileProxy.Tile(15, 17_123, 9_234),
            KartverketTileProxy.parseTile("/15/17123/9234.png"),
        )
        assertNull(KartverketTileProxy.parseTile("/not/a/tile.png"))
    }

    @Test
    fun identifiesCreamNoDataFill() {
        assertTrue(KartverketTileProxy.NoDataFill.isNoDataPixel(0xFFFFFFE6.toInt()))
        assertFalse(KartverketTileProxy.NoDataFill.isNoDataPixel(0xFFFAFFE6.toInt()))
        assertFalse(KartverketTileProxy.NoDataFill.isNoDataPixel(0xFFFFFFF1.toInt()))
    }

    @Test
    fun classifiesRetryableResponses() {
        assertTrue(KartverketTileProxy.isRetryable(429))
        assertTrue(KartverketTileProxy.isRetryable(503))
        assertFalse(KartverketTileProxy.isRetryable(404))
    }
}
