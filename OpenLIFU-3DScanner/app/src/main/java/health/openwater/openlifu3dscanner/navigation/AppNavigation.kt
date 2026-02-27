package health.openwater.openlifu3dscanner.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import health.openwater.openlifu3dscanner.screen.collection.CollectionScreen
import health.openwater.openlifu3dscanner.screen.create.CreateCollectionScreen
import health.openwater.openlifu3dscanner.screen.home.HomeScreen
import health.openwater.openlifu3dscanner.screen.permissions.PermissionsScreen
import health.openwater.openlifu3dscanner.screen.photoscan.PhotoscanScreen
import health.openwater.openlifu3dscanner.screen.processing.ProcessingScreen
import health.openwater.openlifu3dscanner.screen.qr.QrPayload
import health.openwater.openlifu3dscanner.screen.qr.QrScannerScreen
import health.openwater.openlifu3dscanner.screen.scanner.ScannerScreen
import health.openwater.openlifu3dscanner.screen.settings.SettingsScreen
import health.openwater.openlifu3dscanner.screen.signin.LoginScreen
import health.openwater.openlifu3dscanner.screen.transfer.TransferScreen
import health.openwater.openlifu3dscanner.screen.uploading.UploadingScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object SignIn : Screen("signin")
    object Settings : Screen("settings")
    object ViewCollection : Screen("photoscan")

    object Photoscan :
        Screen("photoscan/{collectionName}/{photoscanId}/{photocollectionId}") {
        fun createRoute(
            collectionName: String,
            photoscanId: Long,
            photocollectionId: Long
        ) =
            "photoscan/$collectionName/$photoscanId/$photocollectionId"
    }

    object CreateCollection : Screen("create_collection")
    object Permissions : Screen("permissions")

    object Scanner : Screen("scanner")
    object Processing : Screen("processing")
    object Uploading : Screen("uploading")
    object Transfer : Screen("transfer")
    object QrScanner : Screen("qr_scanner")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route, enterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Left
        )
    }, exitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Left
        )
    }, popEnterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Right
        )
    }, popExitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Right
        )
    }) {
        composable(Screen.Home.route) {
            HomeScreen(
                onCreateCollection = {
                    navController.navigate(Screen.CreateCollection.route)
                },
                onRequestPermissions = {
                    navController.navigate(Screen.Permissions.route)
                },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onViewCollection = {
                    navController.navigate(Screen.ViewCollection.route)
                },
                onSignIn = { navController.navigate(Screen.SignIn.route) })
        }

        composable(Screen.QrScanner.route) {
            QrScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onScanConfirmed = { payload ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("qr_session_id", payload.sessionId)
                        set("qr_session_name", payload.sessionName)
                    }
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.CreateCollection.route) { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            val qrSessionId = savedStateHandle.getStateFlow<String?>("qr_session_id", null)
                .collectAsStateWithLifecycle()
            val qrSessionName = savedStateHandle.getStateFlow<String?>("qr_session_name", null)
                .collectAsStateWithLifecycle()
            val qrPayload = if (qrSessionId.value != null && qrSessionName.value != null) {
                QrPayload(qrSessionId.value!!, qrSessionName.value!!)
            } else null

            CreateCollectionScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartScan = {
                    navController.navigate(Screen.Scanner.route) {
                        popUpTo(Screen.CreateCollection.route) { inclusive = true }
                    }
                },
                onQrScan = { navController.navigate(Screen.QrScanner.route) },
                qrPayload = qrPayload,
                onQrPayloadConsumed = {
                    savedStateHandle.remove<String>("qr_session_id")
                    savedStateHandle.remove<String>("qr_session_name")
                }
            )
        }

        composable(Screen.SignIn.route) {
            LoginScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Permissions.route) {
            PermissionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onPermissionsGranted = { navController.popBackStack() }
            )
        }

        composable(Screen.ViewCollection.route) {
            CollectionScreen(
                onNavigateBack = { navController.popBackStack() },
                onPhotoscanClick = { item ->
                    navController.navigate(
                        Screen.Photoscan.createRoute(
                            collectionName = item.name,
                            photoscanId = item.photoscanId,
                            photocollectionId = item.photocollectionId
                        )
                    )
                })
        }

        composable(
            route = Screen.Photoscan.route,
            arguments = listOf(
                navArgument("collectionName") { type = NavType.StringType },
                navArgument("photoscanId") { type = NavType.LongType },
                navArgument("photocollectionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val collectionName = backStackEntry.arguments?.getString("collectionName") ?: ""
            val photoscanId = backStackEntry.arguments?.getLong("photoscanId") ?: 0L
            val photocollectionId = backStackEntry.arguments?.getLong("photocollectionId") ?: 0L
            val isLocalOnly = photoscanId == 0L

            PhotoscanScreen(
                collectionName = collectionName,
                photoscanId = photoscanId,
                photocollectionId = photocollectionId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTransfer = {
                    navController.navigate(Screen.Transfer.route)
                },
                onStartProcessing = if (isLocalOnly) {
                    {
                        navController.navigate(Screen.Uploading.route) {
                            popUpTo(Screen.Photoscan.route) { inclusive = true }
                        }
                    }
                } else null
            )
        }

        composable(Screen.Scanner.route) {
            ScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProcessing = { autoUploadEnabled, isLoggedIn ->
                    if (autoUploadEnabled && isLoggedIn) {
                        navController.navigate(Screen.Uploading.route) {
                            popUpTo(Screen.Scanner.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Processing.route) {
                            popUpTo(Screen.Scanner.route) { inclusive = true }
                        }
                    }
                })
        }

        composable(Screen.Processing.route) {
            ProcessingScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUploading = {
                    navController.navigate(Screen.Uploading.route) {
                        popUpTo(Screen.Processing.route) { inclusive = true }
                    }
                },
                onNavigateToTransfer = {
                    navController.navigate(Screen.Transfer.route) {
                        popUpTo(Screen.Processing.route) { inclusive = true }
                    }
                })
        }

        composable(Screen.Transfer.route) {
            TransferScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Uploading.route) {
            UploadingScreen(
                onNavigateBack = { navController.popBackStack() },
                onViewModel = { scanId, photocollectionId, collectionName ->
                    navController.navigate(
                        Screen.Photoscan.createRoute(collectionName, scanId, photocollectionId)
                    ) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                })
        }
    }
}