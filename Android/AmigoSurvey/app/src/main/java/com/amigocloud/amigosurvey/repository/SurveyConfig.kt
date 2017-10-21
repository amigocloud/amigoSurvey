package com.amigocloud.amigosurvey.repository

import android.content.Context
import android.os.Environment
import javax.inject.Inject
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.util.Log
import java.io.File


/**
 * Created by victor on 10/20/17.
 */

enum class ConfigKey {
    EMAIL,
    PASSWORD,
    LOGGEDIN,
    DATASET_SELECTED,
    TOKEN,
    USER_ID,
    PROJECT_ID,
    DATASET_ID,
    BASE_URL,
    USER_MODEL,
    AMIGO_TOKEN
}

class SurveyConfig(context: Context) {

    val LOG_TAG = "SurveyConfig"
    val PREFS_NAME = "AmigoSurveyPrefsFile"
    var context: Context? = null

    init {
        this.context = context
    }

//    @Inject
//    fun SurveyConfig(context: Context) {
//        this.context = context
//    }

    fun setString(value: String, forkey: String) {
        val settings = context?.getSharedPreferences(PREFS_NAME, 0)
        val editor = settings?.edit()
        editor?.putString(forkey, value)
        editor?.commit()
    }

    fun getString(forkey: String): String? {
        val settings = context?.getSharedPreferences(PREFS_NAME, 0)
        return settings?.getString(forkey, null)
    }

    fun setBoolean(value: Boolean, forkey: String) {
        val settings = context?.getSharedPreferences(PREFS_NAME, 0)
        val editor = settings?.edit()
        editor?.putBoolean(forkey, value)
        editor?.commit()
    }

    fun getBoolean(forkey: String): Boolean? {
        val settings = context?.getSharedPreferences(PREFS_NAME, 0)
        return settings?.getBoolean(forkey, false)
    }

    fun setLong(value: Long, forkey: String) {
        val settings = context?.getSharedPreferences(PREFS_NAME, 0)
        val editor = settings?.edit()
        editor?.putLong(forkey, value)
        editor?.commit()
    }

    fun getLong(forkey: String): Long? {
        val settings = context?.getSharedPreferences(PREFS_NAME, 0)
        return settings?.getLong(forkey, 0)
    }

    fun setEmail(email: String) {
        setString(email, forkey = ConfigKey.EMAIL.toString())
    }

    fun getEmail(): String? {
        return getString(forkey = ConfigKey.EMAIL.toString())
    }

    fun setPassword(password: String) {
        setString(password, forkey = ConfigKey.PASSWORD.toString())
    }

    fun getPassword(): String? {
        return getString(forkey = ConfigKey.PASSWORD.toString())
    }

    fun setLoggedin(loggedin: Boolean) {
        setBoolean(loggedin, forkey = ConfigKey.LOGGEDIN.toString())
    }

    fun isLoggedin(): Boolean? {
        return getBoolean(forkey = ConfigKey.LOGGEDIN.toString())
    }

    fun setDatasetSelected(selected: Boolean) {
        setBoolean(selected, forkey = ConfigKey.DATASET_SELECTED.toString())
    }

    fun isDatasetSelected(): Boolean? {
        return getBoolean(forkey = ConfigKey.DATASET_SELECTED.toString())
    }

    fun setToken(token: String) {
        setString(token, forkey = ConfigKey.TOKEN.toString())
    }

    fun setUserId(id: Long) {
        setLong(id, forkey = ConfigKey.USER_ID.toString())
    }

    fun getUserId(): Long? {
        return getLong(forkey = ConfigKey.USER_ID.toString())
    }

    fun setProjectId(id: Long) {
        setLong(id, forkey = ConfigKey.PROJECT_ID.toString())
    }

    fun getProjectId(): Long? {
        return getLong(forkey = ConfigKey.PROJECT_ID.toString())
    }

    fun setDatasetId(id: Long) {
        setLong(id, forkey = ConfigKey.DATASET_ID.toString())
        setDatasetSelected(selected = true)
    }

    fun getDatasetId(): Long? {
        return getLong(forkey = ConfigKey.DATASET_ID.toString())
    }

    fun mkdir(dirname: String): Boolean {
        // Get the directory for the user's public pictures directory.
        val file = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), dirname)
        if (!file.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created")
            return false
        }
        return true
    }

    fun getStorageDir(): String {
        return Environment.DIRECTORY_DOCUMENTS
    }

    fun getWebFormDir() : String {
        val documentsPath = getStorageDir()
        mkdir(dirname = "webform")
        return documentsPath  + "/webform"
    }

     fun getPhotoDirName() : String {
        mkdir(dirname = "photos")
        return "photos"
    }

     fun getPhotoDir() : String {
        val documentsPath = getStorageDir()
        mkdir(dirname = getPhotoDirName())
        return documentsPath  + "/" + getPhotoDirName()
    }

     fun setBaseURL(url: String) {
        setString(url, forkey = ConfigKey.BASE_URL.toString())
    }

     fun getBaseURL() : String? {
        return getString(forkey = ConfigKey.BASE_URL.toString())
    }

     fun setUserJSON(json: String) {
        setString(json, forkey = ConfigKey.USER_MODEL.toString())
    }

     fun getUserJSON() : String? {
        return getString(forkey = ConfigKey.USER_MODEL.toString())
    }

     fun setAmigoTokenJSON(json: String) {
        setString(json, forkey = ConfigKey.AMIGO_TOKEN.toString())
     }

     fun getAmigoTokenJSON() : String? {
        return getString(forkey = ConfigKey.AMIGO_TOKEN.toString())
    }

}