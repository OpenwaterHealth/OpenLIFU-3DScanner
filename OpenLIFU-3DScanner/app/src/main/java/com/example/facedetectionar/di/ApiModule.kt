package com.example.facedetectionar.di

import android.util.Log
import com.example.facedetectionar.api.AuthService
import com.example.facedetectionar.api.PhotocollectionService
import com.example.facedetectionar.api.PhotoscanService
import com.example.facedetectionar.api.UserService
import com.example.facedetectionar.api.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApiModule {

    @Provides
    @Singleton
    fun provideUserRepository(
        authService: AuthService,
        userService: UserService,
        photocollectionService: PhotocollectionService
    ): UserRepository {
        return UserRepository(authService, userService, photocollectionService)
    }

    @Provides
    fun provideOkHttpClient(authService: AuthService): OkHttpClient {
        val httpClient = OkHttpClient.Builder()

        val logging = HttpLoggingInterceptor { message: String? ->
            Log.d("OkHttp", message ?: "")
        }
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        httpClient.connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        httpClient.readTimeout(TIMEOUT, TimeUnit.SECONDS)
        httpClient.writeTimeout(TIMEOUT, TimeUnit.SECONDS)

        httpClient.addInterceptor { chain ->
            val original = chain.request()

            var requestBuilder = original.newBuilder()

            runBlocking {
                authService.getToken()?.let {
                    requestBuilder = requestBuilder.header("Authorization", "Bearer $it")
                }
            }

            chain.proceed(requestBuilder.build())
        }
            .addInterceptor(logging)

        return httpClient.build()
    }

    @Provides
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        val builder = Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
        return builder.client(client).build()
    }

    @Provides
    fun providePhotocollectionService(retrofit: Retrofit): PhotocollectionService {
        return retrofit.create(PhotocollectionService::class.java)
    }

    @Provides
    fun providePhotoscanService(retrofit: Retrofit): PhotoscanService {
        return retrofit.create(PhotoscanService::class.java)
    }

    @Provides
    fun provideUsersService(retrofit: Retrofit): UserService {
        return retrofit.create(UserService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthService(): AuthService {
        return AuthService()
    }

    companion object {
        private const val TIMEOUT = 30L
        private const val API_URL = "https://api.nvpsoftware.com"
    }
}