package com.kasumi.boots.ui

import android.os.Bundle
import android.view.View
import com.google.android.material.button.MaterialButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
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
import android.view.animation.AnimationUtils

class MainActivity : AppCompatActivity() {
    
    private lateinit var tvStatus: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnBoost: MaterialButton
    private lateinit var btnDiscord: MaterialButton
    private lateinit var progress: ProgressBar
    private lateinit var discordLink: MaterialButton
    
    // Loading and Result views
    private lateinit var loadingContainer: LinearLayout
    private lateinit var resultContainer: LinearLayout
    private lateinit var ivLoading: ImageView
    private lateinit var tvLoadingText: TextView
    private lateinit var ivResultIcon: ImageView
    private lateinit var tvResultMessage: TextView
    
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
        btnBoost = findViewById(R.id.btnBoost)
        btnDiscord = findViewById(R.id.btnDiscord)
        progress = findViewById(R.id.progress)
        discordLink = findViewById(R.id.discordLink)
        
        // Initialize loading and result views
        loadingContainer = findViewById(R.id.loadingContainer)
        resultContainer = findViewById(R.id.resultContainer)
        ivLoading = findViewById(R.id.ivLoading)
        tvLoadingText = findViewById(R.id.tvLoadingText)
        ivResultIcon = findViewById(R.id.ivResultIcon)
        tvResultMessage = findViewById(R.id.tvResultMessage)
        
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
        showResult(R.drawable.ic_boost, getString(R.string.log_empty))

        btnBoost.setOnClickListener {
            if (!btnBoost.isEnabled) return@setOnClickListener
            
            // Update button state
            btnBoost.isEnabled = false
            btnBoost.text = getString(R.string.btn_checking)
            btnBoost.icon = null
            btnBoost.alpha = 0.6f
            progress.visibility = View.VISIBLE
            tvStatus.text = getString(R.string.status_checking)
            showLoading(getString(R.string.status_checking))
            
            mainScope.launch {
                try {
                    val hasRoot = withContext(Dispatchers.IO) {
                        withTimeout(10000) { // 10s timeout
                            try {
                                val shell = obtainShell()
                                if (!shell.isRoot) {
                                    return@withTimeout false
                                }
                                true
                            } catch (e: Exception) {
                                false
                            }
                        }
                    }
                    
                    if (hasRoot) {
                        tvStatus.text = getString(R.string.status_root_ok)
                        btnBoost.text = getString(R.string.btn_boosting)
                        tvLoadingText.text = getString(R.string.status_boosting)
                        performBoost()
                    } else {
                        tvStatus.text = getString(R.string.status_root_denied)
                        showResult(R.drawable.ic_error, getString(R.string.error_root_denied))
                        progress.visibility = View.GONE
                        btnBoost.text = getString(R.string.boost_now)
                        btnBoost.setIconResource(R.drawable.ic_boost)
                        btnBoost.alpha = 1f
                        btnBoost.isEnabled = true
                    }
                } catch (e: TimeoutCancellationException) {
                    tvStatus.text = getString(R.string.status_timeout)
                    showResult(R.drawable.ic_time, getString(R.string.error_timeout))
                    progress.visibility = View.GONE
                    btnBoost.text = getString(R.string.boost_now)
                    btnBoost.setIconResource(R.drawable.ic_boost)
                    btnBoost.alpha = 1f
                    btnBoost.isEnabled = true
                } catch (e: Exception) {
                    tvStatus.text = getString(R.string.status_error)
                    showResult(R.drawable.ic_error, getString(R.string.error_unknown))
                    progress.visibility = View.GONE
                    btnBoost.text = getString(R.string.boost_now)
                    btnBoost.setIconResource(R.drawable.ic_boost)
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
        showLoading(getString(R.string.status_boosting))
        
        mainScope.launch {
            try {
                val executor = BoostExecutor()
                val result = withContext(Dispatchers.IO) {
                    withTimeout(90000) { // 90s timeout
                        executor.execute { log ->
                            // Ignore log output
                        }
                    }
                }
                
                if (result.success) {
                    tvStatus.text = getString(R.string.status_done)
                    btnBoost.text = getString(R.string.btn_completed)
                    btnBoost.setIconResource(R.drawable.ic_check)
                    showResult(R.drawable.ic_check, "Tối ưu hệ thống thành công!\nĐiện thoại của bạn đã được tăng tốc.")
                } else {
                    tvStatus.text = "Thất Bại"
                    btnBoost.text = getString(R.string.btn_failed)
                    btnBoost.setIconResource(R.drawable.ic_error)
                    val errorMsg = if (result.errors.isNotEmpty()) {
                        "Lỗi: ${result.errors.joinToString(", ")}"
                    } else {
                        "Quá trình tối ưu gặp lỗi.\nVui lòng thử lại."
                    }
                    showResult(R.drawable.ic_error, errorMsg)
                }
                
            } catch (e: TimeoutCancellationException) {
                tvStatus.text = getString(R.string.status_timeout)
                btnBoost.text = getString(R.string.btn_timeout)
                btnBoost.setIconResource(R.drawable.ic_time)
                showResult(R.drawable.ic_time, getString(R.string.error_optimization_timeout))
            } catch (e: Exception) {
                tvStatus.text = getString(R.string.status_error)
                btnBoost.text = getString(R.string.btn_error)
                btnBoost.setIconResource(R.drawable.ic_error)
                showResult(R.drawable.ic_error, e.message ?: getString(R.string.error_unknown))
            } finally {
                progress.visibility = View.GONE
                btnBoost.alpha = 1f
                btnBoost.isEnabled = true
            }
        }
    }
    
    private fun showLoading(message: String) {
        mainScope.launch(Dispatchers.Main.immediate) {
            resultContainer.visibility = View.GONE
            loadingContainer.visibility = View.VISIBLE
            tvLoadingText.text = message
            
            // Start rotation animation
            val rotateAnim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.rotate_loading)
            ivLoading.startAnimation(rotateAnim)
        }
    }
    
    private fun showResult(iconRes: Int, message: String) {
        mainScope.launch(Dispatchers.Main.immediate) {
            // Stop loading animation
            ivLoading.clearAnimation()
            
            loadingContainer.visibility = View.GONE
            resultContainer.visibility = View.VISIBLE
            ivResultIcon.setImageResource(iconRes)
            tvResultMessage.text = message
            
            // Fade in animation
            val fadeIn = AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in)
            resultContainer.startAnimation(fadeIn)
        }
    }
}
