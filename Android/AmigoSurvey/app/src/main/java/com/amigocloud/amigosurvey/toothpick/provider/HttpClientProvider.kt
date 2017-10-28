package com.amigocloud.amigosurvey.toothpick.provider

import com.amigocloud.amigosurvey.repository.AmigoAuthenticator
import okhttp3.OkHttpClient
import toothpick.ProvidesSingletonInScope
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton


@Singleton
@ProvidesSingletonInScope
class HttpClientProvider @Inject constructor(private val authenticator: AmigoAuthenticator) : Provider<OkHttpClient> {
    override fun get(): OkHttpClient = OkHttpClient.Builder()
            .authenticator(authenticator)
            .addInterceptor(authenticator)
            .build()
}
