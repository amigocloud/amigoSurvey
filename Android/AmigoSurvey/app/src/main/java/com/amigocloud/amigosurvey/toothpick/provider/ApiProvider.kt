package com.amigocloud.amigosurvey.toothpick.provider

import com.amigocloud.amigosurvey.repository.AmigoApi
import retrofit2.Retrofit
import toothpick.ProvidesSingletonInScope
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
@ProvidesSingletonInScope
class ApiProvider @Inject constructor(private val retrofit: Retrofit) : Provider<AmigoApi> {
    override fun get(): AmigoApi = retrofit.create(AmigoApi::class.java)
}
