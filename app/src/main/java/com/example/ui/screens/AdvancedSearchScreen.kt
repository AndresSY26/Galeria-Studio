package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PersonProfile
import com.example.data.model.Photo
import com.example.ui.components.PhotoThumbnailCard
import com.example.ui.theme.AuraAmber
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.AuraPrimary
import com.example.ui.theme.AuraRose

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedSearchScreen(
    searchQuery: String,
    filteredPhotos: List<Photo>,
    allPhotos: List<Photo>,
    peopleProfiles: List<PersonProfile>,
    selectedCategory: String?,
    selectedPerson: String?,
    onlyFavorites: Boolean,
    onlyEncrypted: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onPersonFilterChange: (String?) -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onToggleEncryptedFilter: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onToggleFavorite: (Photo) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val allCategories = allPhotos.map { it.category }.filter { it.isNotBlank() }.distinct()
    val meaningfulTags = allPhotos.flatMap { it.tags }
        .filterNot { it.equals("local", true) || it.equals("dispositivo", true) }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .map { it.key }
        .take(15)

    val peopleNames = peopleProfiles.map { it.name }.distinct()

    val quickAiSuggestions = buildList {
        addAll(peopleNames.take(3))
        addAll(meaningfulTags.take(5))
        addAll(allCategories.take(3))
        if (isEmpty()) {
            add("anime")
            add("cámara")
            add("videos")
            add("capturas")
        }
    }.distinct()

    val hasActiveFilters = searchQuery.isNotBlank() || selectedCategory != null || selectedPerson != null || onlyFavorites || onlyEncrypted

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 90.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("advanced_search_screen")
    ) {
        // Search & Filters Header
        item(span = { GridItemSpan(3) }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Ultra-clean Luxury Search Bar with subtle Glass Glow
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF14171E),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                AuraPrimary.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.08f),
                                AuraCyan.copy(alpha = 0.3f)
                            )
                        )
                    ),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AuraPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = AuraCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Búsqueda con IA (escena, personas, lugares)...",
                                    color = Color.White.copy(alpha = 0.42f),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(AuraCyan),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_text_input")
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. AI Quick Prompts Pills (Compact & elegant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickAiSuggestions.forEach { suggestion ->
                        val isSelected = searchQuery.equals(suggestion, ignoreCase = true)
                        Surface(
                            onClick = {
                                if (isSelected) onSearchQueryChange("") else onSearchQueryChange(suggestion)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) AuraPrimary else Color(0xFF161A22),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) AuraPrimary else Color.White.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else AuraCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = suggestion,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Smart Scope Filters Row (Favoritos, Cifradas, Categorías, Personas)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorites Filter
                    ScopeFilterPill(
                        icon = Icons.Default.Favorite,
                        label = "Favoritas",
                        isSelected = onlyFavorites,
                        activeColor = AuraRose,
                        onClick = onToggleFavoritesFilter
                    )

                    // Vault Filter
                    ScopeFilterPill(
                        icon = Icons.Default.Shield,
                        label = "Bóveda E2EE",
                        isSelected = onlyEncrypted,
                        activeColor = AuraEmerald,
                        onClick = onToggleEncryptedFilter
                    )

                    // Dynamic Categories
                    allCategories.forEach { category ->
                        val isCatSelected = selectedCategory.equals(category, ignoreCase = true)
                        ScopeFilterPill(
                            icon = null,
                            label = category,
                            isSelected = isCatSelected,
                            activeColor = AuraPrimary,
                            onClick = {
                                onCategoryFilterChange(if (isCatSelected) null else category)
                            }
                        )
                    }
                }

                // 4. Person Filter Chips (If people detected)
                if (peopleProfiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        peopleProfiles.forEach { person ->
                            val isPersonSelected = selectedPerson.equals(person.name, ignoreCase = true)
                            ScopeFilterPill(
                                icon = Icons.Default.Person,
                                label = person.name,
                                isSelected = isPersonSelected,
                                activeColor = AuraCyan,
                                onClick = {
                                    onPersonFilterChange(if (isPersonSelected) null else person.name)
                                }
                            )
                        }
                    }
                }

                // 5. Popular Tags Cloud (when idle)
                if (!hasActiveFilters && meaningfulTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Etiquetas Populares con IA",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        meaningfulTags.forEach { tag ->
                            Surface(
                                onClick = { onSearchQueryChange(tag) },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF14171E),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AuraCyan.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Results status bar
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (filteredPhotos.size == 1) "1 elemento encontrado" else "${filteredPhotos.size} elementos encontrados",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    if (hasActiveFilters) {
                        Text(
                            text = "Filtros activos",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraCyan
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Search Results Photos Grid
        if (filteredPhotos.isEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF14171E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sin resultados coincidentes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Intenta con términos más generales como personas, lugares o etiquetas IA.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            items(filteredPhotos, key = { it.id }) { photo ->
                PhotoThumbnailCard(
                    photo = photo,
                    columns = 3,
                    isSelected = false,
                    isSelectionMode = false,
                    onClick = { onPhotoClick(photo) },
                    onLongClick = { },
                    onToggleFavorite = { onToggleFavorite(photo) }
                )
            }
        }
    }
}

@Composable
private fun ScopeFilterPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.18f) else Color(0xFF14171E),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) activeColor else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) activeColor else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) activeColor else Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
