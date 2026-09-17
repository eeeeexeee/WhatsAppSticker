package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StickerDatabase
import com.example.data.StickerItemEntity
import com.example.data.StickerPackEntity
import com.example.data.StickerPackWithCount
import com.example.data.StickerPackWithPreviews
import com.example.data.StickerRepository
import com.example.sticker.StickerEditConfig
import com.example.sticker.StickerImageProcessor
import com.example.sticker.WhatsAppStickerHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UiMessage {
    data class Success(val message: String) : UiMessage()
    data class Error(val message: String) : UiMessage()
    data class Info(val message: String) : UiMessage()
}

class StickerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StickerRepository
    init {
        val db = StickerDatabase.getDatabase(application)
        repository = StickerRepository(application, db.stickerDao())
        viewModelScope.launch {
            repository.initializeStarterPackIfEmpty()
        }
    }

    val packs: StateFlow<List<StickerPackWithPreviews>> = repository.allPacksWithPreviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPackId = MutableStateFlow<String?>(null)
    val selectedPackId: StateFlow<String?> = _selectedPackId.asStateFlow()

    val selectedPack: StateFlow<StickerPackEntity?> = _selectedPackId.flatMapLatest { id ->
        if (id != null) repository.getPack(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentStickers: StateFlow<List<StickerItemEntity>> = _selectedPackId.flatMapLatest { id ->
        if (id != null) repository.getStickersForPack(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Dialog & Editor States
    private val _showCreatePackDialog = MutableStateFlow(false)
    val showCreatePackDialog: StateFlow<Boolean> = _showCreatePackDialog.asStateFlow()

    private val _showEditPackDialog = MutableStateFlow(false)
    val showEditPackDialog: StateFlow<Boolean> = _showEditPackDialog.asStateFlow()

    private val _pendingBitmap = MutableStateFlow<Bitmap?>(null)
    val pendingBitmap: StateFlow<Bitmap?> = _pendingBitmap.asStateFlow()

    private val _editingExistingSticker = MutableStateFlow<StickerItemEntity?>(null)
    val editingExistingSticker: StateFlow<StickerItemEntity?> = _editingExistingSticker.asStateFlow()

    private val _editorConfig = MutableStateFlow(StickerEditConfig())
    val editorConfig: StateFlow<StickerEditConfig> = _editorConfig.asStateFlow()

    private val _simulatedChatSticker = MutableStateFlow<StickerItemEntity?>(null)
    val simulatedChatSticker: StateFlow<StickerItemEntity?> = _simulatedChatSticker.asStateFlow()

    private val _uiMessages = MutableSharedFlow<UiMessage>()
    val uiMessages: SharedFlow<UiMessage> = _uiMessages.asSharedFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun selectPack(packId: String?) {
        _selectedPackId.value = packId
    }

    fun openCreatePackDialog() {
        _showCreatePackDialog.value = true
    }

    fun closeCreatePackDialog() {
        _showCreatePackDialog.value = false
    }

    fun openEditPackDialog() {
        _showEditPackDialog.value = true
    }

    fun closeEditPackDialog() {
        _showEditPackDialog.value = false
    }

    fun openSimulatedChat(sticker: StickerItemEntity) {
        _simulatedChatSticker.value = sticker
    }

    fun closeSimulatedChat() {
        _simulatedChatSticker.value = null
    }

    fun createPack(name: String, publisher: String) {
        viewModelScope.launch {
            val newId = repository.createPack(name, publisher)
            _selectedPackId.value = newId
            _showCreatePackDialog.value = false
            _uiMessages.emit(UiMessage.Success("Collection created! Add at least 3 stickers for WhatsApp."))
        }
    }

    fun updatePack(name: String, publisher: String) {
        val packId = _selectedPackId.value ?: return
        viewModelScope.launch {
            repository.updatePackInfo(packId, name, publisher)
            _showEditPackDialog.value = false
            _uiMessages.emit(UiMessage.Success("Collection updated!"))
        }
    }

    fun deletePack(packId: String) {
        viewModelScope.launch {
            repository.deletePack(packId)
            if (_selectedPackId.value == packId) {
                _selectedPackId.value = null
            }
            _uiMessages.emit(UiMessage.Info("Collection deleted."))
        }
    }

    fun onImageSelectedForEditing(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            val bitmap = StickerImageProcessor.decodeBitmapFromUri(getApplication(), uri)
            _isProcessing.value = false
            if (bitmap != null) {
                _editingExistingSticker.value = null
                _pendingBitmap.value = bitmap
                _editorConfig.value = StickerEditConfig()
            } else {
                _uiMessages.emit(UiMessage.Error("Unable to decode this image format."))
            }
        }
    }

    fun startEditingExistingSticker(sticker: StickerItemEntity) {
        viewModelScope.launch {
            _isProcessing.value = true
            val file = repository.getStickerFile(sticker.packId, sticker.fileName)
            val bitmap = if (file.exists()) {
                android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            } else null
            _isProcessing.value = false

            if (bitmap != null) {
                _editingExistingSticker.value = sticker
                _pendingBitmap.value = bitmap
                _editorConfig.value = StickerEditConfig(emojis = sticker.emojis)
                _simulatedChatSticker.value = null
            } else {
                _uiMessages.emit(UiMessage.Error("Sticker image not found to edit."))
            }
        }
    }

    fun updateEditorConfig(config: StickerEditConfig) {
        _editorConfig.value = config
    }

    fun savePendingSticker(customBitmap: Bitmap? = null) {
        val packId = _selectedPackId.value ?: return
        val bitmap = customBitmap ?: _pendingBitmap.value ?: return
        val config = _editorConfig.value
        val existing = _editingExistingSticker.value

        viewModelScope.launch {
            _isProcessing.value = true
            try {
                if (existing != null) {
                    repository.updateStickerInPack(existing, bitmap, config)
                    _uiMessages.emit(UiMessage.Success("Sticker updated successfully!"))
                } else {
                    repository.addStickerToPack(packId, bitmap, config)
                    _uiMessages.emit(UiMessage.Success("Sticker converted and added to collection!"))
                }
                _pendingBitmap.value = null
                _editingExistingSticker.value = null
            } catch (e: Exception) {
                _uiMessages.emit(UiMessage.Error("Error saving sticker: ${e.localizedMessage}"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun cancelStickerEditor() {
        _pendingBitmap.value = null
        _editingExistingSticker.value = null
    }

    fun addQuickSampleSticker() {
        val packId = _selectedPackId.value ?: return
        val currentCount = currentStickers.value.size
        viewModelScope.launch {
            _isProcessing.value = true
            val sampleBmp = StickerImageProcessor.createSampleSticker(currentCount % 4)
            val config = StickerEditConfig(emojis = "🎉,✨")
            repository.addStickerToPack(packId, sampleBmp, config)
            _isProcessing.value = false
            _uiMessages.emit(UiMessage.Success("Sample sticker added!"))
        }
    }

    fun deleteSticker(sticker: StickerItemEntity) {
        viewModelScope.launch {
            repository.deleteSticker(sticker)
            _uiMessages.emit(UiMessage.Info("Sticker removed."))
        }
    }

    fun setAsTrayIcon(fileName: String) {
        val packId = _selectedPackId.value ?: return
        viewModelScope.launch {
            repository.setStickerAsTrayIcon(packId, fileName)
            _uiMessages.emit(UiMessage.Success("Tray icon updated for WhatsApp collection!"))
        }
    }

    fun getStickerFile(packId: String, fileName: String) = repository.getStickerFile(packId, fileName)
}
