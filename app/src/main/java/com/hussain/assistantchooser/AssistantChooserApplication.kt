package com.hussain.assistantchooser

import android.app.Application
import com.hussain.assistantchooser.data.loadApps
import java.util.concurrent.Executors

class AssistantChooserApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Executors.newSingleThreadExecutor().execute { loadApps(packageManager) }
    }
}
