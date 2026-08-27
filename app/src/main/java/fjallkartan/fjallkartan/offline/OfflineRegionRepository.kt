package fjallkartan.fjallkartan.offline

import android.content.Context
import androidx.core.content.edit
import fjallkartan.fjallkartan.elevation.ElevationService
import fjallkartan.fjallkartan.map.MapStyle
import fjallkartan.fjallkartan.map.TilePyramid
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

enum class OfflineStatus {
    Downloading,
    Paused,
    Complete,
    Failed,
}

data class OfflineRegionSummary(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val completedResources: Long,
    val requiredResources: Long,
    val completedBytes: Long,
    val status: OfflineStatus,
    val error: String? = null,
)

class OfflineRegionRepository private constructor(context: Context) {
    private data class Metadata(val id: String, val name: String, val createdAt: Instant)
    private data class ElevationProgress(val done: Int, val total: Int, val bytes: Long)

    private val appContext = context.applicationContext
    private val manager = OfflineManager.getInstance(appContext)
    private val elevation = ElevationService(appContext)
    private val preferences = appContext.getSharedPreferences("offline-regions", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val regionsByMetadataId = mutableMapOf<String, OfflineRegion>()
    private val statuses = mutableMapOf<String, OfflineRegionStatus>()
    private val errors = mutableMapOf<String, String>()
    private val elevationProgress = mutableMapOf<String, ElevationProgress>()
    private val elevationKeys = mutableMapOf<String, List<ElevationService.TileKey>>()
    private val elevationJobs = mutableMapOf<String, Job>()

    private val _regions = MutableStateFlow<List<OfflineRegionSummary>>(emptyList())
    val regions = _regions.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                regionsByMetadataId.clear()
                offlineRegions?.forEach(::attach)
                rebuildSummaries()
            }

