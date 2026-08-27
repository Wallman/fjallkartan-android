package fjallkartan.fjallkartan.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fjallkartan.fjallkartan.elevation.ElevationPoint
import fjallkartan.fjallkartan.elevation.ElevationProfile
import fjallkartan.fjallkartan.elevation.ElevationProfileState
import fjallkartan.fjallkartan.elevation.ElevationService
import fjallkartan.fjallkartan.measurement.DistanceMeasurement
import fjallkartan.fjallkartan.measurement.GeoCoordinate
import fjallkartan.fjallkartan.measurement.MeasurementState
import fjallkartan.fjallkartan.offline.OfflineDownloadService
import fjallkartan.fjallkartan.offline.OfflineRegionRepository
import fjallkartan.fjallkartan.offline.OfflineStatus
import fjallkartan.fjallkartan.settings.RemoteSettings
import fjallkartan.fjallkartan.saved.FeaturedRoute
import fjallkartan.fjallkartan.saved.FeaturedRoutes
import fjallkartan.fjallkartan.saved.JsonFileStore
import fjallkartan.fjallkartan.saved.SavedPin
import fjallkartan.fjallkartan.saved.SavedRoute
import fjallkartan.fjallkartan.search.PlaceResult
import fjallkartan.fjallkartan.search.PlaceSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLngBounds

