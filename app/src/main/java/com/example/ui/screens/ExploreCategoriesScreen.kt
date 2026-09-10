package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush as GBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PersonProfile
import com.example.data.model.Photo
import com.example.ui.components.PersonAvatarCard
import com.example.ui.theme.AuraAmber
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.AuraPrimary
import com.example.ui.theme.AuraRose
import com.example.util.MediaPlatform
import com.example.util.SmartMediaTagger

data class AlbumMeta(
    val name: String,
    val icon: ImageVector,
    val accentColor: Color,
    val coverUri: String?,
    val count: Int,
    val subtitle: String = ""
)

@Composable
fun ExploreCategoriesScreen(
    photos: List<Photo>,
    peopleProfiles: List<PersonProfile>,
    categoryCounts: Map<String, Int>,
    isAiScanning: Boolean = false,
    aiScanProgress: Float = 0f,
    onRunAiScan: () -> Unit = {},
    onSelectAlbum: (AlbumMeta) -> Unit,
    onSelectPerson: (PersonProfile) -> Unit,
    onSelectFavorites: () -> Unit,
    onSelectEncrypted: () -> Unit,
    onSelectLocation: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val favCount = photos.count { it.isFavorite }
    val vaultCount = photos.count { it.isEncrypted }
    val videoCount = photos.count { it.isVideo || it.category.equals("Videos", ignoreCase = true) }

    // 1. Group photos by detected platform / source origin
    val platformAlbums = buildList {
        val platformMap = photos.groupBy { SmartMediaTagger.detectPlatform(it) }

        // Camera
        val cameraPhotos = platformMap[MediaPlatform.CAMERA].orEmpty()
        add(
            AlbumMeta(
                name = "Cámara",
                icon = Icons.Default.CameraAlt,
                accentColor = AuraPrimary,
                coverUri = cameraPhotos.firstOrNull()?.uriString,
                count = cameraPhotos.size,
                subtitle = "Fotos y videos tomados"
            )
        )

        // WhatsApp
        val waPhotos = platformMap[MediaPlatform.WHATSAPP].orEmpty()
        if (waPhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "WhatsApp",
                    icon = Icons.Default.Send,
                    accentColor = Color(0xFF25D366),
                    coverUri = waPhotos.firstOrNull()?.uriString,
                    count = waPhotos.size,
                    subtitle = "Imágenes y chats recibidos"
                )
            )
        }

        // Downloads / Descargas
        val dlPhotos = platformMap[MediaPlatform.DOWNLOADS].orEmpty()
        if (dlPhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "Descargas",
                    icon = Icons.Default.Download,
                    accentColor = AuraCyan,
                    coverUri = dlPhotos.firstOrNull()?.uriString,
                    count = dlPhotos.size,
                    subtitle = "Archivos guardados de la web"
                )
            )
        }

        // Screenshots / Capturas
        val ssPhotos = platformMap[MediaPlatform.SCREENSHOTS].orEmpty()
        if (ssPhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "Capturas de pantalla",
                    icon = Icons.Default.Screenshot,
                    accentColor = AuraAmber,
                    coverUri = ssPhotos.firstOrNull()?.uriString,
                    count = ssPhotos.size,
                    subtitle = "Capturas del sistema"
                )
            )
        }

        // Telegram
        val tgPhotos = platformMap[MediaPlatform.TELEGRAM].orEmpty()
        if (tgPhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "Telegram",
                    icon = Icons.Default.Send,
                    accentColor = Color(0xFF2AABEE),
                    coverUri = tgPhotos.firstOrNull()?.uriString,
                    count = tgPhotos.size,
                    subtitle = "Media de Telegram"
                )
            )
        }

        // Instagram
        val igPhotos = platformMap[MediaPlatform.INSTAGRAM].orEmpty()
        if (igPhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "Instagram",
                    icon = Icons.Default.Image,
                    accentColor = AuraRose,
                    coverUri = igPhotos.firstOrNull()?.uriString,
                    count = igPhotos.size,
                    subtitle = "Descargas de Instagram"
                )
            )
        }

        // Other Storage
        val storagePhotos = platformMap[MediaPlatform.STORAGE].orEmpty()
        if (storagePhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "Imágenes",
                    icon = Icons.Default.Folder,
                    accentColor = AuraEmerald,
                    coverUri = storagePhotos.firstOrNull()?.uriString,
                    count = storagePhotos.size,
                    subtitle = "Almacenamiento del dispositivo"
                )
            )
        }
    }

    // 2. AI Thematic Categories (Only populate if AI has actually classified them)
    val thematicAlbums = buildList {
        // Cine / Películas
        val cinemaPhotos = photos.filter {
            it.category.equals("Cine / Películas", true) || it.category.equals("Cine", true) || it.category.equals("Películas", true)
        }
        if (cinemaPhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "Cine y Películas",
                    icon = Icons.Default.Videocam,
                    accentColor = Color(0xFFEF4444),
                    coverUri = cinemaPhotos.firstOrNull()?.uriString,
                    count = cinemaPhotos.size,
                    subtitle = "Escenas y cinematografía"
                )
            )
        }

        // Anime / Arte
        val animePhotos = photos.filter {
            it.category.equals("Anime / Arte", true) || it.category.equals("Anime", true) || it.category.equals("Arte", true)
        }
        if (animePhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "Anime / Arte",
                    icon = Icons.Default.Brush,
                    accentColor = Color(0xFF8B5CF6),
                    coverUri = animePhotos.firstOrNull()?.uriString,
                    count = animePhotos.size,
                    subtitle = "Ilustraciones y arte digital"
                )
            )
        }

        // Videojuegos / Gaming
        val gamePhotos = photos.filter {
            it.category.equals("Videojuegos", true) || it.category.equals("Gaming", true)
        }
        if (gamePhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "Videojuegos",
                    icon = Icons.Default.Gamepad,
                    accentColor = Color(0xFFEC4899),
                    coverUri = gamePhotos.firstOrNull()?.uriString,
                    count = gamePhotos.size,
                    subtitle = "Gaming y capturas de juegos"
                )
            )
        }

        // Retratos y Personas
        val portraitPhotos = photos.filter {
            it.category.equals("Retratos", true) || (it.faces.isNotEmpty() && !it.category.equals("Videos", true))
        }
        if (portraitPhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "Retratos",
                    icon = Icons.Default.Face,
                    accentColor = AuraCyan,
                    coverUri = portraitPhotos.firstOrNull()?.uriString,
                    count = portraitPhotos.size,
                    subtitle = "Rostros y personas identificadas"
                )
            )
        }

        // Naturaleza
        val naturePhotos = photos.filter {
            it.category.equals("Naturaleza", true)
        }
        if (naturePhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "Naturaleza",
                    icon = Icons.Default.Forest,
                    accentColor = AuraEmerald,
                    coverUri = naturePhotos.firstOrNull()?.uriString,
                    count = naturePhotos.size,
                    subtitle = "Paisajes y exteriores"
                )
            )
        }

        // Mascotas
        val petPhotos = photos.filter {
            it.category.equals("Mascotas", true)
        }
        if (petPhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "Mascotas",
                    icon = Icons.Default.Pets,
                    accentColor = AuraAmber,
                    coverUri = petPhotos.firstOrNull()?.uriString,
                    count = petPhotos.size,
                    subtitle = "Animales y compañía"
                )
            )
        }

        // Comida / Gastronomía
        val foodPhotos = photos.filter {
            it.category.equals("Comida", true)
        }
        if (foodPhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "Comida",
                    icon = Icons.Default.Restaurant,
                    accentColor = Color(0xFFF97316),
                    coverUri = foodPhotos.firstOrNull()?.uriString,
                    count = foodPhotos.size,
                    subtitle = "Gastronomía y platos"
                )
            )
        }

        // Viajes
        val travelPhotos = photos.filter {
            it.category.equals("Viajes", true)
        }
        if (travelPhotos.isNotEmpty()) {
            add(
                AlbumMeta(
                    name = "Viajes",
                    icon = Icons.Default.Flight,
                    accentColor = AuraCyan,
                    coverUri = travelPhotos.firstOrNull()?.uriString,
                    count = travelPhotos.size,
                    subtitle = "Destinos y recuerdos"
                )
            )
        }

        // Any other category not already covered
        categoryCounts.forEach { (catName, count) ->
            if (none { it.name.equals(catName, true) } &&
                !catName.equals("Videos", true) &&
                !catName.equals("Cámara", true) &&
                !catName.equals("Descargas", true) &&
                !catName.equals("WhatsApp", true) &&
                !catName.equals("Capturas de pantalla", true) &&
                !catName.equals("Imágenes", true) &&
                !catName.equals("Dispositivo", true) &&
                count > 0
            ) {
                val catPhoto = photos.firstOrNull { it.category.equals(catName, true) }
                add(
                    AlbumMeta(
                        name = catName,
                        icon = Icons.Default.Folder,
                        accentColor = AuraPrimary,
                        coverUri = catPhoto?.uriString,
                        count = count,
                        subtitle = "Categoría clasificada"
                    )
                )
            }
        }
    }

    // Dynamic unique locations
    val locations = photos
        .mapNotNull { if (it.locationName.isNotBlank() && it.locationName != "Ubicación actual") it.locationName else null }
        .distinct()
        .take(8)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("explore_screen")
    ) {
        // 1. AI Vision Intelligence Banner
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF101720),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraCyan.copy(alpha = 0.3f)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_ai_banner")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            GBrush.horizontalGradient(
                                colors = listOf(
                                    AuraCyan.copy(alpha = 0.16f),
                                    AuraPrimary.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AuraCyan.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraCyan.copy(alpha = 0.4f)),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = AuraCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Visión e IA de Galería",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Clasificación multimodal y rostros",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            if (!isAiScanning) {
                                Surface(
                                    onClick = onRunAiScan,
                                    shape = RoundedCornerShape(12.dp),
                                    color = AuraCyan.copy(alpha = 0.18f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraCyan.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = AuraCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Re-escanear",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AuraCyan,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        if (isAiScanning) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { aiScanProgress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AuraCyan,
                                    trackColor = AuraCyan.copy(alpha = 0.2f)
                                )
                                Text(
                                    text = "${(aiScanProgress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraCyan
                                )
                            }
                        }
                    }
                }
            }
        }
        // 1. Featured Dynamic Bento Header (Favoritos & Bóveda Cifrada)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Favoritos Card
                Surface(
                    onClick = onSelectFavorites,
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF1B1417),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraRose.copy(alpha = 0.25f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(108.dp)
                        .testTag("explore_fav_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                GBrush.radialGradient(
                                    colors = listOf(
                                        AuraRose.copy(alpha = 0.18f),
                                        Color.Transparent
                                    ),
                                    radius = 240f
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AuraRose.copy(alpha = 0.2f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = AuraRose,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "$favCount",
                                    fontWeight = FontWeight.Bold,
                                    color = AuraRose,
                                    fontSize = 18.sp
                                )
                            }

                            Column {
                                Text(
                                    text = "Favoritos",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Text(
                                    text = if (favCount == 1) "1 elemento" else "$favCount elementos",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }

                // Bóveda Cifrada Card
                Surface(
                    onClick = onSelectEncrypted,
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF0F1A15),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraEmerald.copy(alpha = 0.25f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(108.dp)
                        .testTag("explore_vault_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                GBrush.radialGradient(
                                    colors = listOf(
                                        AuraEmerald.copy(alpha = 0.18f),
                                        Color.Transparent
                                    ),
                                    radius = 240f
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AuraEmerald.copy(alpha = 0.2f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = AuraEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "$vaultCount",
                                    fontWeight = FontWeight.Bold,
                                    color = AuraEmerald,
                                    fontSize = 18.sp
                                )
                            }

                            Column {
                                Text(
                                    text = "Bóveda Cifrada",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Text(
                                    text = "Protección E2EE",
                                    fontSize = 11.sp,
                                    color = AuraEmerald.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Orígenes y Plataformas del Dispositivo (Cámara, WhatsApp, Descargas, Capturas, etc.)
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoAlbum,
                            contentDescription = null,
                            tint = AuraCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Plataformas y Origen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${platformAlbums.size} fuentes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    platformAlbums.chunked(2).forEach { rowAlbums ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowAlbums.forEach { album ->
                                DeviceAlbumCard(
                                    album = album,
                                    onClick = { onSelectAlbum(album) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowAlbums.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 3. Categorías Temáticas Inteligentes (IA)
        if (thematicAlbums.isNotEmpty()) {
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AuraAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Categorías Inteligentes IA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${thematicAlbums.size} colecciones",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        thematicAlbums.chunked(2).forEach { rowAlbums ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowAlbums.forEach { album ->
                                    DeviceAlbumCard(
                                        album = album,
                                        onClick = { onSelectAlbum(album) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowAlbums.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Facial Recognition & People Section (if any people detected)
        if (peopleProfiles.isNotEmpty()) {
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = AuraCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Personas y Rostros",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AuraCyan.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${peopleProfiles.size} detectadas",
                                color = AuraCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        peopleProfiles.forEach { profile ->
                            PersonAvatarCard(
                                profile = profile,
                                isSelected = false,
                                onClick = { onSelectPerson(profile) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Ubicaciones encontradas en el dispositivo
        if (locations.isNotEmpty()) {
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AuraAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Carpetas y Ubicaciones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        locations.forEach { loc ->
                            Surface(
                                onClick = { onSelectLocation(loc) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, AuraAmber.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = AuraAmber, modifier = Modifier.size(16.dp))
                                    Text(text = loc, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceAlbumCard(
    album: AlbumMeta,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countText = if (album.count == 1) "1 elemento" else "${album.count} elementos"

    Box(
        modifier = modifier
            .height(124.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF16181F))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .testTag("category_card_${album.name.lowercase().replace(" ", "_")}")
    ) {
        if (!album.coverUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.coverUri)
                    .crossfade(true)
                    .build(),
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        GBrush.linearGradient(
                            listOf(album.accentColor.copy(alpha = 0.25f), Color(0xFF16181F))
                        )
                    )
            )
        }

        // Smooth subtle dark gradient overlay for optimal legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    GBrush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Glass badge with icon
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, album.accentColor.copy(alpha = 0.45f)),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = album.icon,
                        contentDescription = null,
                        tint = album.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Title & Count & Subtitle
            Column {
                Text(
                    text = album.name,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = countText,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
