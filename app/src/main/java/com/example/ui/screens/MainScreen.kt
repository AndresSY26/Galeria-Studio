package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.PersonProfile
import com.example.data.model.Photo
import com.example.ui.components.CloudSyncStatusBadge
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.AuraPrimary
import com.example.ui.viewmodel.GalleryTab
import com.example.ui.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GalleryViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val selectedPhotoIds by viewModel.selectedPhotoIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val activePhoto by viewModel.activePhoto.collectAsState()
    val isAnalyzingAi by viewModel.isAnalyzingAi.collectAsState()
    val aiAnalysisResult by viewModel.aiAnalysisResult.collectAsState()
    val aiAnalysisError by viewModel.aiAnalysisError.collectAsState()
    val isScanningDevice by viewModel.isScanningDevice.collectAsState()
    val isAiScanning by viewModel.isAiScanning.collectAsState()
    val aiScanProgress by viewModel.aiScanProgress.collectAsState()

    val dateGroups by viewModel.groupedPhotos.collectAsState()
    val filteredPhotos by viewModel.filteredPhotos.collectAsState()
    val allPhotos by viewModel.allPhotos.collectAsState()
    val peopleProfiles by viewModel.peopleProfiles.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val selectedPerson by viewModel.selectedPersonFilter.collectAsState()
    val filterOnlyFavorites by viewModel.filterOnlyFavorites.collectAsState()
    val filterOnlyEncrypted by viewModel.filterOnlyEncrypted.collectAsState()
    val selectedStorageFilter by viewModel.selectedStorageFilter.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val storageOverview by viewModel.storageOverview.collectAsState()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsState()
    val autoBackupStatus by viewModel.autoBackupStatus.collectAsState()
    val isWifiOnlyBackup by viewModel.isWifiOnlyBackup.collectAsState()
    val isBiometricLockEnabled by viewModel.isBiometricLockEnabled.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()

    var activeAlbum by remember { mutableStateOf<ActiveAlbum?>(null) }

    val albumPhotos = remember(activeAlbum, allPhotos) {
        val current = activeAlbum ?: return@remember emptyList<Photo>()
        when (current.filterType) {
            AlbumFilterType.CATEGORY_OR_PLATFORM -> {
                allPhotos.filter { photo ->
                    val platform = com.example.util.SmartMediaTagger.detectPlatform(photo)
                    platform.displayName.equals(current.filterValue, ignoreCase = true) ||
                    platform.id.equals(current.filterValue, ignoreCase = true) ||
                    photo.category.equals(current.filterValue, ignoreCase = true) ||
                    photo.tags.any { it.equals(current.filterValue, ignoreCase = true) } ||
                    photo.locationName.contains(current.filterValue, ignoreCase = true) ||
                    photo.title.contains(current.filterValue, ignoreCase = true)
                }
            }
            AlbumFilterType.PERSON -> {
                allPhotos.filter { photo ->
                    photo.faces.any { it.name.equals(current.filterValue, ignoreCase = true) || it.name.contains(current.filterValue, ignoreCase = true) }
                }
            }
            AlbumFilterType.FAVORITES -> {
                allPhotos.filter { it.isFavorite }
            }
            AlbumFilterType.LOCATION -> {
                allPhotos.filter { photo ->
                    photo.locationName.contains(current.filterValue, ignoreCase = true) || photo.title.contains(current.filterValue, ignoreCase = true)
                }
            }
            AlbumFilterType.ALL_VIDEOS -> {
                allPhotos.filter { it.isVideo || it.category.equals("Videos", ignoreCase = true) }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncDeviceMedia()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission launcher for accessing media
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.any { it }
        viewModel.setPermissionGranted(isGranted)
        if (isGranted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Acceso concedido. Escaneando multimedia...")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }
    }

    // Photo picker launcher for importing photos and videos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                viewModel.importMediaUri(uri.toString())
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar("${uris.size} archivo(s) añadido(s) a la galería")
            }
        }
    }

    fun triggerPermissionOrScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }
        viewModel.syncDeviceMedia()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Navigation Rail on tablet / landscape
            if (isWideScreen) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = CircleShape,
                        color = AuraPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Galería", tint = AuraPrimary, modifier = Modifier.padding(10.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    NavigationRailItem(
                        selected = selectedTab == GalleryTab.PHOTOS,
                        onClick = {
                            activeAlbum = null
                            viewModel.clearFilters()
                            viewModel.setTab(GalleryTab.PHOTOS)
                        },
                        icon = { Icon(if (selectedTab == GalleryTab.PHOTOS) Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary, contentDescription = null) },
                        label = { Text("Fotos") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == GalleryTab.EXPLORE,
                        onClick = { viewModel.setTab(GalleryTab.EXPLORE) },
                        icon = { Icon(if (selectedTab == GalleryTab.EXPLORE) Icons.Filled.Explore else Icons.Outlined.Explore, contentDescription = null) },
                        label = { Text("Álbumes") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == GalleryTab.SEARCH,
                        onClick = { viewModel.setTab(GalleryTab.SEARCH) },
                        icon = { Icon(if (selectedTab == GalleryTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search, contentDescription = null) },
                        label = { Text("Buscar") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == GalleryTab.CLOUD_VAULT,
                        onClick = { viewModel.setTab(GalleryTab.CLOUD_VAULT) },
                        icon = { Icon(if (selectedTab == GalleryTab.CLOUD_VAULT) Icons.Filled.Shield else Icons.Outlined.Shield, contentDescription = null) },
                        label = { Text("Bóveda") }
                    )
                }
            }

            // Main Content Area
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (!(selectedTab == GalleryTab.EXPLORE && activeAlbum != null)) {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = AuraPrimary.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AuraPrimary.copy(alpha = 0.35f)),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoLibrary,
                                                contentDescription = null,
                                                tint = AuraCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = when (selectedTab) {
                                                GalleryTab.PHOTOS -> "Galería"
                                                GalleryTab.EXPLORE -> "Álbumes"
                                                GalleryTab.SEARCH -> "Buscar"
                                                GalleryTab.CLOUD_VAULT -> "Bóveda Cifrada"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 19.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        if (selectedTab == GalleryTab.PHOTOS) {
                                            Text(
                                                text = if (isScanningDevice) "Escaneando almacenamiento..." else if (allPhotos.size == 1) "1 elemento del dispositivo" else "${allPhotos.size} fotos y videos locales",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                if (selectedTab == GalleryTab.PHOTOS) {
                                    IconButton(
                                        onClick = {
                                            viewModel.syncDeviceMedia()
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Escaneando multimedia del dispositivo...")
                                            }
                                        },
                                        modifier = Modifier.testTag("top_refresh_button")
                                    ) {
                                        if (isScanningDevice) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AuraPrimary)
                                        } else {
                                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Actualizar multimedia", tint = AuraPrimary)
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                            )
                                        },
                                        modifier = Modifier.testTag("top_add_photo_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Importar fotos", tint = AuraCyan)
                                    }
                                }

                                if (selectedTab != GalleryTab.SEARCH) {
                                    IconButton(
                                        onClick = { viewModel.setTab(GalleryTab.SEARCH) },
                                        modifier = Modifier.testTag("top_search_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                                    }
                                }

                                CloudSyncStatusBadge(
                                    isSyncing = syncState.isSyncing,
                                    progress = syncState.progress,
                                    statusText = syncState.statusMessage,
                                    onClick = { viewModel.setTab(GalleryTab.CLOUD_VAULT) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == GalleryTab.PHOTOS,
                                onClick = {
                                    activeAlbum = null
                                    viewModel.clearFilters()
                                    viewModel.setTab(GalleryTab.PHOTOS)
                                },
                                icon = { Icon(if (selectedTab == GalleryTab.PHOTOS) Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary, contentDescription = null) },
                                label = { Text("Fotos", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AuraPrimary,
                                    indicatorColor = AuraPrimary.copy(alpha = 0.12f)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == GalleryTab.EXPLORE,
                                onClick = { viewModel.setTab(GalleryTab.EXPLORE) },
                                icon = { Icon(if (selectedTab == GalleryTab.EXPLORE) Icons.Filled.Explore else Icons.Outlined.Explore, contentDescription = null) },
                                label = { Text("Álbumes", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AuraCyan,
                                    indicatorColor = AuraCyan.copy(alpha = 0.12f)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == GalleryTab.SEARCH,
                                onClick = { viewModel.setTab(GalleryTab.SEARCH) },
                                icon = { Icon(if (selectedTab == GalleryTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search, contentDescription = null) },
                                label = { Text("Buscar", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AuraPrimary,
                                    indicatorColor = AuraPrimary.copy(alpha = 0.12f)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == GalleryTab.CLOUD_VAULT,
                                onClick = { viewModel.setTab(GalleryTab.CLOUD_VAULT) },
                                icon = { Icon(if (selectedTab == GalleryTab.CLOUD_VAULT) Icons.Filled.Shield else Icons.Outlined.Shield, contentDescription = null) },
                                label = { Text("Bóveda", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AuraEmerald,
                                    indicatorColor = AuraEmerald.copy(alpha = 0.12f)
                                )
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tabContent"
                    ) { tab ->
                        when (tab) {
                            GalleryTab.PHOTOS -> {
                                GalleryTimelineScreen(
                                    dateGroups = dateGroups,
                                    totalPhotoCount = filteredPhotos.size,
                                    columns = gridColumns,
                                    selectedPhotoIds = selectedPhotoIds,
                                    isSelectionMode = isSelectionMode,
                                    storageFilter = selectedStorageFilter,
                                    storageOverview = storageOverview,
                                    isScanning = isScanningDevice,
                                    onSetStorageFilter = { viewModel.setStorageFilter(it) },
                                    onSetColumns = { viewModel.setGridColumns(it) },
                                    onPhotoClick = { viewModel.openPhoto(it) },
                                    onPhotoLongClick = { viewModel.togglePhotoSelection(it.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onClearSelection = { viewModel.clearSelection() },
                                    onSelectAll = { viewModel.selectAll() },
                                    onDeleteSelected = { viewModel.deleteSelectedPhotos() },
                                    onEncryptSelected = { viewModel.encryptSelectedPhotos() },
                                    onBackupSelected = { viewModel.backupSelectedPhotos() },
                                    onFreeSpaceSelected = { viewModel.freeSpaceForSelectedPhotos() },
                                    onDownloadSelected = { viewModel.downloadSelectedToDevice() },
                                    onBackupAllPending = { viewModel.backupAllPendingPhotos() },
                                    onScanMedia = { triggerPermissionOrScan() },
                                    onOpenPicker = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    }
                                )
                            }
                            GalleryTab.EXPLORE -> {
                                if (activeAlbum != null) {
                                    AlbumDetailScreen(
                                        album = activeAlbum!!,
                                        photos = albumPhotos,
                                        onBack = { activeAlbum = null },
                                        onPhotoClick = { viewModel.openPhoto(it) },
                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                        onDeleteSelected = { viewModel.deleteSelectedPhotos() },
                                        onEncryptSelected = { viewModel.encryptSelectedPhotos() },
                                        onBackupSelected = { viewModel.backupSelectedPhotos() }
                                    )
                                } else {
                                    ExploreCategoriesScreen(
                                        photos = allPhotos,
                                        peopleProfiles = peopleProfiles,
                                        categoryCounts = categoryCounts,
                                        isAiScanning = isAiScanning,
                                        aiScanProgress = aiScanProgress,
                                        onRunAiScan = { viewModel.runFullAiScan() },
                                        onSelectAlbum = { albumMeta ->
                                            activeAlbum = ActiveAlbum(
                                                title = albumMeta.name,
                                                subtitle = albumMeta.subtitle,
                                                icon = albumMeta.icon,
                                                filterType = AlbumFilterType.CATEGORY_OR_PLATFORM,
                                                filterValue = albumMeta.name
                                            )
                                        },
                                        onSelectPerson = { profile ->
                                            activeAlbum = ActiveAlbum(
                                                title = profile.name,
                                                subtitle = if (profile.photoCount == 1) "1 foto" else "${profile.photoCount} fotos",
                                                icon = Icons.Default.Face,
                                                filterType = AlbumFilterType.PERSON,
                                                filterValue = profile.name
                                            )
                                        },
                                        onSelectFavorites = {
                                            activeAlbum = ActiveAlbum(
                                                title = "Favoritos",
                                                subtitle = "Tus fotos y videos destacados",
                                                icon = Icons.Default.Favorite,
                                                filterType = AlbumFilterType.FAVORITES,
                                                filterValue = "Favoritos"
                                            )
                                        },
                                        onSelectEncrypted = {
                                            viewModel.setTab(GalleryTab.CLOUD_VAULT)
                                        },
                                        onSelectLocation = { location ->
                                            activeAlbum = ActiveAlbum(
                                                title = location,
                                                subtitle = "Carpeta / Ubicación",
                                                icon = Icons.Default.LocationOn,
                                                filterType = AlbumFilterType.LOCATION,
                                                filterValue = location
                                            )
                                        }
                                    )
                                }
                            }
                            GalleryTab.SEARCH -> {
                                AdvancedSearchScreen(
                                    searchQuery = searchQuery,
                                    filteredPhotos = filteredPhotos,
                                    allPhotos = allPhotos,
                                    peopleProfiles = peopleProfiles,
                                    selectedCategory = selectedCategory,
                                    selectedPerson = selectedPerson,
                                    onlyFavorites = filterOnlyFavorites,
                                    onlyEncrypted = filterOnlyEncrypted,
                                    onSearchQueryChange = { viewModel.searchQuery.value = it },
                                    onCategoryFilterChange = { viewModel.selectedCategoryFilter.value = it },
                                    onPersonFilterChange = { viewModel.selectedPersonFilter.value = it },
                                    onToggleFavoritesFilter = { viewModel.filterOnlyFavorites.value = !filterOnlyFavorites },
                                    onToggleEncryptedFilter = { viewModel.filterOnlyEncrypted.value = !filterOnlyEncrypted },
                                    onPhotoClick = { viewModel.openPhoto(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                                )
                            }
                            GalleryTab.CLOUD_VAULT -> {
                                CloudVaultScreen(
                                    syncState = syncState,
                                    storageOverview = storageOverview,
                                    devices = viewModel.connectedDevices,
                                    totalPhotos = allPhotos.size,
                                    isAutoBackupEnabled = isAutoBackupEnabled,
                                    autoBackupStatus = autoBackupStatus,
                                    onToggleAutoBackup = { viewModel.setAutoBackupEnabled(it) },
                                    onTriggerAutoBackupCheck = { viewModel.triggerAutoBackupCheck() },
                                    isWifiOnlyBackup = isWifiOnlyBackup,
                                    onToggleWifiOnlyBackup = { viewModel.setWifiOnlyBackup(it) },
                                    isBiometricLockEnabled = isBiometricLockEnabled,
                                    isVaultUnlocked = isVaultUnlocked,
                                    onToggleBiometricLock = { viewModel.toggleBiometricLock(it) },
                                    onUnlockVault = { viewModel.unlockVault() },
                                    onLockVault = { viewModel.lockVault() },
                                    onTriggerSync = { viewModel.triggerSyncNow() },
                                    onBackupAllPending = { viewModel.backupAllPendingPhotos() },
                                    onFreeSpaceAll = { viewModel.freeSpaceForAllSyncedPhotos() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen Lightbox & AI Inspector Overlay
        AnimatedVisibility(
            visible = activePhoto != null,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
        ) {
            val currentPhoto = activePhoto
            if (currentPhoto != null) {
                PhotoViewerDialog(
                    photo = currentPhoto,
                    isAnalyzingAi = isAnalyzingAi,
                    aiAnalysisResult = aiAnalysisResult,
                    aiErrorMessage = aiAnalysisError,
                    onClose = { viewModel.closePhoto() },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onDelete = { viewModel.deletePhoto(it) },
                    onBackupPhoto = { viewModel.backupPhoto(it) },
                    onFreeSpacePhoto = { viewModel.freeSpacePhoto(it) },
                    onDownloadPhoto = { viewModel.downloadPhoto(it) },
                    onAnalyzeAi = { viewModel.analyzePhotoWithAi(it) },
                    onApplyAiResult = { p, res -> viewModel.applyAiAnalysisToPhoto(p, res) },
                    onAddTag = { p, tag -> viewModel.addTagToPhoto(p, tag) },
                    onRemoveTag = { p, tag -> viewModel.removeTagFromPhoto(p, tag) }
                )
            }
        }
    }
}
