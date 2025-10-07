package com.kasumi.boots

import android.app.Application
import com.topjohnwu.superuser.Shell

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Configure libsu main shell
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setTimeout(10)
                .setFlags(Shell.FLAG_MOUNT_MASTER)
        )
    }
}
