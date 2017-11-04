package com.amigocloud.amigosurvey.repository

import android.content.SharedPreferences
import com.amigocloud.amigosurvey.toothpick.FileDir
import com.amigocloud.amigosurvey.util.get
import com.amigocloud.amigosurvey.util.mkdir
import com.amigocloud.amigosurvey.util.save
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class ConfigKey {
    EMAIL, PASSWORD, LOGGEDIN, DATASET_SELECTED, TOKEN, USER_ID,
    PROJECT_ID, DATASET_ID, BASE_URL, USER_MODEL, AMIGO_TOKEN, SUPPORT_FILES_HASH
}

@Singleton
class SurveyConfig @Inject constructor(prefs: SharedPreferences,
                                       @FileDir filesDir: File) {

    val email = ConfigPreference(prefs, ConfigKey.EMAIL, String::class.java)
    val password = ConfigPreference(prefs, ConfigKey.PASSWORD, String::class.java)
    val loggedIn = ConfigPreference(prefs, ConfigKey.LOGGEDIN, Boolean::class.java)
    val isDatasetSelected = ConfigPreference(prefs, ConfigKey.DATASET_SELECTED, Boolean::class.java)
    val token = ConfigPreference(prefs, ConfigKey.TOKEN, String::class.java)
    val userId = ConfigPreference(prefs, ConfigKey.USER_ID, Long::class.java)
    val projectId = ConfigPreference(prefs, ConfigKey.PROJECT_ID, Long::class.java)
    val datasetId = ConfigPreference(prefs, ConfigKey.DATASET_ID, Long::class.java)
    val baseUrl = ConfigPreference(prefs, ConfigKey.BASE_URL, String::class.java)
    val userJson = ConfigPreference(prefs, ConfigKey.USER_MODEL, String::class.java)
    val amigoTokenJson = ConfigPreference(prefs, ConfigKey.AMIGO_TOKEN, String::class.java)
    val supportFilesHash = ConfigPreference(prefs, ConfigKey.SUPPORT_FILES_HASH, String::class.java)

    val storageDir: String = filesDir.path
    val webFormDir by lazy { mkdir( storageDir, "webform").let { "$storageDir/webform/" } }
    val photosDir by lazy { mkdir(storageDir, "photos").let { "$storageDir/photos/" } }
}


class ConfigPreference<T>(private val prefs: SharedPreferences,
                          private val key: ConfigKey,
                          private val clazz: Class<T>) {

    var value: T
        set(value) { value?.save(prefs, key.toString()) }
        get() = prefs.get(key.toString(), clazz)
}