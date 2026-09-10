package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Photo
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<Photo>>

    @Query("SELECT * FROM photos")
    suspend fun getAllPhotosDirect(): List<Photo>

    @Query("SELECT * FROM photos WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoritePhotos(): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE category = :category ORDER BY timestamp DESC")
    fun getPhotosByCategory(category: String): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): Photo?

    @Query("SELECT * FROM photos WHERE uriString = :uriString LIMIT 1")
    suspend fun getPhotoByUri(uriString: String): Photo?

    @Query("SELECT uriString FROM photos")
    suspend fun getAllPhotoUris(): List<String>

    @Query("DELETE FROM photos WHERE uriString LIKE 'http%' OR uriString LIKE '%unsplash%'")
    suspend fun purgeMockData()

    @Query("DELETE FROM photos")
    suspend fun deleteAll()

    @Query("SELECT * FROM photos WHERE isEncrypted = 1 ORDER BY timestamp DESC")
    fun getEncryptedPhotos(): Flow<List<Photo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: Photo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<Photo>)

    @Update
    suspend fun updatePhoto(photo: Photo)

    @Delete
    suspend fun deletePhoto(photo: Photo)

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun deletePhotosByIds(ids: List<Long>)

    @Query("UPDATE photos SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE photos SET isSynced = 1 WHERE isSynced = 0")
    suspend fun markAllAsSynced()

    @Query("UPDATE photos SET isInCloudBackup = 1, isSynced = 1 WHERE id IN (:ids)")
    suspend fun backupPhotos(ids: List<Long>)

    @Query("UPDATE photos SET isOnDevice = 0 WHERE id IN (:ids) AND isInCloudBackup = 1")
    suspend fun freeDeviceSpace(ids: List<Long>)

    @Query("UPDATE photos SET isOnDevice = 1 WHERE id IN (:ids)")
    suspend fun downloadToDevice(ids: List<Long>)

    @Query("UPDATE photos SET isEncrypted = 1, encryptionHash = :hash WHERE id IN (:ids)")
    suspend fun encryptPhotos(ids: List<Long>, hash: String)

    @Query("SELECT COUNT(*) FROM photos")
    suspend fun getPhotoCount(): Int
}
