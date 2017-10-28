package com.amigocloud.amigosurvey.toothpick.provider

import com.amigocloud.amigosurvey.repository.AmigoClient
import com.squareup.moshi.Moshi
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import toothpick.ProvidesSingletonInScope
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton


@Singleton
@ProvidesSingletonInScope
class RetrofitProvider @Inject constructor(private val httpClient: OkHttpClient,
                                           private val moshi: Moshi) : Provider<Retrofit> {
    override fun get(): Retrofit = Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(AmigoClient.base_url)
            .client(httpClient)
            .build()
}
