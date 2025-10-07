package com.kasumi.boots.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.kasumi.boots.R
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {
    
    private lateinit var tvStatus: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvLog: TextView
    private lateinit var btnBoost: Button
    private lateinit var progress: ProgressBar
    
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    
    private suspend fun obtainShell(): Shell = suspendCancellableCoroutine { cont ->
        Shell.getShell { shell -> cont.resume(shell) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        tvStatus = findViewById(R.id.tvStatus)
        tvDescription = findViewById(R.id.tvDescription)
        tvLog = findViewById(R.id.tvLog)
        btnBoost = findViewById(R.id.btnBoost)
        progress = findViewById(R.id.progress)
        
        // Lazy check root only when user taps Boost
        tvStatus.text = getString(R.string.status_checking)

        btnBoost.setOnClickListener {
            mainScope.launch {
                val hasRoot = withContext(Dispatchers.IO) {
                    try {
                        obtainShell().isRoot
                    } catch (e: Exception) {
                        appendLog("EXCEPTION: ${e.message}")
                        false
                    }
                }
                if (hasRoot) {
                    tvStatus.text = getString(R.string.status_root_granted)
                    performBoost()
                } else {
                    tvStatus.text = getString(R.string.status_root_denied)
                    appendLog("Root access denied or unavailable")
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
    
    private fun checkRoot() {
        tvStatus.text = getString(R.string.status_checking)
        
        mainScope.launch {
            try {
                val shell = withContext(Dispatchers.IO) { obtainShell() }
                val hasRoot = shell.isRoot
                if (hasRoot) {
                    tvStatus.text = getString(R.string.status_root_granted)
                    btnBoost.isEnabled = true
                } else {
                    tvStatus.text = getString(R.string.status_root_denied)
                    btnBoost.isEnabled = false
                    appendLog("ERROR: Root access denied or unavailable")
                }
            } catch (e: Exception) {
                tvStatus.text = getString(R.string.status_root_denied)
                btnBoost.isEnabled = false
                appendLog("EXCEPTION: ${e.message}")
            }
        }
    }
    
    private fun performBoost() {
        btnBoost.isEnabled = false
        progress.visibility = View.VISIBLE
        tvStatus.text = getString(R.string.status_boosting)
        tvLog.text = ""
        
        mainScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Execute boost script safely from InputStream (recommended by libsu)
                    Shell.cmd(resources.openRawResource(R.raw.boost)).exec()
                }
                
                // Display output
                if (result.isSuccess) {
                    result.out.forEach { line ->
                        appendLog(line)
                    }
                    
                    if (result.err.isNotEmpty()) {
                        appendLog("\n--- Errors ---")
                        result.err.forEach { line ->
                            appendLog(line)
                        }
                    }
                    
                    tvStatus.text = getString(R.string.status_done)
                } else {
                    appendLog("ERROR: Boost script failed")
                    result.err.forEach { line ->
                        appendLog(line)
                    }
                    tvStatus.text = "Failed to apply boost"
                }
                
            } catch (e: Exception) {
                appendLog("EXCEPTION: ${e.message}")
                tvStatus.text = "Error occurred"
            } finally {
                progress.visibility = View.GONE
                btnBoost.isEnabled = true
            }
        }
    }
    
    private fun appendLog(text: String) {
        runOnUiThread {
            val current = tvLog.text.toString()
            tvLog.text = if (current.isEmpty()) text else "$current\n$text"
        }
    }
}
