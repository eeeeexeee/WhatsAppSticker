package com.example.sticker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class StickerShape {
    ORIGINAL,
    CIRCLE,
    ROUNDED_RECT,
    HEART,
    STAR,
    HEXAGON
}

enum class StickerBorder {
    NONE,
    WHITE,
    WHATSAPP_GREEN,
    GOLD,
    CORAL,
    CYAN
}

data class StickerEditConfig(
    val shape: StickerShape = StickerShape.ORIGINAL,
    val border: StickerBorder = StickerBorder.WHITE,
    val removeSolidBackground: Boolean = false,
    val textOverlay: String = "",
    val emojis: String = "✨"
)

object StickerImageProcessor {

    const val STICKER_SIZE = 512
    const val TRAY_ICON_SIZE = 96
    const val MAX_STICKER_BYTES = 98 * 1024 // 98 KB to stay strictly under WhatsApp's 100KB limit
    const val CONTENT_MARGIN = 16 // 16px margin on each side -> 480x480 content area

    /**
     * Decodes any image format (JPEG, PNG, WEBP, GIF, BMP, HEIF) from a content Uri
     */
    fun decodeBitmapFromUri(context: Context, uri: Uri, targetSize: Int = 1024): Bitmap? {
        return try {
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val maxDim = max(options.outWidth, options.outHeight)
            var inSampleSize = 1
            if (maxDim > targetSize) {
                while (maxDim / (inSampleSize * 2) >= targetSize) {
                    inSampleSize *= 2
                }
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Processes any raw bitmap into a 512x512 WhatsApp-compliant sticker bitmap with styling.
     */
    fun processSticker(
        sourceBitmap: Bitmap,
        config: StickerEditConfig
    ): Bitmap {
        var baseBitmap = sourceBitmap

        // 1. If removeSolidBackground is requested, key out near-white or corners
        if (config.removeSolidBackground) {
            baseBitmap = removeCornerBackground(baseBitmap)
        }

        // 2. Crop/mask into chosen shape
        val shapedBitmap = applyShapeMask(baseBitmap, config.shape)

        // 3. Fit shapedBitmap into 480x480 inner box (to leave 16px transparent margin on 512x512)
        val contentBoxSize = STICKER_SIZE - (CONTENT_MARGIN * 2) // 480
        val scale = min(
            contentBoxSize.toFloat() / shapedBitmap.width,
            contentBoxSize.toFloat() / shapedBitmap.height
        )
        val scaledWidth = (shapedBitmap.width * scale).toInt()
        val scaledHeight = (shapedBitmap.height * scale).toInt()
        val scaledContent = Bitmap.createScaledBitmap(shapedBitmap, scaledWidth, scaledHeight, true)

        // 4. Create final 512x512 transparent canvas
        val finalSticker = Bitmap.createBitmap(STICKER_SIZE, STICKER_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalSticker)

        val posX = (STICKER_SIZE - scaledWidth) / 2f
        val posY = (STICKER_SIZE - scaledHeight) / 2f

        // 5. Draw border if configured (Die-cut sticker outline)
        if (config.border != StickerBorder.NONE) {
            val borderColor: Int = when (config.border) {
                StickerBorder.WHITE -> Color.WHITE
                StickerBorder.WHATSAPP_GREEN -> 0xFF25D366.toInt()
                StickerBorder.GOLD -> 0xFFFFD700.toInt()
                StickerBorder.CORAL -> 0xFFFF5722.toInt()
                StickerBorder.CYAN -> 0xFF00E5FF.toInt()
                StickerBorder.NONE -> Color.TRANSPARENT
            }
            drawStickerBorder(canvas, scaledContent, posX, posY, borderColor, strokeWidth = 10f)
        }

        // 6. Draw the actual image
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(scaledContent, posX, posY, paint)

        // 7. Draw text overlay if provided
        if (config.textOverlay.isNotBlank()) {
            drawTextCaption(canvas, config.textOverlay)
        }

        return finalSticker
    }

    private fun applyShapeMask(source: Bitmap, shape: StickerShape): Bitmap {
        if (shape == StickerShape.ORIGINAL) {
            return source
        }

        val dim = min(source.width, source.height)
        val squareSource = if (source.width != source.height) {
            val x = (source.width - dim) / 2
            val y = (source.height - dim) / 2
            Bitmap.createBitmap(source, x, y, dim, dim)
        } else {
            source
        }

        val output = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }

        val path = Path()
        val rect = RectF(0f, 0f, dim.toFloat(), dim.toFloat())

        when (shape) {
            StickerShape.CIRCLE -> {
                path.addOval(rect, Path.Direction.CW)
            }
            StickerShape.ROUNDED_RECT -> {
                val cornerRadius = dim * 0.22f
                path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            }
            StickerShape.HEART -> {
                buildHeartPath(path, rect)
            }
            StickerShape.STAR -> {
                buildStarPath(path, rect)
            }
            StickerShape.HEXAGON -> {
                buildHexagonPath(path, rect)
            }
            StickerShape.ORIGINAL -> {}
        }

        canvas.drawPath(path, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squareSource, 0f, 0f, paint)

        return output
    }

    private fun buildHeartPath(path: Path, rect: RectF) {
        val width = rect.width()
        val height = rect.height()
        path.moveTo(rect.left + width / 2f, rect.top + height * 0.85f)
        path.cubicTo(
            rect.left + width * 0.1f, rect.top + height * 0.6f,
            rect.left, rect.top + height * 0.25f,
            rect.left + width * 0.28f, rect.top + height * 0.1f
        )
        path.cubicTo(
            rect.left + width * 0.45f, rect.top + height * 0.1f,
            rect.left + width / 2f, rect.top + height * 0.32f,
            rect.left + width / 2f, rect.top + height * 0.32f
        )
        path.cubicTo(
            rect.left + width / 2f, rect.top + height * 0.32f,
            rect.left + width * 0.55f, rect.top + height * 0.1f,
            rect.left + width * 0.72f, rect.top + height * 0.1f
        )
        path.cubicTo(
            rect.right, rect.top + height * 0.25f,
            rect.left + width * 0.9f, rect.top + height * 0.6f,
            rect.left + width / 2f, rect.top + height * 0.85f
        )
        path.close()
    }

    private fun buildStarPath(path: Path, rect: RectF) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val rOuter = rect.width() / 2f
        val rInner = rOuter * 0.45f
        val points = 5
        var angle = -Math.PI / 2

        path.moveTo(
            (cx + rOuter * cos(angle)).toFloat(),
            (cy + rOuter * sin(angle)).toFloat()
        )

        val step = Math.PI / points
        for (i in 1 until points * 2) {
            angle += step
            val r = if (i % 2 == 1) rInner else rOuter
            path.lineTo(
                (cx + r * cos(angle)).toFloat(),
                (cy + r * sin(angle)).toFloat()
            )
        }
        path.close()
    }

    private fun buildHexagonPath(path: Path, rect: RectF) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val r = rect.width() / 2f
        for (i in 0 until 6) {
            val angle = Math.toRadians((60 * i - 30).toDouble())
            val x = (cx + r * cos(angle)).toFloat()
            val y = (cy + r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    private fun drawStickerBorder(
        canvas: Canvas,
        content: Bitmap,
        x: Float,
        y: Float,
        borderColor: Int,
        strokeWidth: Float
    ) {
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(borderColor, PorterDuff.Mode.SRC_IN)
        }

        // Draw dilated silhouette offset in 8 directions to form a smooth outline
        val offset = strokeWidth
        val offsets = arrayOf(
            Pair(-offset, 0f), Pair(offset, 0f), Pair(0f, -offset), Pair(0f, offset),
            Pair(-offset * 0.7f, -offset * 0.7f), Pair(offset * 0.7f, -offset * 0.7f),
            Pair(-offset * 0.7f, offset * 0.7f), Pair(offset * 0.7f, offset * 0.7f)
        )
        for (off in offsets) {
            canvas.drawBitmap(content, x + off.first, y + off.second, borderPaint)
        }
    }

    private fun drawTextCaption(canvas: Canvas, text: String) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeWidth = 8f
            strokeJoin = Paint.Join.ROUND
        }

        val textY = STICKER_SIZE - 32f
        val textX = STICKER_SIZE / 2f

        // Draw text stroke then text fill
        canvas.drawText(text, textX, textY, strokePaint)
        canvas.drawText(text, textX, textY, textPaint)
    }

