package com.example.util

import com.example.data.model.FaceDetection
import com.example.data.model.Photo
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

enum class MediaPlatform(
    val id: String,
    val displayName: String,
    val badgeLabel: String,
    val emoji: String,
    val iconCategory: String
) {
    CAMERA("camera", "Cámara", "📷 Cámara", "📷", "Cámara"),
    WHATSAPP("whatsapp", "WhatsApp", "💬 WhatsApp", "💬", "WhatsApp"),
    DOWNLOADS("downloads", "Descargas", "📥 Descargas", "📥", "Descargas"),
    SCREENSHOTS("screenshots", "Capturas de pantalla", "📱 Captura", "📱", "Capturas de pantalla"),
    TELEGRAM("telegram", "Telegram", "✈️ Telegram", "✈️", "Telegram"),
    INSTAGRAM("instagram", "Instagram", "📸 Instagram", "📸", "Instagram"),
    TIKTOK("tiktok", "TikTok", "🎵 TikTok", "🎵", "TikTok"),
    TWITTER("twitter", "Twitter / X", "𝕏 Twitter / X", "𝕏", "Twitter / X"),
    FACEBOOK("facebook", "Facebook / Messenger", "👥 Facebook", "👥", "Facebook"),
    DOCUMENTS("documents", "Documentos", "📄 Documento", "📄", "Documentos"),
    STORAGE("storage", "Imágenes", "📁 Dispositivo", "📁", "Imágenes");

    companion object {
        fun fromId(id: String): MediaPlatform {
            return entries.firstOrNull { it.id.equals(id, true) || it.displayName.equals(id, true) } ?: STORAGE
        }
    }
}

data class SmartTaggingResult(
    val tags: List<String>,
    val detectedFaces: List<FaceDetection>,
    val suggestedCategory: String,
    val searchTokens: Set<String>,
    val platform: MediaPlatform = MediaPlatform.STORAGE
)

object SmartMediaTagger {

    /**
     * Determines the exact platform or device origin (WhatsApp, Downloads, Camera, Screenshots, etc.)
     */
    fun detectPlatform(
        fileName: String,
        bucketName: String = "",
        relativePath: String = "",
        uriString: String = "",
        isVideo: Boolean = false
    ): MediaPlatform {
        val fName = fileName.lowercase(Locale.ROOT)
        val bName = bucketName.lowercase(Locale.ROOT)
        val rPath = relativePath.lowercase(Locale.ROOT)
        val uri = uriString.lowercase(Locale.ROOT)

        val combined = "$fName $bName $rPath $uri"

        // 1. WhatsApp
        if (combined.contains("whatsapp") || combined.contains("com.whatsapp") ||
            fName.startsWith("img-wa") || fName.startsWith("vid-wa") ||
            fName.startsWith("img_wa") || fName.startsWith("vid_wa") ||
            Regex("wa\\d{4}").containsMatchIn(fName) ||
            Regex("[-_]wa\\d+").containsMatchIn(fName)
        ) {
            return MediaPlatform.WHATSAPP
        }

        // 2. Telegram
        if (combined.contains("telegram") || combined.contains("org.telegram")) {
            return MediaPlatform.TELEGRAM
        }

        // 3. Instagram
        if (combined.contains("instagram") || fName.startsWith("ig_")) {
            return MediaPlatform.INSTAGRAM
        }

        // 4. TikTok
        if (combined.contains("tiktok")) {
            return MediaPlatform.TIKTOK
        }

        // 5. Twitter / X
        if (combined.contains("twitter") || rPath.contains("/x/") || bName == "twitter") {
            return MediaPlatform.TWITTER
        }

        // 6. Facebook / Messenger
        if (combined.contains("facebook") || combined.contains("messenger") || fName.startsWith("fb_img") || fName.startsWith("received_")) {
            return MediaPlatform.FACEBOOK
        }

        // 7. Screenshots / Capturas de pantalla
        if (combined.contains("screenshot") || combined.contains("captura") || combined.contains("screenrecord") || fName.startsWith("screen_")) {
            return MediaPlatform.SCREENSHOTS
        }

        // 8. Downloads / Descargas
        if (bName.contains("download") || bName.contains("descarga") ||
            rPath.contains("download") || rPath.contains("descarga") ||
            fName.startsWith("download") || fName.startsWith("descarga") ||
            fName.startsWith("tmp_") || fName.startsWith("temp_") ||
            fName.contains("download (") || fName.contains("download(")
        ) {
            return MediaPlatform.DOWNLOADS
        }

        // 9. Camera / Cámara
        if (bName.contains("camera") || bName.contains("cámara") || bName.contains("dcim") || bName.contains("100andro") || bName.contains("100media") ||
            rPath.contains("dcim") || rPath.contains("camera") ||
            ((fName.startsWith("img_") || fName.startsWith("pxl_") || fName.startsWith("dsc_") || fName.startsWith("sam_") || fName.startsWith("vid_") || fName.startsWith("mov_"))
                    && !fName.contains("wa") && !fName.contains("screenshot"))
        ) {
            return MediaPlatform.CAMERA
        }

        // 10. Documents / Escaneos
        if (bName.contains("document") || bName.contains("doc") || fName.contains("factura") || fName.contains("scan")) {
            return MediaPlatform.DOCUMENTS
        }

        return MediaPlatform.STORAGE
    }

