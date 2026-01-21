package health.openwater.openlifu3dscanner.screen.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.api.DomainResult
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.viewmodel.CollectionViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ViewCollectionScreen(
    onNavigateBack: () -> Unit,
    onPhotoscanClick: (CollectionItem) -> Unit,
    collectionViewModel: CollectionViewModel = hiltViewModel()
) {
    val photocollectionsResponse by collectionViewModel.photocollectionsResponse.collectAsState()
    val photoscansResponse by collectionViewModel.photoscansResponse.collectAsState()

    fun onDeviceScans(): List<CollectionItem> {
        return getModelsDir().listFiles()?.filter { it.isDirectory }?.map {
            CollectionItem(
                id = 0,
                name = it.name,
                creationDate = Date(it.lastModified()),
                status = null,
            )
        } ?: emptyList()
    }

    var collectionItems by remember { mutableStateOf<List<CollectionItem>?>(null) }

    LaunchedEffect(photocollectionsResponse, photoscansResponse) {
        val onDeviceScans = onDeviceScans()
        if (photocollectionsResponse == null
            || photoscansResponse == null
            || photocollectionsResponse is DomainResult.Loading
            || photoscansResponse is DomainResult.Loading
        ) {
            return@LaunchedEffect
        }

        val photocollections = photocollectionsResponse?.let {
            if (it is DomainResult.Success) it.data else null
        }

        val photoscans = photoscansResponse?.let {
            if (it is DomainResult.Success) it.data else null
        }

        collectionItems = (photoscans?.reversed()?.map { photoscan ->
            CollectionItem(
                id = photoscan.id,
                name = photocollections?.find { it.id == photoscan.photocollectionId }?.name,
                creationDate = photoscan.creationDate,
                status = photoscan.status
            )
        } ?: emptyList()).plus(onDeviceScans).distinctBy { it.name }
    }

    fun refresh(showLoading: Boolean) {
        collectionViewModel.getPhotocollections(showLoading)
        collectionViewModel.getPhotoscans(showLoading)
    }

    // Load initial data
    LaunchedEffect(Unit) {
        refresh(showLoading = photocollectionsResponse == null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.view_collection)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { contentPadding ->

        val scroll = rememberScrollState()

        PullToRefreshBox(
            isRefreshing = photocollectionsResponse is DomainResult.Loading || photoscansResponse is DomainResult.Loading,
            onRefresh = {
                refresh(showLoading = true)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                when {
                    collectionItems?.isEmpty() == true -> {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scroll),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_photo_scans_available),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        // List of photo scans
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(collectionItems ?: emptyList()) { scan ->
                                PhotoscanCard(
                                    item = scan,
                                    onClick = {
                                        onPhotoscanClick(scan)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
