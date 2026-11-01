package com.cindrix.scan

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File

object Exporter {

    /**
     * Writes the file into inbox/inventory inside the folder the person picked. That is the
     * mailbox Cindrix Ops reads, so pick the Ops "user" folder (or a folder Ops looks at).
     */
    fun saveToFolder(context: Context, treeUri: Uri, name: String, text: String): Result<String> {
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return Result.failure(Exception("The chosen folder is not available"))
            if (!root.canWrite()) {
                return Result.failure(Exception("No permission to write to the chosen folder"))
            }
            val dir = subfolder(subfolder(root, "inbox"), "inventory")
                ?: return Result.failure(Exception("Could not create the inbox folder"))
            dir.findFile(name)?.delete()
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

    private fun subfolder(parent: DocumentFile?, name: String): DocumentFile? {
        if (parent == null) return null
        val existing = parent.findFile(name)
        return if (existing != null && existing.isDirectory) existing else parent.createDirectory(name)
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
            // Straight to Cindrix AirLink when it is installed; otherwise the usual share list.
            val airlink = "com.cindrix.airlink"
            val installed = try {
                context.packageManager.getPackageInfo(airlink, 0)
                true
            } catch (e: Exception) {
                false
            }
            if (installed) {
                send.setPackage(airlink)
                send.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(send)
            } else {
                context.startActivity(Intent.createChooser(send, "Send inventory file"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