            override fun onError(error: String) {
                _lastError.value = error
            }
        })
    }

    fun startDownload(name: String, bounds: LatLngBounds): Boolean {
        val tilePositions = TilePyramid.tilePositionCount(
            bounds.latitudeNorth,
            bounds.longitudeEast,
            bounds.latitudeSouth,
            bounds.longitudeWest,
        )
        if (tilePositions > MAXIMUM_TILE_POSITIONS) {
            _lastError.value = "This area is too large to download."
            return false
        }

        val metadata = Metadata(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Offline map" },
            createdAt = Instant.now(),
        )
        val definition = OfflineTilePyramidRegionDefinition(
            MapStyle.offlineUrl(),
            bounds,
            TilePyramid.MIN_ZOOM.toDouble(),
            TilePyramid.MAX_ZOOM.toDouble(),
            appContext.resources.displayMetrics.density,
        )
        manager.createOfflineRegion(
            definition,
            encode(metadata),
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    setPaused(metadata.id, false)
                    attach(offlineRegion)
                    offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                    ensureElevation(metadata.id, offlineRegion, start = true)
                    rebuildSummaries()
                }

                override fun onError(error: String) {
                    _lastError.value = error
                }
            },
        )
        return true
    }

    fun pause(id: String) {
        setPaused(id, true)
        regionsByMetadataId[id]?.setDownloadState(OfflineRegion.STATE_INACTIVE)
        elevationJobs.remove(id)?.cancel()
        statuses[id]?.let { rebuildSummaries() }
    }

    fun resume(id: String) {
        errors.remove(id)
        val region = regionsByMetadataId[id] ?: return
        setPaused(id, false)
        region.setDownloadState(OfflineRegion.STATE_ACTIVE)
        ensureElevation(id, region, start = true)
        rebuildSummaries()
    }

    fun delete(id: String) {
        val region = regionsByMetadataId[id] ?: return
        val elevationJob = elevationJobs.remove(id)
        elevationJob?.cancel()
        val ownKeys = elevationKeys[id].orEmpty().toSet()
        val sharedKeys = elevationKeys
            .filterKeys { it != id }
            .values
            .flatten()
            .toSet()
        region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
            override fun onDelete() {
                scope.launch {
                    elevationJob?.cancelAndJoin()
                    withContext(Dispatchers.IO) {
                        elevation.delete(ownKeys - sharedKeys)
                    }
                }
                regionsByMetadataId.remove(id)
                statuses.remove(id)
                errors.remove(id)
                preferences.edit { remove(pausedKey(id)) }
                elevationKeys.remove(id)
                elevationProgress.remove(id)
                rebuildSummaries()
            }

            override fun onError(error: String) {
                errors[id] = error
                rebuildSummaries()
            }
        })
    }

    fun clearError() {
        _lastError.value = null
    }

    private fun attach(region: OfflineRegion) {
        val metadata = decode(region.metadata) ?: return
        regionsByMetadataId[metadata.id] = region
        region.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                statuses[metadata.id] = status
                if (status.isComplete && status.downloadState == OfflineRegion.STATE_ACTIVE) {
                    region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                }
                if (!isPaused(metadata.id)) {
                    ensureElevation(metadata.id, region, start = true)
                }
                rebuildSummaries()
            }

            override fun onError(error: OfflineRegionError) {
                // MapLibre reports every failed tile fetch here, including
                // transient connection/server errors (e.g. HTTP 429/503) that
                // it retries internally without any help from us. Surfacing
                // those as a blocking dialog would interrupt the user for
                // failures that resolve themselves as the download continues.
                if (error.reason == OfflineRegionError.REASON_CONNECTION ||
                    error.reason == OfflineRegionError.REASON_SERVER
                ) {
                    return
                }
                _lastError.value = error.message
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                errors[metadata.id] = "Offline tile limit exceeded ($limit)."
                rebuildSummaries()
            }
        })
        region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
            override fun onStatus(status: OfflineRegionStatus?) {
                status ?: return
                statuses[metadata.id] = status
                if (!isPaused(metadata.id) && !status.isComplete) {
                    region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                } else if (status.isComplete && status.downloadState == OfflineRegion.STATE_ACTIVE) {
                    region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                }
                ensureElevation(
                    metadata.id,
                    region,
                    start = !isPaused(metadata.id),
                )
                rebuildSummaries()
            }

            override fun onError(error: String?) {
                errors[metadata.id] = error ?: "Could not read offline status."
                rebuildSummaries()
            }
        })
    }

    private fun ensureElevation(id: String, region: OfflineRegion, start: Boolean) {
        val definition = region.definition as? OfflineTilePyramidRegionDefinition ?: return
        val bounds = definition.bounds ?: return
        val keys = elevationKeys.getOrPut(id) { ElevationService.tileKeys(bounds) }
        if (elevationProgress[id] == null) {
            val done = keys.count(elevation::isCached)
            elevationProgress[id] = ElevationProgress(done, keys.size, 0)
        }
        val progress = elevationProgress[id] ?: return
        if (!start || progress.done >= progress.total || elevationJobs[id] != null) return

        elevationJobs[id] = scope.launch {
            val currentJob = coroutineContext[Job]
            var downloadedBytes = 0L
            try {
                while (!isPaused(id) && regionsByMetadataId[id] === region) {
                    val previousBytes = downloadedBytes
                    elevation.prefetch(keys) { done, total, passBytes ->
                        elevationProgress[id] =
                            ElevationProgress(done, total, previousBytes + passBytes)
                        rebuildSummaries()
                    }
                    downloadedBytes = elevationProgress[id]?.bytes ?: previousBytes
                    if (keys.all(elevation::isCached)) break
                    delay(ELEVATION_RETRY_DELAY_MILLIS)
                }
            } finally {
                if (elevationJobs[id] === currentJob) elevationJobs.remove(id)
                rebuildSummaries()
            }
        }
    }

    private fun rebuildSummaries() {
        _regions.value = regionsByMetadataId.mapNotNull { (id, region) ->
            val metadata = decode(region.metadata) ?: return@mapNotNull null
            val mapStatus = statuses[id]
            val terrain = elevationProgress[id] ?: ElevationProgress(0, 0, 0)
            val terrainComplete = terrain.done >= terrain.total
            val status = when {
                errors[id] != null -> OfflineStatus.Failed
                mapStatus?.isComplete == true && terrainComplete -> OfflineStatus.Complete
                isPaused(id) -> OfflineStatus.Paused
                else -> OfflineStatus.Downloading
            }
            OfflineRegionSummary(
                id = id,
                name = metadata.name,
                createdAt = metadata.createdAt,
                completedResources = (mapStatus?.completedResourceCount ?: 0) + terrain.done,
                requiredResources = (mapStatus?.requiredResourceCount ?: 0) + terrain.total,
                completedBytes = (mapStatus?.completedResourceSize ?: 0) + terrain.bytes,
                status = status,
                error = errors[id],
            )
        }.sortedByDescending(OfflineRegionSummary::createdAt)
    }

    private fun encode(metadata: Metadata): ByteArray = JSONObject()
        .put("id", metadata.id)
        .put("name", metadata.name)
        .put("createdAt", metadata.createdAt.toString())
        .toString()
        .toByteArray()

    private fun decode(bytes: ByteArray): Metadata? = runCatching {
        val json = JSONObject(String(bytes))
        Metadata(
            id = json.getString("id"),
            name = json.getString("name"),
            createdAt = Instant.parse(json.getString("createdAt")),
        )
    }.getOrNull()

    private fun isPaused(id: String): Boolean = preferences.getBoolean(pausedKey(id), false)

    private fun setPaused(id: String, paused: Boolean) {
        preferences.edit { putBoolean(pausedKey(id), paused) }
    }

    private fun pausedKey(id: String): String = "paused:$id"

    companion object {
        private const val MAXIMUM_TILE_POSITIONS = 50_000
        private const val ELEVATION_RETRY_DELAY_MILLIS = 15_000L

        @Volatile
        private var instance: OfflineRegionRepository? = null

        fun get(context: Context): OfflineRegionRepository {
            return instance ?: synchronized(this) {
                instance ?: OfflineRegionRepository(context).also { instance = it }
            }
        }
    }
}
