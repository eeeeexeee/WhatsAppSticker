package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sticker.WhatsAppStickerHelper
import com.example.ui.theme.WhatsAppGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatsAppStatusBanner(
    packId: String,
    packName: String,
    stickerCount: Int,
    onAddStickersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isReady = stickerCount in 3..30
    val progress = (stickerCount / 30f).coerceIn(0f, 1f)

    // Result launcher for WhatsApp intent
    val whatsAppLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Toast.makeText(context, "Returned from WhatsApp", Toast.LENGTH_SHORT).show()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("whatsapp_status_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReady) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isReady) WhatsAppGreen else Color(0xFFFF9800)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isReady) "WhatsApp Ready" else "Needs More Stickers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isReady) {
                                "$stickerCount / 30 stickers (Min 3 required)"
                            } else {
                                "$stickerCount / 30 stickers (Add ${3 - stickerCount} more for WhatsApp)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isReady) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Count Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isReady) WhatsAppGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant
                ) {
                    Text(
                        text = "$stickerCount/30",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isReady) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isReady) WhatsAppGreen else Color(0xFFFF9800),
                trackColor = Color.LightGray.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (FlowRow prevents wrapping text inside button)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "Add to WhatsApp" Button
                Button(
                    onClick = {
                        val validation = WhatsAppStickerHelper.validatePackForWhatsApp(stickerCount, hasTrayIcon = true)
                        when (validation) {
                            is WhatsAppStickerHelper.ValidationResult.TooFewStickers -> {
                                Toast.makeText(
                                    context,
                                    "WhatsApp requires at least 3 stickers. Please add ${3 - stickerCount} more.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            is WhatsAppStickerHelper.ValidationResult.TooManyStickers -> {
                                Toast.makeText(
                                    context,
                                    "WhatsApp allows maximum 30 stickers per collection.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            is WhatsAppStickerHelper.ValidationResult.MissingTrayIcon -> {
                                Toast.makeText(context, validation.reason, Toast.LENGTH_SHORT).show()
                            }
                            WhatsAppStickerHelper.ValidationResult.Valid -> {
                                val installed = WhatsAppStickerHelper.getInstalledWhatsAppPackages(context)
                                if (installed.isNotEmpty()) {
                                    val intent = WhatsAppStickerHelper.createAddToWhatsAppIntent(
                                        context = context,
                                        packId = packId,
                                        packName = packName,
                                        targetPackage = installed.first()
                                    )
                                    try {
                                        whatsAppLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Could not open WhatsApp: ${e.localizedMessage}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "WhatsApp or WhatsApp Business is not installed on this device. You can still share stickers via the share menu!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReady) WhatsAppGreen else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("add_to_whatsapp_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add to WhatsApp",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Add More Stickers Button
                OutlinedButton(
                    onClick = onAddStickersClick,
                    modifier = Modifier.testTag("banner_add_stickers_button")
                ) {
                    Text(
                        text = "+ Add New Sticker",
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
