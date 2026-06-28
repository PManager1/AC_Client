package com.example.birdy

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.birdy.data.LocationManager
import com.example.birdy.ui.store.DataUriFetcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient

class BirdyApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        LocationManager.init(this)
        Log.d("BirdyApp", "✅ LocationManager initialized")
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(DataUriFetcher.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor(Interceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("User-Agent", "Birdy/1.0 (Android)")
                            .header("Accept", "image/*,*/*;q=0.8")
                            .build()
                        chain.proceed(request)
                    })
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}