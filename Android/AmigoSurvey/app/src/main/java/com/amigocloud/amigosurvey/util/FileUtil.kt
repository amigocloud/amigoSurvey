package com.amigocloud.amigosurvey.util

import android.os.Environment
import android.util.Log
import java.io.File

fun mkdir(dirname: String): Boolean {
    // Get the directory for the user's public pictures directory.
    val file = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS), dirname)
    if (!file.mkdirs()) {
        Log.e("mkdir", "Directory not created")
        return false
    }
    return true
}
