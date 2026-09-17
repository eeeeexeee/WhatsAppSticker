package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StickerDao {

    @Query("SELECT p.id, p.name, p.publisher, p.tray_icon_file_name AS trayIconFileName, p.created_at AS createdAt, COUNT(s.id) AS stickerCount FROM sticker_packs p LEFT JOIN sticker_items s ON p.id = s.pack_id GROUP BY p.id ORDER BY p.created_at DESC")
    fun getAllPacksWithCount(): Flow<List<StickerPackWithCount>>

    @Transaction
    @Query("SELECT * FROM sticker_packs ORDER BY created_at DESC")
    fun getAllPacksWithPreviews(): Flow<List<StickerPackWithPreviews>>

    @Query("SELECT * FROM sticker_packs ORDER BY created_at DESC")
    fun getAllPacks(): Flow<List<StickerPackEntity>>

    @Query("SELECT * FROM sticker_packs WHERE id = :packId LIMIT 1")
    suspend fun getPackById(packId: String): StickerPackEntity?

    @Query("SELECT * FROM sticker_packs WHERE id = :packId LIMIT 1")
    fun getPackFlow(packId: String): Flow<StickerPackEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPack(pack: StickerPackEntity)

    @Update
    suspend fun updatePack(pack: StickerPackEntity)

    @Delete
    suspend fun deletePack(pack: StickerPackEntity)

    @Query("DELETE FROM sticker_packs WHERE id = :packId")
    suspend fun deletePackById(packId: String)

    // Sticker Items
    @Query("SELECT * FROM sticker_items WHERE pack_id = :packId ORDER BY order_index ASC, created_at ASC")
    fun getStickersForPack(packId: String): Flow<List<StickerItemEntity>>

    @Query("SELECT * FROM sticker_items WHERE pack_id = :packId ORDER BY order_index ASC, created_at ASC")
    suspend fun getStickersForPackSync(packId: String): List<StickerItemEntity>

    @Query("SELECT * FROM sticker_packs")
    suspend fun getAllPacksSync(): List<StickerPackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSticker(sticker: StickerItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickers(stickers: List<StickerItemEntity>)

    @Delete
    suspend fun deleteSticker(sticker: StickerItemEntity)

    @Query("DELETE FROM sticker_items WHERE id = :stickerId")
    suspend fun deleteStickerById(stickerId: String)

    @Query("SELECT COUNT(*) FROM sticker_packs")
    suspend fun getPackCount(): Int
}