data class SearchSelection(val place: PlaceResult? = null, val token: Int = 0)

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val _slopeVisible = MutableStateFlow(false)
    val slopeVisible = _slopeVisible.asStateFlow()

    private val _trackingEnabled = MutableStateFlow(false)
    val trackingEnabled = _trackingEnabled.asStateFlow()

    private val _measurement = MutableStateFlow(MeasurementState())
    val measurement = _measurement.asStateFlow()

    private val elevationService = ElevationService(application)
    private val _elevation = MutableStateFlow(ElevationProfileState())
    val elevation = _elevation.asStateFlow()
    private var elevationJob: Job? = null

    private val routeStore = JsonFileStore(
        application,
        "routes",
        SavedRoute::toJson,
        SavedRoute::fromJson,
        SavedRoute::id,
    )
    private val pinStore = JsonFileStore(
        application,
        "pins",
        SavedPin::toJson,
        SavedPin::fromJson,
        SavedPin::id,
    )
    private val placeSearch by lazy { PlaceSearch(application) }
    private val offlineRepository = OfflineRegionRepository.get(application)

    private val _savedRoutes = MutableStateFlow(routeStore.load().sortedByDescending(SavedRoute::createdAt))
    val savedRoutes = _savedRoutes.asStateFlow()

    private val _savedPins = MutableStateFlow(pinStore.load().sortedByDescending(SavedPin::createdAt))
    val savedPins = _savedPins.asStateFlow()

    val featuredRoutes: List<FeaturedRoute> = FeaturedRoutes.load(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _searchResults = MutableStateFlow<List<PlaceResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()
    private val _selectedPlace = MutableStateFlow(SearchSelection())
    val selectedPlace = _selectedPlace.asStateFlow()
    private var searchJob: Job? = null

    private val _routeFitVersion = MutableStateFlow(0)
    val routeFitVersion = _routeFitVersion.asStateFlow()
    val offlineRegions = offlineRepository.regions
    val offlineError = offlineRepository.lastError

    init {
        RemoteSettings.refresh()
        viewModelScope.launch {
            var serviceStarted = false
            offlineRegions.collect { regions ->
                val hasActiveDownload = regions.any { it.status == OfflineStatus.Downloading }
                if (hasActiveDownload && !serviceStarted) {
                    OfflineDownloadService.ensureRunning(getApplication())
                }
                serviceStarted = hasActiveDownload
            }
        }
    }

    fun toggleSlope() {
        _slopeVisible.value = !_slopeVisible.value
    }

    fun toggleTracking() {
        _trackingEnabled.value = !_trackingEnabled.value
    }

    fun toggleMeasuring() {
        val current = _measurement.value
        _measurement.value = current.copy(
            isMeasuring = !current.isMeasuring,
            previewMeters = null,
        )
    }

    fun appendStroke(stroke: List<GeoCoordinate>) {
        _measurement.value = DistanceMeasurement.appendStroke(_measurement.value, stroke)
        refreshElevation()
    }

    fun updatePreview(meters: Double?) {
        _measurement.value = _measurement.value.copy(previewMeters = meters)
    }

    fun undoMeasurement() {
        _measurement.value = DistanceMeasurement.undo(_measurement.value)
        refreshElevation()
    }

    fun clearMeasurement() {
        _measurement.value = DistanceMeasurement.clear(_measurement.value)
        refreshElevation()
    }

    fun saveRoute(name: String?) {
        val current = _measurement.value
        if (current.coordinates.size < 2) return
        val route = SavedRoute.snapshot(current, _elevation.value, name)
        routeStore.save(route)
        _savedRoutes.value = routeStore.load().sortedByDescending(SavedRoute::createdAt)
    }

    fun loadRoute(route: SavedRoute) {
        elevationJob?.cancel()
        _measurement.value = DistanceMeasurement.load(
            _measurement.value,
            route.coordinates,
            route.strokeSizes,
            route.meters,
            route.name,
        )
        _elevation.value = if (route.elevations.isEmpty()) {
            ElevationProfileState()
        } else {
            val step = if (route.elevations.size > 1) route.meters / (route.elevations.size - 1) else 0.0
            ElevationProfile.apply(
                route.elevations.mapIndexed { index, value ->
                    ElevationPoint(index * step, value)
                },
            )
        }
        _routeFitVersion.value += 1
    }

    fun deleteRoute(route: SavedRoute) {
        routeStore.delete(route.id)
        _savedRoutes.value = routeStore.load().sortedByDescending(SavedRoute::createdAt)
    }

    fun renameRoute(route: SavedRoute, name: String) {
        routeStore.save(route.renamed(name))
        _savedRoutes.value = routeStore.load().sortedByDescending(SavedRoute::createdAt)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(150)
            _searchResults.value = withContext(Dispatchers.IO) { placeSearch.search(query) }
        }
    }

    fun selectPlace(place: PlaceResult?) {
        _selectedPlace.value = SearchSelection(place, _selectedPlace.value.token + 1)
    }

    fun savePlace(place: PlaceResult) {
        savePin(SavedPin(coordinate = place.coordinate, name = place.name))
        selectPlace(null)
    }

    fun dropPin(coordinate: GeoCoordinate) {
        savePin(SavedPin(coordinate = coordinate))
    }

    fun updatePin(pin: SavedPin, name: String, notes: String) {
        savePin(pin.updated(name, notes))
    }

    fun deletePin(pin: SavedPin) {
        pinStore.delete(pin.id)
        _savedPins.value = pinStore.load().sortedByDescending(SavedPin::createdAt)
    }

    private fun savePin(pin: SavedPin) {
        pinStore.save(pin)
        _savedPins.value = pinStore.load().sortedByDescending(SavedPin::createdAt)
    }

    fun startOfflineDownload(name: String, bounds: LatLngBounds) {
        if (offlineRepository.startDownload(name, bounds)) {
            OfflineDownloadService.ensureRunning(getApplication())
        }
    }

    fun pauseOfflineRegion(id: String) {
        offlineRepository.pause(id)
    }

    fun resumeOfflineRegion(id: String) {
        offlineRepository.resume(id)
        OfflineDownloadService.ensureRunning(getApplication())
    }

    fun deleteOfflineRegion(id: String) {
        offlineRepository.delete(id)
    }

    fun clearOfflineError() {
        offlineRepository.clearError()
    }

    private fun refreshElevation() {
        elevationJob?.cancel()
        val version = _measurement.value.version
        val coordinates = _measurement.value.coordinates
        if (coordinates.size < 2) {
            _elevation.value = ElevationProfileState()
            return
        }
        _elevation.value = _elevation.value.copy(isLoading = true)
        elevationJob = viewModelScope.launch {
            val samples = ElevationProfile.resample(coordinates)
            val heights = elevationService.heights(samples.map(ElevationProfile.Sample::coordinate))
            if (_measurement.value.version != version) return@launch
            val points = samples.zip(heights) { sample, height ->
                ElevationPoint(sample.distance, height)
            }
            if (points.all { it.elevation == null } && _elevation.value.hasData) {
                _elevation.value = _elevation.value.copy(isLoading = false)
            } else {
                _elevation.value = ElevationProfile.apply(points)
            }
        }
    }
}
