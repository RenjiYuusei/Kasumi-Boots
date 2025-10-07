package com.kasumi.boots.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.kasumi.boots.R
import com.kasumi.boots.databinding.ActivityMainBinding
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val logBuffer = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateRootStatus()

        binding.btnBoost.setOnClickListener {
            setBoosting(true)
            logBuffer.clear()
            binding.tvLog.text = ""
            appendLog("🚀 Bắt đầu tối ưu hiệu suất...")
            
            // Callback list để nhận output realtime
            val outputList = object : CallbackList<String>() {
                override fun onAddElement(s: String?) {
                    s?.let { line ->
                        runOnUiThread {
                            appendLog(line)
                        }
                    }
                }
            }
            
            // Load root boost script from raw resource and execute
            val input = resources.openRawResource(R.raw.boost)
            Shell.cmd(input).to(outputList).submit { result ->
                runOnUiThread {
                    setBoosting(false)
                    if (result.isSuccess) {
                        binding.tvStatus.text = getString(R.string.status_done)
                        appendLog("✅ Hoàn tất!")
                    } else {
                        binding.tvStatus.text = getString(R.string.status_failed)
                        appendLog("❌ Lỗi: exit code ${result.code}")
                    }
                }
            }
        }
    }

    private fun appendLog(line: String) {
        logBuffer.append(line).append("\n")
        binding.tvLog.text = logBuffer.toString()
        // Auto scroll to bottom
        binding.scrollLog.post {
            binding.scrollLog.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun updateRootStatus() {
        val res = Shell.cmd("id").exec()
        val ok = res.isSuccess
        binding.tvStatus.text = if (ok) getString(R.string.status_root_granted) else getString(R.string.status_root_denied)
        if (ok) {
            appendLog("✓ Root: ${res.out.firstOrNull() ?: "OK"}")
        } else {
            appendLog("✗ Root chưa được cấp quyền")
        }
    }

    private fun setBoosting(inProgress: Boolean) {
        binding.progress.visibility = if (inProgress) View.VISIBLE else View.GONE
        binding.btnBoost.isEnabled = !inProgress
        if (inProgress) binding.tvStatus.text = getString(R.string.status_boosting)
    }
}
