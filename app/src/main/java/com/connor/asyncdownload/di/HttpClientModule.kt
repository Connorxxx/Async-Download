package com.connor.asyncdownload.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.http.*
import okhttp3.Cache
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HttpClientModule {

    @Singleton
    @Provides
    fun provideHttpClient(@ApplicationContext ctx: Context) = HttpClient(OkHttp) {
//        defaultRequest {
//            url {
//                protocol = URLProtocol.HTTP
//                host = "192.168.10.1"
//            }
//        }
        engine {
            threadsCount = 32
            clientCacheSize = 1024 * 1204 * 128
            config {
                connectTimeout(60, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
                cache(Cache(ctx.cacheDir, 1024 * 1204 * 128))
                followRedirects(true)
            }
        }
        install(ContentEncoding) {
            deflate(1.0F)
            gzip(0.9F)
        }
    }
}