package com.emnger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emnger.domain.model.*

/**
 * Pantalla 1: Input de IPs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputIPsScreen(
    onNext: (List<String>) -> Unit
) {
    var ipListText by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EMNGER - Gestión de CPEs") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Paso 1: Ingresa las IPs de las antenas",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Una IP por línea o separadas por coma",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = ipListText,
                onValueChange = { ipListText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                label = { Text("Lista de IPs") },
                placeholder = { Text("192.168.1.10\n192.168.1.11\n192.168.1.12") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val ips = ipListText
                        .split(",", "\n", " ", ";")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && it.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) }
                    
                    if (ips.isNotEmpty()) {
                        onNext(ips)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = ipListText.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.NavigateNext, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continuar")
            }
        }
    }
}

/**
 * Pantalla 2: Credenciales
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialsScreen(
    onNext: (Credentials) -> Unit,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("ubnt") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EMNGER") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Paso 2: Credenciales SSH",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Mismo usuario y contraseña para todos los CPEs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Usuario") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (username.isNotEmpty() && password.isNotEmpty()) {
                        onNext(Credentials(username, password))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = username.isNotEmpty() && password.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.NavigateNext, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Escanear Antenas")
            }
        }
    }
}

/**
 * Pantalla 3: Análisis de antenas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    cpes: List<CPE>,
    credentials: Credentials,
    onAllAnalyzed: (List<CPE>) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análisis de Antenas") },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Escaneando ${cpes.size} antenas...",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cpes) { cpe ->
                    CPEListItem(cpe = cpe)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val readyCPEs = cpes.filter { it.status == CPEStatus.READY }
            val failedCPEs = cpes.filter { it.status == CPEStatus.ERROR }
            
            if (readyCPEs.isNotEmpty()) {
                Button(
                    onClick = { onAllAnalyzed(cpes) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Continuar con ${readyCPEs.size} antenas")
                }
            } else {
                Text(
                    text = "No hay antenas disponibles",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun CPEListItem(cpe: CPE) {
    val backgroundColor = when (cpe.status) {
        CPEStatus.READY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        CPEStatus.TEST_OK -> Color(0xFF00FFF5).copy(alpha = 0.2f)
        CPEStatus.TEST_FAIL -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
        CPEStatus.ANALYZING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val indicatorColor = when (cpe.status) {
        CPEStatus.READY -> Color(0xFF4CAF50) // Verde
        CPEStatus.TEST_OK -> Color(0xFF00FFF5) // Azul
        CPEStatus.TEST_FAIL -> Color(0xFFF44336) // Rojo
        CPEStatus.ANALYZING -> Color(0xFFFFEB3B) // Amarillo
        CPEStatus.ERROR -> Color(0xFFF44336)
        else -> Color(0xFF666666)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(indicatorColor)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cpe.ip,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (cpe.model.isNotEmpty()) {
                Text(
                    text = "${cpe.model} - ${cpe.firmware}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            imageVector = when (cpe.status) {
                CPEStatus.READY -> Icons.Default.Check
                CPEStatus.TEST_OK -> Icons.Default.CheckCircle
                CPEStatus.TEST_FAIL -> Icons.Default.Cancel
                CPEStatus.ANALYZING -> Icons.Default.HourglassEmpty
                else -> Icons.Default.Error
            },
            contentDescription = null,
            tint = indicatorColor
        )
    }
}