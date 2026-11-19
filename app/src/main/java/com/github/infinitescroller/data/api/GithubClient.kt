package com.github.infinitescroller.data.api

import com.github.infinitescroller.BuildConfig
import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object GithubClient {

    private const val BASE_URL = "https://api.github.com/"
    private const val CACHE_SIZE_BYTES = 10L * 1024 * 1024

    /**
     * @param tokenProvider returns the user's personal access token, or null/blank for
     *   unauthenticated requests. Called on OkHttp's background threads.
     */
    fun create(moshi: Moshi, cacheDir: File, tokenProvider: () -> String?): GithubApi {
        val client = OkHttpClient.Builder()
            .cache(Cache(File(cacheDir, "http"), CACHE_SIZE_BYTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("User-Agent", "GitHub-Infinite-Scroller-Android/${BuildConfig.VERSION_NAME}")
                tokenProvider()?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    builder.header("Authorization", "Bearer $it")
                }
                chain.proceed(builder.build())
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                            redactHeader("Authorization")
                        },
                    )
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GithubApi::class.java)
    }
}
