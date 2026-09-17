package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.sticker.WhatsAppStickerHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Sticker Maker", appName)
  }

  @Test
  fun `whatsapp validation requires minimum 3 stickers`() {
    val resultTooFew = WhatsAppStickerHelper.validatePackForWhatsApp(2, hasTrayIcon = true)
    assertTrue(resultTooFew is WhatsAppStickerHelper.ValidationResult.TooFewStickers)

    val resultValid = WhatsAppStickerHelper.validatePackForWhatsApp(3, hasTrayIcon = true)
    assertEquals(WhatsAppStickerHelper.ValidationResult.Valid, resultValid)
  }
}

