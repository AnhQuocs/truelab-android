package dev.anhquocs.truelab.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.data.BuildConfig
import dev.anhquocs.truelab.core.data.match.remote.api.MatchApi
import dev.anhquocs.truelab.core.data.odds.remote.api.OddsApi
import dev.anhquocs.truelab.core.data.ranking.remote.api.RankingApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val newRequest = originalRequest.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept-Language", "vi")
                    .header("device_id", "truelab_internal_uuid_2026")
                    .build()
                chain.proceed(newRequest)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideMatchApi(retrofit: Retrofit): MatchApi =
        retrofit.create(MatchApi::class.java)

    @Provides
    @Singleton
    fun provideOddsApi(retrofit: Retrofit): OddsApi =
        retrofit.create(OddsApi::class.java)

    @Provides
    @Singleton
    fun provideRankingApi(retrofit: Retrofit): RankingApi =
        retrofit.create(RankingApi::class.java)
}
