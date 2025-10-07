package com.kasumi.boots

import android.app.Application
import com.topjohnwu.superuser.Shell

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
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