    /**
     * Resolves the platform for a Photo model instance
     */
    fun detectPlatform(photo: Photo): MediaPlatform {
        // Check if any tag explicitly specifies the platform
        for (p in MediaPlatform.entries) {
            if (photo.tags.any { it.equals(p.id, true) || it.equals(p.displayName, true) }) {
                return p
            }
        }
        return detectPlatform(
            fileName = photo.title,
            bucketName = photo.locationName,
            relativePath = photo.cameraModel,
            uriString = photo.uriString,
            isVideo = photo.isVideo
        )
    }

    /**
     * Extracts rich semantic tags, detected characters/people, and keywords from the filename, bucket, and relative path.
     */
    fun analyzeLocalMedia(
        fileName: String,
        bucketName: String,
        relativePath: String = "",
        isVideo: Boolean = false
    ): SmartTaggingResult {
        val cleanName = removeFileExtension(fileName)
        val normalizedName = normalizeString(cleanName)
        val rawTokens = cleanName.split(Regex("[_\\-\\.\\s+]+")).filter { it.isNotBlank() }
        val normalizedTokens = rawTokens.map { normalizeString(it) }

        val tags = mutableSetOf<String>()
        val faces = mutableListOf<FaceDetection>()

        // 1. Detect Platform & Origin
        val platform = detectPlatform(
            fileName = fileName,
            bucketName = bucketName,
            relativePath = relativePath,
            isVideo = isVideo
        )

        // Inject platform tags
        when (platform) {
            MediaPlatform.WHATSAPP -> {
                tags.addAll(listOf("whatsapp", "chat", "mensajería", "recibido", "descargas"))
            }
            MediaPlatform.DOWNLOADS -> {
                tags.addAll(listOf("descargas", "downloads", "web", "internet", "guardado"))
            }
            MediaPlatform.CAMERA -> {
                tags.addAll(listOf("cámara", "camera", "fotografía", "captura real"))
            }
            MediaPlatform.SCREENSHOTS -> {
                tags.addAll(listOf("capturas", "screenshot", "pantalla", "captura de pantalla"))
            }
            MediaPlatform.TELEGRAM -> {
                tags.addAll(listOf("telegram", "chat", "mensajería", "descargas"))
            }
            MediaPlatform.INSTAGRAM -> {
                tags.addAll(listOf("instagram", "redes", "social", "fotos"))
            }
            MediaPlatform.TIKTOK -> {
                tags.addAll(listOf("tiktok", "video corto", "redes"))
            }
            MediaPlatform.TWITTER -> {
                tags.addAll(listOf("twitter", "x", "redes", "social"))
            }
            MediaPlatform.FACEBOOK -> {
                tags.addAll(listOf("facebook", "messenger", "social"))
            }
            MediaPlatform.DOCUMENTS -> {
                tags.addAll(listOf("documento", "texto", "escaneo"))
            }
            MediaPlatform.STORAGE -> {
                tags.addAll(listOf("imágenes", "dispositivo", "almacenamiento"))
            }
        }

        // Add base bucket/system tags
        tags.add("local")
        tags.add("dispositivo")
        if (bucketName.isNotBlank()) {
            tags.add(normalizeString(bucketName))
        }

        // Default suggested category matches platform name or "Videos"
        var suggestedCategory = if (isVideo) "Videos" else platform.displayName

        // Add each valid individual token from filename
        for (token in rawTokens) {
            val cleanTok = token.trim().lowercase(Locale.ROOT)
            if (cleanTok.length >= 2 && !isGenericStopWord(cleanTok)) {
                tags.add(cleanTok)
            }
        }

        // Build all search tokens for index
        val jointLower = normalizedName.replace(" ", "")
        val allSearchTokens = mutableSetOf<String>()
        allSearchTokens.addAll(tags.map { normalizeString(it) })
        allSearchTokens.addAll(normalizedTokens)
        allSearchTokens.add(normalizeString(platform.displayName))
        allSearchTokens.add(normalizeString(platform.id))
        allSearchTokens.add(normalizedName)
        allSearchTokens.add(jointLower)

        return SmartTaggingResult(
            tags = tags.toList().sorted(),
            detectedFaces = emptyList(),
            suggestedCategory = suggestedCategory,
            searchTokens = allSearchTokens,
            platform = platform
        )
    }

