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
        tvLog.text = ""  // Clear default text
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLog("   KASUMI BOOTS - Cloud Optimizer")
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLog("")
        appendLog("✓ Hệ thống sẵn sàng")
        appendLog("✓ Bấm nút BOOST để bắt đầu")
        appendLog("")

        btnBoost.setOnClickListener {
            if (!btnBoost.isEnabled) return@setOnClickListener
            
            btnBoost.isEnabled = false
            progress.visibility = View.VISIBLE
            tvStatus.text = "🔍 Kiểm Tra Root..."
            tvLog.text = ""  // Clear log
            appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLog("   BẮT ĐẦU QUÁ TRÌNH BOOST")
            appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLog("")
            
            mainScope.launch {
                try {
                    appendLog("[1/2] Đang yêu cầu shell root...")
                    val hasRoot = withContext(Dispatchers.IO) {
                        withTimeout(10000) { // 10s timeout
                            try {
                                val shell = obtainShell()
                                Log.d("KasumiBoots", "Shell obtained: ${shell.isRoot}")
                                val status = if (shell.isRoot) "✓ ROOT" else "✗ NO ROOT"
                                appendLog("[1/2] Shell: $status")
                                shell.isRoot
                            } catch (e: Exception) {
                                Log.e("KasumiBoots", "Shell error", e)
                                appendLog("LỖI shell: ${e.message}")
                                false
                            }
                        }
                    }
                    
                    if (hasRoot) {
                        tvStatus.text = "✓ Root OK"
                        appendLog("")
                        appendLog("✓ Quyền ROOT đã được cấp")
                        appendLog("")
                        performBoost()
                    } else {
                        tvStatus.text = "✗ Thiếu Root"
                        appendLog("")
                        appendLog("✗ KHÔNG CÓ QUYỀN ROOT")
                        appendLog("")
                        appendLog("Vui lòng cấp quyền root cho app")
                        progress.visibility = View.GONE
                        btnBoost.isEnabled = true
                    }
                } catch (e: TimeoutCancellationException) {
                    tvStatus.text = "⏱ Timeout"
                    appendLog("")
                    appendLog("✗ TIMEOUT sau 10 giây")
                    appendLog("")
                    appendLog("Hãy thử lại")
                    progress.visibility = View.GONE
                    btnBoost.isEnabled = true
                } catch (e: Exception) {
                    tvStatus.text = "❌ Lỗi"
                    appendLog("")
                    appendLog("✗ LỖI: ${e.message ?: "Unknown"}")
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
        tvStatus.text = "🚀 Đang Boost..."
        appendLog("[2/2] Bắt đầu chạy script boost...")
        appendLog("")
        
        mainScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    withTimeout(90000) { // 90s timeout for script
                        appendLog("⏳ Thực thi script (có thể mất 30-60 giây)...")
                        appendLog("")
                        Log.d("KasumiBoots", "Executing boost script")
                        
                        // Execute boost script with stdout redirect for realtime output
                        val scriptResult = Shell.cmd(resources.openRawResource(R.raw.boost)).exec()
                        Log.d("KasumiBoots", "Script completed: ${scriptResult.isSuccess}, code: ${scriptResult.code}")
                        scriptResult
                    }
                }
                
                appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                appendLog("   KẾT QUẢ THỰC THI")
                appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                appendLog("")
                
                // Display output with grouping
                if (result.isSuccess) {
                    if (result.out.isEmpty()) {
                        appendLog("⚠ Script chạy nhưng không có output")
                    } else {
                        var lineCount = 0
                        result.out.forEach { line ->
                            if (line.isNotBlank()) {
                                appendLog(line)
                                lineCount++
                            }
                        }
                        appendLog("")
                        appendLog("→ Đã xử lý $lineCount dòng output")
                    }
                    
                    if (result.err.isNotEmpty()) {
                        appendLog("")
                        appendLog("━━━ Warnings ━━━")
                        result.err.take(10).forEach { line ->
                            if (line.isNotBlank()) appendLog("⚠ $line")
                        }
                        if (result.err.size > 10) {
                            appendLog("... và ${result.err.size - 10} dòng nữa")
                        }
                    }
                    
                    appendLog("")
                    appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    appendLog("   ✓ HOÀN TẤT - Hiệu suất tối đa!")
                    appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    tvStatus.text = "✓ Hoàn Tất"
                } else {
                    appendLog("✗ Script thất bại (exit code: ${result.code})")
                    appendLog("")
                    result.out.take(20).forEach { line ->
                        if (line.isNotBlank()) appendLog(line)
                    }
                    if (result.err.isNotEmpty()) {
                        appendLog("")
                        appendLog("━━━ Errors ━━━")
                        result.err.take(10).forEach { line ->
                            appendLog("❌ $line")
                        }
                    }
                    tvStatus.text = "❌ Thất Bại"
                }
                
            } catch (e: TimeoutCancellationException) {
                appendLog("")
                appendLog("✗ TIMEOUT: Script chạy quá 90 giây")
                appendLog("")
                appendLog("Điều này bất thường - kiểm tra thiết bị")
                tvStatus.text = "⏱ Timeout"
                Log.e("KasumiBoots", "Script timeout")
            } catch (e: Exception) {
                appendLog("")
                appendLog("❌ LỖI: ${e.message ?: "Unknown"}")
                appendLog("")
                val stack = e.stackTraceToString().take(300)
                appendLog("Debug: $stack")
                tvStatus.text = "❌ Lỗi"
                Log.e("KasumiBoots", "Script error", e)
            } finally {
                appendLog("")
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
            
            // Auto scroll to bottom after a brief delay
            tvLog.postDelayed({
                val parent = tvLog.parent
                if (parent is android.widget.ScrollView) {
                    parent.fullScroll(android.view.View.FOCUS_DOWN)
                } else {
                    // If parent is not directly ScrollView, try parent's parent
                    val grandParent = parent?.parent
                    if (grandParent is android.widget.ScrollView) {
                        grandParent.fullScroll(android.view.View.FOCUS_DOWN)
                    }
                }
            }, 50)
            
            // Also log to logcat for debugging
            if (text.isNotBlank()) {
                Log.d("KasumiBoots", text)
            }
        }
    }
}
