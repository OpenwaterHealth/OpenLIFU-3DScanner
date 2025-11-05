package health.openwater.openlifu3dscanner.di

import android.util.Log
import health.openwater.openlifu3dscanner.api.AuthService
import health.openwater.openlifu3dscanner.api.PhotocollectionService
import health.openwater.openlifu3dscanner.api.PhotoscanService
import health.openwater.openlifu3dscanner.api.UserService
import health.openwater.openlifu3dscanner.api.WebsocketService
import health.openwater.openlifu3dscanner.api.repository.CloudRepository
import health.openwater.openlifu3dscanner.api.repository.UserRepository
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    @Singleton
    fun provideReconstructionRepository(
        authService: AuthService,
        photocollectionService: PhotocollectionService,
        photoscanService: PhotoscanService,
        websocketService: WebsocketService,
        userRepository: UserRepository
    ): CloudRepository {
        return CloudRepository(
            authService, photocollectionService, photoscanService, websocketService, userRepository
        )
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
            val response = chain.proceed(requestBuilder.build())

            if (response.code == 401) {
                authService.signOut()
            }

            response
        }
            .addInterceptor(logging)

        return httpClient.build()
    }

    @Provides
    fun provideGson(): Gson {
        return GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()
    }

    @Provides
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit {
        val builder = Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
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

    @Provides
    fun provideWebsocketService(authService: AuthService): WebsocketService {
        val scope = CoroutineScope(Dispatchers.Main)
        return WebsocketService(authService, scope)
    }

    companion object {
        private const val TIMEOUT = 30L
        const val API_URL = "https://api.nvpsoftware.com"
    }
}