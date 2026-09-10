package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Photo
import com.example.ui.components.PhotoThumbnailCard
import com.example.ui.theme.AuraAmber
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.AuraPrimary
import com.example.ui.theme.AuraRose
import com.example.ui.viewmodel.DateGroup
import com.example.ui.viewmodel.StorageFilter
import com.example.ui.viewmodel.StorageOverview

@Composable
fun GalleryTimelineScreen(
    dateGroups: List<DateGroup>,
    totalPhotoCount: Int,
    columns: Int,
    selectedPhotoIds: Set<Long>,
    isSelectionMode: Boolean,
    storageFilter: StorageFilter,
    storageOverview: StorageOverview,
    isScanning: Boolean = false,
    onSetStorageFilter: (StorageFilter) -> Unit,
    onSetColumns: (Int) -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit,
    onToggleFavorite: (Photo) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onEncryptSelected: () -> Unit,
    onBackupSelected: () -> Unit,
    onFreeSpaceSelected: () -> Unit,
    onDownloadSelected: () -> Unit,
    onBackupAllPending: () -> Unit,
    onScanMedia: () -> Unit = {},
    onOpenPicker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(
                start = 4.dp,
                end = 4.dp,
                top = 4.dp,
                bottom = if (isSelectionMode) 100.dp else 80.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("gallery_grid")
        ) {
            // Scanning Banner or Empty state if no photos match
            if (isScanning && totalPhotoCount == 0) {
                item(span = { GridItemSpan(columns) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = AuraCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Detectando multimedia del dispositivo...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Escaneando fotos y videos almacenados localmente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (dateGroups.isEmpty() || totalPhotoCount == 0) {
                item(span = { GridItemSpan(columns) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AuraPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(76.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = AuraPrimary,
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Galería del Dispositivo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Concede acceso o escanea el almacenamiento para cargar todas las fotos y videos de tu dispositivo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onScanMedia,
                            colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(48.dp)
                                .testTag("scan_device_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Escanear Multimedia del Dispositivo", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onOpenPicker,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(48.dp)
                                .testTag("open_picker_button")
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Seleccionar Fotos del Dispositivo", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Date Groups
                dateGroups.forEach { group ->
                    if (group.photos.isNotEmpty()) {
                        item(span = { GridItemSpan(columns) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, end = 6.dp, top = 16.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = group.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${group.photos.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        items(group.photos, key = { it.id }) { photo ->
                            PhotoThumbnailCard(
                                photo = photo,
                                columns = columns,
                                isSelected = selectedPhotoIds.contains(photo.id),
                                isSelectionMode = isSelectionMode,
                                onClick = { onPhotoClick(photo) },
                                onLongClick = { onPhotoLongClick(photo) },
                                onToggleFavorite = { onToggleFavorite(photo) }
                            )
                        }
                    }
                }
            }
        }

        // Multi-select Action Bar (Floating Pill)
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraPrimary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("multi_select_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClearSelection) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancelar")
                        }
                        Text(
                            text = "${selectedPhotoIds.size} selec.",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = AuraPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSelectAll, modifier = Modifier.testTag("select_all_button")) {
                            Icon(imageVector = Icons.Default.SelectAll, contentDescription = "Seleccionar todo")
                        }
                        IconButton(onClick = onBackupSelected, modifier = Modifier.testTag("backup_batch_button")) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Respaldar", tint = AuraCyan)
                        }
                        IconButton(onClick = onFreeSpaceSelected, modifier = Modifier.testTag("free_space_batch_button")) {
                            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = "Liberar espacio", tint = AuraAmber)
                        }
                        IconButton(onClick = onDownloadSelected, modifier = Modifier.testTag("download_batch_button")) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Descargar", tint = AuraEmerald)
                        }
                        IconButton(onClick = onEncryptSelected, modifier = Modifier.testTag("encrypt_batch_button")) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = "Cifrar", tint = AuraEmerald)
                        }
                        IconButton(onClick = onDeleteSelected, modifier = Modifier.testTag("delete_batch_button")) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = AuraRose)
                        }
                    }
                }
            }
        }
    }
}
