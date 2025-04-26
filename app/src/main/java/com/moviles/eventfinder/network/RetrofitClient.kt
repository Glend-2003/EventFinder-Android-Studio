package com.moviles.eventfinder.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.content.Context
import com.moviles.eventfinder.common.NetworkUtils
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import java.io.File

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:5275/"
    private const val CACHE_SIZE = 10 * 1024 * 1024L // 10MB

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)

    fun createClient(context: Context): Retrofit {
        // Configurar el caché
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, CACHE_SIZE)

        // Interceptor para logging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Interceptor para modo offline
        val offlineCacheInterceptor = Interceptor { chain ->
            var request = chain.request()

            if (!NetworkUtils.isNetworkAvailable(context)) {
                // Si no hay red, intentar usar caché por 7 días
                val cacheControl = CacheControl.Builder()
                    .maxStale(7, TimeUnit.DAYS)
                    .build()

                request = request.newBuilder()
                    .cacheControl(cacheControl)
                    .build()
            }

            chain.proceed(request)
        }

        // Interceptor para respuestas en caché
        val networkCacheInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())

            // Almacenar en caché por 1 hora
            val cacheControl = CacheControl.Builder()
                .maxAge(1, TimeUnit.HOURS)
                .build()

            response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", cacheControl.toString())
                .build()
        }

        // Construir cliente OkHttp con caché
        val okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(offlineCacheInterceptor)
            .addNetworkInterceptor(networkCacheInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // Construir y devolver Retrofit
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}