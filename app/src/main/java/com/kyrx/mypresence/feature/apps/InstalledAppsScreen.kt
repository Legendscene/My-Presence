package com.kyrx.mypresence.feature.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyrx.mypresence.domain.model.AppCategory
import com.kyrx.mypresence.domain.model.AppInfo
import com.kyrx.mypresence.domain.repository.AppSort
import com.kyrx.mypresence.ui.animation.StaggeredReveal
import com.kyrx.mypresence.ui.components.GlassCard
import com.kyrx.mypresence.ui.components.PremiumSwitch
import com.kyrx.mypresence.ui.components.ShimmerCard
import com.kyrx.mypresence.ui.theme.Accent
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Success
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.Surface
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.theme.TextTertiary
import com.kyrx.mypresence.ui.theme.Warning
import com.kyrx.mypresence.domain.repository.AppRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstalledAppsScreen(
    vm: InstalledAppsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val apps by vm.filteredApps.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val currentSort by vm.currentSort.collectAsStateWithLifecycle()
    val isScanning by vm.isScanning.collectAsStateWithLifecycle()
    val foregroundApp by vm.foregroundApp.collectAsStateWithLifecycle()
    val enabledApps by vm.enabledApps.collectAsStateWithLifecycle()
    val showFavoritesOnly by vm.showFavoritesOnly.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }
    var showBulkActions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        StaggeredReveal(0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Installed Apps",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${apps.size}",
                        color = Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W600
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        StaggeredReveal(1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = searchQuery,
                    onValueChange = { vm.setSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Search apps...", color = TextTertiary, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { vm.setSearchQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextTertiary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Surface,
                        unfocusedContainerColor = Surface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Accent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (showBulkActions) Accent.copy(alpha = 0.2f) else Surface),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { showBulkActions = !showBulkActions }) {
                        Icon(
                            Icons.Filled.DoneAll,
                            contentDescription = "Bulk",
                            tint = if (showBulkActions) Accent else TextTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (showSortMenu) Accent.copy(alpha = 0.2f) else Surface),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { showSortMenu = !showSortMenu }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = if (showSortMenu) Accent else TextTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = showSortMenu) {
            StaggeredReveal(2) {
                GlassCard(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    AppSort.entries.forEach { sort ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        vm.setSort(sort)
                                        showSortMenu = false
                                    }
                                )
                                .background(if (sort == currentSort) Accent.copy(alpha = 0.1f) else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                sort.displayName,
                                color = if (sort == currentSort) Accent else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (sort == currentSort) FontWeight.W600 else FontWeight.W400
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(visible = showBulkActions) {
            StaggeredReveal(2) {
                GlassCard(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                    Button(
                        onClick = { vm.enableAll() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent.copy(alpha = 0.15f)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Enable All", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.W600)
                    }
                    Button(
                        onClick = { vm.disableAll() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Warning.copy(alpha = 0.15f)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Icon(Icons.Filled.Clear, contentDescription = null, tint = Warning, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Disable All", color = Warning, fontSize = 13.sp, fontWeight = FontWeight.W600)
                    }
                    Button(
                        onClick = { vm.invertSelection() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TextTertiary.copy(alpha = 0.15f)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Invert", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.W600)
                    }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Favorites only", color = TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = showFavoritesOnly,
                            onCheckedChange = { vm.setFavoritesOnly(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Accent,
                                checkedTrackColor = Accent.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = SurfaceBorder
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (foregroundApp != null) {
            StaggeredReveal(3) {
                CurrentAppBanner(app = foregroundApp!!)
            }
            Spacer(Modifier.height(8.dp))
        }

        if (isScanning && apps.isEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(6) {
                    ShimmerCard(height = 60.dp)
                }
            }
        } else if (apps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No apps found", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.W600)
                    if (searchQuery.isNotEmpty()) {
                        Text("Try a different search", color = TextTertiary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                val grouped = apps.groupBy { it.category }
                grouped.forEach { (category, categoryApps) ->
                    item(key = "header_${category.name}") {
                        CategoryHeader(
                            category = category,
                            count = categoryApps.size,
                            enabledCount = categoryApps.count { it.packageName in enabledApps },
                            onEnableCategory = { vm.enableCategory(category) },
                            onDisableCategory = { vm.disableCategory(category) }
                        )
                    }
                    items(categoryApps, key = { it.packageName }) { app ->
                        AppCard(
                            app = app,
                            isFavorite = app.isFavorite,
                            isEnabled = app.packageName in enabledApps,
                            onToggleFavorite = { vm.toggleFavorite(app.packageName) },
                            onToggleEnabled = { vm.toggleEnabled(app.packageName, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    category: AppCategory,
    count: Int,
    enabledCount: Int,
    onEnableCategory: () -> Unit,
    onDisableCategory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Accent)
        )
        Spacer(Modifier.width(10.dp))
        Text(category.displayName, color = Accent, fontSize = 13.sp, fontWeight = FontWeight.W700, letterSpacing = 0.5.sp)
        Spacer(Modifier.width(8.dp))
        Text("$enabledCount/$count", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.W500)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Accent.copy(alpha = 0.12f))
                    .clickable(onClick = onEnableCategory)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("On", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.W600)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Warning.copy(alpha = 0.12f))
                    .clickable(onClick = onDisableCategory)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Off", color = Warning, fontSize = 11.sp, fontWeight = FontWeight.W600)
            }
        }
    }
}

@Composable
private fun AppCard(
    app: AppInfo,
    isFavorite: Boolean,
    isEnabled: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (isFavorite) Accent else TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500)
                Row {
                    Text(app.packageName, color = TextTertiary, fontSize = 10.sp, maxLines = 1)
                    if (app.versionName.isNotBlank()) {
                        Text(" v${app.versionName}", color = TextTertiary, fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            PremiumSwitch(
                checked = isEnabled,
                onCheckedChange = onToggleEnabled
            )
        }
    }
}

@Composable
private fun CurrentAppBanner(app: AppInfo) {
    GlassCard(
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    app.appName.first().uppercase(),
                    color = Accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W700
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Now Active", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.W500)
                Text(app.appName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.W600)
            }
            Text("●", color = Success, fontSize = 10.sp)
        }
    }
}
