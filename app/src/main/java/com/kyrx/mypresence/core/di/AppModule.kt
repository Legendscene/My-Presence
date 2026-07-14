package com.kyrx.mypresence.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import okio.Path.Companion.toOkioPath
import com.kyrx.mypresence.core.analytics.CrashReporter
import com.kyrx.mypresence.core.di.dataStore
import androidx.datastore.preferences.core.Preferences
import com.kyrx.mypresence.core.gateway.GatewayConfig
import com.kyrx.mypresence.core.gateway.GatewayEngine
import com.kyrx.mypresence.core.network.DnsProvider
import com.kyrx.mypresence.data.gateway.DiscordGatewayEngine
import com.kyrx.mypresence.data.gateway.GatewayRepositoryImpl
import com.kyrx.mypresence.data.remote.DiscordApi
import com.kyrx.mypresence.data.repository.AppRepositoryImpl
import com.kyrx.mypresence.data.repository.AssetRepositoryImpl
import com.kyrx.mypresence.data.repository.AuthRepositoryImpl
import com.kyrx.mypresence.data.repository.PreferencesRepositoryImpl
import com.kyrx.mypresence.domain.repository.AppRepository
import com.kyrx.mypresence.domain.repository.AssetRepository
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.GatewayRepository
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(dns: DnsProvider): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(dns)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json, dns: DnsProvider): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    this.dns(dns)
                    addInterceptor { chain ->
                        chain.proceed(chain.request().newBuilder()
                            .header("User-Agent", "MyPresence/1.0 (Android)")
                            .build())
                    }
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    @Provides
    @Singleton
    fun provideDiscordApi(httpClient: HttpClient, json: Json, crashReporter: CrashReporter): DiscordApi {
        return DiscordApi(httpClient, json, crashReporter)
    }

    @Provides
    @Singleton
    fun provideGatewayConfig(): GatewayConfig {
        return GatewayConfig()
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image-cache").toOkioPath())
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideGatewayEngine(
        json: Json,
        okHttpClient: OkHttpClient,
        crashReporter: CrashReporter,
        discordApi: DiscordApi,
        config: GatewayConfig
    ): GatewayEngine {
        return DiscordGatewayEngine(json, okHttpClient, crashReporter, discordApi, config)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository

    @Binds
    @Singleton
    abstract fun bindGatewayRepository(impl: GatewayRepositoryImpl): GatewayRepository

    @Binds
    @Singleton
    abstract fun bindAssetRepository(impl: AssetRepositoryImpl): AssetRepository
}
