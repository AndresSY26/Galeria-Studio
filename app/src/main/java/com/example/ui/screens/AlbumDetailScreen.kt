package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Photo
import com.example.ui.components.PhotoThumbnailCard
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.AuraPrimary
import com.example.ui.theme.AuraRose

data class ActiveAlbum(
    val title: String,
    val subtitle: String = "",
    val icon: ImageVector = Icons.Default.PhotoLibrary,
    val filterType: AlbumFilterType,
    val filterValue: String = ""
)

enum class AlbumFilterType {
    CATEGORY_OR_PLATFORM,
    PERSON,
    FAVORITES,
    LOCATION,
    ALL_VIDEOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    album: ActiveAlbum,
    photos: List<Photo>,
    onBack: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onToggleFavorite: (Photo) -> Unit,
    onDeleteSelected: () -> Unit,
    onEncryptSelected: () -> Unit,
    onBackupSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var columns by remember { mutableIntStateOf(3) }
    var selectedPhotoIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isSelectionMode = selectedPhotoIds.isNotEmpty()

    // Handle Android hardware/system back gesture
    BackHandler {
        if (isSelectionMode) {
            selectedPhotoIds = emptySet()
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AuraCyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AuraCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = album.icon,
                                    contentDescription = null,
                                    tint = AuraCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = album.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (photos.size == 1) "1 elemento" else "${photos.size} elementos" + if (album.subtitle.isNotBlank()) " • ${album.subtitle}" else "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSelectionMode) {
                                selectedPhotoIds = emptySet()
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("album_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a álbumes",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Column Density Switcher
                    IconButton(
                        onClick = {
                            columns = when (columns) {
                                1 -> 2
                                2 -> 3
                                3 -> 4
                                else -> 1
                            }
                        },
                        modifier = Modifier.testTag("album_grid_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Cambiar columnas",
                            tint = AuraCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Selection actions bar if selecting photos
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedPhotoIds.size} seleccionados",
                            fontWeight = FontWeight.Bold,
                            color = AuraCyan
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = onBackupSelected) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Respaldo", tint = AuraCyan)
                            }
                            IconButton(onClick = onEncryptSelected) {
                                Icon(Icons.Default.Lock, contentDescription = "Cifrar", tint = AuraEmerald)
                            }
                            IconButton(onClick = onDeleteSelected) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = AuraRose)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (photos.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = album.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay elementos en este álbum",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Los archivos de este origen aparecerán aquí automáticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("album_photos_grid")
                ) {
                    items(
                        items = photos,
                        key = { it.id }
                    ) { photo ->
                        val isSelected = selectedPhotoIds.contains(photo.id)
                        PhotoThumbnailCard(
                            photo = photo,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            columns = columns,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedPhotoIds = if (isSelected) {
                                        selectedPhotoIds - photo.id
                                    } else {
                                        selectedPhotoIds + photo.id
                                    }
                                } else {
                                    onPhotoClick(photo)
                                }
                            },
                            onLongClick = {
                                selectedPhotoIds = if (isSelected) {
                                    selectedPhotoIds - photo.id
                                } else {
                                    selectedPhotoIds + photo.id
                                }
                            },
                            onToggleFavorite = { onToggleFavorite(photo) }
                        )
                    }
                }
            }
        }
    }
}
