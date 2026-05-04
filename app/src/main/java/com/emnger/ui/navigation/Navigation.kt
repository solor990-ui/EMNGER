package com.emnger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emnger.domain.model.*
import com.emnger.ui.screens.*

sealed class Screen(val route: String) {
    object InputIPs = Screen("input_ips")
    object Credentials = Screen("credentials")
    object Analysis = Screen("analysis")
    object AirOSEmulated = Screen("airos_emulated")
    object Confirmation = Screen("confirmation")
}

@Composable
fun EMNGERNavigation(
    navController: NavHostController = rememberNavController(),
    onExit: () -> Unit = {}
) {
    var credentials by remember { mutableStateOf<Credentials?>(null) }
    var cpes by remember { mutableStateOf<List<CPE>>(emptyList()) }
    
    NavHost(
        navController = navController,
        startDestination = Screen.InputIPs.route
    ) {
        composable(Screen.InputIPs.route) {
            InputIPsScreen(
                onNext = { ips ->
                    cpes = ips.map { ip ->
                        CPE(ip = ip, status = CPEStatus.PENDING)
                    }
                    navController.navigate(Screen.Credentials.route)
                }
            )
        }
        
        composable(Screen.Credentials.route) {
            CredentialsScreen(
                onNext = { creds ->
                    credentials = creds
                    navController.navigate(Screen.Analysis.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Analysis.route) {
            AnalysisScreen(
                cpes = cpes,
                credentials = credentials!!,
                onAllAnalyzed = { analyzedCPEs ->
                    cpes = analyzedCPEs
                    navController.navigate(Screen.AirOSEmulated.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.AirOSEmulated.route) {
            val readyCPEs = cpes.filter { it.status == CPEStatus.READY || it.status == CPEStatus.TEST_OK }
            
            AirOSEmulatedScreen(
                cpes = readyCPEs,
                credentials = credentials!!,
                onTest = { testCPEs, config ->
                    // TODO: Apply test changes
                    navController.navigate(Screen.Confirmation.route)
                },
                onApply = { applyCPEs, config ->
                    // TODO: Apply permanently
                    navController.navigate(Screen.Confirmation.route)
                },
                onDiscard = {
                    // TODO: Discard changes
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Confirmation.route) {
            // Confirmation screen with list of changes to be applied
            ConfirmationScreen(
                cpes = cpes,
                onConfirm = {
                    // Apply changes permanently
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
    }
}