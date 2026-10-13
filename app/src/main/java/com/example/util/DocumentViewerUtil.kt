package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.domain.model.ResourceItem
import com.example.domain.model.Submission

/**
 * Robust Document Viewer and Downloader utility.
 * Handles both local content URIs and cloud Google Drive links cleanly across all devices.
 * Prevents blank viewer screens caused by passing HTTP URLs with local MIME types.
 */
object DocumentViewerUtil {

    /**
     * Extracts Google Drive file ID from various Drive URL formats.
     */
    fun extractDriveFileId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (!url.contains("google.com") && !url.contains("http") && url.length in 25..50) {
            return url.trim()
        }
        val match1 = Regex("/file/d/([a-zA-Z0-9_-]+)").find(url)
        if (match1 != null) return match1.groupValues[1]

        val match2 = Regex("[?&]id=([a-zA-Z0-9_-]+)").find(url)
        if (match2 != null) return match2.groupValues[1]

        val match3 = Regex("/d/([a-zA-Z0-9_-]+)").find(url)
        if (match3 != null) return match3.groupValues[1]

        return null
    }

    /**
     * Returns Google Drive web preview URL suitable for browsers and Drive preview viewer.
     */
    fun getPreviewUrl(fileUri: String?, driveFileId: String?): String? {
        val id = driveFileId ?: extractDriveFileId(fileUri)
        if (!id.isNullOrBlank()) {
            return "https://drive.google.com/file/d/$id/view?usp=sharing"
        }
        if (!fileUri.isNullOrBlank() && (fileUri.startsWith("http://") || fileUri.startsWith("https://"))) {
            return fileUri
        }
        return null
    }

    /**
     * Returns Google Drive direct download URL.
     */
    fun getDirectDownloadUrl(fileUri: String?, driveFileId: String?): String? {
        val id = driveFileId ?: extractDriveFileId(fileUri)
        if (!id.isNullOrBlank()) {
            return "https://drive.google.com/uc?export=download&id=$id"
        }
        if (!fileUri.isNullOrBlank() && (fileUri.startsWith("http://") || fileUri.startsWith("https://"))) {
            return fileUri
        }
        return null
    }

    /**
     * Returns Google Drive high-resolution direct image thumbnail URL.
     */
    fun getDirectImageUrl(fileUri: String?, driveFileId: String?): String? {
        val id = driveFileId ?: extractDriveFileId(fileUri)
        if (!id.isNullOrBlank()) {
            return "https://drive.google.com/thumbnail?id=$id&sz=w1600"
        }
        if (!fileUri.isNullOrBlank() && (fileUri.startsWith("http://") || fileUri.startsWith("https://"))) {
            return fileUri
        }
        return null
    }

    /**
     * Opens a Resource document cleanly on any device.
     */
    fun openResourceDocument(context: Context, resource: ResourceItem) {
        val driveId = resource.driveFileId ?: extractDriveFileId(resource.driveFileUrl) ?: extractDriveFileId(resource.fileUri)
        val cloudUrl = getPreviewUrl(resource.driveFileUrl ?: resource.fileUri, driveId)

        // If cloud URL is available (always preferred for cross-device consistency)
        if (!cloudUrl.isNullOrBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cloudUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Toast.makeText(context, "Opening in browser: ${resource.title}", Toast.LENGTH_SHORT).show()
            }
        }

        // Fallback: If only a local URI is present (on original device)
        val localUri = resource.fileUri
        if (!localUri.isNullOrBlank() && (localUri.startsWith("content://") || localUri.startsWith("file://"))) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(localUri), resource.fileMimeType ?: "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to open local file on this device.", Toast.LENGTH_SHORT).show()
            }
        }

        Toast.makeText(context, "Document ready: ${resource.title}", Toast.LENGTH_SHORT).show()
    }

    /**
     * Opens a Submission document cleanly on any device.
     */
    fun openSubmissionDocument(context: Context, submission: Submission) {
        val driveId = submission.driveFileId ?: extractDriveFileId(submission.driveFileUrl) ?: extractDriveFileId(submission.fileUri)
        val cloudUrl = getPreviewUrl(submission.driveFileUrl ?: submission.fileUri, driveId)

        if (!cloudUrl.isNullOrBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cloudUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Toast.makeText(context, "Opening in browser: ${submission.title}", Toast.LENGTH_SHORT).show()
            }
        }

        val localUri = submission.fileUri
        if (!localUri.isNullOrBlank() && (localUri.startsWith("content://") || localUri.startsWith("file://"))) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(localUri), submission.fileMimeType ?: "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to open local file on this device.", Toast.LENGTH_SHORT).show()
            }
        }

        Toast.makeText(context, "Document: ${submission.title}", Toast.LENGTH_SHORT).show()
    }

    /**
     * Downloads the document to the device's public Downloads directory using Android DownloadManager.
     */
    fun downloadResourceDocument(context: Context, resource: ResourceItem) {
        val driveId = resource.driveFileId ?: extractDriveFileId(resource.driveFileUrl) ?: extractDriveFileId(resource.fileUri)
        val downloadUrl = getDirectDownloadUrl(resource.driveFileUrl ?: resource.fileUri, driveId)

        if (downloadUrl.isNullOrBlank()) {
            Toast.makeText(context, "Direct download link not available yet.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager == null) {
                openResourceDocument(context, resource)
                return
            }

            val fileName = resource.fileName?.takeIf { it.isNotBlank() }
                ?: "${resource.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.pdf"

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle(resource.title)
                setDescription("CSJMU Academic Resource - ${resource.subjectName}")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            downloadManager.enqueue(request)
            Toast.makeText(context, "Downloading $fileName to your Downloads folder...", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Fallback to opening in browser which handles downloading
            openResourceDocument(context, resource)
        }
    }
}
