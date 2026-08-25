package fjallkartan.fjallkartan.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import fjallkartan.fjallkartan.settings.RemoteSettings
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request

object KartverketTileProxy {
    private const val TAG = "KartverketTileProxy"
    private const val PORT = 8062
    private const val REWRITE_MINIMUM_ZOOM = 15
    private const val MAXIMUM_HEADER_BYTES = 8_192
    private val forwardedHeaders = listOf("Date", "Cache-Control", "ETag", "Last-Modified", "Expires")

    private val client = OkHttpClient.Builder()
        .cache(null)
        .dispatcher(
            Dispatcher().apply {
                maxRequests = 64
                maxRequestsPerHost = 32
            },
        )
        .callTimeout(15, TimeUnit.SECONDS)
        .build()
    private val executor: ExecutorService = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "kartverket-proxy-worker").apply { isDaemon = true }
    }
    private val started = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null

    @Volatile
    var tileUrlTemplate: String? = null
        private set

    @Volatile
    var norwaySlopeUrlTemplate: String? = null
        private set

    @Volatile
    var swedenSlopeUrlTemplate: String? = null
        private set

    fun start() {
        if (!started.compareAndSet(false, true)) return
        runCatching {
            ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT))
            }
        }.onSuccess { server ->
            serverSocket = server
            tileUrlTemplate = proxyTemplate("kartverket")
            norwaySlopeUrlTemplate = proxyTemplate("norway-slope")
            swedenSlopeUrlTemplate = proxyTemplate("sweden-slope")
            executor.execute { acceptConnections(server) }
            Log.d(TAG, "Listening on 127.0.0.1:$PORT")
        }.onFailure { error ->
            started.set(false)
            Log.e(TAG, "Could not start local tile proxy", error)
        }
    }

    fun offlineStyleUrl(): String = "http://127.0.0.1:$PORT/style.json"

    private fun acceptConnections(server: ServerSocket) {
        while (!server.isClosed) {
            try {
                val socket = server.accept()
                executor.execute { handle(socket) }
            } catch (error: Exception) {
                if (!server.isClosed) Log.e(TAG, "Accept failed", error)
            }
        }
    }

    private fun handle(socket: Socket) {
        socket.use {
            socket.soTimeout = 15_000
            val path = readRequestPath(socket)
            if (path?.substringBefore('?') == "/style.json") {
                respond(
                    socket,
                    200,
                    "OK",
                    MapStyle.json().toByteArray(StandardCharsets.UTF_8),
                    "application/json",
                )
                return
            }
            val requestTile = path?.let(::parseRequestTile)
            if (requestTile == null) {
                respond(socket, 400, "Bad Request")
                return
            }

            val request = Request.Builder()
                .url(
                    RemoteSettings.tileUrl(
                        requestTile.source,
                        requestTile.tile.z,
                        requestTile.tile.x,
                        requestTile.tile.y,
                    ),
                )
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (requestTile.source.isSlope && response.code == 404) {
                            respond(socket, 200, "OK", TRANSPARENT_PNG, "image/png")
                            return
                        }
                        val status = if (isRetryable(response.code)) response.code else 404
                        respond(socket, status, reasonPhrase(status))
                        return
                    }
                    val original = response.body.bytes()
                    val body = if (
                        requestTile.source == TileServer.Kartverket &&
                        requestTile.tile.z >= REWRITE_MINIMUM_ZOOM
                    ) {
                        NoDataFill.rewrite(original)
                    } else {
                        original
                    }
                    val headers = forwardedHeaders.mapNotNull { name ->
                        response.header(name)?.let { name to it }
                    }
                    respond(socket, 200, "OK", body, "image/png", headers)
                }
            } catch (error: Exception) {
                Log.e(
                    TAG,
                    "Tile ${requestTile.tile.z}/${requestTile.tile.x}/${requestTile.tile.y} failed",
                    error,
                )
                respond(socket, 503, "Service Unavailable")
            }
        }
    }

    private fun readRequestPath(socket: Socket): String? {
        val input = BufferedInputStream(socket.getInputStream())
        val header = ByteArrayOutputStream()
        var matched = 0
        while (header.size() < MAXIMUM_HEADER_BYTES) {
            val byte = input.read()
            if (byte == -1) return null
            header.write(byte)
            matched = when {
                matched == 0 && byte == '\r'.code -> 1
                matched == 1 && byte == '\n'.code -> 2
                matched == 2 && byte == '\r'.code -> 3
                matched == 3 && byte == '\n'.code -> 4
                byte == '\r'.code -> 1
                else -> 0
            }
            if (matched == 4) break
        }
        val firstLine = header.toString(StandardCharsets.US_ASCII.name())
            .lineSequence()
            .firstOrNull()
            ?: return null
        val parts = firstLine.split(' ')
        return parts.takeIf { it.size >= 2 && it[0] == "GET" }?.get(1)
    }

    private fun respond(
        socket: Socket,
        status: Int,
        reason: String,
        body: ByteArray? = null,
        contentType: String? = null,
        headers: List<Pair<String, String>> = emptyList(),
    ) {
        try {
            val output = socket.getOutputStream()
            val head = buildString {
                append("HTTP/1.1 $status $reason\r\n")
                if (body != null && contentType != null) append("Content-Type: $contentType\r\n")
                append("Content-Length: ${body?.size ?: 0}\r\n")
                headers.forEach { (name, value) -> append("$name: $value\r\n") }
                append("Connection: close\r\n\r\n")
            }
            output.write(head.toByteArray(StandardCharsets.US_ASCII))
            if (body != null) output.write(body)
            output.flush()
        } catch (_: IOException) {
            Log.d(TAG, "Tile client closed the connection before the response completed")
        }
    }

    internal data class Tile(val z: Int, val x: Int, val y: Int)
    internal data class RequestTile(val source: TileServer, val tile: Tile)

    internal fun parseTile(path: String): Tile? {
        return parseRequestTile(path)?.tile
    }

    internal fun parseRequestTile(path: String): RequestTile? {
        val parts = path
            .substringBefore('?')
            .removePrefix("/")
            .removeSuffix(".png")
            .split('/')
        val (source, tileParts) = when (parts.size) {
            3 -> TileServer.Kartverket to parts
            4 -> source(parts[0]) to parts.drop(1)
            else -> return null
        }
        source ?: return null
        return RequestTile(
            source,
            Tile(
                z = tileParts[0].toIntOrNull() ?: return null,
                x = tileParts[1].toIntOrNull() ?: return null,
                y = tileParts[2].toIntOrNull() ?: return null,
            ),
        )
    }

    internal fun isRetryable(status: Int): Boolean = status == 429 || status >= 500

    private fun proxyTemplate(source: String): String =
        "http://127.0.0.1:$PORT/$source/{z}/{x}/{y}.png"

    private fun source(value: String): TileServer? = when (value) {
        "kartverket" -> TileServer.Kartverket
        "norway-slope" -> TileServer.NorwaySlope
        "sweden-slope" -> TileServer.SwedenSlope
        else -> null
    }

    private fun reasonPhrase(status: Int): String = when (status) {
        400 -> "Bad Request"
        404 -> "Not Found"
        429 -> "Too Many Requests"
        500 -> "Internal Server Error"
        502 -> "Bad Gateway"
        503 -> "Service Unavailable"
        504 -> "Gateway Timeout"
        else -> "Error"
    }

    private val TRANSPARENT_PNG by lazy {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        output.toByteArray()
    }

    object NoDataFill {
        fun rewrite(data: ByteArray): ByteArray {
            val decoded = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return data
            val bitmap = decoded.copy(Bitmap.Config.ARGB_8888, true)
            if (bitmap !== decoded) decoded.recycle()
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            var changed = false
            for (index in pixels.indices) {
                if (isNoDataPixel(pixels[index])) {
                    pixels[index] = 0
                    changed = true
                }
            }
            if (!changed) {
                bitmap.recycle()
                return data
            }

            bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val output = ByteArrayOutputStream()
            val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            return if (compressed) output.toByteArray() else data
        }

        internal fun isNoDataPixel(argb: Int): Boolean {
            val red = argb shr 16 and 0xFF
            val green = argb shr 8 and 0xFF
            val blue = argb and 0xFF
            return red > 250 && green > 250 && blue in 222..240
        }
    }
}
