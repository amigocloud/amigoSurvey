package com.amigocloud.amigosurvey.models

import android.content.Context
import com.amigocloud.amigosurvey.repository.SurveyConfig
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import java.util.*

/**
 * Created by victor on 10/20/17.
 */
class AmigoToken {
    var access_token: String = ""
    var token_type: String = ""
    var expires_in: Long = 0
    var refresh_token: String = ""
    var scope: String = ""

    fun save(context: Context) {
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        val adapter = moshi.adapter(AmigoToken::class.java)
        val json = adapter.toJson(this)
        print(json)
        val sc =  SurveyConfig(context)
        sc.setAmigoTokenJSON(json)
    }
}