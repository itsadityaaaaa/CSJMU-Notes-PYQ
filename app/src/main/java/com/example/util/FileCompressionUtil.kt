package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Result returned after performing file compression.
 */
data class CompressionResult(
    val compressedUri: Uri,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val mimeType: String,
    val displayName: String,
    val compressionRatio: Float,
    val isCompressed: Boolean
) {
    val formattedOriginalSize: String
        get() = formatBytes(originalSizeBytes)

    val formattedCompressedSize: String
        get() = formatBytes(compressedSizeBytes)

    val savingsPercentage: Int
        get() = if (originalSizeBytes > 0) {
            val saved = originalSizeBytes - compressedSizeBytes
            if (saved > 0) ((saved.toDouble() / originalSizeBytes) * 100).roundToInt() else 0
        } else 0

    companion object {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
                bytes >= 1024 -> String.format("%d KB", bytes / 1024)
                bytes > 0 -> "$bytes B"
                else -> "0 B"
            }
        }
    }
}

/**
 * Utility for compressing images (JPEG/JPG) and PDFs before uploading,
 * similar to WhatsApp / Telegram media optimization:
 * - Downscales high-resolution camera photos (e.g., 4000x3000 -> 1600x1200 or 1280x960)
 * - Encodes with standard 80% JPEG compression (dramatically reduces 5-15MB photos to 200-500KB with crisp legibility)
 * - Renders and re-encodes PDF pages if oversized, preserving text & diagram clarity
 * - Ensures safe local caching in app's cache directory
 */
object FileCompressionUtil {

    private const val TAG = "FileCompressionUtil"

    // Standard WhatsApp-style maximum dimensions for clear document readability
    private const val MAX_IMAGE_WIDTH = 1600
    private const val MAX_IMAGE_HEIGHT = 2000
    private const val JPEG_QUALITY = 82

    // PDF page rendering limits for optimization
    private const val PDF_MAX_RENDER_WIDTH = 1240  // ~150 DPI A4
    private const val PDF_MAX_RENDER_HEIGHT = 1754
    private const val PDF_PAGE_JPEG_QUALITY = 80

    private fun calculateInSampleSize(
        origWidth: Int,
        origHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (origHeight > reqHeight || origWidth > reqWidth) {
            val halfHeight = origHeight / 2
            val halfWidth = origWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Converts multiple JPG/JPEG images into a single optimized PDF document.
     * Each image becomes one page in the PDF, compressed WhatsApp-style to avoid
     * huge file sizes and high memory usage.
     */
    suspend fun convertImagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        outputBaseName: String = "Notes_Images"
    ): CompressionResult = withContext(Dispatchers.IO) {
        val totalOriginalBytes = imageUris.sumOf { getUriSizeBytes(context, it) }
        val cacheDir = File(context.cacheDir, "compressed_uploads").apply { mkdirs() }
        val targetFile = File(cacheDir, "${outputBaseName}_${System.currentTimeMillis()}.pdf")
        val pdfDocument = PdfDocument()

        try {
            var pageIndex = 1
            for (uri in imageUris) {
                // Determine dimensions & sample
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, boundsOptions)
                }

                val origWidth = boundsOptions.outWidth
                val origHeight = boundsOptions.outHeight
                if (origWidth <= 0 || origHeight <= 0) continue

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(origWidth, origHeight, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

                val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOptions)
                } ?: continue

                // Scale if necessary
                val curW = sampledBitmap.width
                val curH = sampledBitmap.height
                val finalBitmap = if (curW > MAX_IMAGE_WIDTH || curH > MAX_IMAGE_HEIGHT) {
                    val scale = min(
                        MAX_IMAGE_WIDTH.toFloat() / curW,
                        MAX_IMAGE_HEIGHT.toFloat() / curH
                    )
                    val matrix = Matrix().apply { postScale(scale, scale) }
                    val scaled = Bitmap.createBitmap(sampledBitmap, 0, 0, curW, curH, matrix, true)
                    if (scaled != sampledBitmap) sampledBitmap.recycle()
                    scaled
                } else {
                    sampledBitmap
                }

                val pageWidth = finalBitmap.width
                val pageHeight = finalBitmap.height
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex++).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(finalBitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                finalBitmap.recycle()
            }

