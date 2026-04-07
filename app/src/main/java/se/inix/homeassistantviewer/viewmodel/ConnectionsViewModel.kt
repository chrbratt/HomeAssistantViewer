package se.inix.homeassistantviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import se.inix.homeassistantviewer.data.AddressProbeResult
import se.inix.homeassistantviewer.data.ApiProbeResult
import se.inix.homeassistantviewer.data.HomeAssistantConnectionTester
import se.inix.homeassistantviewer.data.SettingsRepository
import se.inix.homeassistantviewer.data.model.HaConnection
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionsViewModel(
    private val settingsRepository: SettingsRepository,
    private val connectionTester: HomeAssistantConnectionTester
) : ViewModel() {

    val connections: StateFlow<List<HaConnection>> = settingsRepository.connections

    private val _addressResult = MutableStateFlow<AddressProbeResult?>(null)
    val addressResult: StateFlow<AddressProbeResult?> = _addressResult.asStateFlow()

    private val _apiResult = MutableStateFlow<ApiProbeResult?>(null)
    val apiResult: StateFlow<ApiProbeResult?> = _apiResult.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    fun addConnection(name: String, baseUrl: String, token: String) {
        settingsRepository.addConnection(name, baseUrl, token)
    }

    fun updateConnection(id: String, name: String, baseUrl: String, token: String) {
        settingsRepository.updateConnection(id, name, baseUrl, token)
    }

    fun deleteConnection(id: String) {
        settingsRepository.deleteConnection(id)
    }

    fun clearTestResults() {
        _addressResult.value = null
        _apiResult.value = null
    }

    fun runConnectionTest(baseUrl: String, token: String) {
        viewModelScope.launch {
            _isTesting.value = true
            _addressResult.value = null
            _apiResult.value = null
            coroutineScope {
                val addr = async { connectionTester.probeAddress(baseUrl) }
                val api = async { connectionTester.probeApi(baseUrl, token) }
                _addressResult.value = addr.await()
                _apiResult.value = api.await()
            }
            _isTesting.value = false
        }
    }
}
