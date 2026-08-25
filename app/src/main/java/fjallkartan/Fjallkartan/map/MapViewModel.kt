package fjallkartan.fjallkartan.map

import androidx.lifecycle.ViewModel
import fjallkartan.fjallkartan.settings.RemoteSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapViewModel : ViewModel() {
    private val _slopeVisible = MutableStateFlow(false)
    val slopeVisible = _slopeVisible.asStateFlow()

    private val _trackingEnabled = MutableStateFlow(false)
    val trackingEnabled = _trackingEnabled.asStateFlow()

    init {
        RemoteSettings.refresh()
    }

    fun toggleSlope() {
        _slopeVisible.value = !_slopeVisible.value
    }

    fun toggleTracking() {
        _trackingEnabled.value = !_trackingEnabled.value
    }
}
