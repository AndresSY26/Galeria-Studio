package com.example.data.local

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.Photo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceMediaScanner(private val context: Context) {

    fun scanDeviceMedia(): List<Photo> {
        val mediaList = mutableListOf<Photo>()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))

        // 1. Query Device Images
        try {
            val imageProjection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.RELATIVE_PATH,
                    MediaStore.Images.Media.DATA
                )
            } else {
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.DATA
                )
            }

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                } else -1
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else -1
                val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Foto" else "Foto"
                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else 0L
                    val dateMod = if (dateModCol != -1) cursor.getLong(dateModCol) else 0L
                    val rawDate = when {
                        dateAdded > 0 -> dateAdded * 1000L
                        dateMod > 0 -> dateMod * 1000L
                        else -> System.currentTimeMillis()
                    }
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 1920
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 1080
                    val bucketName = if (bucketCol != -1) cursor.getString(bucketCol) ?: "" else ""
                    val relPath = if (relPathCol != -1) cursor.getString(relPathCol) ?: "" else ""
                    val dataPath = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""

                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val smartAnalysis = com.example.util.SmartMediaTagger.analyzeLocalMedia(
                        fileName = name,
                        bucketName = bucketName.ifBlank { if (dataPath.isNotBlank()) java.io.File(dataPath).parentFile?.name ?: "Dispositivo" else "Dispositivo" },
                        relativePath = if (relPath.isNotBlank()) relPath else dataPath,
                        isVideo = false
                    )

                    val resolvedLocation = when {
                        smartAnalysis.platform == com.example.util.MediaPlatform.WHATSAPP -> "WhatsApp"
                        smartAnalysis.platform == com.example.util.MediaPlatform.DOWNLOADS -> "Descargas"
                        smartAnalysis.platform == com.example.util.MediaPlatform.CAMERA -> "Cámara"
                        smartAnalysis.platform == com.example.util.MediaPlatform.SCREENSHOTS -> "Capturas de pantalla"
                        smartAnalysis.platform == com.example.util.MediaPlatform.TELEGRAM -> "Telegram"
                        smartAnalysis.platform == com.example.util.MediaPlatform.INSTAGRAM -> "Instagram"
                        bucketName.isNotBlank() -> bucketName
                        else -> "Almacenamiento del dispositivo"
                    }

                    mediaList.add(
                        Photo(
                            id = 0,
                            title = name,
                            description = "Elemento de $resolvedLocation",
                            uriString = contentUri.toString(),
                            category = smartAnalysis.suggestedCategory,
                            timestamp = rawDate,
                            dateDisplay = dateFormat.format(Date(rawDate)),
                            tags = smartAnalysis.tags,
                            faces = smartAnalysis.detectedFaces,
                            isFavorite = false,
                            isSynced = false,
                            isOnDevice = true,
                            isInCloudBackup = false,
                            isEncrypted = false,
                            encryptionHash = "",
                            locationName = resolvedLocation,
                            fileSizeKb = if (size > 0) size / 1024 else 1800,
                            width = if (width > 0) width else 1920,
                            height = if (height > 0) height else 1080,
                            cameraModel = "${smartAnalysis.platform.badgeLabel} • $resolvedLocation",
                            isVideo = false,
                            durationFormatted = ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceMediaScanner", "Error scanning images: ${e.message}")
        }

        // 2. Query Device Videos
        try {
            val videoProjection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.DATE_MODIFIED,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.WIDTH,
                    MediaStore.Video.Media.HEIGHT,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Video.Media.RELATIVE_PATH,
                    MediaStore.Video.Media.DATA
                )
            } else {
                arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.DATE_MODIFIED,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.WIDTH,
                    MediaStore.Video.Media.HEIGHT,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.DATA
                )
            }

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                } else -1
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else -1
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Video" else "Video"
                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else 0L
                    val dateMod = if (dateModCol != -1) cursor.getLong(dateModCol) else 0L
                    val rawDate = when {
                        dateAdded > 0 -> dateAdded * 1000L
                        dateMod > 0 -> dateMod * 1000L
                        else -> System.currentTimeMillis()
                    }
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 1920
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 1080
                    val durationMs = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val bucketName = if (bucketCol != -1) cursor.getString(bucketCol) ?: "" else ""
                    val relPath = if (relPathCol != -1) cursor.getString(relPathCol) ?: "" else ""
                    val dataPath = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""

                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val durationFormatted = formatDuration(durationMs)
                    val smartAnalysis = com.example.util.SmartMediaTagger.analyzeLocalMedia(
                        fileName = name,
                        bucketName = bucketName.ifBlank { if (dataPath.isNotBlank()) java.io.File(dataPath).parentFile?.name ?: "Videos" else "Videos" },
                        relativePath = if (relPath.isNotBlank()) relPath else dataPath,
                        isVideo = true
                    )

                    val resolvedLocation = when {
                        smartAnalysis.platform == com.example.util.MediaPlatform.WHATSAPP -> "WhatsApp Video"
                        smartAnalysis.platform == com.example.util.MediaPlatform.DOWNLOADS -> "Descargas"
                        smartAnalysis.platform == com.example.util.MediaPlatform.CAMERA -> "Cámara (Video)"
                        smartAnalysis.platform == com.example.util.MediaPlatform.SCREENSHOTS -> "Grabación de pantalla"
                        smartAnalysis.platform == com.example.util.MediaPlatform.TELEGRAM -> "Telegram"
                        bucketName.isNotBlank() -> bucketName
                        else -> "Videos del dispositivo"
                    }

                    mediaList.add(
                        Photo(
                            id = 0,
                            title = name,
                            description = "Video de $resolvedLocation ($durationFormatted)",
                            uriString = contentUri.toString(),
                            category = "Videos",
                            timestamp = rawDate,
                            dateDisplay = dateFormat.format(Date(rawDate)),
                            tags = smartAnalysis.tags,
                            faces = smartAnalysis.detectedFaces,
                            isFavorite = false,
                            isSynced = false,
                            isOnDevice = true,
                            isInCloudBackup = false,
                            isEncrypted = false,
                            encryptionHash = "",
                            locationName = resolvedLocation,
                            fileSizeKb = if (size > 0) size / 1024 else 4500,
                            width = if (width > 0) width else 1920,
                            height = if (height > 0) height else 1080,
                            cameraModel = "${smartAnalysis.platform.badgeLabel} • $resolvedLocation",
                            isVideo = true,
                            durationFormatted = durationFormatted
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceMediaScanner", "Error scanning videos: ${e.message}")
        }

        return mediaList.sortedByDescending { it.timestamp }
    }

    private fun mapFolderToCategory(bucketName: String): String {
        val lower = bucketName.lowercase()
        return when {
            lower.contains("camera") || lower.contains("dcim") || lower.contains("cámara") -> "Cámara"
            lower.contains("screenshot") || lower.contains("captura") -> "Capturas de pantalla"
            lower.contains("download") || lower.contains("descarga") -> "Descargas"
            lower.contains("whatsapp") -> "WhatsApp"
            lower.contains("telegram") -> "Telegram"
            lower.contains("instagram") -> "Instagram"
            lower.contains("picture") || lower.contains("foto") || lower.contains("imagen") -> "Imágenes"
            lower.contains("document") || lower.contains("doc") -> "Documentos"
            else -> bucketName.ifBlank { "Dispositivo" }
        }
    }

    private fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "0:00"
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes >= 60) {
            val hours = minutes / 60
            val remMinutes = minutes % 60
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, remMinutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }
}
