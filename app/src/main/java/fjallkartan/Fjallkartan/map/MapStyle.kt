package fjallkartan.fjallkartan.map

import fjallkartan.fjallkartan.settings.RemoteSettings
import org.json.JSONArray
import org.json.JSONObject

object MapStyle {
    const val NORWAY_SLOPE_LAYER = "norway-slope-layer"
    const val SWEDEN_SLOPE_LAYER = "sweden-slope-layer"

    fun json(
        kartverketTemplate: String =
            KartverketTileProxy.tileUrlTemplate ?: RemoteSettings.settings.kartverketUrl,
    ): String {
        val settings = RemoteSettings.settings
        val sources = JSONObject()
            .put("lantmateriet", rasterSource(settings.lantmaterietUrl, 0, 16))
            .put("kartverket", rasterSource(kartverketTemplate, 0, 18))
            .put("norway-slope", rasterSource(settings.norwaySlopeUrl, 5, 16))
            .put("sweden-slope", rasterSource(settings.swedenSlopeUrl, 5, 13))

        val layers = JSONArray()
            .put(rasterLayer("lantmateriet-layer", "lantmateriet"))
            .put(rasterLayer("kartverket-layer", "kartverket"))
            .put(rasterLayer(NORWAY_SLOPE_LAYER, "norway-slope", opacity = 0.6, visible = false))
            .put(rasterLayer(SWEDEN_SLOPE_LAYER, "sweden-slope", opacity = 0.6, visible = false))

        return JSONObject()
            .put("version", 8)
            .put("sources", sources)
            .put("layers", layers)
            .toString()
    }

    private fun rasterSource(url: String, minZoom: Int, maxZoom: Int): JSONObject {
        return JSONObject()
            .put("type", "raster")
            .put("tiles", JSONArray().put(url))
            .put("tileSize", 256)
            .put("minzoom", minZoom)
            .put("maxzoom", maxZoom)
    }

    private fun rasterLayer(
        id: String,
        source: String,
        opacity: Double? = null,
        visible: Boolean = true,
    ): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("type", "raster")
            .put("source", source)
            .apply {
                if (opacity != null) put("paint", JSONObject().put("raster-opacity", opacity))
                if (!visible) put("layout", JSONObject().put("visibility", "none"))
            }
    }
}
