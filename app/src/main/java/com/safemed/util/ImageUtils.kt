package com.safemed.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Utility object để xử lý ảnh trước khi upload
 * - Resize về kích thước tối đa 512x512
 * - Compress với quality 80%
 * - Xử lý rotation từ EXIF data
 */
object ImageUtils {

    private const val MAX_SIZE = 512
    private const val COMPRESS_QUALITY = 80

    /**
     * Compress và resize ảnh từ Uri
     * @param context Context để đọc file
     * @param imageUri Uri của ảnh gốc
     * @param maxSize Kích thước tối đa (width hoặc height), mặc định 512
     * @param quality Chất lượng compress (0-100), mặc định 80
     * @return ByteArray của ảnh đã compress, hoặc null nếu lỗi
     */
    fun compressImage(
        context: Context,
        imageUri: Uri,
        maxSize: Int = MAX_SIZE,
        quality: Int = COMPRESS_QUALITY
    ): ByteArray? {
        return try {
            // Đọc ảnh từ Uri
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Xử lý rotation từ EXIF
            val rotatedBitmap = rotateImageIfRequired(context, originalBitmap, imageUri)

            // Resize ảnh
            val resizedBitmap = resizeBitmap(rotatedBitmap, maxSize)

            // Compress thành JPEG
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            // Cleanup
            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            if (resizedBitmap != rotatedBitmap) {
                rotatedBitmap.recycle()
            }
            resizedBitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Resize bitmap giữ tỷ lệ khung hình
     */
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio: Float = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Xoay ảnh theo EXIF orientation nếu cần
     */
    private fun rotateImageIfRequired(context: Context, bitmap: Bitmap, imageUri: Uri): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotationDegrees == 0f) {
                bitmap
            } else {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Lấy kích thước file ước tính sau khi compress (KB)
     */
    fun getCompressedSizeKB(data: ByteArray): Int {
        return data.size / 1024
    }
}
