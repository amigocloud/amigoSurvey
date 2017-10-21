package com.amigocloud.amigosurvey

import android.app.Application
import com.amigocloud.amigosurvey.repository.SurveyConfig
import toothpick.Toothpick
import toothpick.config.Module
import javax.inject.Inject



/**
 * Created by victor on 10/20/17.
 */

class MyApplication : Application() {

    @Inject internal var config: SurveyConfig? = null

    override fun onCreate() {
        super.onCreate()

        val appScope = Toothpick.openScope(this)
        appScope.installModules(object : Module() {
            init {
//                bind(Machine::class.java).to(SurveyConfig::class.java)
            }
        })
        Toothpick.inject(this, appScope)
    }
}

