package com.example.widget

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class HWidgetSurfaceProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String = "image/png"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") {
            throw SecurityException("Only read-only access is allowed for HWidget surfaces.")
        }
        val ctx = context ?: throw FileNotFoundException("Provider context unavailable")
        val fileName = uri.lastPathSegment.orEmpty()
        if (!SURFACE_NAME_REGEX.matches(fileName)) {
            throw SecurityException("Invalid widget surface path: $fileName")
        }
        val dir = File(ctx.filesDir, SURFACE_DIR)
        val file = File(dir, fileName)
        if (!file.exists()) {
            throw FileNotFoundException("Surface file not found: $fileName")
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    companion object {
        private const val SURFACE_DIR = "widget_surfaces"
        private val SURFACE_NAME_REGEX = Regex("^widget_[0-9]+_[0-9]+\\.png$")

        fun saveSurfaceAndGetUri(context: Context, appWidgetId: Int, bitmap: Bitmap): Uri {
            val appCtx = context.applicationContext
            val dir = File(appCtx.filesDir, SURFACE_DIR).apply { if (!exists()) mkdirs() }
            val prefix = "widget_${appWidgetId}_"
            dir.listFiles()?.forEach { existing ->
                if (existing.name.startsWith(prefix) && existing.name.endsWith(".png")) {
                    existing.delete()
                }
            }
            val fileName = "widget_${appWidgetId}_${System.currentTimeMillis()}.png"
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            return Uri.parse("content://${appCtx.packageName}.widgetsurface/$fileName")
        }

        fun deleteSurfacesForWidget(context: Context, appWidgetId: Int) {
            val dir = File(context.applicationContext.filesDir, SURFACE_DIR)
            if (!dir.exists()) return
            val prefix = "widget_${appWidgetId}_"
            dir.listFiles()?.forEach { existing ->
                if (existing.name.startsWith(prefix)) {
                    existing.delete()
                }
            }
        }
    }
}
