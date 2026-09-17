package com.example.sticker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object WhatsAppStickerHelper {

    const val EXTRA_STICKER_PACK_ID = "sticker_pack_id"
    const val EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority"
    const val EXTRA_STICKER_PACK_NAME = "sticker_pack_name"

    const val ACTION_ENABLE_STICKER_PACK = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
    const val PACKAGE_WHATSAPP = "com.whatsapp"
    const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    sealed class WhatsAppStatus {
        data class Ready(val packages: List<String>) : WhatsAppStatus()
        object NotInstalled : WhatsAppStatus()
    }

    fun getInstalledWhatsAppPackages(context: Context): List<String> {
        val pm = context.packageManager
        val installed = mutableListOf<String>()

        try {
            pm.getPackageInfo(PACKAGE_WHATSAPP, PackageManager.GET_ACTIVITIES)
            installed.add(PACKAGE_WHATSAPP)
        } catch (_: PackageManager.NameNotFoundException) {}

        try {
            pm.getPackageInfo(PACKAGE_WHATSAPP_BUSINESS, PackageManager.GET_ACTIVITIES)
            installed.add(PACKAGE_WHATSAPP_BUSINESS)
        } catch (_: PackageManager.NameNotFoundException) {}

        return installed
    }

    fun isWhatsAppInstalled(context: Context): Boolean {
        return getInstalledWhatsAppPackages(context).isNotEmpty()
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class TooFewStickers(val currentCount: Int) : ValidationResult()
        data class TooManyStickers(val currentCount: Int) : ValidationResult()
        data class MissingTrayIcon(val reason: String) : ValidationResult()
    }

    fun validatePackForWhatsApp(stickerCount: Int, hasTrayIcon: Boolean): ValidationResult {
        if (!hasTrayIcon) {
            return ValidationResult.MissingTrayIcon("Collection must have a tray icon.")
        }
        if (stickerCount < 3) {
            return ValidationResult.TooFewStickers(stickerCount)
        }
        if (stickerCount > 30) {
            return ValidationResult.TooManyStickers(stickerCount)
        }
        return ValidationResult.Valid
    }

    fun createAddToWhatsAppIntent(
        context: Context,
        packId: String,
        packName: String,
        targetPackage: String? = null
    ): Intent {
        val authority = StickerContentProvider.getAuthority(context.packageName)
        val intent = Intent(ACTION_ENABLE_STICKER_PACK).apply {
            putExtra(EXTRA_STICKER_PACK_ID, packId)
            putExtra(EXTRA_STICKER_PACK_AUTHORITY, authority)
            putExtra(EXTRA_STICKER_PACK_NAME, packName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            targetPackage?.let { setPackage(it) }
        }
        return intent
    }

    /**
     * Share sticker as WebP image via system share sheet
     */
    fun createShareStickerIntent(context: Context, packId: String, fileName: String): Intent? {
        val file = File(context.filesDir, "stickers/$packId/$fileName")
        if (!file.exists()) return null

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "image/webp"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
