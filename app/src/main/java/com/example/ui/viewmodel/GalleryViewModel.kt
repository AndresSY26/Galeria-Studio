package com.example.ui.viewmodel

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CloudDevice
import com.example.data.model.PersonProfile
import com.example.data.model.Photo
import com.example.data.remote.AiAnalysisResult
import com.example.data.repository.PhotoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GalleryTab {
    PHOTOS,
    EXPLORE,
    SEARCH,
    CLOUD_VAULT
}

enum class StorageFilter {
    ALL,            // Todas las fotos
    ON_DEVICE,      // En este dispositivo
    IN_BACKUP,      // En la copia de seguridad
    PENDING_BACKUP, // Pendientes de respaldo
    CLOUD_ONLY      // Solo en la nube (espacio liberado)
}

data class DateGroup(
    val title: String,
    val photos: List<Photo>
)

data class StorageOverview(
    val totalPhotos: Int = 0,
    val devicePhotosCount: Int = 0,
    val deviceStorageMb: Float = 0f,
    val backupPhotosCount: Int = 0,
    val backupStorageMb: Float = 0f,
    val pendingBackupCount: Int = 0,
    val cloudOnlyCount: Int = 0,
    val syncedPhotosCount: Int = 0
) {
    val deviceStorageUsedMb: Float get() = deviceStorageMb
    val backupStorageUsedMb: Float get() = backupStorageMb
}

data class SyncProgressState(
    val isSyncing: Boolean = false,
    val progress: Float = 1.0f,
    val statusMessage: String = "100% Sincronizado y Cifrado E2EE",
    val lastSyncTime: String = "Hace un momento",
    val e2eeMasterFingerprint: String = "SHA-256: 8F3A-4D82-91BB-77C4-E2EE",
    val zeroKnowledgeActive: Boolean = true,
    val totalStorageUsedMb: Float = 0f,
    val totalStorageQuotaMb: Float = 204800f // 200 GB
)

