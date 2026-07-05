package com.kyrx.mypresence.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.kyrx.mypresence.data.local.PresenceDatabase
import com.kyrx.mypresence.data.remote.DiscordApi
import com.kyrx.mypresence.data.repository.AuthRepositoryImpl
import com.kyrx.mypresence.data.repository.GoogleAuthRepositoryImpl
import com.kyrx.mypresence.data.repository.PreferencesRepositoryImpl
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.GoogleAuthRepository
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
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
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
    fun provideRoomDatabase(@ApplicationContext context: Context): PresenceDatabase {
        return Room.databaseBuilder(
            context,
            PresenceDatabase::class.java,
            "mypresence_db"
        ).build()
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
    fun provideHttpClient(json: Json): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
            install(WebSockets)
        }
    }

    @Provides
    @Singleton
    fun provideDiscordApi(httpClient: HttpClient, json: Json): DiscordApi {
        return DiscordApi(httpClient, json)
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
    abstract fun bindGoogleAuthRepository(impl: GoogleAuthRepositoryImpl): GoogleAuthRepository
}
