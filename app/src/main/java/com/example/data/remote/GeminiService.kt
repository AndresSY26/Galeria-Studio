package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    @Json(name = "inline_data") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mime_type") val mimeType: String = "image/jpeg",
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequestBody(
    val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class AiAnalysisResult(
    val suggestedTags: List<String> = emptyList(),
    val detectedFaces: List<String> = emptyList(),
    val sceneDescription: String = "",
    val suggestedCategory: String = "",
    val aestheticScore: String = "9.4/10",
    val cameraLighting: String = "Luz natural difusa"
)

class GeminiService {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequestBody::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    suspend fun analyzeImageWithAi(
        title: String,
        category: String,
        currentTags: List<String>,
        base64Jpeg: String? = null
    ): Result<AiAnalysisResult> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // If no Gemini API key is configured or is default, do nothing and return failure
            return@withContext Result.failure(IllegalStateException("La clave de Gemini API no está configurada."))
        }

        try {
            val prompt = """
                Eres una IA experta en visión por computadora, fotografía, arte y cultura visual para una galería de imágenes.
                Analiza detalladamente esta imagen y genera un análisis exhaustivo en formato JSON estricto.

                Instrucciones de análisis:
                1. 'suggestedTags': Lista de 6 a 12 etiquetas descriptivas, específicas y reales que se aprecian en la imagen.
                2. 'detectedFaces': Nombres de personajes conocidos o personas identificables (o déjalo vacío si no hay rostros claros).
                3. 'sceneDescription': Una descripción concisa y fiel de lo que se observa en la imagen.
                4. 'suggestedCategory': Una de las categorías principales ("Anime / Arte", "Retratos", "Naturaleza", "Cine / Películas", "Videojuegos", "Viajes", "Capturas de pantalla", "Documentos", "Comida", "Mascotas", "Vehículos").
                5. 'aestheticScore': Puntuación estimada (ej: "9.5/10").
                6. 'cameraLighting': Tipo de iluminación o estilo apreciable (ej: "Luz natural difusa", "Iluminación de estudio").

                Información previa:
                Título de archivo: $title
                Categoría actual: $category
                Etiquetas existentes: ${currentTags.joinToString(", ")}

                Formato de salida JSON EXACTO requerido (sin markdown ```json ni texto adicional):
                {
                   "suggestedTags": ["etiqueta1", "etiqueta2"],
                   "detectedFaces": [],
                   "sceneDescription": "Descripción de la escena",
                   "suggestedCategory": "Naturaleza",
                   "aestheticScore": "9.5/10",
                   "cameraLighting": "Luz natural diurna"
                }
            """.trimIndent()

            val parts = mutableListOf<GeminiPart>()
            parts.add(GeminiPart(text = prompt))
            if (base64Jpeg != null && base64Jpeg.length > 50) {
                parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Jpeg)))
            }

            val requestBodyObj = GeminiRequestBody(
                contents = listOf(GeminiContent(parts = parts))
            )

            val jsonPayload = requestAdapter.toJson(requestBodyObj)
            val modelsToTry = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")
            var rawText: String? = null

            for (model in modelsToTry) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful && responseBody.isNotBlank()) {
                        val geminiResponse = responseAdapter.fromJson(responseBody)
                        val candidateText = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!candidateText.isNullOrBlank()) {
                            rawText = candidateText
                            break
                        }
                    } else {
                        Log.w("GeminiService", "Model $model returned ${response.code}: $responseBody")
                    }
                } catch (e: Exception) {
                    Log.w("GeminiService", "Attempt with $model failed: ${e.message}")
                }
            }

            if (rawText != null) {
                val cleanJson = rawText.replace("```json", "").replace("```", "").trim()
                val adapter = moshi.adapter(AiAnalysisResult::class.java)
                val result = adapter.fromJson(cleanJson)
                if (result != null && result.suggestedTags.isNotEmpty()) {
                    return@withContext Result.success(result)
                }
            }
            Result.failure(Exception("La IA no devolvió un análisis válido."))
        } catch (e: Exception) {
            Log.e("GeminiService", "Error calling Gemini", e)
            Result.failure(e)
        }
    }

    suspend fun processNaturalLanguageSearch(
        query: String,
        photosSummary: List<String>
    ): List<Int> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext emptyList()
        }

        try {
            val prompt = """
                Eres el motor de búsqueda semántica de una galería inteligente. El usuario busca: "$query".
                Aquí está el catálogo de fotos disponibles (ID | Título | Categoría | Etiquetas | Personas):
                ${photosSummary.joinToString("\n")}

                Devuelve únicamente una lista JSON con los IDs de las fotos que mejor coinciden conceptualmente con la búsqueda del usuario (por temática, personaje, ambiente, color o sinónimos).
                Ejemplo: [1, 4]
                Si no hay coincidencias devuelve [].
            """.trimIndent()

            val requestBodyObj = GeminiRequestBody(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
            )

            val jsonPayload = requestAdapter.toJson(requestBodyObj)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext emptyList()

            val geminiResponse = responseAdapter.fromJson(responseBody)
            val rawText = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = rawText.replace("```json", "").replace("```", "").trim()
            val match = Regex("""\d+""").findAll(cleanJson).map { it.value.toInt() }.toList()
            match
        } catch (e: Exception) {
            emptyList()
        }
    }
}
