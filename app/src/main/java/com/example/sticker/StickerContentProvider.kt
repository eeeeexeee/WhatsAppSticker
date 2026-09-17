package com.example.sticker

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.StickerDatabase
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException

class StickerContentProvider : ContentProvider() {

    companion object {
        private const val TAG = "StickerProvider"

        private const val METADATA = 1
        private const val METADATA_PACK = 2
        private const val STICKERS = 3
        private const val STICKERS_ASSET = 4

        // WhatsApp Sticker Provider Column Names
        const val STICKER_PACK_IDENTIFIER_IN_QUERY = "sticker_pack_identifier"
        const val STICKER_PACK_NAME_IN_QUERY = "sticker_pack_name"
        const val STICKER_PACK_PUBLISHER_IN_QUERY = "sticker_pack_publisher"
        const val STICKER_PACK_ICON_IN_QUERY = "sticker_pack_icon"
        const val ANDROID_APP_DOWNLOAD_LINK_IN_QUERY = "android_play_store_link"
        const val IOS_APP_DOWNLOAD_LINK_IN_QUERY = "ios_app_store_link"
        const val PUBLISHER_EMAIL = "publisher_email"
        const val PUBLISHER_WEBSITE = "publisher_website"
        const val PRIVACY_POLICY_WEBSITE = "privacy_policy_website"
        const val LICENSE_AGREEMENT_WEBSITE = "license_agreement_website"
        const val IMAGE_DATA_VERSION = "image_data_version"
        const val AVOID_CACHE = "avoid_cache"
        const val ANIMATED_STICKER_PACK = "animated_sticker_pack"

        const val STICKER_FILE_NAME_IN_QUERY = "sticker_file_name"
        const val STICKER_FILE_EMOJI_IN_QUERY = "sticker_pack_emojis"
        const val STICKER_PACK_ANIMATION_DATA = "sticker_pack_animation_data"

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        fun getAuthority(packageName: String): String {
            return "$packageName.stickercontentprovider"
        }
    }

    private lateinit var authority: String

    override fun onCreate(): Boolean {
        val appContext = context ?: return false
        authority = getAuthority(appContext.packageName)

        uriMatcher.addURI(authority, "metadata", METADATA)
        uriMatcher.addURI(authority, "metadata/*", METADATA_PACK)
        uriMatcher.addURI(authority, "stickers/*", STICKERS)
        uriMatcher.addURI(authority, "stickers_asset/*/*", STICKERS_ASSET)

        Log.d(TAG, "StickerContentProvider initialized with authority: $authority")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val appContext = context ?: return null
        val db = StickerDatabase.getDatabase(appContext)

        return when (uriMatcher.match(uri)) {
            METADATA -> {
                val matrixCursor = MatrixCursor(
                    arrayOf(
                        STICKER_PACK_IDENTIFIER_IN_QUERY,
                        STICKER_PACK_NAME_IN_QUERY,
                        STICKER_PACK_PUBLISHER_IN_QUERY,
                        STICKER_PACK_ICON_IN_QUERY,
                        ANDROID_APP_DOWNLOAD_LINK_IN_QUERY,
                        IOS_APP_DOWNLOAD_LINK_IN_QUERY,
                        PUBLISHER_EMAIL,
                        PUBLISHER_WEBSITE,
                        PRIVACY_POLICY_WEBSITE,
                        LICENSE_AGREEMENT_WEBSITE,
                        IMAGE_DATA_VERSION,
                        AVOID_CACHE,
                        ANIMATED_STICKER_PACK
                    )
                )

                runBlocking {
                    val packs = db.stickerDao().getAllPacksSync()
                    for (pack in packs) {
                        val stickers = db.stickerDao().getStickersForPackSync(pack.id)
                        // WhatsApp requires minimum 3 stickers and max 30
                        if (stickers.size >= 3) {
                            matrixCursor.addRow(
                                arrayOf(
                                    pack.id,
                                    pack.name,
                                    pack.publisher,
                                    pack.trayIconFileName,
                                    "", // play store link
                                    "", // ios link
                                    "", // email
                                    "", // website
                                    "", // privacy
                                    "", // license
                                    "1", // data version
                                    0, // avoid cache
                                    0 // static stickers
                                )
                            )
                        }
                    }
                }
                matrixCursor
            }

            STICKERS -> {
                val packId = uri.lastPathSegment ?: return null
                val matrixCursor = MatrixCursor(
                    arrayOf(
                        STICKER_FILE_NAME_IN_QUERY,
                        STICKER_FILE_EMOJI_IN_QUERY,
                        STICKER_PACK_ANIMATION_DATA
                    )
                )

                runBlocking {
                    val stickers = db.stickerDao().getStickersForPackSync(packId)
                    for (item in stickers) {
                        val emojiList = item.emojis.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val emojiString = if (emojiList.isNotEmpty()) emojiList.joinToString(",") else "✨"
                        matrixCursor.addRow(
                            arrayOf(
                                item.fileName,
                                emojiString,
                                ""
                            )
                        )
                    }
                }
                matrixCursor
            }

            else -> null
        }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val match = uriMatcher.match(uri)
        if (match == STICKERS_ASSET) {
            val segments = uri.pathSegments
            if (segments.size >= 3) {
                val packId = segments[1]
                val fileName = segments[2]
                val file = File(context?.filesDir, "stickers/$packId/$fileName")
                if (file.exists()) {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    return AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
                }
            }
        }
        throw FileNotFoundException("File not found for URI: $uri")
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            METADATA -> "vnd.android.cursor.dir/vnd.$authority.metadata"
            METADATA_PACK -> "vnd.android.cursor.item/vnd.$authority.metadata"
            STICKERS -> "vnd.android.cursor.dir/vnd.$authority.stickers"
            STICKERS_ASSET -> "image/webp"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