    /**
     * Smart solid color removal: checks corners, and if almost uniform color (e.g. white or light grey),
     * converts pixels matching that corner color within a tolerance into transparent alpha.
     */
    private fun removeCornerBackground(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val topLeft = pixels[0]
        val tr = Color.red(topLeft)
        val tg = Color.green(topLeft)
        val tb = Color.blue(topLeft)
        val tolerance = 40

        for (i in pixels.indices) {
            val p = pixels[i]
            val a = Color.alpha(p)
            if (a > 0) {
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)
                if (Math.abs(r - tr) < tolerance &&
                    Math.abs(g - tg) < tolerance &&
                    Math.abs(b - tb) < tolerance
                ) {
                    pixels[i] = Color.TRANSPARENT
                }
            }
        }
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Compresses bitmap to WebP format, guaranteeing size <= 98 KB (WhatsApp strictly requires < 100 KB).
     */
    fun compressToWebPBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

        var quality = 90
        do {
            stream.reset()
            bitmap.compress(compressFormat, quality, stream)
            quality -= 10
        } while (stream.size() > MAX_STICKER_BYTES && quality >= 30)

        return stream.toByteArray()
    }

    /**
     * Generates a 96x96 WebP tray icon from a 512x512 sticker.
     */
    fun generateTrayIconBytes(source: Bitmap): ByteArray {
        val trayBitmap = Bitmap.createScaledBitmap(source, TRAY_ICON_SIZE, TRAY_ICON_SIZE, true)
        val stream = ByteArrayOutputStream()
        val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        trayBitmap.compress(compressFormat, 85, stream)
        return stream.toByteArray()
    }

    /**
     * Saves a sticker into the internal sticker directory for a given pack.
     */
    fun saveStickerFile(
        context: Context,
        packId: String,
        fileName: String,
        bytes: ByteArray
    ): File {
        val packDir = File(context.filesDir, "stickers/$packId")
        if (!packDir.exists()) {
            packDir.mkdirs()
        }
        val file = File(packDir, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    /**
     * Helper to create starter stickers for a newly initialized pack.
     */
    fun createSampleSticker(type: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(STICKER_SIZE, STICKER_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (type) {
            0 -> {
                // Thumbs Up / Awesome Sticker
                // Die-cut white border pill
                paint.color = Color.WHITE
                canvas.drawCircle(256f, 256f, 205f, paint)
                paint.color = 0xFF25D366.toInt() // WhatsApp green
                canvas.drawCircle(256f, 256f, 185f, paint)

                // Large emoji / symbol
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 170f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("👍", 256f, 310f, textPaint)

                // Banner text
                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 42f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("AWESOME!", 256f, 410f, labelPaint)
            }
            1 -> {
                // Cool Emoji Sticker
                paint.color = Color.WHITE
                canvas.drawCircle(256f, 256f, 205f, paint)
                paint.color = 0xFFFFC107.toInt() // Amber
                canvas.drawCircle(256f, 256f, 185f, paint)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 175f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("😎", 256f, 315f, textPaint)

                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF1E1E1E.toInt()
                    textSize = 42f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("SO COOL", 256f, 410f, labelPaint)
            }
            2 -> {
                // Party / Celebration Sticker
                paint.color = Color.WHITE
                canvas.drawCircle(256f, 256f, 205f, paint)
                paint.color = 0xFF9C27B0.toInt() // Purple
                canvas.drawCircle(256f, 256f, 185f, paint)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 170f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("🎉", 256f, 310f, textPaint)

                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 40f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("LET'S PARTY!", 256f, 410f, labelPaint)
            }
            else -> {
                // Love / Heart Sticker
                paint.color = Color.WHITE
                canvas.drawCircle(256f, 256f, 205f, paint)
                paint.color = 0xFFE91E63.toInt() // Pink/Red
                canvas.drawCircle(256f, 256f, 185f, paint)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 170f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("❤️", 256f, 315f, textPaint)

                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 42f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("LOVE IT!", 256f, 410f, labelPaint)
            }
        }

        return bitmap
    }
}
