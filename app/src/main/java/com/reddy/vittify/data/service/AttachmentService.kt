package com.reddy.vittify.data.service

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling transaction attachment file operations.
 * Manages saving, deleting, and retrieving attachment files.
 */
@Singleton
class AttachmentService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val ATTACHMENTS_DIR = "attachments"
    }

    private val attachmentsDir: File
        get() = File(context.filesDir, ATTACHMENTS_DIR).also { 
            if (!it.exists()) it.mkdirs() 
        }

    /**
     * Save an attachment file from a content URI to internal storage.
     * @param uri The content URI of the file to save
     * @param transactionId The transaction ID this attachment belongs to
     * @return The relative path of the saved file, or null if save failed
     */
    fun saveAttachment(uri: Uri, transactionId: Long): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val mimeType = context.contentResolver.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
            
            // Generate unique filename: transactionId_uuid.extension
            val fileName = "${transactionId}_${UUID.randomUUID()}.$extension"
            val outputFile = File(attachmentsDir, fileName)
            
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            
            // Return relative path for storage in database
            "$ATTACHMENTS_DIR/$fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Delete an attachment file from internal storage.
     * @param relativePath The relative path of the file to delete
     * @return true if deletion was successful
     */
    fun deleteAttachment(relativePath: String): Boolean {
        return try {
            val file = File(context.filesDir, relativePath)
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get a content URI for viewing an attachment file.
     * Uses FileProvider for secure access.
     * @param relativePath The relative path of the file
     * @return Content URI for the file, or null if file doesn't exist
     */
    fun getAttachmentUri(relativePath: String): Uri? {
        return try {
            val file = File(context.filesDir, relativePath)
            if (file.exists()) {
                val authority = "${context.packageName}.fileprovider"
                FileProvider.getUriForFile(context, authority, file)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get the MIME type of an attachment file.
     * @param relativePath The relative path of the file
     * @return MIME type string, or "application/octet-stream" as fallback
     */
    fun getAttachmentMimeType(relativePath: String): String {
        val extension = relativePath.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }

    /**
     * Check if a file is an image based on its MIME type.
     * @param relativePath The relative path of the file
     * @return true if the file is an image
     */
    fun isImage(relativePath: String): Boolean {
        return getAttachmentMimeType(relativePath).startsWith("image/")
    }

    /**
     * Get the absolute file path for an attachment.
     * @param relativePath The relative path of the file
     * @return Absolute file path
     */
    fun getAbsolutePath(relativePath: String): String {
        return File(context.filesDir, relativePath).absolutePath
    }

    /**
     * Get all attachment files in the attachments directory.
     * Useful for backup operations.
     * @return List of all attachment files
     */
    fun getAllAttachmentFiles(): List<File> {
        return attachmentsDir.listFiles()?.toList() ?: emptyList()
    }

    /**
     * Parse comma-separated attachment paths into a list.
     * @param attachments The comma-separated string from database
     * @return List of attachment paths
     */
    fun parseAttachments(attachments: String): List<String> {
        return if (attachments.isBlank()) emptyList()
        else attachments.split(",").filter { it.isNotBlank() }
    }

    /**
     * Join attachment paths into a comma-separated string for database storage.
     * @param paths List of attachment paths
     * @return Comma-separated string
     */
    fun joinAttachments(paths: List<String>): String {
        return paths.joinToString(",")
    }
}
