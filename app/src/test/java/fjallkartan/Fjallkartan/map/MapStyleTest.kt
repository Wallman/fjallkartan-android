package fjallkartan.fjallkartan.map

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MapStyleTest {
    @Test
    fun buildsFourLayerRasterStyleWithHiddenSlope() {
        val style = JSONObject(MapStyle.json())
        assertEquals(8, style.getInt("version"))
        assertEquals(4, style.getJSONObject("sources").length())
        assertEquals(4, style.getJSONArray("layers").length())
        assertFalse(
            style.getJSONArray("layers")
                .getJSONObject(2)
                .getJSONObject("layout")
                .getString("visibility") == "visible",
        )
    }
}
