package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FaceDetection(
    val id: String,
    val name: String,
    val confidence: Float = 0.95f,
    val boxX: Float = 0.3f,
    val boxY: Float = 0.2f,
    val boxWidth: Float = 0.4f,
    val boxHeight: Float = 0.4f
)

@JsonClass(generateAdapter = true)
data class PersonProfile(
    val id: String,
    val name: String,
    val samplePhotoUrl: String,
    val photoCount: Int,
    val relation: String
)

@JsonClass(generateAdapter = true)
data class CloudDevice(
    val id: String,
    val name: String,
    val deviceType: String, // "Smartphone", "Laptop", "Tablet", "Web"
    val lastSyncFormatted: String,
    val isOnline: Boolean,
    val isCurrentDevice: Boolean
)

@Entity(tableName = "photos")
@JsonClass(generateAdapter = true)
data class Photo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val uriString: String,
    val category: String, // "Naturaleza", "Viajes", "Retratos", "Arquitectura", "Eventos", "Documentos", "Familia", "Comida", "Mascotas"
    val timestamp: Long,
    val dateDisplay: String,
    val tags: List<String>,
    val faces: List<FaceDetection>,
    val isFavorite: Boolean = false,
    val isSynced: Boolean = true,
    val isOnDevice: Boolean = true,
    val isInCloudBackup: Boolean = true,
    val isEncrypted: Boolean = true,
    val encryptionHash: String = "",
    val locationName: String = "",
    val fileSizeKb: Long = 2450,
    val width: Int = 3840,
    val height: Int = 2160,
    val iso: String = "ISO 100",
    val aperture: String = "f/2.0",
    val shutterSpeed: String = "1/500s",
    val cameraModel: String = "Sony A7 IV • 35mm f/1.4 GM",
    val isVideo: Boolean = false,
    val durationFormatted: String = ""
) {
    val isStoredOnDevice: Boolean get() = isOnDevice
    val isBackedUp: Boolean get() = isInCloudBackup
    val isFullySynced: Boolean get() = isOnDevice && isInCloudBackup
    val isLocalOnly: Boolean get() = isOnDevice && !isInCloudBackup
    val isCloudOnly: Boolean get() = !isOnDevice && isInCloudBackup
}
