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

class MainActivity : AppCompatActivity() {
    
    private lateinit var tvStatus: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvLog: TextView
    private lateinit var btnBoost: Button
    private lateinit var progress: ProgressBar
    
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        tvStatus = findViewById(R.id.tvStatus)
        tvDescription = findViewById(R.id.tvDescription)
        tvLog = findViewById(R.id.tvLog)
        btnBoost = findViewById(R.id.btnBoost)
        progress = findViewById(R.id.progress)
        
        // Check root on start
        checkRoot()
        
        // Set button click listener
        btnBoost.setOnClickListener {
            performBoost()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
    
    private fun checkRoot() {
        tvStatus.text = getString(R.string.status_checking)
        
        mainScope.launch {
            val hasRoot = withContext(Dispatchers.IO) {
                Shell.getShell().isRoot
            }
            
            if (hasRoot) {
                tvStatus.text = getString(R.string.status_root_granted)
                btnBoost.isEnabled = true
            } else {
                tvStatus.text = getString(R.string.status_root_denied)
                btnBoost.isEnabled = false
                appendLog("ERROR: Root access denied or unavailable")
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
                    // Read boost script from raw resources
                    val scriptContent = resources.openRawResource(R.raw.boost)
                        .bufferedReader()
                        .use { it.readText() }
                    
                    // Execute boost script
                    Shell.cmd(scriptContent).exec()
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
