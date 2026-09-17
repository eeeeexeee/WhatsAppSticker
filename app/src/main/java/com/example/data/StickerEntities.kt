package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sticker_packs")
data class StickerPackEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val publisher: String,
    @ColumnInfo(name = "tray_icon_file_name")
    val trayIconFileName: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sticker_items",
    foreignKeys = [
        ForeignKey(
            entity = StickerPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["pack_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pack_id"])]
)
data class StickerItemEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "pack_id")
    val packId: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    val emojis: String = "✨",
    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

data class StickerPackWithCount(
    val id: String,
    val name: String,
    val publisher: String,
    val trayIconFileName: String,
    val createdAt: Long,
    val stickerCount: Int
)

data class StickerPackWithPreviews(
    @androidx.room.Embedded val pack: StickerPackEntity,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "pack_id"
    )
    val stickers: List<StickerItemEntity>
) {
    val id: String get() = pack.id
    val name: String get() = pack.name
    val publisher: String get() = pack.publisher
    val trayIconFileName: String get() = pack.trayIconFileName
    val createdAt: Long get() = pack.createdAt
    val stickerCount: Int get() = stickers.size
    val previewStickers: List<StickerItemEntity> get() = stickers.sortedBy { it.orderIndex }.take(4)
}