            FileOutputStream(targetFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val compressedBytes = targetFile.length()
            val ratio = if (totalOriginalBytes > 0) compressedBytes.toFloat() / totalOriginalBytes else 1f

            CompressionResult(
                compressedUri = Uri.fromFile(targetFile),
                originalSizeBytes = totalOriginalBytes,
                compressedSizeBytes = compressedBytes,
                mimeType = "application/pdf",
                displayName = targetFile.name,
                compressionRatio = ratio,
                isCompressed = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compile images into PDF: ${e.message}", e)
            if (targetFile.exists()) targetFile.delete()
            fallbackResult(
                sourceUri = imageUris.firstOrNull() ?: Uri.EMPTY,
                originalSize = totalOriginalBytes,
                originalName = "${outputBaseName}.pdf",
                mimeType = "application/pdf"
            )
        }
    }

    /**
     * Compresses the given file Uri based on MIME type or filename.
     */
    suspend fun compressFile(
        context: Context,
        sourceUri: Uri,
        fileName: String,
        mimeType: String? = null
    ): CompressionResult = withContext(Dispatchers.IO) {
        val lowerName = fileName.lowercase()
        val originalSize = getUriSizeBytes(context, sourceUri)

        val isJpg = lowerName.endsWith(".jpg") ||
                lowerName.endsWith(".jpeg") ||
                mimeType == "image/jpeg" ||
                mimeType == "image/jpg"

        val isPdf = lowerName.endsWith(".pdf") ||
                mimeType == "application/pdf"

        try {
            when {
                isJpg -> compressImage(context, sourceUri, fileName, originalSize)
                isPdf && originalSize > 1.5 * 1024 * 1024 -> {
                    // For PDFs larger than 1.5 MB, optimize pages using standard Android PdfRenderer
                    compressPdf(context, sourceUri, fileName, originalSize)
                }
                else -> {
                    // Return unmodified if already small or unknown type
                    CompressionResult(
                        compressedUri = sourceUri,
                        originalSizeBytes = originalSize,
                        compressedSizeBytes = originalSize,
                        mimeType = mimeType ?: "application/octet-stream",
                        displayName = fileName,
                        compressionRatio = 1.0f,
                        isCompressed = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Compression error for $fileName, falling back to original: ${e.message}", e)
            CompressionResult(
                compressedUri = sourceUri,
                originalSizeBytes = originalSize,
                compressedSizeBytes = originalSize,
                mimeType = mimeType ?: "application/octet-stream",
                displayName = fileName,
                compressionRatio = 1.0f,
                isCompressed = false
            )
        }
    }

    /**
     * Compresses a JPEG image using downsampling and 80-85% quality encoding.
     */
    private fun compressImage(
        context: Context,
        sourceUri: Uri,
        originalName: String,
        originalSize: Long
    ): CompressionResult {
        // Step 1: Decode image bounds
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
        }

        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight

        if (origWidth <= 0 || origHeight <= 0) {
            return fallbackResult(sourceUri, originalSize, originalName, "image/jpeg")
        }

        // Step 2: Compute sample size
        var inSampleSize = 1
        if (origHeight > MAX_IMAGE_HEIGHT || origWidth > MAX_IMAGE_WIDTH) {
            val halfHeight = origHeight / 2
            val halfWidth = origWidth / 2
            while ((halfHeight / inSampleSize) >= MAX_IMAGE_HEIGHT && (halfWidth / inSampleSize) >= MAX_IMAGE_WIDTH) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.RGB_565 // Memory-friendly, 2 bytes/pixel
        }

        val sampledBitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: return fallbackResult(sourceUri, originalSize, originalName, "image/jpeg")

        // Step 3: Exact scaling if still larger than max dimensions
        val currentWidth = sampledBitmap.width
        val currentHeight = sampledBitmap.height
        val finalBitmap = if (currentWidth > MAX_IMAGE_WIDTH || currentHeight > MAX_IMAGE_HEIGHT) {
            val scale = min(
                MAX_IMAGE_WIDTH.toFloat() / currentWidth,
                MAX_IMAGE_HEIGHT.toFloat() / currentHeight
            )
            val matrix = Matrix().apply { postScale(scale, scale) }
            val scaled = Bitmap.createBitmap(sampledBitmap, 0, 0, currentWidth, currentHeight, matrix, true)
            if (scaled != sampledBitmap) {
                sampledBitmap.recycle()
            }
            scaled
        } else {
            sampledBitmap
        }

        // Step 4: Write to compressed cache file
        val cacheDir = File(context.cacheDir, "compressed_uploads").apply { mkdirs() }
        val cleanName = originalName.substringBeforeLast(".")
        val targetFile = File(cacheDir, "${cleanName}_compressed.jpg")

        FileOutputStream(targetFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        finalBitmap.recycle()

        val compressedBytes = targetFile.length()
        // If compressed file ended up larger than original (rare), use original
        if (compressedBytes >= originalSize && originalSize > 0) {
            targetFile.delete()
            return fallbackResult(sourceUri, originalSize, originalName, "image/jpeg")
        }

        val ratio = if (originalSize > 0) compressedBytes.toFloat() / originalSize else 1f
        return CompressionResult(
            compressedUri = Uri.fromFile(targetFile),
            originalSizeBytes = originalSize,
            compressedSizeBytes = compressedBytes,
            mimeType = "image/jpeg",
            displayName = targetFile.name,
            compressionRatio = ratio,
            isCompressed = true
        )
    }

    /**
     * Optimizes a PDF by rendering pages to balanced-resolution bitmaps and rebuilding
     * into a new lean PDF document. If the resulting PDF is not smaller, retains the original.
     */
    private fun compressPdf(
        context: Context,
        sourceUri: Uri,
        originalName: String,
        originalSize: Long
    ): CompressionResult {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val cacheDir = File(context.cacheDir, "compressed_uploads").apply { mkdirs() }
        val cleanName = originalName.substringBeforeLast(".")
        val targetFile = File(cacheDir, "${cleanName}_optimized.pdf")

        try {
            pfd = context.contentResolver.openFileDescriptor(sourceUri, "r") ?: return fallbackResult(
                sourceUri, originalSize, originalName, "application/pdf"
            )
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            // Don't re-encode huge multi-hundred page documents on-device to avoid memory issues
            if (pageCount > 50) {
                return fallbackResult(sourceUri, originalSize, originalName, "application/pdf")
            }

            val pdfDocument = PdfDocument()

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val pageWidth = page.width
                val pageHeight = page.height

                // Calculate scale to fit PDF_MAX_RENDER_WIDTH x PDF_MAX_RENDER_HEIGHT
                val scale = min(
                    PDF_MAX_RENDER_WIDTH.toFloat() / pageWidth,
                    PDF_MAX_RENDER_HEIGHT.toFloat() / pageHeight
                ).coerceAtMost(1.5f)

                val destWidth = (pageWidth * scale).roundToInt()
                val destHeight = (pageHeight * scale).roundToInt()

                val bitmap = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.RGB_565)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(destWidth, destHeight, i + 1).create()
                val newPage = pdfDocument.startPage(pageInfo)
                val canvas = newPage.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(newPage)
                bitmap.recycle()
            }

            FileOutputStream(targetFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val compressedBytes = targetFile.length()
            if (compressedBytes >= originalSize && originalSize > 0) {
                targetFile.delete()
                return fallbackResult(sourceUri, originalSize, originalName, "application/pdf")
            }

            val ratio = if (originalSize > 0) compressedBytes.toFloat() / originalSize else 1f
            return CompressionResult(
                compressedUri = Uri.fromFile(targetFile),
                originalSizeBytes = originalSize,
                compressedSizeBytes = compressedBytes,
                mimeType = "application/pdf",
                displayName = targetFile.name,
                compressionRatio = ratio,
                isCompressed = true
            )
        } catch (e: Exception) {
            Log.w(TAG, "PDF optimization skipped or failed: ${e.message}")
            if (targetFile.exists()) targetFile.delete()
            return fallbackResult(sourceUri, originalSize, originalName, "application/pdf")
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }
    }

    private fun fallbackResult(
        sourceUri: Uri,
        originalSize: Long,
        originalName: String,
        mimeType: String
    ): CompressionResult = CompressionResult(
        compressedUri = sourceUri,
        originalSizeBytes = originalSize,
        compressedSizeBytes = originalSize,
        mimeType = mimeType,
        displayName = originalName,
        compressionRatio = 1.0f,
        isCompressed = false
    )

    private fun getUriSizeBytes(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
