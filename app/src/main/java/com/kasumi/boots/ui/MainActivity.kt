package com.kasumi.boots.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
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
    private lateinit var btnBoost: Button
    private lateinit var btnDiscord: Button
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
        btnDiscord = findViewById(R.id.btnDiscord)
        progress = findViewById(R.id.progress)
        
        // Discord button
        btnDiscord.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/kasumi"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Không thể mở Discord link", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Set initial status
        tvStatus.text = "⚡ Sẵn Sàng Boost"
        tvLog.text = "✓ Hệ thống sẵn sàng\n✓ Bấm nút BOOST để bắt đầu"

        btnBoost.setOnClickListener {
            if (!btnBoost.isEnabled) return@setOnClickListener
            
            btnBoost.isEnabled = false
            progress.visibility = View.VISIBLE
            tvStatus.text = "🔍 Kiểm Tra Root..."
            tvLog.text = "[1/2] Đang yêu cầu quyền root..."
            
            mainScope.launch {
                try {
                    val hasRoot = withContext(Dispatchers.IO) {
                        withTimeout(10000) { // 10s timeout
                            try {
                                val shell = obtainShell()
                                appendLog("[1/2] ✓ Quyền ROOT đã được cấp")
                                shell.isRoot
                            } catch (e: Exception) {
                                appendLog("✗ Lỗi: ${e.message}")
                                false
                            }
                        }
                    }
                    
                    if (hasRoot) {
                        tvStatus.text = "✓ Root OK"
                        performBoost()
                    } else {
                        tvStatus.text = "✗ Thiếu Root"
                        appendLog("\n✗ KHÔNG CÓ QUYỀN ROOT\nVui lòng cấp quyền root cho app")
                        progress.visibility = View.GONE
                        btnBoost.isEnabled = true
                    }
                } catch (e: TimeoutCancellationException) {
                    tvStatus.text = "⏱ Timeout"
                    appendLog("\n✗ TIMEOUT sau 10 giây - Hãy thử lại")
                    progress.visibility = View.GONE
                    btnBoost.isEnabled = true
                } catch (e: Exception) {
                    tvStatus.text = "❌ Lỗi"
                    appendLog("\n✗ LỖI: ${e.message ?: "Unknown"}")
                    progress.visibility = View.GONE
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
        tvStatus.text = "🚀 Đang Boost..."
        appendLog("\n[2/2] Đang tối ưu hóa hệ thống...")
        
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
                    tvStatus.text = "✓ Hoàn Tất"
                } else {
                    tvStatus.text = "❌ Thất Bại"
                    if (result.errors.isNotEmpty()) {
                        appendLog("\nLỗi: ${result.errors.joinToString(", ")}")
                    }
                }
                
            } catch (e: TimeoutCancellationException) {
                appendLog("\n✗ TIMEOUT: Tối ưu hóa chạy quá 90 giây")
                tvStatus.text = "⏱ Timeout"
            } catch (e: Exception) {
                appendLog("\n❌ LỖI: ${e.message ?: "Unknown"}")
                tvStatus.text = "❌ Lỗi"
            } finally {
                progress.visibility = View.GONE
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
