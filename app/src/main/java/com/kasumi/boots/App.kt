package com.kasumi.boots

import android.app.Application
import android.content.res.Configuration
import com.topjohnwu.superuser.Shell
import java.util.*

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Set Vietnamese as default locale
        val locale = Locale("vi")
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        
        // Configure libsu main shell
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setTimeout(10)
                .setFlags(Shell.FLAG_MOUNT_MASTER)
        )
        // Preload root shell early so user gets 1-click experience later
        Shell.getShell { /* warm up */ }
    }
}
