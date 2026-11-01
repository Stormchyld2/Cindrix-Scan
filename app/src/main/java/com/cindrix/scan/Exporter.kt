package com.cindrix.scan

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File

object Exporter {

    /** Writes the file into an "inventory" folder inside the folder the person picked. */
    fun saveToFolder(context: Context, treeUri: Uri, name: String, text: String): Result<String> {
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return Result.failure(Exception("The chosen folder is not available"))
            if (!root.canWrite()) {
                return Result.failure(Exception("No permission to write to the chosen folder"))
            }
            val existing = root.findFile("inventory")
            val dir = if (existing != null && existing.isDirectory) existing else root.createDirectory("inventory")
            if (dir == null) {
                return Result.failure(Exception("Could not create the inventory folder"))
            }
            val file = dir.createFile("text/csv", name)
                ?: return Result.failure(Exception("Could not create the file"))
            val out = context.contentResolver.openOutputStream(file.uri)
                ?: return Result.failure(Exception("Could not open the file for writing"))
            out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            Result.success(file.name ?: name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Opens the Android share sheet so the file can go out through Cindrix AirLink (or any other app). */
    fun share(context: Context, name: String, text: String): Result<Unit> {
        return try {
            val dir = File(context.cacheDir, "export")
            dir.mkdirs()
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, name)
            file.writeText(text, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Send inventory file"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
