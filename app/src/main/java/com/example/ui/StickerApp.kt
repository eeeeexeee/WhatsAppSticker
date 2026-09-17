package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StickerItemEntity
import com.example.data.StickerPackEntity
import com.example.data.StickerPackWithCount
import com.example.data.StickerPackWithPreviews
import com.example.ui.components.CollectionItemCard
import com.example.ui.components.CreatePackDialog
import com.example.ui.components.SimulatedChatPreviewDialog
import com.example.ui.components.StickerEditorDialog
import com.example.ui.components.StickerItemCard
import com.example.ui.components.WhatsAppStatusBanner
import com.example.ui.theme.WhatsAppDarkTeal
import com.example.ui.theme.WhatsAppGreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerApp(
    viewModel: StickerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val packs by viewModel.packs.collectAsStateWithLifecycle()
    val selectedPackId by viewModel.selectedPackId.collectAsStateWithLifecycle()
    val selectedPack by viewModel.selectedPack.collectAsStateWithLifecycle()
    val currentStickers by viewModel.currentStickers.collectAsStateWithLifecycle()

    val showCreateDialog by viewModel.showCreatePackDialog.collectAsStateWithLifecycle()
    val showEditDialog by viewModel.showEditPackDialog.collectAsStateWithLifecycle()
    val pendingBitmap by viewModel.pendingBitmap.collectAsStateWithLifecycle()
    val editorConfig by viewModel.editorConfig.collectAsStateWithLifecycle()
    val simulatedChatSticker by viewModel.simulatedChatSticker.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

    var packToDelete by remember { mutableStateOf<String?>(null) }
    var stickerToDelete by remember { mutableStateOf<StickerItemEntity?>(null) }

    // Visual Media Photo Picker for picking ANY image format
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageSelectedForEditing(uri)
        }
    }

    // Observe UI messages
    LaunchedEffect(Unit) {
        viewModel.uiMessages.collectLatest { msg ->
            when (msg) {
                is UiMessage.Success -> snackbarHostState.showSnackbar(msg.message)
                is UiMessage.Error -> snackbarHostState.showSnackbar("⚠️ ${msg.message}")
                is UiMessage.Info -> snackbarHostState.showSnackbar(msg.message)
            }
        }
    }

    // Auto-select first pack on tablet if none selected
    LaunchedEffect(packs) {
        if (selectedPackId == null && packs.isNotEmpty()) {
            // keep null on phones until user taps, but allow auto selection if on wide screen
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpanded = maxWidth >= 600.dp

            if (isExpanded) {
                // TABLET / EXPANDED: Canonical 2-Pane Layout
                TabletTwoPaneLayout(
                    packs = packs,
                    selectedPackId = selectedPackId ?: packs.firstOrNull()?.id,
                    selectedPack = selectedPack ?: packs.firstOrNull()?.let {
                        StickerPackEntity(it.id, it.name, it.publisher, it.trayIconFileName)
                    },
                    currentStickers = currentStickers,
                    onSelectPack = { viewModel.selectPack(it) },
                    onCreatePackClick = { viewModel.openCreatePackDialog() },
                    onEditPackClick = { viewModel.openEditPackDialog() },
                    onDeletePackClick = { packToDelete = it },
                    onAddImageClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onAddSampleClick = { viewModel.addQuickSampleSticker() },
                    onStickerClick = { viewModel.openSimulatedChat(it) },
                    onDeleteStickerClick = { stickerToDelete = it },
                    getStickerFile = { packId, fileName -> viewModel.getStickerFile(packId, fileName) }
                )
            } else {
                // PHONE / COMPACT: Animated Single Pane Navigation
                AnimatedContent(
                    targetState = selectedPackId,
                    transitionSpec = {
                        if (targetState != null) {
                            (slideInHorizontally { width -> width / 4 } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width / 4 } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width / 4 } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width / 4 } + fadeOut()
                            )
                        }
                    },
                    label = "phone_screen_transition"
                ) { currentPackId ->
                    if (currentPackId == null) {
                        PhonePacksListScreen(
                            packs = packs,
                            onSelectPack = { viewModel.selectPack(it) },
                            onCreatePackClick = { viewModel.openCreatePackDialog() },
                            onDeletePackClick = { packToDelete = it },
                            getStickerFile = { packId, fileName -> viewModel.getStickerFile(packId, fileName) }
                        )
                    } else {
                        PhonePackDetailScreen(
                            pack = selectedPack,
                            stickers = currentStickers,
                            onBack = { viewModel.selectPack(null) },
                            onEditPackClick = { viewModel.openEditPackDialog() },
                            onDeletePackClick = { selectedPackId?.let { packToDelete = it } },
                            onAddImageClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onAddSampleClick = { viewModel.addQuickSampleSticker() },
                            onStickerClick = { viewModel.openSimulatedChat(it) },
                            onDeleteStickerClick = { stickerToDelete = it },
                            getStickerFile = { packId, fileName -> viewModel.getStickerFile(packId, fileName) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreatePackDialog(
            isEditing = false,
            onConfirm = { name, publisher -> viewModel.createPack(name, publisher) },
            onDismiss = { viewModel.closeCreatePackDialog() }
        )
    }

    if (showEditDialog && selectedPack != null) {
        CreatePackDialog(
            initialName = selectedPack!!.name,
            initialPublisher = selectedPack!!.publisher,
            isEditing = true,
            onConfirm = { name, publisher -> viewModel.updatePack(name, publisher) },
            onDismiss = { viewModel.closeEditPackDialog() }
        )
    }

    if (pendingBitmap != null) {
        StickerEditorDialog(
            sourceBitmap = pendingBitmap!!,
            initialConfig = editorConfig,
            isProcessing = isProcessing,
            onSave = { editedBitmap, updatedConfig ->
                viewModel.updateEditorConfig(updatedConfig)
                viewModel.savePendingSticker(editedBitmap)
            },
            onDismiss = { viewModel.cancelStickerEditor() }
        )
    }

    if (simulatedChatSticker != null) {
        val file = viewModel.getStickerFile(
            simulatedChatSticker!!.packId,
            simulatedChatSticker!!.fileName
        )
        SimulatedChatPreviewDialog(
            sticker = simulatedChatSticker!!,
            stickerFile = file,
            onSetTrayIcon = { viewModel.setAsTrayIcon(simulatedChatSticker!!.fileName) },
            onEditSticker = { viewModel.startEditingExistingSticker(simulatedChatSticker!!) },
            onDismiss = { viewModel.closeSimulatedChat() }
        )
    }

    // Delete Collection Confirmation Dialog
    if (packToDelete != null) {
        AlertDialog(
            onDismissRequest = { packToDelete = null },
            title = { Text("Delete Collection?") },
            text = { Text("All stickers in this collection will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = packToDelete
                        packToDelete = null
                        if (id != null) viewModel.deletePack(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { packToDelete = null }) {
                    Text("Cancel", maxLines = 1, softWrap = false)
                }
            }
        )
    }

    // Delete Sticker Confirmation Dialog
    if (stickerToDelete != null) {
        AlertDialog(
            onDismissRequest = { stickerToDelete = null },
            title = { Text("Delete Sticker?") },
            text = { Text("Remove this sticker from the collection?") },
            confirmButton = {
                Button(
                    onClick = {
                        val s = stickerToDelete
                        stickerToDelete = null
                        if (s != null) viewModel.deleteSticker(s)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove", maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { stickerToDelete = null }) {
                    Text("Cancel", maxLines = 1, softWrap = false)
                }
            }
        )
    }
}

/**
 * Tablet / Wide Canonical 2-Pane Layout
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TabletTwoPaneLayout(
    packs: List<StickerPackWithPreviews>,
    selectedPackId: String?,
    selectedPack: StickerPackEntity?,
    currentStickers: List<StickerItemEntity>,
    onSelectPack: (String) -> Unit,
    onCreatePackClick: () -> Unit,
    onEditPackClick: () -> Unit,
    onDeletePackClick: (String) -> Unit,
    onAddImageClick: () -> Unit,
    onAddSampleClick: () -> Unit,
    onStickerClick: (StickerItemEntity) -> Unit,
    onDeleteStickerClick: (StickerItemEntity) -> Unit,
    getStickerFile: (String, String) -> File
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Pane: Collections List (360dp)
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Collections",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${packs.size} WhatsApp Packs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = onCreatePackClick,
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    modifier = Modifier.testTag("tablet_new_collection_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New Pack",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (packs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sticker collections yet.\nTap 'New Pack' to start!",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(packs, key = { it.id }) { pack ->
                        val trayFile = getStickerFile(pack.id, pack.trayIconFileName)
                        CollectionItemCard(
                            pack = pack,
                            trayIconFile = trayFile,
                            isSelected = pack.id == selectedPackId,
                            onClick = { onSelectPack(pack.id) },
                            onDelete = { onDeletePackClick(pack.id) },
                            getStickerFile = getStickerFile
                        )
                    }
                }
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // Right Pane: Active Collection Details & Stickers Grid
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
        ) {
            if (selectedPack != null) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = selectedPack.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Created by ${selectedPack.publisher}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Action buttons (FlowRow wrapped)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onEditPackClick) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Pack", maxLines = 1, softWrap = false)
                        }

                        Button(
                            onClick = onAddImageClick,
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            modifier = Modifier.testTag("tablet_add_photo_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Convert Image",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // WhatsApp Status Banner
                WhatsAppStatusBanner(
                    packId = selectedPack.id,
                    packName = selectedPack.name,
                    stickerCount = currentStickers.size,
                    onAddStickersClick = onAddImageClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Grid of stickers
                if (currentStickers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No stickers in this collection yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Convert any image format into 512x512 WhatsApp stickers",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onAddImageClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick from Gallery", color = Color.White, maxLines = 1, softWrap = false)
                                }
                                OutlinedButton(onClick = onAddSampleClick) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Quick Sample", maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(currentStickers, key = { it.id }) { sticker ->
                            val stickerFile = getStickerFile(sticker.packId, sticker.fileName)
                            StickerItemCard(
                                sticker = sticker,
                                stickerFile = stickerFile,
                                onClick = { onStickerClick(sticker) },
                                onDelete = { onDeleteStickerClick(sticker) }
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a collection from the left pane or create a new one.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Phone: Collections Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhonePacksListScreen(
    packs: List<StickerPackWithPreviews>,
    onSelectPack: (String) -> Unit,
    onCreatePackClick: () -> Unit,
    onDeletePackClick: (String) -> Unit,
    getStickerFile: (String, String) -> File
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(WhatsAppGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sticker Maker",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WhatsAppDarkTeal,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreatePackClick,
                containerColor = WhatsAppGreen,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Pack", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
                modifier = Modifier.testTag("create_pack_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "WhatsApp Collections",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create separate collections and export stickers to WhatsApp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (packs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Collections,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Sticker Collections",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button below to create your first WhatsApp sticker collection!",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(packs, key = { it.id }) { pack ->
                        val trayFile = getStickerFile(pack.id, pack.trayIconFileName)
                        CollectionItemCard(
                            pack = pack,
                            trayIconFile = trayFile,
                            isSelected = false,
                            onClick = { onSelectPack(pack.id) },
                            onDelete = { onDeletePackClick(pack.id) },
                            getStickerFile = getStickerFile
                        )
                    }
                }
            }
        }
    }
}

/**
 * Phone: Collection Detail Screen
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhonePackDetailScreen(
    pack: StickerPackEntity?,
    stickers: List<StickerItemEntity>,
    onBack: () -> Unit,
    onEditPackClick: () -> Unit,
    onDeletePackClick: () -> Unit,
    onAddImageClick: () -> Unit,
    onAddSampleClick: () -> Unit,
    onStickerClick: (StickerItemEntity) -> Unit,
    onDeleteStickerClick: (StickerItemEntity) -> Unit,
    getStickerFile: (String, String) -> File
) {
    if (pack == null) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = pack.name, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(text = "by ${pack.publisher}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onEditPackClick, modifier = Modifier.testTag("edit_pack_button")) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Collection", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WhatsAppDarkTeal,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddImageClick,
                containerColor = WhatsAppGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("phone_add_photo_fab")
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Sticker")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // WhatsApp Status Banner with Add to WhatsApp button
            WhatsAppStatusBanner(
                packId = pack.id,
                packName = pack.name,
                stickerCount = stickers.size,
                onAddStickersClick = onAddImageClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action options (FlowRow ensures buttons wrap properly)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onAddSampleClick) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Quick Sample", maxLines = 1, softWrap = false)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stickers Grid
            if (stickers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No stickers added yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the green button or pick an image to create 512x512 stickers.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(stickers, key = { it.id }) { sticker ->
                        val file = getStickerFile(sticker.packId, sticker.fileName)
                        StickerItemCard(
                            sticker = sticker,
                            stickerFile = file,
                            onClick = { onStickerClick(sticker) },
                            onDelete = { onDeleteStickerClick(sticker) }
                        )
                    }
                }
            }
        }
    }
}
