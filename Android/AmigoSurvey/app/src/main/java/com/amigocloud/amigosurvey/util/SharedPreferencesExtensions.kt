package com.amigocloud.amigosurvey.util

import android.content.SharedPreferences

fun SharedPreferences.setString(value: String, forkey: String) = edit().putString(forkey, value).apply()

fun SharedPreferences.getString(forkey: String) = getString(forkey, "")

fun SharedPreferences.setBoolean(value: Boolean, forkey: String) = edit().putBoolean(forkey, value).apply()

fun SharedPreferences.getBoolean(forkey: String) = getBoolean(forkey, false)

fun SharedPreferences.setLong(value: Long, forkey: String) = edit().putLong(forkey, value).apply()

fun SharedPreferences.getLong(forkey: String) = getLong(forkey, 0)

@Suppress("UNCHECKED_CAST")
fun <T> SharedPreferences.get(key: String, clazz: Class<T>): T = when {
    clazz.isAssignableFrom(String::class.java) -> getString(key) as T
    clazz.isAssignableFrom(Long::class.java) -> getLong(key) as T
    clazz.isAssignableFrom(Boolean::class.java) -> getBoolean(key) as T
    else -> throw IllegalArgumentException("SharedPreferences.get not implemented for ${clazz.simpleName}")
}

fun String.save(prefs: SharedPreferences, key: String) = prefs.setString(this, key)

fun Boolean.save(prefs: SharedPreferences, key: String) = prefs.setBoolean(this, key)

fun Long.save(prefs: SharedPreferences, key: String) = prefs.setLong(this, key)

fun Any.save(prefs: SharedPreferences, key: String) = when(this) {
    is String -> save(prefs, key)
    is Boolean -> save(prefs, key)
    is Long -> save(prefs, key)
    else -> throw IllegalArgumentException("save not implemented for ${this::class.simpleName}")
}