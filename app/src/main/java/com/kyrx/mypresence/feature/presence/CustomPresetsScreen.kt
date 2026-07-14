package com.kyrx.mypresence.feature.presence

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyrx.mypresence.domain.model.CustomRpcPreset
import com.kyrx.mypresence.ui.components.GlassCard
import com.kyrx.mypresence.ui.components.PremiumTextField
import com.kyrx.mypresence.ui.theme.Accent
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Dimens
import com.kyrx.mypresence.ui.theme.Error
import com.kyrx.mypresence.ui.theme.Success
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPresetsScreen(
    viewModel: CustomPresetsViewModel = hiltViewModel(),
    onNavigateToEditor: (CustomRpcPreset?) -> Unit,
    onNavigateBack: () -> Unit
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val showCreateDialog = remember { mutableStateOf(false) }
    val newPresetName = remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            TopAppBar(
                title = { Text("Custom Presets", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog.value = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create new preset",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = Dimens.lg, vertical = Dimens.md),
                verticalArrangement = Arrangement.spacedBy(Dimens.md)
            ) {
                items(presets) { preset ->
                    PresetCard(
                        preset = preset,
                        onEdit = { onNavigateToEditor(preset) },
                        onDuplicate = { viewModel.duplicatePreset(preset) },
                        onDelete = { viewModel.deletePreset(preset.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(preset.id) },
                        onApply = { viewModel.applyPreset(preset); onNavigateToEditor(preset) }
                    )
                }
            }
        }

        if (presets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimens.lg)
                    .wrapContentSize(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "No Custom Presets",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Create your first custom Rich Presence preset",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showCreateDialog.value) {
        AlertDialog(
            onDismissRequest = { showCreateDialog.value = false },
            title = { Text("Create New Preset") },
            text = {
                PremiumTextField(
                    value = newPresetName.value,
                    onValueChange = { newPresetName.value = it },
                    label = "Preset Name",
                    placeholder = "e.g., Gaming, Studying, Working",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newPresetName.value.isNotBlank()) {
                                viewModel.createPreset(newPresetName.value.trim())
                                newPresetName.value = ""
                                showCreateDialog.value = false
                            }
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPresetName.value.isNotBlank()) {
                            viewModel.createPreset(newPresetName.value.trim())
                            newPresetName.value = ""
                            showCreateDialog.value = false
                        }
                    }
                ) {
                    Text("Create", color = Accent)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDialog.value = false }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PresetCard(
    preset: CustomRpcPreset,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onApply: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        contentPadding = PaddingValues(Dimens.lg)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (preset.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (preset.isFavorite) "Favorited" else "Not favorited",
                    tint = if (preset.isFavorite) Accent else TextTertiary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onToggleFavorite)
                        .padding(end = Dimens.sm)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.W600
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Type: ${activityTypeLabel(preset.activityType)} | Status: ${preset.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (preset.details.isNotBlank() || preset.state.isNotBlank()) {
                        Text(
                            text = "${preset.details.ifBlank { "" }} ${preset.state.ifBlank { "" }}".trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onApply) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Apply this preset",
                        tint = Success
                    )
                }
            }

            Spacer(Modifier.height(Dimens.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDuplicate) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Duplicate preset",
                        tint = TextTertiary
                    )
                }
                Spacer(Modifier.width(Dimens.sm))
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit preset",
                        tint = TextTertiary
                    )
                }
                Spacer(Modifier.width(Dimens.sm))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete preset",
                        tint = Error
                    )
                }
            }
        }
    }
}

private fun activityTypeLabel(type: Int): String = when (type) {
    0 -> "Playing"
    1 -> "Streaming"
    2 -> "Listening"
    3 -> "Watching"
    5 -> "Competing"
    else -> "Activity($type)"
}
