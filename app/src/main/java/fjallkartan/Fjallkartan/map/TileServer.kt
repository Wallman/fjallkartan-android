package fjallkartan.fjallkartan.map

import fjallkartan.fjallkartan.settings.RemoteSettings

enum class TileServer(
    val sourceMaximumZoom: Int,
    val isSlope: Boolean = false,
    val isData: Boolean = false,
) {
    Kartverket(18),
    Lantmateriet(16),
    NorwaySlope(16, isSlope = true),
    SwedenSlope(13, isSlope = true),
    Elevation(12, isData = true);

    val offlineMinimumZoom: Int
        get() = if (isData) sourceMaximumZoom else TilePyramid.MIN_ZOOM

    val offlineMaximumZoom: Int
        get() = minOf(TilePyramid.MAX_ZOOM, sourceMaximumZoom)

    fun covers(zoom: Int): Boolean = zoom in offlineMinimumZoom..offlineMaximumZoom

    fun url(z: Int, x: Int, y: Int): String = RemoteSettings.tileUrl(this, z, x, y)
}
