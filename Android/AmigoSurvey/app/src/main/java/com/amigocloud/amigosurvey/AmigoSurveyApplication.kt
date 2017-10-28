package com.amigocloud.amigosurvey

import android.app.Application
import com.amigocloud.amigosurvey.toothpick.ApplicationModule
import toothpick.Toothpick
import toothpick.configuration.Configuration
import toothpick.registries.FactoryRegistryLocator
import toothpick.registries.MemberInjectorRegistryLocator
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


