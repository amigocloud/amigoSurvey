package com.amigocloud.amigosurvey.toothpick

import android.app.Application
import com.amigocloud.amigosurvey.repository.AmigoApi
import com.amigocloud.amigosurvey.toothpick.provider.ApiProvider
import com.amigocloud.amigosurvey.toothpick.provider.HttpClientProvider
import com.amigocloud.amigosurvey.toothpick.provider.RetrofitProvider
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import ru.solodovnikov.rx2locationmanager.RxLocationManager
import toothpick.smoothie.module.SmoothieApplicationModule
import java.io.File
import javax.inject.Qualifier

@Qualifier annotation class FileDir
@Qualifier annotation class CacheDir

class ApplicationModule(app: Application) : SmoothieApplicationModule(app) {
    init {
        bind(Moshi::class.java).toInstance(Moshi.Builder().add(KotlinJsonAdapterFactory()).build())
        bind(OkHttpClient::class.java).toProvider(HttpClientProvider::class.java)
        bind(Retrofit::class.java).toProvider(RetrofitProvider::class.java)
        bind(AmigoApi::class.java).toProvider(ApiProvider::class.java)
        bind(RxLocationManager::class.java).toInstance(RxLocationManager(app.applicationContext))
        bind(File::class.java).withName(FileDir::class.java).toInstance(app.filesDir)
        bind(File::class.java).withName(CacheDir::class.java).toInstance(app.cacheDir)
    }
}
