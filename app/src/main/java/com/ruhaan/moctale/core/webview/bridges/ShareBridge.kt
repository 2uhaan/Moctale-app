package com.ruhaan.moctale.core.webview.bridges

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast

class ShareBridge(private val context: Context) {

    @JavascriptInterface
    fun shareImage(
        base64Data: String,
        title: String?,
        text: String?,
    ) {
        try {
            val pureBase64 = base64Data.substringAfter(",")
            val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            val fileName = "moctale_share_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Moctale")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                val stream = context.contentResolver.openOutputStream(it)
                stream?.use { output -> output.write(imageBytes) }
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Share Image")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        } catch (_: Exception) {
            Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show()
        }
    }
}
