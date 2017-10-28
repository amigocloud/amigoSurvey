package com.amigocloud.amigosurvey.repository

import okhttp3.*
import toothpick.Lazy
import javax.inject.Inject
import javax.inject.Singleton

private const val AUTH = "Authorization"

@Singleton
class AmigoAuthenticator @Inject constructor(private val lazyAmigoRest: Lazy<AmigoRest>) : Authenticator, Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = lazyAmigoRest.get().authHeader?.let {
            chain.request().newBuilder()
                    .addHeader(AUTH, it)
                    .build()
        } ?: chain.request()
        return chain.proceed(request)
    }

    override fun authenticate(route: Route, response: Response): Request? {
        val amigoRest = lazyAmigoRest.get()

        if (amigoRest.authHeader == null || response.request().header(AUTH) != null) {
            return null
        }

        return response.request().newBuilder()
                .header(AUTH, amigoRest.refreshToken().blockingGet().header)
                .build()
    }
}
