package com.emnger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emnger.data.ssh.AirOSSSHClient
import com.emnger.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main ViewModel para la app EMNGER
 */
class MainViewModel : ViewModel() {
    
    private val sshClient = AirOSSSHClient()
    
    // Estado de laUI
    private val _uiState = MutableStateFlow(EMNGERUiState())
    val uiState: StateFlow<EMNGERUiState> = _uiState.asStateFlow()
    
    // Lista de CPEs
    private val _cpes = MutableStateFlow<List<CPE>>(emptyList())
    val cpes: StateFlow<List<CPE>> = _cpes.asStateFlow()
    
    // Credenciales
    private var credentials: Credentials? = null
    
    // Timer para modo test
    private val _testTimer = MutableStateFlow(0)
    val testTimer: StateFlow<Int> = _testTimer.asStateFlow()
    
    /**
     * Guardar credenciales
     */
    fun setCredentials(creds: Credentials) {
        credentials = creds
    }
    
    /**
     * Escanear antenas (Análisis)
     */
    fun analyzeCPEs(ipList: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val analyzedCPEs = mutableListOf<CPE>()
            
            for (ip in ipList) {
                // Update status to analyzing
                updateCPEStatus(ip, CPEStatus.ANALYZING)
                
                val result = sshClient.connectAndGetInfo(
                    ip = ip,
                    credentials = credentials!!,
                    port = 22
                )
                
                result.fold(
                    onSuccess = { cpe ->
                        analyzedCPEs.add(cpe.copy(status = CPEStatus.READY))
                        updateCPEStatus(ip, CPEStatus.READY)
                    },
                    onFailure = { error ->
                        analyzedCPEs.add(
                            CPE(
                                ip = ip,
                                status = CPEStatus.ERROR
                            )
                        )
                        updateCPEStatus(ip, CPEStatus.ERROR)
                    }
                )
            }
            
            _cpes.value = analyzedCPEs
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Aplicar cambios en modo test (temporal)
     */
    fun applyTestChanges(config: ChangeConfig, selectedCPEs: List<CPE>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Start 3 minute timer
            _testTimer.value = 180
            
            for (cpe in selectedCPEs) {
                val result = sshClient.applyConfig(
                    ip = cpe.ip,
                    credentials = credentials!!,
                    config = getConfigString(config),
                    isTest = true // Test mode
                )
                
                result.fold(
                    onSuccess = { changeResult ->
                        updateCPEStatus(
                            cpe.ip, 
                            if (changeResult.success) CPEStatus.TEST_OK else CPEStatus.TEST_FAIL
                        )
                    },
                    onFailure = {
                        updateCPEStatus(cpe.ip, CPEStatus.TEST_FAIL)
                    }
                )
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Aplicar cambios permanentemente
     */
    fun applyPermanentChanges(config: ChangeConfig, selectedCPEs: List<CPE>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            for (cpe in selectedCPEs) {
                val result = sshClient.applyConfig(
                    ip = cpe.ip,
                    credentials = credentials!!,
                    config = getConfigString(config),
                    isTest = false // Permanent
                )
                
                result.fold(
                    onSuccess = { changeResult ->
                        updateCPEStatus(
                            cpe.ip,
                            if (changeResult.success) CPEStatus.APPLIED else CPEStatus.TEST_FAIL
                        )
                    },
                    onFailure = {
                        updateCPEStatus(cpe.ip, CPEStatus.TEST_FAIL)
                    }
                )
            }
            
            _testTimer.value = 0
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Descartar cambios (revertir modo test)
     */
    fun discardChanges(selectedCPEs: List<CPE>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            for (cpe in selectedCPEs) {
                sshClient.discardChanges(
                    ip = cpe.ip,
                    credentials = credentials!!
                )
                
                // Revert status to READY
                updateCPEStatus(cpe.ip, CPEStatus.READY)
            }
            
            _testTimer.value = 0
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Actualizar firmware (downgrade)
     */
    fun upgradeFirmware(firmwareUrl: String, selectedCPEs: List<CPE>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            for (cpe in selectedCPEs) {
                sshClient.upgradeFirmware(
                    ip = cpe.ip,
                    credentials = credentials!!,
                    firmwareUrl = firmwareUrl
                )
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    private fun updateCPEStatus(ip: String, status: CPEStatus) {
        _cpes.value = _cpes.value.map { cpe ->
            if (cpe.ip == ip) cpe.copy(status = status) else cpe
        }
    }
    
    private fun getConfigString(config: ChangeConfig): String {
        return when (config.changeType) {
            ChangeType.SSID -> "wireless.1.ssid=${config.newValue}"
            ChangeType.WPA_KEY -> "wireless.1.wpa.apkey=${config.newValue}"
            ChangeType.CHANNEL -> "wireless.1.channel=${config.newValue}"
            ChangeType.CHANNEL_WIDTH -> "wireless.1.channel_width=${config.newValue}"
            ChangeType.FREQUENCY -> "radio.1.freq=${config.newValue}"
            ChangeType.NTP -> "ntp.1.timeserver1=${config.newValue}"
            ChangeType.CUSTOM -> config.command
        }
    }
}

data class EMNGERUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)