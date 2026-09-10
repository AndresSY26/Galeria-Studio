package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.local.DeviceMediaScanner
import com.example.data.local.PhotoDao
import com.example.data.model.CloudDevice
import com.example.data.model.FaceDetection
import com.example.data.model.Photo
import com.example.data.remote.AiAnalysisResult
import com.example.data.remote.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PhotoRepository(
    private val photoDao: PhotoDao,
    private val geminiService: GeminiService = GeminiService()
) {
    val allPhotos: Flow<List<Photo>> = photoDao.getAllPhotos()
    val favoritePhotos: Flow<List<Photo>> = photoDao.getFavoritePhotos()
    val encryptedPhotos: Flow<List<Photo>> = photoDao.getEncryptedPhotos()

    suspend fun purgeMockData() = withContext(Dispatchers.IO) {
        try {
            photoDao.purgeMockData()
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error purging mock data", e)
        }
    }

    suspend fun syncDeviceMedia(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            // First purge any mock / placeholder data
            photoDao.purgeMockData()

            val scanner = DeviceMediaScanner(context)
            val scannedItems = scanner.scanDeviceMedia()

            if (scannedItems.isNotEmpty()) {
                val scannedUris = scannedItems.map { it.uriString }.toSet()
                for (item in scannedItems) {
                    val existing = photoDao.getPhotoByUri(item.uriString)
                    if (existing == null) {
                        photoDao.insertPhoto(item)
                    } else {
                        // Merge tags and faces to preserve user additions while applying smart tags
                        val mergedTags = (existing.tags + item.tags).distinct()
                        val existingFaceNames = existing.faces.map { it.name }.toSet()
                        val mergedFaces = existing.faces + item.faces.filterNot { existingFaceNames.contains(it.name) }
                        val category = if (existing.category == "Importadas" || existing.category.isBlank() || existing.category == "Dispositivo") item.category else existing.category

                        val updated = existing.copy(
                            title = item.title,
                            fileSizeKb = item.fileSizeKb,
                            width = item.width,
                            height = item.height,
                            category = category,
                            tags = mergedTags,
                            faces = mergedFaces,
                            isOnDevice = true,
                            isVideo = item.isVideo,
                            durationFormatted = item.durationFormatted
                        )
                        photoDao.updatePhoto(updated)
                    }
                }

                // Clean up or mark not-on-device for items deleted externally
                val allExisting = photoDao.getAllPhotosDirect()
                for (stored in allExisting) {
                    if (stored.isOnDevice && !scannedUris.contains(stored.uriString) && !stored.uriString.startsWith("content://com.android.providers.media.documents/document")) {
                        if (stored.isInCloudBackup) {
                            photoDao.updatePhoto(stored.copy(isOnDevice = false))
                        } else {
                            photoDao.deletePhoto(stored)
                        }
                    }
                }
            }
            scannedItems.size
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error syncing device media", e)
            0
        }
    }

    suspend fun insertPhoto(photo: Photo): Long = withContext(Dispatchers.IO) {
        photoDao.insertPhoto(photo)
    }

    suspend fun updatePhoto(photo: Photo) = withContext(Dispatchers.IO) {
        photoDao.updatePhoto(photo)
    }

    suspend fun deletePhoto(photo: Photo) = withContext(Dispatchers.IO) {
        photoDao.deletePhoto(photo)
    }

    suspend fun deletePhotosByIds(ids: List<Long>) = withContext(Dispatchers.IO) {
        photoDao.deletePhotosByIds(ids)
    }

    suspend fun clearAllPhotos() = withContext(Dispatchers.IO) {
        photoDao.deleteAll()
    }

    suspend fun toggleFavorite(id: Long, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        photoDao.updateFavoriteStatus(id, !currentStatus)
    }

    suspend fun markAllSynced() = withContext(Dispatchers.IO) {
        photoDao.markAllAsSynced()
    }

    suspend fun backupPhotos(ids: List<Long>) = withContext(Dispatchers.IO) {
        photoDao.backupPhotos(ids)
    }

    suspend fun freeDeviceSpace(ids: List<Long>) = withContext(Dispatchers.IO) {
        photoDao.freeDeviceSpace(ids)
    }

    suspend fun downloadToDevice(ids: List<Long>) = withContext(Dispatchers.IO) {
        photoDao.downloadToDevice(ids)
    }

    suspend fun encryptBatch(ids: List<Long>) = withContext(Dispatchers.IO) {
        val hash = "AES256-GCM:" + UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
        photoDao.encryptPhotos(ids, hash)
    }

    suspend fun analyzeWithAi(
        context: Context,
        photo: Photo,
        forceCloud: Boolean = false
    ): Result<AiAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            val base64 = com.example.util.ImageUtils.getBase64FromUri(context, photo.uriString, maxDimension = 512)
            val geminiResult = geminiService.analyzeImageWithAi(
                title = photo.title,
                category = photo.category,
                currentTags = photo.tags,
                base64Jpeg = base64
            )

            if (geminiResult.isFailure) {
                val error = geminiResult.exceptionOrNull() ?: Exception("La IA no está disponible")
                Log.w("PhotoRepository", "AI Analysis failed: ${error.message}. Photo left untouched.")
                return@withContext Result.failure(error)
            }

            val result = geminiResult.getOrThrow()
            // Only update when the AI actually succeeded and returned valid metadata
            val mergedTags = (photo.tags + result.suggestedTags).distinct()
            val existingFaceNames = photo.faces.map { it.name }.toSet()
            val newFaces = result.detectedFaces.filterNot { existingFaceNames.contains(it) }.map {
                FaceDetection(
                    id = UUID.randomUUID().toString(),
                    name = it,
                    confidence = 0.98f
                )
            }
            val updatedFaces = photo.faces + newFaces
            val cat = if (result.suggestedCategory.isNotBlank()) {
                result.suggestedCategory
            } else {
                photo.category
            }
            val updated = photo.copy(
                tags = mergedTags,
                faces = updatedFaces,
                category = cat,
                description = if (result.sceneDescription.isNotBlank()) result.sceneDescription else photo.description
            )
            photoDao.updatePhoto(updated)
            Result.success(result)
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error analyzing photo with AI: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun analyzeBatchWithAi(
        context: Context,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val photos = photoDao.getAllPhotosDirect()
        var analyzedCount = 0
        for ((index, photo) in photos.withIndex()) {
            try {
                val base64 = com.example.util.ImageUtils.getBase64FromUri(context, photo.uriString, maxDimension = 512)
                val geminiResult = geminiService.analyzeImageWithAi(
                    title = photo.title,
                    category = photo.category,
                    currentTags = photo.tags,
                    base64Jpeg = base64
                )

                if (geminiResult.isSuccess) {
                    val result = geminiResult.getOrNull()
                    if (result != null && result.suggestedTags.isNotEmpty()) {
                        val mergedTags = (photo.tags + result.suggestedTags).distinct()
                        val existingFaceNames = photo.faces.map { it.name }.toSet()
                        val newFaces = result.detectedFaces.filterNot { existingFaceNames.contains(it) }.map {
                            FaceDetection(
                                id = UUID.randomUUID().toString(),
                                name = it,
                                confidence = 0.98f
                            )
                        }
                        val updatedFaces = photo.faces + newFaces
                        val cat = if (result.suggestedCategory.isNotBlank()) {
                            result.suggestedCategory
                        } else {
                            photo.category
                        }
                        val updated = photo.copy(
                            tags = mergedTags,
                            faces = updatedFaces,
                            category = cat,
                            description = if (result.sceneDescription.isNotBlank()) result.sceneDescription else photo.description
                        )
                        photoDao.updatePhoto(updated)
                        analyzedCount++
                    }
                }
                onProgress(index + 1, photos.size)
            } catch (e: Exception) {
                Log.w("PhotoRepository", "Error during batch AI analysis for ${photo.id}: ${e.message}")
            }
        }
        analyzedCount
    }

    suspend fun importUserPhoto(
        context: Context,
        uriString: String,
        suggestedCategory: String = "Importadas"
    ): Photo = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
        val dateDisplay = dateFormat.format(Date(now))

        var fileName = "Foto importada " + SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now))
        var fileSizeKb = 2500L
        var isVideo = false

        try {
            val uri = Uri.parse(uriString)
            val mimeType = context.contentResolver.getType(uri) ?: ""
            if (mimeType.startsWith("video")) {
                isVideo = true
            }

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) {
                        fileName = cursor.getString(nameIdx) ?: fileName
                    }
                    if (sizeIdx != -1) {
                        val sizeBytes = cursor.getLong(sizeIdx)
                        if (sizeBytes > 0) fileSizeKb = sizeBytes / 1024
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error resolving imported uri: $uriString", e)
        }

        val smartAnalysis = com.example.util.SmartMediaTagger.analyzeLocalMedia(
            fileName = fileName,
            bucketName = suggestedCategory,
            relativePath = uriString,
            isVideo = isVideo
        )

        val finalCategory = if (isVideo) "Videos" else if (smartAnalysis.suggestedCategory != "Dispositivo") smartAnalysis.suggestedCategory else suggestedCategory
        val locationName = when (smartAnalysis.platform) {
            com.example.util.MediaPlatform.WHATSAPP -> "WhatsApp"
            com.example.util.MediaPlatform.DOWNLOADS -> "Descargas"
            com.example.util.MediaPlatform.CAMERA -> "Cámara"
            com.example.util.MediaPlatform.SCREENSHOTS -> "Capturas de pantalla"
            com.example.util.MediaPlatform.TELEGRAM -> "Telegram"
            com.example.util.MediaPlatform.INSTAGRAM -> "Instagram"
            else -> "Almacenamiento del dispositivo"
        }

        val newPhoto = Photo(
            title = fileName,
            description = if (isVideo) "Video de $locationName" else "Elemento de $locationName",
            uriString = uriString,
            category = finalCategory,
            timestamp = now,
            dateDisplay = dateDisplay,
            tags = smartAnalysis.tags,
            faces = smartAnalysis.detectedFaces,
            isFavorite = false,
            isSynced = false,
            isOnDevice = true,
            isInCloudBackup = false,
            isEncrypted = false,
            encryptionHash = "",
            locationName = locationName,
            fileSizeKb = fileSizeKb,
            width = 1920,
            height = 1080,
            cameraModel = "${smartAnalysis.platform.badgeLabel} • $locationName",
            isVideo = isVideo,
            durationFormatted = if (isVideo) "Video" else ""
        )
        val id = photoDao.insertPhoto(newPhoto)
        newPhoto.copy(id = id)
    }

    fun getConnectedDevices(): List<CloudDevice> {
        return listOf(
            CloudDevice(
                id = "dev_1",
                name = "Dispositivo Android (Este teléfono)",
                deviceType = "Smartphone",
                lastSyncFormatted = "En tiempo real",
                isOnline = true,
                isCurrentDevice = true
            ),
            CloudDevice(
                id = "dev_2",
                name = "Bóveda Cifrada E2EE",
                deviceType = "Cloud Vault",
                lastSyncFormatted = "Sincronizado",
                isOnline = true,
                isCurrentDevice = false
            )
        )
    }
}
