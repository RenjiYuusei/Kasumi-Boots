package com.kasumi.boots.ui

import android.os.Bundle
import android.view.View
import com.google.android.material.button.MaterialButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kasumi.boots.R
import com.kasumi.boots.core.BoostExecutor
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.content.Intent
import android.net.Uri
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    
    private lateinit var tvStatus: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvLog: TextView
    private lateinit var btnBoost: MaterialButton
    private lateinit var btnDiscord: MaterialButton
    private lateinit var progress: ProgressBar
    private lateinit var discordLink: MaterialButton
    
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
        btnDiscord = findViewById(R.id.btnDiscord)
        progress = findViewById(R.id.progress)
        discordLink = findViewById(R.id.discordLink)
        
        // Discord button (hidden old one)
        btnDiscord.visibility = android.view.View.GONE
        
        // Discord button click handler
        discordLink.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/kasumi"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open Discord", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Set initial status
        tvStatus.text = "Ready"
        tvLog.text = "System ready\nPress START BOOST to begin"

        btnBoost.setOnClickListener {
            if (!btnBoost.isEnabled) return@setOnClickListener
            
            // Update button state
            btnBoost.isEnabled = false
            btnBoost.text = "CHECKING..."
            btnBoost.alpha = 0.6f
            progress.visibility = View.VISIBLE
            tvStatus.text = "Checking root access..."
            tvLog.text = "[1/2] Requesting root access..."
            
            mainScope.launch {
                try {
                    val hasRoot = withContext(Dispatchers.IO) {
                        withTimeout(10000) { // 10s timeout
                            try {
                                val shell = obtainShell()
                                if (!shell.isRoot) {
                                    appendLog("[1/2] Root access denied")
                                    return@withTimeout false
                                }
                                appendLog("[1/2] Root access granted")
                                
                                // Check for Magisk/su binary
                                val suCheck = Shell.cmd("which su").exec()
                                val magiskCheck = Shell.cmd("which magisk").exec()
                                
                                if (magiskCheck.isSuccess) {
                                    appendLog("[1/2] Magisk detected")
                                } else if (suCheck.isSuccess) {
                                    appendLog("[1/2] SuperSU/other root detected")
                                } else {
                                    appendLog("[1/2] WARNING: No root manager found")
                                    appendLog("[1/2] Install Magisk for best results")
                                }
                                
                                true
                            } catch (e: Exception) {
                                appendLog("ERROR: ${e.message}")
                                false
                            }
                        }
                    }
                    
                    if (hasRoot) {
                        tvStatus.text = "Root OK"
                        btnBoost.text = "BOOSTING..."
                        performBoost()
                    } else {
                        tvStatus.text = "Root denied"
                        appendLog("\nROOT ACCESS DENIED\nPlease grant root permission")
                        progress.visibility = View.GONE
                        btnBoost.text = "⚡ START BOOST"
                        btnBoost.alpha = 1f
                        btnBoost.isEnabled = true
                    }
                } catch (e: TimeoutCancellationException) {
                    tvStatus.text = "Timeout"
                    appendLog("\nTIMEOUT after 10s - Try again")
                    progress.visibility = View.GONE
                    btnBoost.text = "⚡ START BOOST"
                    btnBoost.alpha = 1f
                    btnBoost.isEnabled = true
                } catch (e: Exception) {
                    tvStatus.text = "Error"
                    appendLog("\nERROR: ${e.message ?: "Unknown"}")
                    progress.visibility = View.GONE
                    btnBoost.text = "⚡ START BOOST"
                    btnBoost.alpha = 1f
                    btnBoost.isEnabled = true
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
    
    private fun performBoost() {
        tvStatus.text = "Optimizing..."
        appendLog("\n[2/2] Starting system optimization...")
        
        mainScope.launch {
            try {
                val executor = BoostExecutor()
                val result = withContext(Dispatchers.IO) {
                    withTimeout(90000) { // 90s timeout
                        executor.execute { log ->
                            appendLog(log)
                        }
                    }
                }
                
                if (result.success) {
                    tvStatus.text = "Completed"
                    btnBoost.text = "✓ COMPLETED"
                } else {
                    tvStatus.text = "Failed"
                    btnBoost.text = "⚠ FAILED"
                    if (result.errors.isNotEmpty()) {
                        appendLog("\nErrors: ${result.errors.joinToString(", ")}")
                    }
                }
                
            } catch (e: TimeoutCancellationException) {
                appendLog("\nTIMEOUT: Optimization took over 90s")
                tvStatus.text = "Timeout"
                btnBoost.text = "⏱ TIMEOUT"
            } catch (e: Exception) {
                appendLog("\nERROR: ${e.message ?: "Unknown"}")
                tvStatus.text = "Error"
                btnBoost.text = "❌ ERROR"
            } finally {
                progress.visibility = View.GONE
                btnBoost.alpha = 1f
                btnBoost.isEnabled = true
            }
        }
    }
    
    private fun appendLog(text: String) {
        mainScope.launch(Dispatchers.Main.immediate) {
            val current = tvLog.text.toString()
            val newText = if (current.isEmpty()) text else "$current\n$text"
            tvLog.text = newText
            
            // Auto scroll to bottom
            tvLog.postDelayed({
                val parent = tvLog.parent
                if (parent is android.widget.ScrollView) {
                    parent.fullScroll(android.view.View.FOCUS_DOWN)
                }
            }, 50)
        }
    }
}
