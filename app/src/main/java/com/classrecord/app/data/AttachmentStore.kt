package com.classrecord.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class AttachmentStore(context: Context) {
    private val appContext = context.applicationContext
    val root: File = File(appContext.filesDir, DIR).also { it.mkdirs() }

    fun fileFor(relativePath: String): File {
        return File(appContext.filesDir, relativePath)
    }

    fun exists(relativePath: String?): Boolean {
        if (relativePath.isNullOrBlank()) return false
        return fileFor(relativePath).isFile
    }

    fun providerUri(file: File): Uri {
        return FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file
        )
    }

    fun createCameraTarget(activityId: Long, memberId: Long): Pair<File, Uri> {
        root.mkdirs()
        val file = File(root, "capture_${activityId}_${memberId}_${System.currentTimeMillis()}.jpg")
        return file to providerUri(file)
    }

    fun saveFromUri(uri: Uri, activityId: Long, memberId: Long): Pair<String, String> {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: error("无法读取所选图片")
        val decoded = input.use { BitmapFactory.decodeStream(it) }
            ?: error("无法解析图片")
        return saveBitmap(decoded, activityId, memberId)
    }

    fun saveBitmap(bitmap: Bitmap, activityId: Long, memberId: Long): Pair<String, String> {
        root.mkdirs()
        val relative = "$DIR/${activityId}_${memberId}_${System.currentTimeMillis()}.jpg"
        val dest = File(appContext.filesDir, relative)
        dest.parentFile?.mkdirs()
        val scaled = scaleDown(bitmap, 1600)
        FileOutputStream(dest).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        if (scaled !== bitmap) scaled.recycle()
        return relative to "image/jpeg"
    }

    fun delete(relativePath: String?) {
        if (relativePath.isNullOrBlank()) return
        val file = fileFor(relativePath)
        if (file.exists()) file.delete()
    }

    fun deleteAll() {
        root.listFiles()?.forEach { it.delete() }
    }

    fun copyIntoRoot(relativePath: String, bytes: ByteArray) {
        val dest = File(appContext.filesDir, relativePath)
        dest.parentFile?.mkdirs()
        dest.writeBytes(bytes)
    }

    fun loadThumb(relativePath: String?, maxPx: Int = 256): Bitmap? {
        if (relativePath.isNullOrBlank()) return null
        val file = fileFor(relativePath)
        if (!file.isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        while (longest / sample > maxPx * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    fun listRelativeFiles(): List<String> {
        return root.listFiles()?.map { "$DIR/${it.name}" }.orEmpty()
    }

    private fun scaleDown(src: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxEdge) return src
        val scale = maxEdge.toFloat() / longest
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    companion object {
        const val DIR = "attachments"
    }
}
