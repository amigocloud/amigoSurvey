package com.amigocloud.amigosurvey.toothpick.provider

import android.net.ConnectivityManager
import android.util.Log
import com.amigocloud.amigosurvey.BuildConfig
import com.amigocloud.amigosurvey.repository.AmigoAuthenticator
import com.amigocloud.amigosurvey.repository.CacheInterceptor
import com.amigocloud.amigosurvey.repository.OfflineCacheInterceptor
import com.amigocloud.amigosurvey.toothpick.CacheDir
import com.facebook.stetho.okhttp3.StethoInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import toothpick.ProvidesSingletonInScope
import java.io.File
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton



@Singleton
@ProvidesSingletonInScope
class HttpClientProvider @Inject constructor(private val authenticator: AmigoAuthenticator,
                                             @CacheDir val cacheDir: File,
                                             private val connectivityManager: ConnectivityManager) : Provider<OkHttpClient> {

    val TAG = "HttpClientProvider"
    val cacheInterceptor = CacheInterceptor(connectivityManager)
    val offlineCacheInterceptor = OfflineCacheInterceptor(connectivityManager)
    val logging = HttpLoggingInterceptor()

    override fun get(): OkHttpClient = OkHttpClient.Builder()
            .authenticator(authenticator)
            .addInterceptor(authenticator)
            .addInterceptor(offlineCacheInterceptor)
            .addNetworkInterceptor(cacheInterceptor)
            .cache(provideCache())
            .apply { if (BuildConfig.DEBUG) addNetworkInterceptor(StethoInterceptor()) }
//            .addInterceptor(logging)
            .build()

    private fun provideCache(): Cache? {
        logging.level = HttpLoggingInterceptor.Level.BASIC
        var cache: Cache? = null

        try {
            cache = Cache( File(cacheDir.absoluteFile, "http-cache"),
                    (1000 * 1024 * 1024).toLong()) // 1000 MB
        } catch (e: Exception) {
            Log.e(TAG, "Could not create Cache!")
        }

        return cache
    }

}


