package com.emnger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emnger.domain.model.*
import kotlinx.coroutines.delay

/**
 * Pantalla 4: Interfaz AirOS 8 emulada
 * Donde defines los cambios a aplicar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirOSEmulatedScreen(
    cpes: List<CPE>,
    credentials: Credentials,
    onTest: (List<CPE>, ChangeConfig) -> Unit,
    onApply: (List<CPE>, ChangeConfig) -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0=Wireless, 1=Network, 2=Services, 3=System
    
    // Values from the first CPE as defaults
    val firstCPE = cpes.firstOrNull()
    var ssid by remember { mutableStateOf(firstCPE?.ssid ?: "") }
    var channel by remember { mutableStateOf(firstCPE?.channel?.toString() ?: "20") }
    var channelWidth by remember { mutableStateOf(firstCPE?.channelWidth?.toString() ?: "20") }
    var frequency by remember { mutableStateOf(firstCPE?.frequency?.toString() ?: "5180") }
    var wpaKey by remember { mutableStateOf(firstCPE?.wpaKey ?: "") }
    var ntpServer by remember { mutableStateOf("pool.ntp.org") }
    
    var testTimer by remember { mutableStateOf(0) } // 0 = no test running
    var testMode by remember { mutableStateOf(false) }
    
    // Test mode timer
    LaunchedEffect(testMode) {
        if (testMode) {
            testTimer = 180 // 3 minutes
            while (testTimer > 0) {
                delay(1000)
                testTimer--
            }
            testMode = false // Auto expire
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AirOS 8 - Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs estilo AirOS 8
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Wireless") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Network") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Services") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("System") }
                )
            }
            
            // Contenido según la pestaña
            when (selectedTab) {
                0 -> WirelessTabContent(
                    ssid = ssid,
                    onSSIDChange = { ssid = it },
                    channel = channel,
                    onChannelChange = { channel = it },
                    channelWidth = channelWidth,
                    onChannelWidthChange = { channelWidth = it },
                    frequency = frequency,
                    onFrequencyChange = { frequency = it },
                    wpaKey = wpaKey,
                    onWPAKeyChange = { wpaKey = it }
                )
                1 -> NetworkTabContent()
                2 -> ServicesTabContent(
                    ntpServer = ntpServer,
                    onNTPServerChange = { ntpServer = it }
                )
                3 -> SystemTabContent()
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Timer display when in test mode
            if (testMode && testTimer > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Modo Test: ${testTimer / 60}:${String.format("%02d", testTimer % 60)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Botones de acción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Test Changes
                OutlinedButton(
                    onClick = {
                        // Determine which config to change
                        val changeConfig = when (selectedTab) {
                            0 -> ChangeConfig(ChangeType.SSID, ssid)
                            2 -> ChangeConfig(ChangeType.NTP, ntpServer)
                            else -> ChangeConfig(ChangeType.CUSTOM, "")
                        }
                        onTest(cpes, changeConfig)
                        testMode = true
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !testMode
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test")
                }
                
                // Botón Apply
                Button(
                    onClick = {
                        val changeConfig = when (selectedTab) {
                            0 -> ChangeConfig(ChangeType.SSID, ssid)
                            2 -> ChangeConfig(ChangeType.NTP, ntpServer)
                            else -> ChangeConfig(ChangeType.CUSTOM, "")
                        }
                        onApply(cpes, changeConfig)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = testMode // Only enable if test was activated
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply")
                }
                
                // Botón Discard
                OutlinedButton(
                    onClick = {
                        onDiscard()
                        testMode = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discard")
                }
            }
        }
    }
}

@Composable
fun WirelessTabContent(
    ssid: String,
    onSSIDChange: (String) -> Unit,
    channel: String,
    onChannelChange: (String) -> Unit,
    channelWidth: String,
    onChannelWidthChange: (String) -> Unit,
    frequency: String,
    onFrequencyChange: (String) -> Unit,
    wpaKey: String,
    onWPAKeyChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configuración Wireless",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        // SSID
        OutlinedTextField(
            value = ssid,
            onValueChange = onSSIDChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("SSID") },
            singleLine = true
        )
        
        // Frequency
        OutlinedTextField(
            value = frequency,
            onValueChange = onFrequencyChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Frecuencia (MHz)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        // Channel Width dropdown
        var expanded by remember { mutableStateOf(false) }
        val widths = listOf("10", "20", "30", "40", "50", "60", "80")
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = channelWidth,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text("Channel Width (MHz)") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                widths.forEach { width ->
                    DropdownMenuItem(
                        text = { Text("${width} MHz") },
                        onClick = {
                            onChannelWidthChange(width)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        // WPA Key
        OutlinedTextField(
            value = wpaKey,
            onValueChange = onWPAKeyChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("WPA Key") },
            singleLine = true
        )
    }
}

@Composable
fun NetworkTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Configuración de Red",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Próximamente...",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ServicesTabContent(
    ntpServer: String,
    onNTPServerChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Servicios",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        // NTP Server
        OutlinedTextField(
            value = ntpServer,
            onValueChange = onNTPServerChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("NTP Server") },
            singleLine = true
        )
    }
}

@Composable
fun SystemTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sistema",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Acerca de, Firmware, etc.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}