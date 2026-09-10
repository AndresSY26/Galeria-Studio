package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.FaceDetection
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    private val faceListType = Types.newParameterizedType(List::class.java, FaceDetection::class.java)
    private val faceListAdapter = moshi.adapter<List<FaceDetection>>(faceListType)

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return stringListAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            stringListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromFaceList(list: List<FaceDetection>?): String {
        return faceListAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun toFaceList(json: String?): List<FaceDetection> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            faceListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
