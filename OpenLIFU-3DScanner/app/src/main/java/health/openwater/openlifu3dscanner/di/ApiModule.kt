package health.openwater.openlifu3dscanner.di

import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import android.content.Context
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import health.openwater.openlifu3dscanner.core.ConnectivityObserver
import health.openwater.openlifu3dscanner.network.adapter.DateTypeAdapter
import health.openwater.openlifu3dscanner.network.api.AuthService
import health.openwater.openlifu3dscanner.network.api.PhotocollectionService
import health.openwater.openlifu3dscanner.network.api.PhotoscanService
import health.openwater.openlifu3dscanner.network.api.SubjectService
import health.openwater.openlifu3dscanner.network.api.UserService
import health.openwater.openlifu3dscanner.network.api.WebsocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WebSocketScope

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    private const val CONNECT_TIMEOUT = 5L
    private const val READ_TIMEOUT = 15L
    private const val WRITE_TIMEOUT = 15L

    const val API_URL = "https://api.openwater.health/"

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(Date::class.java, DateTypeAdapter)
        .create()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor { message -> Log.d("OkHttp", message) }
            .apply { setLevel(HttpLoggingInterceptor.Level.BODY) }

    @Provides
    @Singleton
    fun provideAuthenticator(authService: AuthService): Authenticator =
        Authenticator { _, response ->
            if (response.code == 401) {
                authService.signOut()
                null
            } else {
                null
            }
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authService: AuthService,
        loggingInterceptor: HttpLoggingInterceptor,
        authenticator: Authenticator
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .authenticator(authenticator)
        .addInterceptor { chain ->
            val original = chain.request()
            val token = authService.getTokenSync()
            val request = if (token != null) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        gson: Gson
    ): Retrofit = Retrofit.Builder()
        .baseUrl(API_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun providePhotocollectionService(retrofit: Retrofit): PhotocollectionService =
        retrofit.create(PhotocollectionService::class.java)

    @Provides
    @Singleton
    fun providePhotoscanService(retrofit: Retrofit): PhotoscanService =
        retrofit.create(PhotoscanService::class.java)

    @Provides
    @Singleton
    fun provideUserService(retrofit: Retrofit): UserService =
        retrofit.create(UserService::class.java)

    @Provides
    @Singleton
    fun provideSubjectService(retrofit: Retrofit): SubjectService =
        retrofit.create(SubjectService::class.java)

    @Provides
    @Singleton
    fun provideAuthService(): AuthService = AuthService()

    @Provides
    @Singleton
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver =
        ConnectivityObserver(context)

    @Provides
    @Singleton
    @WebSocketScope
    fun provideWebSocketCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Provides
    @Singleton
    fun provideWebsocketService(
        authService: AuthService,
        @WebSocketScope scope: CoroutineScope
    ): WebsocketService = WebsocketService(authService, scope)
}