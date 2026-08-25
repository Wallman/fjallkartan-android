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
import fjallkartan.fjallkartan.settings.RemoteSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

    init {
        RemoteSettings.refresh()
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
