package com.example.diplom

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.intercept.Interceptor
import com.example.diplom.ui.theme.UmamiOrange

class UmamiApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .components {
                // Добавляем интерцептор, который исправляет URL для эмулятора
                add(Interceptor { chain ->
                    val request = chain.request
                    val data = request.data
                    if (data is String && data.contains("127.0.0.1")) {
                        // Меняем localhost на ваш реальный IP, который доступен извне
                        val newUrl = data.replace("127.0.0.1", "188.233.238.70")
                        android.util.Log.d("UmamiCoil", "Replacing URL: $data -> $newUrl")
                        chain.proceed(request.newBuilder().data(newUrl).build())
                    } else {
                        android.util.Log.d("UmamiCoil", "Loading URL: $data")
                        chain.proceed(request)
                    }
                })
            }
            .build()
    }
}
