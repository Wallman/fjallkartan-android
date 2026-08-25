package fjallkartan.fjallkartan.elevation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import fjallkartan.fjallkartan.map.TileServer
import fjallkartan.fjallkartan.map.TilePyramid
import fjallkartan.fjallkartan.measurement.GeoCoordinate
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.maplibre.android.geometry.LatLngBounds

class ElevationService(
    context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    data class TileKey(val x: Int, val y: Int) {
        val relativePath: String get() = "$ZOOM/$x/$y.png"
    }

    internal data class PixelAddress(val key: TileKey, val pixelX: Int, val pixelY: Int)

    private val diskDirectory = File(context.noBackupFilesDir, "elevation")
    private val memory = object : LruCache<TileKey, HeightTile>(64) {
        override fun sizeOf(key: TileKey, value: HeightTile): Int = 1
    }
    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<TileKey, CompletableDeferred<HeightTile?>>()

    suspend fun heights(coordinates: List<GeoCoordinate>): List<Double?> = coroutineScope {
        coordinates.map { coordinate -> async { height(coordinate) } }.awaitAll()
    }

    suspend fun height(coordinate: GeoCoordinate): Double? {
        val address = pixelAddress(coordinate) ?: return null
        return tile(address.key)?.height(address.pixelX, address.pixelY)
    }

    suspend fun prefetch(
        keys: List<TileKey>,
        onProgress: (done: Int, total: Int, bytes: Long) -> Unit = { _, _, _ -> },
    ) {
        val distinctKeys = keys.distinct()
        var done = distinctKeys.count(::isCached)
        var bytes = 0L
        for (key in distinctKeys) {
            val wasCached = isCached(key)
            val before = file(key).takeIf(File::exists)?.length() ?: 0
            tile(key)
            val after = file(key).takeIf(File::exists)?.length() ?: 0
            bytes += (after - before).coerceAtLeast(0)
            if (!wasCached && isCached(key)) done += 1
            onProgress(done, distinctKeys.size, bytes)
        }
    }

    fun isCached(key: TileKey): Boolean = file(key).exists() || missingFile(key).exists()

    fun delete(keys: Set<TileKey>) {
        keys.forEach { key ->
            memory.remove(key)
            file(key).delete()
            missingFile(key).delete()
        }
    }

    private suspend fun tile(key: TileKey): HeightTile? {
        memory.get(key)?.let { return it }

        val (deferred, owner) = inFlightMutex.withLock {
            inFlight[key]?.let { it to false } ?: (CompletableDeferred<HeightTile?>().also {
                inFlight[key] = it
            } to true)
        }
        if (!owner) return deferred.await()

        val result = runCatching { loadTile(key) }.getOrNull()
        if (result != null) memory.put(key, result)
        deferred.complete(result)
        inFlightMutex.withLock { inFlight.remove(key) }
        return result
    }

    private suspend fun loadTile(key: TileKey): HeightTile? = withContext(Dispatchers.IO) {
        val disk = file(key)
        if (missingFile(key).exists()) return@withContext null
        if (disk.exists()) {
            HeightTile.decode(disk.readBytes())?.let { return@withContext it }
            disk.delete()
        }

        val request = Request.Builder()
            .url(TileServer.Elevation.url(ZOOM, key.x, key.y))
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 404 || response.code == 410) {
                missingFile(key).apply {
                    parentFile?.mkdirs()
                    createNewFile()
                }
                return@withContext null
            }
            if (!response.isSuccessful) return@withContext null
            val bytes = response.body.bytes()
            val decoded = HeightTile.decode(bytes) ?: return@withContext null
            disk.parentFile?.mkdirs()
            val temporary = File(disk.parentFile, "${disk.name}.tmp")
            temporary.writeBytes(bytes)
            if (!temporary.renameTo(disk)) {
                temporary.delete()
            }
            missingFile(key).delete()
            decoded
        }
    }

    private fun file(key: TileKey): File = File(diskDirectory, key.relativePath)

    private fun missingFile(key: TileKey): File = File(diskDirectory, "${key.relativePath}.missing")

    companion object {
        const val ZOOM = 12
        private const val TILE_SIZE = 256

        internal fun pixelAddress(coordinate: GeoCoordinate): PixelAddress? {
            if (coordinate.latitude !in -90.0..90.0 || coordinate.longitude !in -180.0..180.0) {
                return null
            }
            val latitude = coordinate.latitude.coerceIn(-85.0511, 85.0511)
            val n = 1 shl ZOOM
            val worldX = (coordinate.longitude + 180.0) / 360.0 * n
            val radians = Math.toRadians(latitude)
            val worldY = (1 - ln(tan(radians) + 1 / cos(radians)) / PI) / 2 * n
            val tileX = floor(worldX).toInt().coerceIn(0, n - 1)
            val tileY = floor(worldY).toInt().coerceIn(0, n - 1)
            val pixelX = floor((worldX - floor(worldX)) * TILE_SIZE).toInt().coerceIn(0, 255)
            val pixelY = floor((worldY - floor(worldY)) * TILE_SIZE).toInt().coerceIn(0, 255)
            return PixelAddress(TileKey(tileX, tileY), pixelX, pixelY)
        }

        fun tileKeys(coordinates: List<GeoCoordinate>): List<TileKey> {
            return coordinates.mapNotNull(::pixelAddress).map(PixelAddress::key).distinct()
        }

        fun tileKeys(bounds: LatLngBounds): List<TileKey> {
            val range = TilePyramid.tileRange(
                bounds.latitudeNorth,
                bounds.longitudeEast,
                bounds.latitudeSouth,
                bounds.longitudeWest,
                ZOOM,
            ) ?: return emptyList()
            return buildList {
                for (x in range.minX..range.maxX) {
                    for (y in range.minY..range.maxY) add(TileKey(x, y))
                }
            }
        }
    }
}

internal class HeightTile private constructor(
    private val width: Int,
    private val height: Int,
    private val pixels: IntArray,
) {
    fun height(x: Int, y: Int): Double? {
        if (x !in 0 until width || y !in 0 until height) return null
        return decodePixel(pixels[y * width + x])
    }

    companion object {
        fun decode(bytes: ByteArray): HeightTile? {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val tile = HeightTile(bitmap.width, bitmap.height, pixels)
            bitmap.recycle()
            return tile
        }

        internal fun decodePixel(argb: Int): Double? {
            val alpha = argb ushr 24 and 0xFF
            if (alpha == 0) return null
            val red = argb shr 16 and 0xFF
            val green = argb shr 8 and 0xFF
            return ((red shl 8) or green) - 32_768.0
        }
    }
}
