package com.amigocloud.amigosurvey.util
import android.util.Log
import com.amigocloud.amigosurvey.repository.SurveyConfig
import okhttp3.ResponseBody
import java.io.*
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

fun writeResponseBodyToDisk(body: ResponseBody?, fullPath: String): Boolean {
    try {
        // todo change the file location/name according to your needs
        val futureFile = File(fullPath)

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            val fileReader = ByteArray(4096*10)

            val fileSize = body?.contentLength()
            var fileSizeDownloaded: Long = 0

            inputStream = body?.byteStream()
            futureFile.createNewFile()
            outputStream = FileOutputStream(futureFile)

            while (true) {
                val read = inputStream!!.read(fileReader)

                if (read == -1) {
                    break
                }

                outputStream.write(fileReader, 0, read)

                fileSizeDownloaded += read.toLong()

//                Log.d(TAG, "file download: $fileSizeDownloaded of $fileSize")
            }

            outputStream.flush()

            return true
        } catch (e: IOException) {
            print(e)
            return false
        } finally {
                inputStream?.close()
                outputStream?.close()
        }
    } catch (e: IOException) {
        print(e)
        return false
    }
}

@Throws(IOException::class)
fun unzipFile(zipFile: String, location: String): Boolean  {
    try {
        val f = File(location)
        if (!f.isDirectory) {
            f.mkdirs()
        }
        val zin = ZipInputStream(FileInputStream(zipFile))
        try {
            var ze: ZipEntry? = zin.getNextEntry()
            while (ze != null) {
                ze = zin.getNextEntry()
                ze?.let { ze ->
                    val path = location + ze.getName()

                    if (ze.isDirectory()) {
                        val unzipFile = File(path)
                        if (!unzipFile.isDirectory) {
                            unzipFile.mkdirs()
                        }
                    } else {
                        val fout = FileOutputStream(path, false)
                        try {
                            var c = zin.read()
                            while (c != -1) {
                                fout.write(c)
                                c = zin.read()
                            }
                            zin.closeEntry()
                        } finally {
                            fout.close()
                        }
                    }
                }
            }
        } finally {
            zin.close()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Unzip exception", e)
        return false
    }
    return true
}