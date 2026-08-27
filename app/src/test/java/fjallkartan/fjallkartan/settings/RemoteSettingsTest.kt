package fjallkartan.fjallkartan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteSettingsTest {
    @Test
    fun validatesTileTemplates() {
        assertTrue(RemoteSettings.builtIn.isUsable())
        assertFalse(RemoteSettings.builtIn.copy(kartverketUrl = "https://example.com/tile").isUsable())
        assertFalse(RemoteSettings.builtIn.copy(minAppVersion = "version one").isUsable())
    }

    @Test
    fun substitutesTileCoordinates() {
        assertEquals(
            "https://example.com/12/34/56.png",
            TileSettings.substitute("https://example.com/{z}/{x}/{y}.png", 12, 34, 56),
        )
    }
}
