package com.emnger.data.ssh

import com.emnger.domain.model.*
import com.jcraft.jsch.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Properties

/**
 * SSH Client para conectar a dispositivos AirOS 8
 */
class AirOSSSHClient {
    private val jsch = JSch()
    
    /**
     * Conectar a un dispositivo AirOS y obtener información
     */
    suspend fun connectAndGetInfo(
        ip: String,
        credentials: Credentials,
        port: Int = 22
    ): Result<CPE> = withContext(Dispatchers.IO) {
        try {
            val session = createSession(ip, credentials.username, credentials.password, port)
            session.connect(10000) // 10 sec timeout
            
            val execChannel = session.openChannel("exec") as ChannelExec
            execChannel.setCommand("cat /tmp/system.cfg && echo '---INFO---' && info")
            execChannel.setInputStream(null)
            
            val output = ByteArrayOutputStream()
            val error = ByteArrayOutputStream()
            execChannel.setOutputStream(output)
            execChannel.setExtOutputStream(error)
            
            execChannel.connect(15000)
            
            // Wait for command to complete
            while (!execChannel.isClosed) {
                Thread.sleep(100)
            }
            
            val result = output.toString()
            val exitCode = execChannel.channelCloseState
            
            session.disconnect()
            
            // Parse response
            val cpe = parseCPEInfo(ip, result)
            Result.success(cpe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Ejecutar un comando en el dispositivo
     */
    suspend fun executeCommand(
        ip: String,
        credentials: Credentials,
        command: String,
        port: Int = 22
    ): Result<ChangeResult> = withContext(Dispatchers.IO) {
        try {
            val session = createSession(ip, credentials.username, credentials.password, port)
            session.connect(10000)
            
            // First, apply the configuration change
            val execChannel = session.openChannel("exec") as ChannelExec
            execChannel.setCommand(command)
            execChannel.setInputStream(null)
            
            val output = ByteArrayOutputStream()
            val error = ByteArrayOutputStream()
            execChannel.setOutputStream(output)
            execChannel.setExtOutputStream(error)
            
            execChannel.connect(15000)
            
            while (!execChannel.isClosed) {
                Thread.sleep(100)
            }
            
            val result = output.toString()
            val errorOutput = error.toString()
            val exitCode = execChannel.exitStatus
            
            session.disconnect()
            
            if (exitCode == 0) {
                Result.success(ChangeResult(ip, true, "OK", result))
            } else {
                Result.success(ChangeResult(ip, false, errorOutput.ifEmpty { "Error $exitCode" }, result))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Aplicar cambios usando el sistema cfg de AirOS
     * Modo test (temporal) o permanente
     */
    suspend fun applyConfig(
        ip: String,
        credentials: Credentials,
        config: String, // ej: "wireless.1.ssid=MiSSID"
        isTest: Boolean = false, // true = test mode (3 min), false = permanent
        port: Int = 22
    ): Result<ChangeResult> = withContext(Dispatchers.IO) {
        try {
            // Multiple commands: set config, apply, commit
            val commands = buildList {
                add(config)
                if (isTest) {
                    add("cfg -a")  // Apply pending (test mode)
                } else {
                    add("cfg -a")
                    add("cfgcommit") // Permanent
                }
            }
            
            var success = true
            var output = ""
            var lastError = ""
            
            for (cmd in commands) {
                val result = executeSingleCommand(ip, credentials.username, credentials.password, cmd, port)
                output += result.first
                lastError = result.second
                if (!result.first.contains("done") && !result.first.isEmpty()) {
                    success = result.first.isNotEmpty()
                }
                Thread.sleep(500)
            }
            
            if (success) {
                Result.success(ChangeResult(ip, true, if (isTest) "Test aplicado" else "Aplicado", output))
            } else {
                Result.success(ChangeResult(ip, false, lastError, output))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Descartar cambios (revertir en modo test)
     */
    suspend fun discardChanges(
        ip: String,
        credentials: Credentials,
        port: Int = 22
    ): Result<ChangeResult> = withContext(Dispatchers.IO) {
        try {
            executeCommand(ip, credentials, "cfg revert", port)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reiniciar el dispositivo (para firmware update)
     */
    suspend fun reboot(
        ip: String,
        credentials: Credentials,
        port: Int = 22
    ): Result<ChangeResult> = withContext(Dispatchers.IO) {
        try {
            executeCommand(ip, credentials, "reboot", port)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Actualizar firmware
     */
    suspend fun upgradeFirmware(
        ip: String,
        credentials: Credentials,
        firmwareUrl: String,
        port: Int = 22
    ): Result<ChangeResult> = withContext(Dispatchers.IO) {
        try {
            // Download firmware to /tmp/
            val downloadCmd = "wget -O /tmp/fwupdate.bin $firmwareUrl"
            executeCommand(ip, credentials, downloadCmd, port)
            
            // Validate firmware
            val validateCmd = "/sbin/fwupdate -c /tmp/fwupdate.bin"
            executeCommand(ip, credentials, validateCmd, port)
            
            // Apply firmware update
            val updateCmd = "/sbin/fwupdate"
            executeCommand(ip, credentials, updateCmd, port)
            
            Result.success(ChangeResult(ip, true, "Actualizando firmware...", ""))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Private helper functions
    
    private fun createSession(ip: String, user: String, pass: String, port: Int): Session {
        val session = jsch.getSession(user, ip, port)
        session.setPassword(pass)
        
        val config = Properties()
        config["StrictHostKeyChecking"] = "no"
        session.setConfig(config)
        session.setTimeout(15000)
        
        return session
    }
    
    private fun executeSingleCommand(
        ip: String,
        user: String,
        pass: String,
        command: String,
        port: Int
    ): Pair<String, String> {
        val session = createSession(ip, user, pass, port)
        session.connect(10000)
        
        val execChannel = session.openChannel("exec") as ChannelExec
        execChannel.setCommand(command)
        
        val output = ByteArrayOutputStream()
        val error = ByteArrayOutputStream()
        execChannel.setOutputStream(output)
        execChannel.setExtOutputStream(error)
        
        execChannel.connect(15000)
        
        while (!execChannel.isClosed) {
            Thread.sleep(100)
        }
        
        val result = output.toString()
        val errorResult = error.toString()
        
        session.disconnect()
        
        return Pair(result, errorResult)
    }
    
    private fun parseCPEInfo(ip: String, rawOutput: String): CPE {
        var hostname = ""
        var model = ""
        var firmware = ""
        var ssid = ""
        var channel = 20
        var freq = 0
        var mode = ""
        
        // Parse system.cfg output
        if (rawOutput.contains("system.1.hostname=")) {
            val regex = Regex("system\\.1\\.hostname=([^\\s]+)")
            hostname = regex.find(rawOutput)?.groupValues?.get(1) ?: ""
        }
        
        if (rawOutput.contains("radio.1 freq=")) {
            val regex = Regex("radio\\.1\\.freq=(\\d+)")
            freq = regex.find(rawOutput)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }
        
        if (rawOutput.contains("wireless.1.ssid=")) {
            val regex = Regex("wireless\\.1\\.ssid=([^\\s]+)")
            ssid = regex.find(rawOutput)?.groupValues?.get(1) ?: ""
        }
        
        if (rawOutput.contains("wireless.1.channel_width=")) {
            val regex = Regex("wireless\\.1\\.channel_width=(\\d+)")
            channel = regex.find(rawOutput)?.groupValues?.get(1)?.toIntOrNull() ?: 20
        }
        
        // Parse info output for firmware version
        if (rawOutput.contains("Version:")) {
            val regex = Regex("Version:\\s*([\\d.]+)")
            firmware = regex.find(rawOutput)?.groupValues?.get(1) ?: ""
        }
        
        // Try to detect model
        model = when {
            rawOutput.contains("LBE-5AC") -> "LiteBeam AC"
            rawOutput.contains("LBE-5AC-GEN2") -> "LiteBeam AC Gen2"
            rawOutput.contains("NBE-5AC") -> "NanoBeam AC"
            rawOutput.contains("PBE-5AC") -> "PowerBeam AC"
            rawOutput.contains("RM5AC") -> "Rocket AC"
            rawOutput.contains("NS5AC") -> "NanoStation AC"
            else -> "Unknown"
        }
        
        return CPE(
            ip = ip,
            hostname = hostname,
            model = model,
            firmware = firmware,
            ssid = ssid,
            channel = channel,
            frequency = freq,
            mode = mode,
            status = CPEStatus.READY
        )
    }
}