package com.runningcompanion.app

import android.app.Application
import com.runningcompanion.app.di.AppContainer
import org.osmdroid.config.Configuration

class RunningApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        Configuration.getInstance().userAgentValue = packageName
    }
}
