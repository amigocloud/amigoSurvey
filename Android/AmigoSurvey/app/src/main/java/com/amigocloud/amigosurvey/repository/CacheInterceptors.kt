package com.amigocloud.amigosurvey.repository

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import retrofit2.adapter.rxjava2.Result.response
import com.amigocloud.amigosurvey.R.id.header



class CacheInterceptor constructor(private val connectivityManager: ConnectivityManager) : Interceptor {
    val TAG = "CacheInterceptor"
    val HEADER_CACHE_CONTROL = "Cache-Control"
    val HEADER_PRAGMA = "Pragma"

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val cacheControl: CacheControl

        if (isConnected()) {
            cacheControl = CacheControl.Builder()
                    .maxAge(0, TimeUnit.SECONDS)
                    .build()
        } else {
            cacheControl = CacheControl.Builder()
                    .maxStale(7, TimeUnit.DAYS)
                    .build()
        }

        return response.newBuilder()
                .removeHeader(HEADER_PRAGMA)
                .removeHeader(HEADER_CACHE_CONTROL)
                .header(HEADER_CACHE_CONTROL, cacheControl.toString())
                .build()
    }

    fun isConnected(): Boolean {
        try {
            val activeNetwork = connectivityManager.activeNetworkInfo
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
        }
        return false
    }
}

class OfflineCacheInterceptor @Inject constructor(private val connectivityManager: ConnectivityManager) : Interceptor {

    val TAG = "OfflineCacheInterceptor"
    val HEADER_CACHE_CONTROL = "Cache-Control"
    val HEADER_PRAGMA = "Pragma"

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (!isConnected()) {
            val cacheControl = CacheControl.Builder()
                    .maxStale(7, TimeUnit.DAYS)
                    .build()

            request = request.newBuilder()
                    .removeHeader(HEADER_PRAGMA)
                    .removeHeader(HEADER_CACHE_CONTROL)
                    .cacheControl(cacheControl)
                    .build()
        }

        return chain.proceed(request)
    }

    fun isConnected(): Boolean {
        try {
            val activeNetwork = connectivityManager.activeNetworkInfo
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
        }
        return false
    }
}