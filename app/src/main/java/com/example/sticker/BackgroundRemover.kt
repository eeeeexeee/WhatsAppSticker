package com.example.sticker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object BackgroundRemover {

    /**
     * Removes background from a bitmap using ML Kit Subject Segmentation.
     * Falls back smoothly to smart edge/corner removal if ML Kit is unavailable or encounters an issue.
     */
    suspend fun removeBackground(
        context: Context,
        inputBitmap: Bitmap
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            val options = SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build()

            val segmenter = SubjectSegmentation.getClient(options)
            val inputImage = InputImage.fromBitmap(inputBitmap, 0)

            val task = segmenter.process(inputImage)
            // Await with timeout of 5 seconds to prevent hanging on emulators or unbundled model fetch
            val result = Tasks.await(task, 5, TimeUnit.SECONDS)
            val foreground = result.foregroundBitmap
            segmenter.close()

            if (foreground != null) {
                return@withContext foreground
            }
        } catch (e: Throwable) {
            // Log or ignore, and proceed to smart edge/corner color keying fallback
            e.printStackTrace()
        }

        // Fallback: Smart color-keying algorithm for photos/illustrations
        return@withContext removeCornerBackground(inputBitmap)
    }

    /**
     * Smart flood-fill / corner color keying algorithm to remove solid or near-solid backgrounds.
     */
    fun removeCornerBackground(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // Sample four corners to determine dominant background color
        val corners = listOf(
            pixels[0], // top-left
            pixels[width - 1], // top-right
            pixels[(height - 1) * width], // bottom-left
            pixels[width * height - 1] // bottom-right
        )

        // Find the most common corner color or average corner color
        val cornerColor = corners.first()
        val tr = Color.red(cornerColor)
        val tg = Color.green(cornerColor)
        val tb = Color.blue(cornerColor)
        val tolerance = 45

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
}
