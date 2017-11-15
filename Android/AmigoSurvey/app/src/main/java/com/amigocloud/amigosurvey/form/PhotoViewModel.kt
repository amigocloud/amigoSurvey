package com.amigocloud.amigosurvey.form

import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.ViewModel
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.amigocloud.amigosurvey.models.RelatedRecord
import com.amigocloud.amigosurvey.repository.SurveyConfig
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormat
import java.util.*
import javax.inject.Inject

class PhotoInfo(val datasetId: Long, val relatedTableId: Long, val sourceAmigoId: String, var imageFile: File?)

class PhotoViewModel(private val config: SurveyConfig
//                     ,private val repository: Repository
                                            ): ViewModel() {

    fun addImageToGallery(activity: Activity, filePath: String) {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.MediaColumns.DATA, filePath)
        activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun Bitmap.scaleBitmap(maxSize: Int): Bitmap? {
        var bmp = this
        if (height > maxSize || width > maxSize) {
            val outWidth: Int
            val outHeight: Int
            if (width > height) {
                outWidth = maxSize
                outHeight = maxSize * height / width
            } else {
                outHeight = maxSize
                outWidth = maxSize * width / height
            }
            bmp = Bitmap.createScaledBitmap(bmp, outWidth, outHeight, true)
        }
        return bmp
    }

    private fun Bitmap.scaleAndRotate(filepath: String): Bitmap? {
        val maxSize = 2048
        var bitmap: Bitmap? = this
        if (height > maxSize || width > maxSize) {
            val outWidth: Int
            val outHeight: Int
            if (width > height) {
                outWidth = maxSize
                outHeight = maxSize * height / width
            } else {
                outHeight = maxSize
                outWidth = maxSize * width / height
            }
            bitmap = Bitmap.createScaledBitmap(this, outWidth, outHeight, true)
            bitmap.let {
                try {
                    val exif = ExifInterface(filepath)
                    var rotate = 0
                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90
                    }
                    if (rotate != 0) {
                        val matrix = Matrix()
                        matrix.postRotate(rotate.toFloat())
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, it.width, it.height, matrix, true)
                    }
                } catch (e: IOException) {
                    // Rotate Failed
                }
            }

        }
        return bitmap
    }
    @Throws(IOException::class)
    fun copyFile(src: File?, dst: File?) {
        if (src == null || dst == null)
            throw IOException("File does not exist")

        val inChannel = FileInputStream(src).channel
        val outChannel = FileOutputStream(dst).channel
        try {
            inChannel?.transferTo(0, inChannel.size(), outChannel)
        } finally {
            inChannel?.close()
            outChannel.close()
        }
    }

    fun copyAndScaleBitmap(maxSize: Int, filepathSrs: String, fileDst: File): Boolean {
        BitmapFactory.decodeFile(filepathSrs).scaleBitmap(maxSize)?.apply {
            try {
                val fOut = FileOutputStream(fileDst)
                compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.flush()
                fOut.close()
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        return true
    }

    fun savePhoto(activity: Activity, photoInfo: PhotoInfo): Completable {
        return Completable.fromAction {
            photoInfo.imageFile?.let {
                imageFile ->
                imageFile.name?.let {

                    addImageToGallery(activity, imageFile.path)
                    storeFile(photoInfo.datasetId, photoInfo.relatedTableId, photoInfo.sourceAmigoId, it)

                    if (!it.isEmpty()) {
                        val dst = File(config.photosDir + it)
                        try {
                            copyFile(imageFile, dst)
                            val imageBitmap = getBitmap(dst.absolutePath)
                            saveThumbnail(imageBitmap, it)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }.subscribeOn(Schedulers.io())
    }

    fun saveVideo(activity: Activity, photoInfo: PhotoInfo): Completable {
        return Completable.fromAction {
            photoInfo.imageFile?.let {
                imageFile ->
                imageFile.name?.let {

                    addImageToGallery(activity, imageFile.path)
                    storeFile(photoInfo.datasetId, photoInfo.relatedTableId, photoInfo.sourceAmigoId, it)

                    if (!it.isEmpty()) {
                        val dst = File(config.photosDir + it)
                        try {
                            copyFile(imageFile, dst)
                            val imageBitmap = getBitmap(dst.absolutePath)
                            saveThumbnail(imageBitmap, it)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }.subscribeOn(Schedulers.io())
    }

    fun getMetadata(activity: Activity, column: String?, selectedImage: Uri) :String? {
        val filePathColumn = arrayOf(column)
        var metadata: String? = null
        activity.contentResolver.query(selectedImage, filePathColumn, null, null, null)?.apply {
            moveToFirst()
            val columnIndex = getColumnIndex(filePathColumn[0])
            metadata = getString(columnIndex)
        }?.close()
        return metadata
    }

    fun storeFile(datasetId: Long, relatedTableId: Long, sourceAmigoId: String, filename: String): Boolean {
        val currentDateTimeString = DateFormat.getDateTimeInstance().format(Date())

        val record = RelatedRecord(
                filename = filename,
                source_amigo_id = sourceAmigoId,
                datetime = currentDateTimeString,
                location = "",
                amigo_id = UUID.randomUUID().toString().replace("-", ""),
                relatedTableId = relatedTableId.toString())
//        repository.db.relatedRecordDao().insert(record)
        return true
    }

    fun saveFile(activity: Activity, photoInfo: PhotoInfo, selectedImage: Uri): Completable {
        return Completable.fromAction {
            // Extract file name
            var filename = getMetadata(activity, MediaStore.Images.Media.DISPLAY_NAME, selectedImage)
            filename?.let {
                if (storeFile(photoInfo.datasetId, photoInfo.relatedTableId, photoInfo.sourceAmigoId, it)) {
                    val dst = File(config.photosDir + filename)
                        getPath(activity.applicationContext, selectedImage)?.let {
                        path ->
                        val photo = File(path)
                        try {
                            copyFile(photo, dst)
                            val imageBitmap = getBitmap(dst.absolutePath)
                            saveThumbnail(imageBitmap, it)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }.subscribeOn(Schedulers.io())
    }

    @Throws(IOException::class)
    private fun saveThumbnail(imageBitmap: Bitmap?, filename: String) {
        val thumbSize = 100
        val width = imageBitmap?.width ?: 0
        val height = imageBitmap?.height ?: 0
        val outWidth: Int
        val outHeight: Int
        if (width > 0 && height > 0) {
            if (width > height) {
                outWidth = thumbSize
                outHeight = thumbSize * height / width
            } else {
                outHeight = thumbSize
                outWidth = thumbSize * width / height
            }
            val bitmap = Bitmap.createScaledBitmap(imageBitmap, outWidth, outHeight, true)
            val file = File(config.photosDir + "thumbnail_" + filename)
            val fOut = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
            fOut.flush()
            fOut.close()
        }
    }

    /**
     * @param uri The Uri to check.
     * *
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * *
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * *
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * *
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.

     * @param context The context.
     * *
     * @param uri The Uri to query.
     * *
     * @param selection (Optional) Filter used in the query.
     * *
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * *
     * @return The value of the _data column, which is typically a file path.
     */
    fun getDataColumn(context: Context, uri: Uri, selection: String?,
                      selectionArgs: Array<String>?): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs,
                    null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            if (cursor != null)
                cursor.close()
        }
        return null
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.

     * @param context The context.
     * *
     * @param uri The Uri to query.
     */
    @SuppressLint("NewApi")
    fun getPath(context: Context, uri: Uri): String? {

        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), DocumentsContract.getDocumentId(uri).toLong())
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return contentUri?.let { getDataColumn(context, it, selection, selectionArgs) }
            }// MediaProvider
            // DownloadsProvider
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.lastPathSegment

            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }// File
        // MediaStore (and general)

        return null
    }

    fun getBitmap(filepath: String): Bitmap? {
        return BitmapFactory.decodeFile(filepath)?.scaleAndRotate(filepath)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val config: SurveyConfig) : ViewModelFactory<PhotoViewModel>() {

        override val modelClass = PhotoViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(this.modelClass)) {
                return PhotoViewModel(config) as T
            }
            throw IllegalArgumentException(INFLATION_EXCEPTION)
        }
    }
}
