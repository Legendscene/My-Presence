package com.kyrx.mypresence.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.kyrx.mypresence.core.network.DnsProvider
import com.kyrx.mypresence.data.local.PresenceDatabase
import com.kyrx.mypresence.data.remote.DiscordApi
import com.kyrx.mypresence.data.remote.DiscordGateway
import com.kyrx.mypresence.data.repository.AuthRepositoryImpl
import com.kyrx.mypresence.data.repository.PreferencesRepositoryImpl
import com.kyrx.mypresence.domain.repository.AuthRepository
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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mypresence_prefs")

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
    fun provideDiscordApi(httpClient: HttpClient, json: Json): DiscordApi {
        return DiscordApi(httpClient, json)
    }

    @Provides
    @Singleton
    fun provideDiscordGateway(json: Json, okHttpClient: OkHttpClient): DiscordGateway {
        return DiscordGateway(json, okHttpClient)
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
}
