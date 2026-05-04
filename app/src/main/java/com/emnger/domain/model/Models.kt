package com.emnger.domain.model

/**
 * Represents a CPE (Customer Premises Equipment) - Ubiquiti antenna
 */
data class CPE(
    val ip: String,
    val hostname: String = "",
    val model: String = "",
    val firmware: String = "",
    val mode: String = "", // AP, Station, etc.
    val ssid: String = "",
    val channel: Int = 0,
    val channelWidth: Int = 20,
    val frequency: Int = 0,
    val wpaKey: String = "",
    val signal: Int = 0,
    val uptime: Long = 0,
    val status: CPEStatus = CPEStatus.PENDING
)

enum class CPEStatus {
    PENDING,       // En lista, sin analizar
    ANALYZING,     // Escaneando/conectando
    READY,         // Lista para cambios (verde)
    TEST_OK,       // Cambio aplicado en modo test (azul)
    TEST_FAIL,     // Error en modo test (rojo)
    APPLIED,       // Cambio aplicado permanentemente
    ERROR          // Error de conexión
}

/**
 * Resultado de aplicar cambios
 */
data class ChangeResult(
    val ip: String,
    val success: Boolean,
    val message: String = "",
    val output: String = ""
)

/**
 * Tipos de cambios que se pueden hacer en AirOS
 */
enum class ChangeType {
    SSID,
    WPA_KEY,
    CHANNEL,
    CHANNEL_WIDTH,
    FREQUENCY,
    NTP,
    FIRMWARE,
    CUSTOM
}

/**
 * Configuración de un cambio a aplicar
 */
data class ChangeConfig(
    val changeType: ChangeType,
    val newValue: String,
    val command: String = "" // Comando SSH personalizado
)

/**
 * Credenciales para SSH
 */
data class Credentials(
    val username: String,
    val password: String
)

/**
 * Estado de la conexión SSH
 */
sealed class SSHConnectionState {
    object Idle : SSHConnectionState()
    object Connecting : SSHConnectionState()
    data class Connected(val message: String) : SSHConnectionState()
    data class Error(val message: String) : SSHConnectionState()
}