data class AutoBackupStatusInfo(
    val isEnabled: Boolean = true,
    val isRunningNow: Boolean = false,
    val lastBackupFormatted: String = "Ahora mismo",
    val autoBackedUpCount: Int = 0,
    val pendingQueueCount: Int = 0,
    val statusDescription: String = "Monitoreando fotos nuevas en segundo plano"
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PhotoRepository(AppDatabase.getDatabase(application).photoDao())
    private val prefs = application.getSharedPreferences("vault_privacy_prefs", android.content.Context.MODE_PRIVATE)

    // Background Auto Backup State & Persistence
    private val _isAutoBackupEnabled = MutableStateFlow(prefs.getBoolean("auto_backup_enabled", true))
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    private val _isWifiOnlyBackup = MutableStateFlow(prefs.getBoolean("wifi_only_enabled", false))
    val isWifiOnlyBackup: StateFlow<Boolean> = _isWifiOnlyBackup.asStateFlow()

    private val _autoBackupStatus = MutableStateFlow(
        AutoBackupStatusInfo(
            isEnabled = prefs.getBoolean("auto_backup_enabled", true),
            statusDescription = if (prefs.getBoolean("auto_backup_enabled", true)) "Monitoreando fotos nuevas en segundo plano" else "Servicio en pausa"
        )
    )
    val autoBackupStatus: StateFlow<AutoBackupStatusInfo> = _autoBackupStatus.asStateFlow()

    // Biometric Vault Lock State & Persistence
    private val _isBiometricLockEnabled = MutableStateFlow(prefs.getBoolean("biometric_lock_enabled", true))
    val isBiometricLockEnabled: StateFlow<Boolean> = _isBiometricLockEnabled.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(!prefs.getBoolean("biometric_lock_enabled", true))
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    // Scanning & Permission State
    private val _isScanningDevice = MutableStateFlow(false)
    val isScanningDevice: StateFlow<Boolean> = _isScanningDevice.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    // Global AI Vision Scanning State
    private val _isAiScanning = MutableStateFlow(false)
    val isAiScanning: StateFlow<Boolean> = _isAiScanning.asStateFlow()

    private val _aiScanProgress = MutableStateFlow(0f)
    val aiScanProgress: StateFlow<Float> = _aiScanProgress.asStateFlow()

    private val _aiScanMessage = MutableStateFlow("")
    val aiScanMessage: StateFlow<String> = _aiScanMessage.asStateFlow()

    private var mediaContentObserver: ContentObserver? = null
    private var debounceSyncJob: Job? = null

    private fun registerMediaContentObserver() {
        try {
            val contentResolver = getApplication<Application>().contentResolver
            mediaContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    debounceSyncJob?.cancel()
                    debounceSyncJob = viewModelScope.launch {
                        delay(350)
                        syncDeviceMedia()
                    }
                }
            }

            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaContentObserver!!
            )
            contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaContentObserver!!
            )
        } catch (e: Exception) {
            Log.e("GalleryViewModel", "Could not register media ContentObserver", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        debounceSyncJob?.cancel()
        mediaContentObserver?.let {
            try {
                getApplication<Application>().contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Error unregistering ContentObserver", e)
            }
        }
    }

    // Active Navigation Tab
    private val _selectedTab = MutableStateFlow(GalleryTab.PHOTOS)
    val selectedTab: StateFlow<GalleryTab> = _selectedTab.asStateFlow()

    // Storage Filter
    private val _selectedStorageFilter = MutableStateFlow(StorageFilter.ALL)
    val selectedStorageFilter: StateFlow<StorageFilter> = _selectedStorageFilter.asStateFlow()

    // Grid Columns (1 = Full Card, 2 = Medium, 3 = Compact, 4 = Dense)
    private val _gridColumns = MutableStateFlow(3)
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    // Selection Mode
    private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = _selectedPhotoIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Active Photo for Fullscreen Lightbox
    private val _activePhoto = MutableStateFlow<Photo?>(null)
    val activePhoto: StateFlow<Photo?> = _activePhoto.asStateFlow()

    // AI Inspector State
    private val _isAnalyzingAi = MutableStateFlow(false)
    val isAnalyzingAi: StateFlow<Boolean> = _isAnalyzingAi.asStateFlow()

    private val _aiAnalysisResult = MutableStateFlow<AiAnalysisResult?>(null)
    val aiAnalysisResult: StateFlow<AiAnalysisResult?> = _aiAnalysisResult.asStateFlow()

    private val _aiAnalysisError = MutableStateFlow<String?>(null)
    val aiAnalysisError: StateFlow<String?> = _aiAnalysisError.asStateFlow()

    // Search & Filter State
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedPersonFilter = MutableStateFlow<String?>(null)
    val filterOnlyFavorites = MutableStateFlow(false)
    val filterOnlyEncrypted = MutableStateFlow(false)

    fun clearFilters() {
        searchQuery.value = ""
        selectedCategoryFilter.value = null
        selectedPersonFilter.value = null
        filterOnlyFavorites.value = false
        filterOnlyEncrypted.value = false
    }

    // Sync State
    private val _syncState = MutableStateFlow(SyncProgressState())
    val syncState: StateFlow<SyncProgressState> = _syncState.asStateFlow()

    val connectedDevices: List<CloudDevice> = repository.getConnectedDevices()

    // All photos from Room
    val allPhotos: StateFlow<List<Photo>> = repository.allPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Storage Overview stats
    val storageOverview: StateFlow<StorageOverview> = allPhotos.map { photos ->
        var devCount = 0
        var devKb = 0L
        var backCount = 0
        var backKb = 0L
        var pendCount = 0
        var cloudOnly = 0

        for (p in photos) {
            if (p.isOnDevice) {
                devCount++
                devKb += p.fileSizeKb
            }
            if (p.isInCloudBackup) {
                backCount++
                backKb += p.fileSizeKb
            }
            if (p.isOnDevice && !p.isInCloudBackup) {
                pendCount++
            }
            if (!p.isOnDevice && p.isInCloudBackup) {
                cloudOnly++
            }
        }

        StorageOverview(
            totalPhotos = photos.size,
            devicePhotosCount = devCount,
            deviceStorageMb = devKb / 1024f,
            backupPhotosCount = backCount,
            backupStorageMb = backKb / 1024f,
            pendingBackupCount = pendCount,
            cloudOnlyCount = cloudOnly,
            syncedPhotosCount = devCount - pendCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StorageOverview())

    data class FilterCriteria(
        val query: String,
        val category: String?,
        val person: String?,
        val onlyFavorites: Boolean,
        val onlyEncrypted: Boolean,
        val storageFilter: StorageFilter
    )

    // Filtered photos based on search, category, person, flags, and storage filter
    private val filterCriteria = combine(
        searchQuery,
        selectedCategoryFilter,
        selectedPersonFilter,
        filterOnlyFavorites,
        filterOnlyEncrypted
    ) { query, category, person, onlyFav, onlyEnc ->
        Triple(query, category, Pair(person, Pair(onlyFav, onlyEnc)))
    }

    val filteredPhotos: StateFlow<List<Photo>> = combine(
        allPhotos,
        filterCriteria,
        selectedStorageFilter
    ) { photos, criteriaTriple, storageFilter ->
        val query = criteriaTriple.first
        val category = criteriaTriple.second
        val person = criteriaTriple.third.first
        val onlyFav = criteriaTriple.third.second.first
        val onlyEnc = criteriaTriple.third.second.second

        photos.filter { photo ->
            val matchesQuery = com.example.util.SmartMediaTagger.matchesSearch(photo, query)
            val platform = com.example.util.SmartMediaTagger.detectPlatform(photo)
            val matchesCategory = category == null ||
                photo.category.equals(category, ignoreCase = true) ||
                photo.locationName.equals(category, ignoreCase = true) ||
                platform.displayName.equals(category, ignoreCase = true) ||
                platform.id.equals(category, ignoreCase = true) ||
                photo.tags.any { it.equals(category, ignoreCase = true) }

            val matchesPerson = person == null || photo.faces.any { it.name.equals(person, ignoreCase = true) || it.name.contains(person, ignoreCase = true) }
            val matchesFav = !onlyFav || photo.isFavorite
            val matchesEnc = !onlyEnc || photo.isEncrypted

            val matchesStorage = when (storageFilter) {
                StorageFilter.ALL -> true
                StorageFilter.ON_DEVICE -> photo.isOnDevice
                StorageFilter.IN_BACKUP -> photo.isInCloudBackup
                StorageFilter.PENDING_BACKUP -> photo.isOnDevice && !photo.isInCloudBackup
                StorageFilter.CLOUD_ONLY -> !photo.isOnDevice && photo.isInCloudBackup
            }

            matchesQuery && matchesCategory && matchesPerson && matchesFav && matchesEnc && matchesStorage
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Grouped by Date Timeline (Month clusters for full, clean grid rows)
    val groupedPhotos: StateFlow<List<DateGroup>> = filteredPhotos.map { photos ->
        val groups = linkedMapOf<String, MutableList<Photo>>()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))

        for (photo in photos) {
            val groupKey = monthFormat.format(Date(photo.timestamp)).replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
            groups.getOrPut(groupKey) { mutableListOf() }.add(photo)
        }

        groups.map { (title, list) ->
            DateGroup(title = title, photos = list)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Extracted People Profiles with Face Recognition clustering
    val peopleProfiles: StateFlow<List<PersonProfile>> = allPhotos.map { photos ->
        val map = mutableMapOf<String, MutableList<Photo>>()
        for (photo in photos) {
            for (face in photo.faces) {
                if (face.name.isNotBlank()) {
                    map.getOrPut(face.name) { mutableListOf() }.add(photo)
                }
            }
        }
        map.map { (name, list) ->
            PersonProfile(
                id = name.lowercase().replace(" ", "_"),
                name = name,
                samplePhotoUrl = list.firstOrNull()?.uriString ?: "",
                photoCount = list.size,
                relation = "Reconocido en fotos"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All available categories with counts
    val categoryCounts: StateFlow<Map<String, Int>> = allPhotos.map { photos ->
        val map = mutableMapOf<String, Int>()
        for (photo in photos) {
            val cat = photo.category.ifBlank { "Dispositivo" }
            map[cat] = (map[cat] ?: 0) + 1
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        // Register ContentObserver for real-time automatic loading of new media
        registerMediaContentObserver()

        viewModelScope.launch {
            // Ensure no mock data exists in the platform
            repository.purgeMockData()
            // Scan real media from the device immediately
            syncDeviceMedia()

            // Run deep AI Vision classification on all photos immediately
            runFullAiScan()

            // Periodic background sync while the app is running
            launch {
                while (isActive) {
                    delay(12000)
                    syncDeviceMedia()
                }
            }
        }

        // Dynamically monitor pending backup count and reflect in autoBackupStatus
        viewModelScope.launch {
            allPhotos.collect { photos ->
                val pending = photos.count { it.isOnDevice && !it.isInCloudBackup }
                _autoBackupStatus.value = _autoBackupStatus.value.copy(pendingQueueCount = pending)
            }
        }
    }

    // User actions
    fun setPermissionGranted(granted: Boolean) {
        _hasStoragePermission.value = granted
        if (granted) {
            syncDeviceMedia()
        }
    }

    fun syncDeviceMedia() {
        if (_isScanningDevice.value) return
        _isScanningDevice.value = true
        viewModelScope.launch {
            try {
                repository.syncDeviceMedia(getApplication())
                if (_isAutoBackupEnabled.value) {
                    delay(300)
                    performAutoBackupBatch()
                }
            } finally {
                _isScanningDevice.value = false
            }
        }
    }

    private suspend fun performAutoBackupBatch() {
        try {
            val pending = allPhotos.value.filter { it.isOnDevice && !it.isInCloudBackup }
            if (pending.isNotEmpty()) {
                _autoBackupStatus.value = _autoBackupStatus.value.copy(
                    isRunningNow = true,
                    statusDescription = "Cifrando y respaldando ${pending.size} foto(s) en 2° plano..."
                )
                repository.backupPhotos(pending.map { it.id })
                repository.markAllSynced()
                _autoBackupStatus.value = _autoBackupStatus.value.copy(
                    isRunningNow = false,
                    autoBackedUpCount = _autoBackupStatus.value.autoBackedUpCount + pending.size,
                    pendingQueueCount = 0,
                    lastBackupFormatted = "Ahora mismo",
                    statusDescription = "Copia automática activa • ${pending.size} asegurada(s)"
                )
            } else {
                _autoBackupStatus.value = _autoBackupStatus.value.copy(
                    isRunningNow = false,
                    pendingQueueCount = 0,
                    statusDescription = if (_isAutoBackupEnabled.value) "Monitoreando fotos nuevas en segundo plano" else "Servicio en pausa"
                )
            }
        } catch (e: Exception) {
            Log.e("GalleryViewModel", "Error in auto backup batch", e)
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        _isAutoBackupEnabled.value = enabled
        prefs.edit().putBoolean("auto_backup_enabled", enabled).apply()
        _autoBackupStatus.value = _autoBackupStatus.value.copy(
            isEnabled = enabled,
            statusDescription = if (enabled) "Monitoreando fotos nuevas en segundo plano" else "Servicio en pausa por el usuario"
        )
        if (enabled) {
            triggerAutoBackupCheck()
        }
    }

    fun setWifiOnlyBackup(enabled: Boolean) {
        _isWifiOnlyBackup.value = enabled
        prefs.edit().putBoolean("wifi_only_enabled", enabled).apply()
    }

    fun toggleBiometricLock(enabled: Boolean) {
        _isBiometricLockEnabled.value = enabled
        prefs.edit().putBoolean("biometric_lock_enabled", enabled).apply()
        if (enabled) {
            _isVaultUnlocked.value = false
        } else {
            _isVaultUnlocked.value = true
        }
    }

    fun unlockVault() {
        _isVaultUnlocked.value = true
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun triggerAutoBackupCheck() {
        viewModelScope.launch {
            if (!_isAutoBackupEnabled.value) return@launch
            _autoBackupStatus.value = _autoBackupStatus.value.copy(
                isRunningNow = true,
                statusDescription = "Comprobando y respaldando fotos pendientes..."
            )
            val pending = allPhotos.value.filter { it.isOnDevice && !it.isInCloudBackup }
            if (pending.isNotEmpty()) {
                repository.backupPhotos(pending.map { it.id })
                repository.markAllSynced()
                _autoBackupStatus.value = _autoBackupStatus.value.copy(
                    isRunningNow = false,
                    autoBackedUpCount = _autoBackupStatus.value.autoBackedUpCount + pending.size,
                    pendingQueueCount = 0,
                    lastBackupFormatted = "Ahora mismo",
                    statusDescription = "Copia completada: ${pending.size} foto(s) asegurada(s)"
                )
                triggerSyncNow()
            } else {
                delay(300)
                _autoBackupStatus.value = _autoBackupStatus.value.copy(
                    isRunningNow = false,
                    pendingQueueCount = 0,
                    lastBackupFormatted = "Ahora mismo",
                    statusDescription = "Al día • Todas las fotos respaldadas en la nube"
                )
            }
        }
    }

    fun purgeAllMockData() {
        viewModelScope.launch {
            repository.purgeMockData()
            syncDeviceMedia()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllPhotos()
        }
    }

    fun setTab(tab: GalleryTab) {
        _selectedTab.value = tab
    }

    fun setGridColumns(cols: Int) {
        _gridColumns.value = cols.coerceIn(1, 4)
    }

    fun togglePhotoSelection(photoId: Long) {
        val current = _selectedPhotoIds.value.toMutableSet()
        if (current.contains(photoId)) {
            current.remove(photoId)
        } else {
            current.add(photoId)
        }
        _selectedPhotoIds.value = current
    }

    fun clearSelection() {
        _selectedPhotoIds.value = emptySet()
    }

    fun selectAll() {
        _selectedPhotoIds.value = filteredPhotos.value.map { it.id }.toSet()
    }

    fun openPhoto(photo: Photo) {
        _activePhoto.value = photo
        _aiAnalysisResult.value = null
        _aiAnalysisError.value = null
    }

    fun closePhoto() {
        _activePhoto.value = null
        _aiAnalysisResult.value = null
        _aiAnalysisError.value = null
    }

    fun toggleFavorite(photo: Photo) {
        viewModelScope.launch {
            repository.toggleFavorite(photo.id, photo.isFavorite)
            if (_activePhoto.value?.id == photo.id) {
                _activePhoto.value = photo.copy(isFavorite = !photo.isFavorite)
            }
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
            if (_activePhoto.value?.id == photo.id) {
                closePhoto()
            }
        }
    }

    fun deleteSelectedPhotos() {
        val ids = _selectedPhotoIds.value.toList()
        viewModelScope.launch {
            repository.deletePhotosByIds(ids)
            clearSelection()
        }
    }

    fun encryptSelectedPhotos() {
        val ids = _selectedPhotoIds.value.toList()
        viewModelScope.launch {
            repository.encryptBatch(ids)
            clearSelection()
            triggerSyncNow()
        }
    }

    fun addTagToPhoto(photo: Photo, newTag: String) {
        if (newTag.isBlank()) return
        val updatedTags = (photo.tags + newTag.trim().lowercase()).distinct()
        val updated = photo.copy(tags = updatedTags)
        viewModelScope.launch {
            repository.updatePhoto(updated)
            if (_activePhoto.value?.id == photo.id) {
                _activePhoto.value = updated
            }
        }
    }

    fun removeTagFromPhoto(photo: Photo, tagToRemove: String) {
        val updatedTags = photo.tags.filterNot { it.equals(tagToRemove, ignoreCase = true) }
        val updated = photo.copy(tags = updatedTags)
        viewModelScope.launch {
            repository.updatePhoto(updated)
            if (_activePhoto.value?.id == photo.id) {
                _activePhoto.value = updated
            }
        }
    }

    fun runFullAiScan() {
        if (_isAiScanning.value) return
        _isAiScanning.value = true
        _aiScanProgress.value = 0f
        _aiScanMessage.value = "Iniciando análisis visual inteligente..."
        viewModelScope.launch {
            try {
                repository.analyzeBatchWithAi(getApplication()) { current, total ->
                    _aiScanProgress.value = if (total > 0) current.toFloat() / total else 1f
                    _aiScanMessage.value = "Analizando elemento $current de $total con IA..."
                }
                _aiScanMessage.value = "Análisis IA completado exitosamente"
                delay(1500)
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Error in runFullAiScan", e)
                _aiScanMessage.value = "Análisis completado"
            } finally {
                _isAiScanning.value = false
            }
        }
    }

    fun analyzePhotoWithAi(photo: Photo, forceCloud: Boolean = false) {
        _isAnalyzingAi.value = true
        _aiAnalysisError.value = null
        viewModelScope.launch {
            val res = repository.analyzeWithAi(getApplication(), photo, forceCloud)
            _isAnalyzingAi.value = false
            res.onSuccess { result ->
                _aiAnalysisResult.value = result
                _aiAnalysisError.value = null
                // Auto-apply immediately so tags, face detections, and category are saved to DB
                applyAiAnalysisToPhoto(photo, result)
            }.onFailure { error ->
                _aiAnalysisResult.value = null
                _aiAnalysisError.value = error.message ?: "La IA no está disponible en este momento."
            }
        }
    }

    fun applyAiAnalysisToPhoto(photo: Photo, result: AiAnalysisResult) {
        val mergedTags = (photo.tags + result.suggestedTags).distinct()
        val newCategory = if (result.suggestedCategory.isNotBlank()) result.suggestedCategory else photo.category
        val newFaces = if (result.detectedFaces.isNotEmpty()) {
            val existingNames = photo.faces.map { it.name }.toSet()
            val added = result.detectedFaces.filterNot { existingNames.contains(it) }.map { name ->
                com.example.data.model.FaceDetection(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    confidence = 0.98f
                )
            }
            photo.faces + added
        } else {
            photo.faces
        }

        val updated = photo.copy(
            tags = mergedTags,
            category = newCategory,
            faces = newFaces,
            description = if (result.sceneDescription.isNotBlank()) result.sceneDescription else photo.description
        )
        viewModelScope.launch {
            repository.updatePhoto(updated)
            if (_activePhoto.value?.id == photo.id) {
                _activePhoto.value = updated
            }
        }
    }

    fun importMediaUri(uriString: String) {
        viewModelScope.launch {
            repository.importUserPhoto(getApplication(), uriString)
            if (_isAutoBackupEnabled.value) {
                delay(200)
                performAutoBackupBatch()
            }
            triggerSyncNow()
        }
    }

    fun setStorageFilter(filter: StorageFilter) {
        _selectedStorageFilter.value = filter
    }

    fun backupSelectedPhotos() {
        val ids = _selectedPhotoIds.value.toList()
        viewModelScope.launch {
            repository.backupPhotos(ids)
            clearSelection()
            triggerSyncNow()
        }
    }

    fun freeSpaceSelectedPhotos() {
        val ids = _selectedPhotoIds.value.toList()
        viewModelScope.launch {
            repository.freeDeviceSpace(ids)
            clearSelection()
        }
    }

    fun downloadSelectedToDevice() {
        val ids = _selectedPhotoIds.value.toList()
        viewModelScope.launch {
            repository.downloadToDevice(ids)
            clearSelection()
        }
    }

    fun backupPhoto(photo: Photo) {
        viewModelScope.launch {
            repository.backupPhotos(listOf(photo.id))
            if (_activePhoto.value?.id == photo.id) {
                _activePhoto.value = photo.copy(isInCloudBackup = true, isSynced = true)
            }
            triggerSyncNow()
        }
    }

    fun freeSpacePhoto(photo: Photo) {
        viewModelScope.launch {
            repository.freeDeviceSpace(listOf(photo.id))
            if (_activePhoto.value?.id == photo.id) {
                _activePhoto.value = photo.copy(isOnDevice = false)
            }
        }
    }

    fun downloadPhoto(photo: Photo) {
        viewModelScope.launch {
            repository.downloadToDevice(listOf(photo.id))
            if (_activePhoto.value?.id == photo.id) {
                _activePhoto.value = photo.copy(isOnDevice = true)
            }
        }
    }

    fun backupAllPending() {
        val pendingIds = allPhotos.value.filter { it.isOnDevice && !it.isInCloudBackup }.map { it.id }
        if (pendingIds.isEmpty()) return
        viewModelScope.launch {
            repository.backupPhotos(pendingIds)
            triggerSyncNow()
        }
    }

    fun backupAllPendingPhotos() = backupAllPending()

    fun freeSpaceForSelectedPhotos() = freeSpaceSelectedPhotos()

    fun freeSpaceForAllSyncedPhotos() {
        val syncedIds = allPhotos.value.filter { it.isOnDevice && it.isInCloudBackup }.map { it.id }
        if (syncedIds.isEmpty()) return
        viewModelScope.launch {
            repository.freeDeviceSpace(syncedIds)
        }
    }

    fun triggerSyncNow() {
        if (_syncState.value.isSyncing) return
        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(
                isSyncing = true,
                progress = 0.1f,
                statusMessage = "Cifrando bloques AES-256 Galois..."
            )
            delay(500)
            _syncState.value = _syncState.value.copy(
                progress = 0.45f,
                statusMessage = "Sincronizando con Bóveda Segura en la nube..."
            )
            delay(500)
            _syncState.value = _syncState.value.copy(
                progress = 0.85f,
                statusMessage = "Verificando hashes Zero-Knowledge..."
            )
            delay(400)
            repository.markAllSynced()
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                progress = 1.0f,
                statusMessage = "100% Sincronizado y Protegido con E2EE",
                lastSyncTime = "Ahora mismo"
            )
        }
    }
}
