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
import android.util.Log

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
        
        // Set initial status
        tvStatus.text = "Sẵn sàng tăng tốc"
        appendLog("Kasumi Boots khởi động thành công")
        appendLog("Bấm nút bên dưới để bắt đầu")

        btnBoost.setOnClickListener {
            if (!btnBoost.isEnabled) return@setOnClickListener
            
            btnBoost.isEnabled = false
            progress.visibility = View.VISIBLE
            tvStatus.text = "Đang kiểm tra quyền root..."
            appendLog("\n=== Bắt đầu quá trình boost ===")
            
            mainScope.launch {
                try {
                    appendLog("Đang yêu cầu shell root...")
                    
                    val hasRoot = withContext(Dispatchers.IO) {
                        withTimeout(10000) { // 10s timeout
                            try {
                                val shell = obtainShell()
                                Log.d("KasumiBoots", "Shell obtained: ${shell.isRoot}")
                                appendLog("Shell đã khởi tạo: ${if (shell.isRoot) "ROOT" else "NO ROOT"}")
                                shell.isRoot
                            } catch (e: Exception) {
                                Log.e("KasumiBoots", "Shell error", e)
                                appendLog("LỖI shell: ${e.message}")
                                false
                            }
                        }
                    }
                    
                    if (hasRoot) {
                        tvStatus.text = getString(R.string.status_root_granted)
                        appendLog("✓ Đã có quyền ROOT")
                        performBoost()
                    } else {
                        tvStatus.text = getString(R.string.status_root_denied)
                        appendLog("✗ Không có quyền ROOT hoặc bị từ chối")
                        progress.visibility = View.GONE
                        btnBoost.isEnabled = true
                    }
                } catch (e: TimeoutCancellationException) {
                    tvStatus.text = "Timeout - Thử lại"
                    appendLog("✗ TIMEOUT: Quá thời gian chờ shell")
                    progress.visibility = View.GONE
                    btnBoost.isEnabled = true
                } catch (e: Exception) {
                    tvStatus.text = "Lỗi - Thử lại"
                    appendLog("✗ EXCEPTION: ${e.message}")
                    Log.e("KasumiBoots", "Root check error", e)
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
        tvStatus.text = getString(R.string.status_boosting)
        appendLog("\n--- Bắt đầu chạy script boost ---")
        
        mainScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    withTimeout(60000) { // 60s timeout for script
                        appendLog("Đang thực thi script...")
                        Log.d("KasumiBoots", "Executing boost script")
                        
                        // Execute boost script safely from InputStream
                        val scriptResult = Shell.cmd(resources.openRawResource(R.raw.boost)).exec()
                        Log.d("KasumiBoots", "Script completed: ${scriptResult.isSuccess}")
                        scriptResult
                    }
                }
                
                appendLog("\n--- Kết quả thực thi ---")
                
                // Display output line by line for better UX
                if (result.isSuccess) {
                    if (result.out.isEmpty()) {
                        appendLog("⚠ Không có output từ script")
                    } else {
                        result.out.forEach { line ->
                            appendLog(line)
                        }
                    }
                    
                    if (result.err.isNotEmpty()) {
                        appendLog("\n--- Warnings/Errors ---")
                        result.err.forEach { line ->
                            appendLog(line)
                        }
                    }
                    
                    tvStatus.text = getString(R.string.status_done)
                    appendLog("\n✓ HOÀN TẤT!")
                } else {
                    appendLog("✗ Script thất bại (code: ${result.code})")
                    result.err.forEach { line ->
                        appendLog(line)
                    }
                    tvStatus.text = "Thất bại - Xem log"
                }
                
            } catch (e: TimeoutCancellationException) {
                appendLog("\n✗ TIMEOUT: Script chạy quá 60 giây")
                tvStatus.text = "Timeout - Thử lại"
                Log.e("KasumiBoots", "Script timeout")
            } catch (e: Exception) {
                appendLog("\n✗ EXCEPTION: ${e.message}")
                appendLog("Stack: ${e.stackTraceToString().take(500)}")
                tvStatus.text = "Lỗi - Xem log"
                Log.e("KasumiBoots", "Script error", e)
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
            tvLog.post {
                val scrollView = tvLog.parent as? android.widget.ScrollView
                scrollView?.fullScroll(android.view.View.FOCUS_DOWN)
            }
            
            Log.d("KasumiBoots", "LOG: $text")
        }
    }
}
