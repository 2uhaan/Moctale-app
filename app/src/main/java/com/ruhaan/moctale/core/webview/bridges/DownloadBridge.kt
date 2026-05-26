package com.ruhaan.moctale.core.webview.bridges

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import java.io.OutputStream

class DownloadBridge(
    private val context: Context,
    private val onDownloadSuccess: () -> Unit
) {

    @JavascriptInterface
    fun downloadBase64Image(base64Data: String) {
        try {
            val pureBase64 = base64Data.substringAfter(",")
            val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            val fileName = "moctale_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Moctale")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                val stream: OutputStream? = context.contentResolver.openOutputStream(it)
                stream?.use { output -> output.write(imageBytes) }
                onDownloadSuccess()
            }
        } catch (_: Exception) {
            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
        }
    }
}
