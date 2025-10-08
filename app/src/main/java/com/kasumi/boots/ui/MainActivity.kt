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
                Toast.makeText(this, getString(R.string.toast_discord_error), Toast.LENGTH_SHORT).show()
            }
        }
        
        // Set initial status
        tvStatus.text = getString(R.string.status_ready)
        tvLog.text = getString(R.string.log_empty)

        btnBoost.setOnClickListener {
            if (!btnBoost.isEnabled) return@setOnClickListener
            
            // Update button state
            btnBoost.isEnabled = false
            btnBoost.text = getString(R.string.btn_checking)
            btnBoost.alpha = 0.6f
            progress.visibility = View.VISIBLE
            tvStatus.text = getString(R.string.status_checking)
            tvLog.text = getString(R.string.log_root_request)
            
            mainScope.launch {
                try {
                    val hasRoot = withContext(Dispatchers.IO) {
                        withTimeout(10000) { // 10s timeout
                            try {
                                val shell = obtainShell()
                                if (!shell.isRoot) {
                                    appendLog(getString(R.string.log_root_denied))
                                    return@withTimeout false
                                }
                                appendLog(getString(R.string.log_root_granted))
                                
                                // Check for Magisk/su binary
                                val suCheck = Shell.cmd("which su").exec()
                                val magiskCheck = Shell.cmd("which magisk").exec()
                                
                                if (magiskCheck.isSuccess) {
                                    appendLog(getString(R.string.log_magisk_detected))
                                } else if (suCheck.isSuccess) {
                                    appendLog(getString(R.string.log_other_root))
                                } else {
                                    appendLog(getString(R.string.log_no_root_manager))
                                    appendLog(getString(R.string.log_install_magisk))
                                }
                                
                                true
                            } catch (e: Exception) {
                                appendLog("ERROR: ${e.message}")
                                false
                            }
                        }
                    }
                    
                    if (hasRoot) {
                        tvStatus.text = getString(R.string.status_root_ok)
                        btnBoost.text = getString(R.string.btn_boosting)
                        performBoost()
                    } else {
                        tvStatus.text = getString(R.string.status_root_denied)
                        appendLog("\n" + getString(R.string.error_root_denied))
                        progress.visibility = View.GONE
                        btnBoost.text = getString(R.string.boost_now)
                        btnBoost.alpha = 1f
                        btnBoost.isEnabled = true
                    }
                } catch (e: TimeoutCancellationException) {
                    tvStatus.text = getString(R.string.status_timeout)
                    appendLog("\n" + getString(R.string.error_timeout))
                    progress.visibility = View.GONE
                    btnBoost.text = getString(R.string.boost_now)
                    btnBoost.alpha = 1f
                    btnBoost.isEnabled = true
                } catch (e: Exception) {
                    tvStatus.text = getString(R.string.status_error)
                    appendLog("\n${getString(R.string.error_unknown).replace("Không xác định", e.message ?: "Không xác định")}")
                    progress.visibility = View.GONE
                    btnBoost.text = getString(R.string.boost_now)
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
        tvStatus.text = getString(R.string.status_boosting)
        appendLog("\n" + getString(R.string.log_optimization_start))
        
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
                    tvStatus.text = getString(R.string.status_done)
                    btnBoost.text = getString(R.string.btn_completed)
                } else {
                    tvStatus.text = "Thất Bại"
                    btnBoost.text = getString(R.string.btn_failed)
                    if (result.errors.isNotEmpty()) {
                        appendLog("\nLỗi: ${result.errors.joinToString(", ")}")
                    }
                }
                
            } catch (e: TimeoutCancellationException) {
                appendLog("\n" + getString(R.string.error_optimization_timeout))
                tvStatus.text = getString(R.string.status_timeout)
                btnBoost.text = getString(R.string.btn_timeout)
            } catch (e: Exception) {
                appendLog("\n${getString(R.string.error_unknown).replace("Không xác định", e.message ?: "Không xác định")}")
                tvStatus.text = getString(R.string.status_error)
                btnBoost.text = getString(R.string.btn_error)
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
