package com.connor.asyncdownload

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        app = this
    }

    companion object {
        lateinit var app: App private set
    }
}