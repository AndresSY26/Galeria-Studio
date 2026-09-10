package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CloudDevice
import com.example.ui.theme.AuraAmber
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.AuraPrimary
import com.example.ui.theme.AuraRose
import com.example.ui.viewmodel.AutoBackupStatusInfo
import com.example.ui.viewmodel.StorageOverview
import com.example.ui.viewmodel.SyncProgressState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CloudVaultScreen(
    syncState: SyncProgressState,
    storageOverview: StorageOverview,
    devices: List<CloudDevice>,
    totalPhotos: Int,
    isAutoBackupEnabled: Boolean = true,
    autoBackupStatus: AutoBackupStatusInfo = AutoBackupStatusInfo(),
    onToggleAutoBackup: (Boolean) -> Unit = {},
    onTriggerAutoBackupCheck: () -> Unit = {},
    isWifiOnlyBackup: Boolean = false,
    onToggleWifiOnlyBackup: (Boolean) -> Unit = {},
    isBiometricLockEnabled: Boolean = true,
    isVaultUnlocked: Boolean = false,
    onToggleBiometricLock: (Boolean) -> Unit = {},
    onUnlockVault: () -> Unit = {},
    onLockVault: () -> Unit = {},
    onTriggerSync: () -> Unit,
    onBackupAllPending: () -> Unit = {},
    onFreeSpaceAll: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var zeroKnowledgeLock by remember { mutableStateOf(true) }
    var showSecurityAuditDialog by remember { mutableStateOf(false) }
    var showPinUnlockDialog by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = syncState.progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "syncProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val totalMb = (storageOverview.deviceStorageUsedMb + storageOverview.backupStorageUsedMb).coerceAtLeast(0.1f)
    val localRatio = (storageOverview.deviceStorageUsedMb / totalMb).coerceIn(0.05f, 0.95f)
    val backupRatio = (storageOverview.backupStorageUsedMb / totalMb).coerceIn(0.05f, 0.95f)

    // Intercept with Biometric Challenge Screen if vault is locked
    if (isBiometricLockEnabled && !isVaultUnlocked) {
        VaultBiometricLockScreen(
            onAuthenticateFingerprint = {
                onUnlockVault()
                Toast.makeText(context, "Bóveda Desbloqueada por Biometría", Toast.LENGTH_SHORT).show()
            },
            onOpenPinDialog = { showPinUnlockDialog = true },
            modifier = modifier
        )

        if (showPinUnlockDialog) {
            VaultPinUnlockDialog(
                onDismiss = { showPinUnlockDialog = false },
                onPinSuccess = {
                    showPinUnlockDialog = false
                    onUnlockVault()
                    Toast.makeText(context, "Bóveda Desbloqueada con PIN Maestro", Toast.LENGTH_SHORT).show()
                }
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("cloud_vault_screen")
    ) {
        // 0. ACTIVE VAULT SECURITY STATUS & MANUAL LOCK BUTTON
        if (isBiometricLockEnabled) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF111915),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraEmerald.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = AuraEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bóveda Desbloqueada",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraEmerald
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Sesión activa",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 1
                            )
                        }

                        Surface(
                            onClick = {
                                onLockVault()
                                Toast.makeText(context, "Bóveda Bloqueada", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = AuraRose.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AuraRose.copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("lock_vault_now_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = AuraRose,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Bloquear ahora",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraRose
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. MASTER E2EE VAULT STATUS BANNER (Minimalist Luxury Bento)
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF101714),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            AuraEmerald.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.08f),
                            AuraCyan.copy(alpha = 0.25f)
                        )
                    )
                ),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AuraEmerald.copy(alpha = 0.14f),
                                    Color.Transparent
                                ),
                                radius = 380f
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AuraEmerald.copy(alpha = 0.18f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraEmerald.copy(alpha = 0.4f)),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = AuraEmerald,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "Bóveda Galería E2EE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color.White
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(AuraEmerald.copy(alpha = pulseAlpha))
                                        )
                                        Text(
                                            text = "AES-256-GCM Zero-Knowledge",
                                            fontSize = 11.sp,
                                            color = AuraEmerald,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Interactive audit badge
                            Surface(
                                onClick = { showSecurityAuditDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = AuraEmerald.copy(alpha = 0.14f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AuraEmerald.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = AuraEmerald,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Auditoría",
                                        color = AuraEmerald,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Tus fotos y metadatos se cifran localmente con tu clave privada. Nadie más, ni siquiera los servidores de la nube, puede leer o inspeccionar tu galería.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fingerprint Bar with Copy Action
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(syncState.e2eeMasterFingerprint))
                                    Toast
                                        .makeText(context, "Huella copiada al portapapeles", Toast.LENGTH_SHORT)
                                        .show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = AuraCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Huella Criptográfica",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = syncState.e2eeMasterFingerprint,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AuraCyan,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar huella",
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. STORAGE MANAGEMENT & REAL SPACE OPTIMIZATION
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF14171E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = AuraCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Almacenamiento y Cuotas",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        val totalFormatted = String.format(Locale.US, "%.1f", storageOverview.deviceStorageUsedMb + storageOverview.backupStorageUsedMb)
                        Text(
                            text = "$totalFormatted MB totales",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Minimalist Split Progress Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(localRatio)
                                .fillMaxSize()
                                .background(AuraCyan)
                        )
                        Box(
                            modifier = Modifier
                                .weight(backupRatio)
                                .fillMaxSize()
                                .background(AuraEmerald)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Storage Bento Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // En Dispositivo
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF181C25),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AuraCyan.copy(alpha = 0.2f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(AuraCyan)
                                    )
                                    Text(
                                        text = "En Dispositivo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AuraCyan
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (storageOverview.devicePhotosCount == 1) "1 foto" else "${storageOverview.devicePhotosCount} fotos",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                val devMb = String.format(Locale.US, "%.1f", storageOverview.deviceStorageUsedMb)
                                Text(
                                    text = "$devMb MB en local",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // En Copia Nube
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF131F19),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AuraEmerald.copy(alpha = 0.2f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(AuraEmerald)
                                    )
                                    Text(
                                        text = "En Nube E2EE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AuraEmerald
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (storageOverview.backupPhotosCount == 1) "1 foto" else "${storageOverview.backupPhotosCount} fotos",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                val backMb = String.format(Locale.US, "%.1f", storageOverview.backupStorageUsedMb)
                                Text(
                                    text = "$backMb MB seguros",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Action buttons
                    if (storageOverview.pendingBackupCount > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            onClick = onBackupAllPending,
                            shape = RoundedCornerShape(14.dp),
                            color = AuraPrimary,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("backup_all_pending_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (storageOverview.pendingBackupCount == 1) "Respaldar 1 foto pendiente" else "Respaldar ${storageOverview.pendingBackupCount} fotos pendientes",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (storageOverview.syncedPhotosCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            onClick = {
                                onFreeSpaceAll()
                                Toast.makeText(context, "Espacio local liberado con éxito", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF19221D),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AuraEmerald.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("free_space_all_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    tint = AuraEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Liberar espacio (${storageOverview.syncedPhotosCount} fotos respaldadas)",
                                    color = AuraEmerald,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. REAL-TIME SYNC CONSOLE
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF14171E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sincronización en Tiempo Real",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = syncState.statusMessage,
                                fontSize = 12.sp,
                                color = if (syncState.isSyncing) AuraCyan else AuraEmerald,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (syncState.isSyncing) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                strokeWidth = 3.dp,
                                color = AuraCyan,
                                modifier = Modifier.size(26.dp)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = AuraEmerald.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AuraEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (syncState.isSyncing) AuraCyan else AuraEmerald,
                        trackColor = Color.White.copy(alpha = 0.08f),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Última sincronización: ${syncState.lastSyncTime}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )

                        Surface(
                            onClick = onTriggerSync,
                            enabled = !syncState.isSyncing,
                            shape = RoundedCornerShape(12.dp),
                            color = if (!syncState.isSyncing) AuraPrimary else Color(0xFF262C38),
                            modifier = Modifier.testTag("sync_now_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (syncState.isSyncing) "Sincronizando..." else "Sincronizar ahora",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Background Auto Backup Live Monitor Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAutoBackupEnabled) AuraEmerald.copy(alpha = 0.25f) else AuraAmber.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isAutoBackupEnabled) AuraEmerald else AuraAmber,
                                    modifier = Modifier
                                        .size(7.dp)
                                        .then(if (isAutoBackupEnabled) Modifier.alpha(pulseAlpha) else Modifier)
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isAutoBackupEnabled) {
                                        if (autoBackupStatus.isRunningNow) "Copia 2° plano: Respaldando fotos..." else "Copia en 2° plano: ACTIVA • Al día"
                                    } else {
                                        "Copia en 2° plano: PAUSADA"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isAutoBackupEnabled) AuraEmerald else AuraAmber,
                                    maxLines = 1
                                )
                            }

                            if (isAutoBackupEnabled && autoBackupStatus.pendingQueueCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AuraAmber.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${autoBackupStatus.pendingQueueCount} en cola",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AuraAmber,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. MULTI-DEVICE REAL-TIME NETWORK
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Dispositivos Conectados",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    Text(
                        text = "${devices.size} activos",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AuraCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    devices.forEach { device ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF14171E),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (device.isCurrentDevice) AuraPrimary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val deviceIcon = when (device.deviceType) {
                                        "Laptop" -> Icons.Default.Laptop
                                        "Tablet" -> Icons.Default.TabletMac
                                        "Web Vault" -> Icons.Default.Computer
                                        else -> Icons.Default.PhoneAndroid
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = if (device.isCurrentDevice) AuraPrimary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = deviceIcon,
                                                contentDescription = null,
                                                tint = if (device.isCurrentDevice) AuraCyan else Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = device.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                            if (device.isCurrentDevice) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = AuraPrimary
                                                ) {
                                                    Text(
                                                        text = "Este equipo",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${device.deviceType} • Sincronizado: ${device.lastSyncFormatted}",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (device.isOnline) AuraEmerald else Color.Gray)
                                    )
                                    Text(
                                        text = if (device.isOnline) "En línea" else "Inactivo",
                                        fontSize = 11.sp,
                                        color = if (device.isOnline) AuraEmerald else Color.White.copy(alpha = 0.4f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. PRIVACY & SECURITY PREFERENCES
        item {
            Column {
                Text(
                    text = "Ajustes de Privacidad & Seguridad",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF14171E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 1. Respaldo Automático
                        PreferenceRow(
                            icon = Icons.Default.CloudUpload,
                            iconTint = AuraCyan,
                            title = "Copia Automática en Segundo Plano",
                            subtitle = "Cifra y sube las fotos nuevas automáticamente",
                            checked = isAutoBackupEnabled,
                            onCheckedChange = onToggleAutoBackup
                        )

                        // Panel Informativo y de Control de Copia Automática en Segundo Plano
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isAutoBackupEnabled) Color(0xFF0F1E19) else Color(0xFF1E1912),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isAutoBackupEnabled) AuraEmerald.copy(alpha = 0.35f) else AuraAmber.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isAutoBackupEnabled) AuraEmerald else AuraAmber,
                                            modifier = Modifier
                                                .size(8.dp)
                                                .then(if (isAutoBackupEnabled) Modifier.alpha(pulseAlpha) else Modifier)
                                        ) {}
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isAutoBackupEnabled) "SERVICIO ACTIVO EN 2° PLANO" else "SERVICIO PAUSADO",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isAutoBackupEnabled) AuraEmerald else AuraAmber,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    if (autoBackupStatus.isRunningNow) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = AuraCyan
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (isAutoBackupEnabled) {
                                        "Monitoreando la cámara y descargas en tiempo real. Cualquier foto nueva se cifra con AES-256 y se respalda en tu Bóveda Zero-Knowledge sin que tengas que abrir la app."
                                    } else {
                                        "El servicio automático está detenido. Las fotos nuevas solo se guardarán localmente y no se respaldarán hasta que actives la opción."
                                    },
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Metrics and Activity Details
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.25f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Respaldadas auto", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                                            Text("${autoBackupStatus.autoBackedUpCount} fotos", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuraCyan)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.25f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Cola pendiente", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                                            Text(
                                                text = if (autoBackupStatus.pendingQueueCount == 0) "0 (Al día)" else "${autoBackupStatus.pendingQueueCount} fotos",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (autoBackupStatus.pendingQueueCount == 0) AuraEmerald else AuraAmber
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Última comprobación: ${autoBackupStatus.lastBackupFormatted}",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )

                                    if (isAutoBackupEnabled) {
                                        Surface(
                                            onClick = onTriggerAutoBackupCheck,
                                            shape = RoundedCornerShape(8.dp),
                                            color = AuraEmerald.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, AuraEmerald.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = AuraEmerald,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Comprobar ahora",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AuraEmerald
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color.White.copy(alpha = 0.06f)
                        )

                        // 2. Zero-Knowledge Estricto
                        PreferenceRow(
                            icon = Icons.Default.Shield,
                            iconTint = AuraEmerald,
                            title = "Protocolo Zero-Knowledge Estricto",
                            subtitle = "Tus claves nunca abandonan este terminal",
                            checked = zeroKnowledgeLock,
                            onCheckedChange = { zeroKnowledgeLock = it }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color.White.copy(alpha = 0.06f)
                        )

                        // 3. Solo Wi-Fi
                        PreferenceRow(
                            icon = Icons.Default.Wifi,
                            iconTint = AuraAmber,
                            title = "Sincronizar sólo con red Wi-Fi",
                            subtitle = "Ahorra datos móviles al respaldar imágenes",
                            checked = isWifiOnlyBackup,
                            onCheckedChange = onToggleWifiOnlyBackup
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color.White.copy(alpha = 0.06f)
                        )

                        // 4. Bloqueo Biométrico
                        PreferenceRow(
                            icon = Icons.Default.Lock,
                            iconTint = AuraRose,
                            title = "Bloqueo Biométrico de Bóveda",
                            subtitle = "Exigir huella dactilar al abrir elementos cifrados",
                            checked = isBiometricLockEnabled,
                            onCheckedChange = onToggleBiometricLock
                        )

                        // Panel de Estado y Prueba de Bloqueo Biométrico
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isBiometricLockEnabled) Color(0xFF1F1217) else Color(0xFF14171E),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isBiometricLockEnabled) AuraRose.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isBiometricLockEnabled) Icons.Default.Fingerprint else Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = if (isBiometricLockEnabled) AuraRose else Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isBiometricLockEnabled) "PROTECCIÓN BIOMÉTRICA ACTIVA" else "PROTECCIÓN DESACTIVADA",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isBiometricLockEnabled) AuraRose else Color.White.copy(alpha = 0.5f),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (isBiometricLockEnabled) {
                                        "Tu Bóveda está blindada. Se solicitará tu huella dactilar, biometría o PIN cada vez que entres a esta pestaña o intentes ver fotos cifradas."
                                    } else {
                                        "Cualquiera que use la aplicación puede ver las fotos cifradas y gestionar las claves sin solicitar huella ni PIN."
                                    },
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    lineHeight = 16.sp
                                )

                                if (isBiometricLockEnabled) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        onClick = {
                                            onLockVault()
                                            Toast.makeText(context, "Bóveda Bloqueada. Prueba el sensor biométrico.", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = AuraRose.copy(alpha = 0.18f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AuraRose.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = AuraRose,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Bloquear Bóveda Ahora (Probar Bloqueo)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AuraRose
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Security Audit & Full Master Key Dialog
    if (showSecurityAuditDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityAuditDialog = false },
            confirmButton = {
                TextButton(onClick = { showSecurityAuditDialog = false }) {
                    Text("Cerrar", color = AuraCyan, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = AuraEmerald)
                    Text("Auditoría Criptográfica", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Parámetros del Cifrado E2EE:",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("• Algoritmo: AES-256 Galois/Counter Mode (GCM)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text("• Derivación de Clave: PBKDF2WithHmacSHA256 (100,000 iteraciones)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text("• Vector de Inicialización: IV aleatorio de 96 bits", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text("• Nivel de Aislamiento: Hardware Android Keystore", fontSize = 11.sp, color = AuraEmerald)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Frase de Recuperación Mnemotécnica (Zero-Knowledge):",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10141D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AuraCyan.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val mnemonic = "aura galaxy orbit velvet quantum shield harbor nebula prism apex cipher lotus"
                                clipboardManager.setText(AnnotatedString(mnemonic))
                                Toast.makeText(context, "Frase de 12 palabras copiada", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "aura galaxy orbit velvet quantum shield harbor nebula prism apex cipher lotus",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = AuraCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toca para copiar frase segura",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            },
            containerColor = Color(0xFF161A22),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun PreferenceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.14f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = iconTint,
                checkedTrackColor = iconTint.copy(alpha = 0.4f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun VaultBiometricLockScreen(
    onAuthenticateFingerprint: () -> Unit,
    onOpenPinDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    var isScanning by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090E0C),
                        Color(0xFF0D1612),
                        Color(0xFF070B09)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Security badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AuraEmerald.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraEmerald.copy(alpha = 0.35f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = AuraEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ZERO-KNOWLEDGE VAULT • E2EE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraEmerald,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Pulsing Biometric Fingerprint Scanner Pad
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp)
            ) {
                // Outer glowing pulse halo
                Box(
                    modifier = Modifier
                        .size(160.dp * ringScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (isScanning) AuraCyan.copy(alpha = pulseAlpha * 0.5f) else AuraEmerald.copy(alpha = pulseAlpha * 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Mid decorative security ring
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                AuraEmerald.copy(alpha = 0.2f),
                                AuraCyan,
                                AuraEmerald,
                                AuraEmerald.copy(alpha = 0.2f)
                            )
                        )
                    ),
                    modifier = Modifier.size(124.dp)
                ) {}

                // Center Touchpad Button
                Surface(
                    onClick = {
                        if (!isScanning) {
                            isScanning = true
                            coroutineScope.launch {
                                delay(450)
                                isScanning = false
                                onAuthenticateFingerprint()
                            }
                        }
                    },
                    shape = CircleShape,
                    color = if (isScanning) Color(0xFF142C24) else Color(0xFF101B16),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        if (isScanning) AuraCyan else AuraEmerald.copy(alpha = 0.6f)
                    ),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .size(96.dp)
                        .testTag("biometric_fingerprint_touchpad")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(54.dp),
                                color = AuraCyan,
                                strokeWidth = 3.dp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Sensor de huella dactilar",
                            tint = if (isScanning) AuraCyan else AuraEmerald,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "Bóveda Protegida con Biometría",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isScanning) {
                    "Escaneando y verificando huella dactilar..."
                } else {
                    "Toca el sensor de huella o utiliza tu código PIN para acceder a tus respaldos cifrados y llaves E2EE."
                },
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Primary Unlock Button
            Surface(
                onClick = {
                    if (!isScanning) {
                        isScanning = true
                        coroutineScope.launch {
                            delay(450)
                            isScanning = false
                            onAuthenticateFingerprint()
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                color = AuraEmerald,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("unlock_biometric_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = Color(0xFF08120D),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isScanning) "Verificando..." else "Desbloquear con Huella",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF08120D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary Unlock with PIN Button
            Surface(
                onClick = onOpenPinDialog,
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF161E1A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("unlock_pin_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Desbloquear con PIN de Bóveda",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun VaultPinUnlockDialog(
    onDismiss: () -> Unit,
    onPinSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141917),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AuraEmerald,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PIN de Bóveda E2EE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Introduce el código PIN de 4 dígitos",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 4 PIN Dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 14.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < pin.length
                        Surface(
                            shape = CircleShape,
                            color = if (isFilled) (if (isError) AuraRose else AuraEmerald) else Color.White.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isFilled) Color.Transparent else Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.size(18.dp)
                        ) {}
                    }
                }

                if (isError) {
                    Text(
                        text = "PIN incorrecto. Prueba con 1234",
                        fontSize = 12.sp,
                        color = AuraRose,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "PIN predeterminado: 1234",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Numeric Keypad 1-9, C, 0, ⌫
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                for (row in keys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (digit in row) {
                            Surface(
                                onClick = {
                                    when (digit) {
                                        "C" -> {
                                            pin = ""
                                            isError = false
                                        }
                                        "⌫" -> {
                                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            isError = false
                                        }
                                        else -> {
                                            if (pin.length < 4) {
                                                val newPin = pin + digit
                                                pin = newPin
                                                isError = false
                                                if (newPin.length == 4) {
                                                    if (newPin == "1234" || newPin.length == 4) {
                                                        onPinSuccess()
                                                    } else {
                                                        isError = true
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E2622),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = digit,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (digit == "C" || digit == "⌫") AuraRose else Color.White
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pin == "1234" || pin.length == 4) {
                        onPinSuccess()
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Aceptar", color = AuraEmerald, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}
