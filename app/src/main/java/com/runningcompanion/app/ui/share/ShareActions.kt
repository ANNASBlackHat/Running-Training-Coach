package com.runningcompanion.app.ui.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.share.CardTemplate
import com.runningcompanion.app.domain.share.GpxExporter
import com.runningcompanion.app.domain.share.ShareCardModel
import java.io.File
import java.io.FileOutputStream

/**
 * File-based share/save/export actions for a session:
 * renders the share card PNG, shares it via the system share sheet,
 * saves it to the gallery, and exports the GPX track.
 */
object ShareActions {

    const val FILE_PROVIDER_AUTHORITIES_SUFFIX = ".fileprovider"

    fun writeCardPng(context: Context, session: Session, template: CardTemplate = CardTemplate.FULL): File {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "${session.id}-${template.name.lowercase()}.png")
        val bitmap = ShareCardRenderer.render(ShareCardModel.fromSession(session), template)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
        return file
    }

    fun shareImage(context: Context, file: File, subject: String) {
        val intent = buildShareIntent(context, file, "image/png", subject)
        context.startActivity(Intent.createChooser(intent, "Share workout"))
    }

    fun writeGpx(context: Context, session: Session): File {
        val dir = File(context.cacheDir, "gpx").apply { mkdirs() }
        val safeName = session.workoutSnapshot.name
            .replace(Regex("[^a-zA-Z0-9 _-]"), "")
            .trim()
            .ifEmpty { "workout" }
        val file = File(dir, "${session.id}-${safeName}.gpx")
        file.writeText(GpxExporter.export(session))
        return file
    }

    fun shareGpx(context: Context, file: File, subject: String) {
        val intent = buildShareIntent(context, file, "application/octet-stream", subject)
        context.startActivity(Intent.createChooser(intent, "Export GPX"))
    }

    /**
     * Saves the PNG to the device. On API 29+ writes to
     * Pictures/RunningCompanion via MediaStore; on older devices to the
     * app's public pictures folder. Returns a human-readable location.
     */
    fun savePng(context: Context, file: File, displayName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/RunningCompanion")
            }
            val contentResolver = context.contentResolver
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Failed to create MediaStore entry")
            contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } ?: throw IllegalStateException("Failed to write to MediaStore")
            return "Photos · RunningCompanion"
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: File(context.filesDir, "pictures")
            dir.mkdirs()
            val target = File(dir, "$displayName.png")
            file.copyTo(target, overwrite = true)
            return "Pictures · RunningCompanion folder"
        }
    }

    private fun buildShareIntent(context: Context, file: File, mimeType: String, subject: String): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + FILE_PROVIDER_AUTHORITIES_SUFFIX,
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
