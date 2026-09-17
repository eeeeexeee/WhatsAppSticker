package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.sticker.StickerEditConfig
import com.example.sticker.StickerImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class StickerRepository(
    private val context: Context,
    private val dao: StickerDao
) {
    val allPacksWithCount: Flow<List<StickerPackWithCount>> = dao.getAllPacksWithCount()
    val allPacksWithPreviews: Flow<List<StickerPackWithPreviews>> = dao.getAllPacksWithPreviews()

    fun getStickersForPack(packId: String): Flow<List<StickerItemEntity>> {
        return dao.getStickersForPack(packId)
    }

    fun getPack(packId: String): Flow<StickerPackEntity?> {
        return dao.getPackFlow(packId)
    }

    suspend fun initializeStarterPackIfEmpty() = withContext(Dispatchers.IO) {
        val count = dao.getPackCount()
        if (count == 0) {
            val starterPackId = "starter_reactions"
            val trayIconName = "tray_icon.webp"

            // 1. Generate 4 sample stickers
            val sampleStickers = mutableListOf<StickerItemEntity>()
            for (i in 0..3) {
                val sampleBmp = StickerImageProcessor.createSampleSticker(i)
                val fileName = "starter_$i.webp"
                val webpBytes = StickerImageProcessor.compressToWebPBytes(sampleBmp)
                StickerImageProcessor.saveStickerFile(context, starterPackId, fileName, webpBytes)

                val emojis = when (i) {
                    0 -> "👍,🎉"
                    1 -> "😎,🔥"
                    2 -> "🎉,🥳"
                    else -> "❤️,✨"
                }

                sampleStickers.add(
                    StickerItemEntity(
                        id = UUID.randomUUID().toString(),
                        packId = starterPackId,
                        fileName = fileName,
                        emojis = emojis,
                        orderIndex = i
                    )
                )

                if (i == 0) {
                    val trayBytes = StickerImageProcessor.generateTrayIconBytes(sampleBmp)
                    StickerImageProcessor.saveStickerFile(context, starterPackId, trayIconName, trayBytes)
                }
            }

            val pack = StickerPackEntity(
                id = starterPackId,
                name = "Expressive Reactions",
                publisher = "Sticker Studio",
                trayIconFileName = trayIconName
            )

            dao.insertPack(pack)
            dao.insertStickers(sampleStickers)
        }
    }

    suspend fun createPack(name: String, publisher: String): String = withContext(Dispatchers.IO) {
        val packId = UUID.randomUUID().toString().replace("-", "").take(16)
        val trayIconName = "tray_icon.webp"

        // Generate a default tray icon
        val defaultTrayBmp = StickerImageProcessor.createSampleSticker(0)
        val trayBytes = StickerImageProcessor.generateTrayIconBytes(defaultTrayBmp)
        StickerImageProcessor.saveStickerFile(context, packId, trayIconName, trayBytes)

        val pack = StickerPackEntity(
            id = packId,
            name = name.trim().ifEmpty { "My Stickers" },
            publisher = publisher.trim().ifEmpty { "Creator" },
            trayIconFileName = trayIconName
        )
        dao.insertPack(pack)
        packId
    }

    suspend fun updatePackInfo(packId: String, name: String, publisher: String) = withContext(Dispatchers.IO) {
        val pack = dao.getPackById(packId) ?: return@withContext
        val updated = pack.copy(
            name = name.trim().ifEmpty { pack.name },
            publisher = publisher.trim().ifEmpty { pack.publisher }
        )
        dao.updatePack(updated)
    }

    suspend fun addStickerToPack(
        packId: String,
        sourceBitmap: Bitmap,
        config: StickerEditConfig
    ): StickerItemEntity = withContext(Dispatchers.IO) {
        val processedBitmap = StickerImageProcessor.processSticker(sourceBitmap, config)
        val fileName = "sticker_${System.currentTimeMillis()}.webp"
        val webpBytes = StickerImageProcessor.compressToWebPBytes(processedBitmap)

        StickerImageProcessor.saveStickerFile(context, packId, fileName, webpBytes)

        // Also update tray icon if it's the first sticker or default
        val pack = dao.getPackById(packId)
        if (pack != null) {
            val trayBytes = StickerImageProcessor.generateTrayIconBytes(processedBitmap)
            StickerImageProcessor.saveStickerFile(context, packId, pack.trayIconFileName, trayBytes)
        }

        val sticker = StickerItemEntity(
            id = UUID.randomUUID().toString(),
            packId = packId,
            fileName = fileName,
            emojis = config.emojis.ifBlank { "✨" }
        )
        dao.insertSticker(sticker)
        sticker
    }

    suspend fun updateStickerInPack(
        sticker: StickerItemEntity,
        sourceBitmap: Bitmap,
        config: StickerEditConfig
    ): StickerItemEntity = withContext(Dispatchers.IO) {
        val processedBitmap = StickerImageProcessor.processSticker(sourceBitmap, config)
        val webpBytes = StickerImageProcessor.compressToWebPBytes(processedBitmap)

        StickerImageProcessor.saveStickerFile(context, sticker.packId, sticker.fileName, webpBytes)

        val updatedSticker = sticker.copy(
            emojis = config.emojis.ifBlank { sticker.emojis }
        )
        dao.insertSticker(updatedSticker)
        updatedSticker
    }

    suspend fun addStickerFromUri(
        packId: String,
        uri: Uri,
        config: StickerEditConfig
    ): StickerItemEntity? = withContext(Dispatchers.IO) {
        val bitmap = StickerImageProcessor.decodeBitmapFromUri(context, uri) ?: return@withContext null
        addStickerToPack(packId, bitmap, config)
    }

    suspend fun setStickerAsTrayIcon(packId: String, fileName: String) = withContext(Dispatchers.IO) {
        val pack = dao.getPackById(packId) ?: return@withContext
        val stickerFile = File(context.filesDir, "stickers/$packId/$fileName")
        if (stickerFile.exists()) {
            val stickerBitmap = android.graphics.BitmapFactory.decodeFile(stickerFile.absolutePath)
            if (stickerBitmap != null) {
                val trayBytes = StickerImageProcessor.generateTrayIconBytes(stickerBitmap)
                StickerImageProcessor.saveStickerFile(context, packId, pack.trayIconFileName, trayBytes)
            }
        }
    }

    suspend fun deleteSticker(sticker: StickerItemEntity) = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "stickers/${sticker.packId}/${sticker.fileName}")
        if (file.exists()) {
            file.delete()
        }
        dao.deleteSticker(sticker)
    }

    suspend fun deletePack(packId: String) = withContext(Dispatchers.IO) {
        val packDir = File(context.filesDir, "stickers/$packId")
        if (packDir.exists()) {
            packDir.deleteRecursively()
        }
        dao.deletePackById(packId)
    }

    fun getStickerFile(packId: String, fileName: String): File {
        return File(context.filesDir, "stickers/$packId/$fileName")
    }
}
