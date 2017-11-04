package com.amigocloud.amigosurvey.util
import android.util.Log
import io.reactivex.Single
import okhttp3.ResponseBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Clock
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


const val TAG = "FileUtil"

fun mkdir(path: String, dirname: String): Boolean {
    // Get the directory for the user's public pictures directory.
    val file = File("$path/$dirname")
    if (!file.exists() && !file.mkdirs()) {
        Log.e("mkdir", "Directory not created")
        return false
    }
    return true
}

fun ResponseBody.writeToDisk(fullPath: String): Single<File> = Single.fromCallable<File> {
    val futureFile = File(fullPath)
    val fileReader = ByteArray(4096 )
    var fileSizeDownloaded: Long = 0

    byteStream().use { inputStream ->
        futureFile.createNewFile()
        FileOutputStream(futureFile).use { outputStream ->
            while (true) {
                val read = inputStream.read(fileReader)

                if (read == -1) break

                outputStream.write(fileReader, 0, read)

                fileSizeDownloaded += read.toLong()
            }
            outputStream.flush()
        }
    }

    futureFile
}

fun unzipFile(zipFile: String, location: String): Single<File> = Single.fromCallable<File> {
    val fileInLocation = File(location)
    if (!fileInLocation.isDirectory) {
        fileInLocation.mkdirs()
    }
    ZipInputStream(FileInputStream(zipFile)).use { zin ->
        do {
            val nextEntry = zin.nextEntry
            nextEntry?.let { entry ->
                val path = location + entry.name
                if (entry.isDirectory) {
                    val unzipFile = File(path)
                    if (!unzipFile.isDirectory) {
                        unzipFile.mkdirs()
                    }
                } else {
                    FileOutputStream(path, false).use { fout ->
                        var c = zin.read()
                        while (c != -1) {
                            fout.write(c)
                            c = zin.read()
                        }
                        zin.closeEntry()
                    }
                }
            }
        } while (nextEntry != null)
        fileInLocation
    }
}