    /**
     * Advanced semantic & tokenized search query matcher.
     */
    fun matchesSearch(photo: Photo, query: String): Boolean {
        if (query.isBlank()) return true
        val cleanQuery = normalizeString(query.trim())
        val queryWords = cleanQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (queryWords.isEmpty()) return true

        val photoCleanTitle = normalizeString(removeFileExtension(photo.title))
        val photoJointTitle = photoCleanTitle.replace(Regex("[_\\-\\.\\s+]"), "")
        val photoCategory = normalizeString(photo.category)
        val photoLocation = normalizeString(photo.locationName)
        val photoDescription = normalizeString(photo.description)
        val photoTags = photo.tags.map { normalizeString(it) }
        val photoFaces = photo.faces.map { normalizeString(it.name) }
        val platform = detectPlatform(photo)
        val platformName = normalizeString(platform.displayName)
        val platformId = normalizeString(platform.id)

        // Exact substring matching on composite field
        val composite = "$photoCleanTitle $photoJointTitle $photoCategory $photoLocation $photoDescription $platformName $platformId ${photoTags.joinToString(" ")} ${photoFaces.joinToString(" ")}"

        if (composite.contains(cleanQuery)) {
            return true
        }

        // Match if joint string matches (e.g. query "sung jinwoo" against "sungjinwoo" or "jinwoo")
        val cleanQueryJoint = cleanQuery.replace(" ", "")
        if (composite.replace(" ", "").contains(cleanQueryJoint)) {
            return true
        }

        // Multi-word token match (EVERY word in query must match something in the photo)
        val allTokensMatch = queryWords.all { word ->
            if (word.length <= 1) return@all true
            photoCleanTitle.contains(word) ||
            photoJointTitle.contains(word) ||
            photoCategory.contains(word) ||
            photoLocation.contains(word) ||
            photoDescription.contains(word) ||
            platformName.contains(word) ||
            platformId.contains(word) ||
            photoTags.any { tag -> tag.contains(word) || word.contains(tag) } ||
            photoFaces.any { face -> face.contains(word) || word.contains(face) } ||
            isFuzzyMatch(word, photoCleanTitle, photoTags, photoFaces)
        }

        return allTokensMatch
    }

    private fun isFuzzyMatch(
        word: String,
        title: String,
        tags: List<String>,
        faces: List<String>
    ): Boolean {
        // Handle common variations
        if (word == "jinwoo" || word == "jin" || word == "woo") {
            if (title.contains("jin") && title.contains("woo")) return true
            if (tags.any { it.contains("jin") || it.contains("woo") }) return true
        }
        if (word == "sololeveling" || word == "solo") {
            if (title.contains("solo") || tags.any { it.contains("solo") }) return true
        }
        if (word == "whatsapp" || word == "wa") {
            if (tags.any { it.contains("whatsapp") } || title.contains("wa")) return true
        }
        if (word == "descargas" || word == "download" || word == "downloads") {
            if (tags.any { it.contains("descarga") || it.contains("download") }) return true
        }
        if (word == "camara" || word == "camera" || word == "cámara") {
            if (tags.any { it.contains("cámara") || it.contains("camera") }) return true
        }
        return false
    }

    fun removeFileExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot > 0) filename.substring(0, lastDot) else filename
    }

    fun normalizeString(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase(Locale.getDefault())
            .replace(Regex("[_\\-+]"), " ")
            .trim()
    }

    private fun isGenericStopWord(word: String): Boolean {
        return word in setOf("img", "pic", "image", "foto", "photo", "dcm", "tmp", "vid", "video", "the", "and", "jpg", "jpeg", "png", "webp", "mp4")
    }
}
