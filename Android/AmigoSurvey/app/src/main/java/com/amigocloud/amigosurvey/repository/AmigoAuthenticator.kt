package com.amigocloud.amigosurvey.repository

import okhttp3.*
import toothpick.Lazy
import javax.inject.Inject
import javax.inject.Singleton

private const val AUTH = "Authorization"

@Singleton
class AmigoAuthenticator @Inject constructor(private val lazyAmigoRest: Lazy<AmigoRest>) : Authenticator, Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val url = request.url()
        val rest = lazyAmigoRest.get()

        rest.apiToken?.let {
            val newUrl = url.newBuilder().addQueryParameter("token", it).build()
            request = request.newBuilder().url(newUrl).build()
        } ?: rest.authHeader?.let {
            request = request.newBuilder()
                    .addHeader(AUTH, it)
                    .build()
        }

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
