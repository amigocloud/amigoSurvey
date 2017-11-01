package com.amigocloud.amigosurvey

import android.app.Application
import com.amigocloud.amigosurvey.toothpick.ApplicationModule
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.facebook.stetho.Stetho
import okhttp3.OkHttpClient
import toothpick.Toothpick
import toothpick.configuration.Configuration
import toothpick.registries.FactoryRegistryLocator
import toothpick.registries.MemberInjectorRegistryLocator
import java.io.InputStream
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Stetho.initializeWithDefaults(this)

        Toothpick.setConfiguration(Configuration.forProduction().disableReflection())
        FactoryRegistryLocator.setRootRegistry(FactoryRegistry())
        MemberInjectorRegistryLocator.setRootRegistry(MemberInjectorRegistry())

        Toothpick.openScope(ApplicationScope::class.java).apply {
            installModules(ApplicationModule(this@MyApplication))

            Glide.get(applicationContext).registry.append(
                    GlideUrl::class.java,
                    InputStream::class.java,
                    OkHttpUrlLoader.Factory(getInstance(OkHttpClient::class.java)))
        }
    }
}


