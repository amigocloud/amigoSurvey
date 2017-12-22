package com.amigocloud.amigosurvey.util

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.view.LayoutInflater

val Context.layoutInflater: LayoutInflater get() = LayoutInflater.from(this)

fun ConnectivityManager.isConnected(): Boolean {
    try {
        return activeNetworkInfo?.isConnectedOrConnecting?: false
    } catch (e: Exception) {
        Log.w(TAG, e.toString())
    }
    return false
}