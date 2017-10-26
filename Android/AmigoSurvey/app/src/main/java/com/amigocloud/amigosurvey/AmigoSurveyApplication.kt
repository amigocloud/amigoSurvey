package com.amigocloud.amigosurvey

import android.app.Application
import com.amigocloud.amigosurvey.repository.*
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import toothpick.Toothpick
import toothpick.config.Module
import toothpick.configuration.Configuration
import toothpick.registries.FactoryRegistryLocator
import toothpick.registries.MemberInjectorRegistryLocator
import toothpick.smoothie.module.SmoothieApplicationModule
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Toothpick.setConfiguration(Configuration.forProduction().disableReflection())
        FactoryRegistryLocator.setRootRegistry(FactoryRegistry())
        MemberInjectorRegistryLocator.setRootRegistry(MemberInjectorRegistry())

        Toothpick.openScope(ApplicationScope::class.java).apply {
            installModules(ApplicationModule(this@MyApplication))
        }
    }
}

class ApplicationModule(app: Application) : SmoothieApplicationModule(app) {
    init {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .baseUrl(AmigoClient.base_url)
                .build()

        bind(Moshi::class.java).toInstance(moshi)
        bind(AmigoApi::class.java).toInstance(retrofit.create(AmigoApi::class.java))
    }
}

