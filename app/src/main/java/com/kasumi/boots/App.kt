package com.kasumi.boots

import android.app.Application
import android.util.Log
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Global crash logger to help diagnose startup issues
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val log = "Thread: ${thread.name}\n" + sw.toString()
                val f = File(cacheDir, "crash.log")
                f.writeText(log)
                // Also write to external app files for easier access without root
                try {
                    val ext = getExternalFilesDir(null)
                    if (ext != null) {
                        File(ext, "crash.log").writeText(log)
                    }
                } catch (_: Exception) { }
                Log.e("KasumiBoots", "Uncaught exception captured: ${throwable.message}")
            } catch (e: Exception) {
                // ignore
            }
        }

        // Configure libsu main shell
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setTimeout(10)
                .setFlags(Shell.FLAG_MOUNT_MASTER)
        )
    }
}
