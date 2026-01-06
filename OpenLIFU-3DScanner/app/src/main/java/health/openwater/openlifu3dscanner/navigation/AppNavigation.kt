package health.openwater.openlifu3dscanner.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import health.openwater.openlifu3dscanner.screen.home.HomeScreen
import health.openwater.openlifu3dscanner.screen.processing.ProcessingScreen
import health.openwater.openlifu3dscanner.screen.scanner.ScannerScreen
import health.openwater.openlifu3dscanner.screen.uploading.UploadingScreen
import health.openwater.openlifu3dscanner.screen.collection.ViewCollectionScreen
import health.openwater.openlifu3dscanner.screen.photoscan.PhotoscanScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ViewCollection : Screen("photoscan")

    object Photoscan : Screen("photoscan/{scanId}") {
        fun createRoute(scanId: String) = "photoscan/$scanId"
    }

    object Scanner : Screen("scanner/{collectionName}") {
        fun createRoute(collectionName: String) = "scanner/$collectionName"
    }

    object Processing : Screen("processing/{collectionName}") {
        fun createRoute(collectionName: String) = "processing/$collectionName"
    }

    object Uploading : Screen("uploading/{collectionName}") {
        fun createRoute(collectionName: String) = "uploading/$collectionName"
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
                onStartScan = { collectionName ->
                    navController.navigate(Screen.Scanner.createRoute(collectionName))
                },
                onViewCollection = {
                    navController.navigate(Screen.ViewCollection.route)
                }
            )
        }

        composable(Screen.ViewCollection.route) {
            ViewCollectionScreen(
                onNavigateBack = { navController.popBackStack() },
                onPhotoscanClick = { scanId ->
                    navController.navigate(Screen.Photoscan.createRoute(scanId.toString()))
                }
            )
        }

        composable(
            route = Screen.Photoscan.route,
            arguments = listOf(
                navArgument("scanId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
            PhotoscanScreen(
                scanId = scanId.toLong(),
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Scanner.route,
            arguments = listOf(
                navArgument("collectionName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val collectionName = backStackEntry.arguments?.getString("collectionName") ?: ""
            ScannerScreen(
                collectionName = collectionName,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProcessing = {
                    navController.navigate(
                        Screen.Processing.createRoute(collectionName)
                    ) {
                        popUpTo(Screen.Scanner.route) { inclusive = true }
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
                }
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
            )
        }
    }
}