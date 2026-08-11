package com.zakiy.platform.network

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** يبني عملاء Retrofit/OkHttp لمرة وحدة (singletons) - وحد لباك إند ذكيّ
 * (توكن Bearer تلقائي من TokenHolder)، ووحد لـ Supabase Auth مباشرة
 * (apikey العام بس). */
object NetworkModule {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }
    private val jsonMediaType = "application/json".toMediaType()

    private val backendAuthInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder().apply {
            TokenHolder.accessToken?.let { addHeader("Authorization", "Bearer $it") }
        }.build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val backendClient = OkHttpClient.Builder()
        .addInterceptor(backendAuthInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val goTrueInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("apikey", ApiConfig.SUPABASE_ANON_KEY)
            .build()
        chain.proceed(request)
    }

    private val goTrueClient = OkHttpClient.Builder()
        .addInterceptor(goTrueInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val backendApi: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("${ApiConfig.API_BASE}/")
            .client(backendClient)
            .addConverterFactory(json.asConverterFactory(jsonMediaType))
            .build()
            .create(ApiService::class.java)
    }

    val goTrueApi: GoTrueApi by lazy {
        Retrofit.Builder()
            .baseUrl("${ApiConfig.SUPABASE_URL}/")
            .client(goTrueClient)
            .addConverterFactory(json.asConverterFactory(jsonMediaType))
            .build()
            .create(GoTrueApi::class.java)
    }
}
