package fjallkartan.fjallkartan.map

import androidx.lifecycle.ViewModel
import fjallkartan.fjallkartan.measurement.DistanceMeasurement
import fjallkartan.fjallkartan.measurement.GeoCoordinate
import fjallkartan.fjallkartan.measurement.MeasurementState
import fjallkartan.fjallkartan.settings.RemoteSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapViewModel : ViewModel() {
    private val _slopeVisible = MutableStateFlow(false)
    val slopeVisible = _slopeVisible.asStateFlow()

    private val _trackingEnabled = MutableStateFlow(false)
    val trackingEnabled = _trackingEnabled.asStateFlow()

    private val _measurement = MutableStateFlow(MeasurementState())
    val measurement = _measurement.asStateFlow()

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
    }

    fun updatePreview(meters: Double?) {
        _measurement.value = _measurement.value.copy(previewMeters = meters)
    }

    fun undoMeasurement() {
        _measurement.value = DistanceMeasurement.undo(_measurement.value)
    }

    fun clearMeasurement() {
        _measurement.value = DistanceMeasurement.clear(_measurement.value)
    }
}
