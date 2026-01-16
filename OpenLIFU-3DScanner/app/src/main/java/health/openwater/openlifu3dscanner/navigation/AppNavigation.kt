package health.openwater.openlifu3dscanner.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import health.openwater.openlifu3dscanner.screen.collection.ViewCollectionScreen
import health.openwater.openlifu3dscanner.screen.home.HomeScreen
import health.openwater.openlifu3dscanner.screen.photoscan.PhotoscanScreen
import health.openwater.openlifu3dscanner.screen.processing.ProcessingScreen
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

    object Photoscan : Screen("photoscan/{scanId}/{autoDownloadEnabled}") {
        fun createRoute(scanId: Long, autoDownloadEnabled: Boolean) =
            "photoscan/$scanId/$autoDownloadEnabled"
    }

    object Scanner : Screen("scanner/{collectionName}/{autoUploadEnabled}") {
        fun createRoute(collectionName: String, autoUploadEnabled: Boolean) =
            "scanner/$collectionName/$autoUploadEnabled"
    }

    object Processing : Screen("processing/{collectionName}") {
        fun createRoute(collectionName: String) = "processing/$collectionName"
    }

    object Uploading : Screen("uploading/{collectionName}") {
        fun createRoute(collectionName: String) = "uploading/$collectionName"
    }

    object Transfer : Screen("transfer/{collectionName}") {
        fun createRoute(collectionName: String) = "transfer/$collectionName"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStartScan = { collectionName, autoUploadEnabled ->
                    navController.navigate(
                        Screen.Scanner.createRoute(
                            collectionName,
                            autoUploadEnabled
                        )
                    )
                },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onViewCollection = {
                    navController.navigate(Screen.ViewCollection.route)
                },
                onSignIn = { navController.navigate(Screen.SignIn.route) }
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

        composable(Screen.ViewCollection.route) {
            ViewCollectionScreen(
                onNavigateBack = { navController.popBackStack() },
                onPhotoscanClick = { scanId ->
                    navController.navigate(Screen.Photoscan.createRoute(scanId, false))
                }
            )
        }

        composable(
            route = Screen.Photoscan.route,
            arguments = listOf(
                navArgument("scanId") { type = NavType.StringType },
                navArgument("autoDownloadEnabled") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
            val autoDownloadEnabled =
                backStackEntry.arguments?.getBoolean("autoDownloadEnabled") ?: false

            PhotoscanScreen(
                scanId = scanId.toLong(),
                autoDownloadEnabled = autoDownloadEnabled,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Scanner.route,
            arguments = listOf(
                navArgument("collectionName") { type = NavType.StringType },
                navArgument("autoUploadEnabled") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val collectionName = backStackEntry.arguments?.getString("collectionName") ?: ""
            val autoUploadEnabled =
                backStackEntry.arguments?.getBoolean("autoUploadEnabled") ?: false

            ScannerScreen(
                autoUploadEnabled = autoUploadEnabled,
                collectionName = collectionName,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProcessing = { autoUploadEnabled, isLoggedIn ->

                    if (autoUploadEnabled && isLoggedIn) {
                        navController.navigate(
                            Screen.Uploading.createRoute(collectionName)
                        ) {
                            popUpTo(Screen.Scanner.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(
                            Screen.Processing.createRoute(collectionName)
                        ) {
                            popUpTo(Screen.Scanner.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.Processing.route,
            arguments = listOf(
                navArgument("collectionName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val collectionName = backStackEntry.arguments?.getString("collectionName") ?: ""
            ProcessingScreen(
                collectionName = collectionName,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUploading = {
                    navController.navigate(
                        Screen.Uploading.createRoute(collectionName)
                    ) {
                        popUpTo(Screen.Processing.route) { inclusive = true }
                    }
                },
                onNavigateToTransfer = {
                    navController.navigate(
                        Screen.Transfer.createRoute(collectionName)
                    ) {
                        popUpTo(Screen.Processing.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Transfer.route,
            arguments = listOf(
                navArgument("collectionName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val collectionName = backStackEntry.arguments?.getString("collectionName") ?: ""
            TransferScreen(
                collectionName = collectionName,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Uploading.route,
            arguments = listOf(
                navArgument("collectionName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val collectionName = backStackEntry.arguments?.getString("collectionName") ?: ""
            UploadingScreen(
                collectionName = collectionName,
                onNavigateBack = { navController.popBackStack() },
                onViewModel = { scanId ->
                    navController.navigate(
                        Screen.Photoscan.createRoute(scanId, true)
                    ) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }
    }
}