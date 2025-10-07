package com.kasumi.boots.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.kasumi.boots.R
import com.kasumi.boots.databinding.ActivityMainBinding
import com.topjohnwu.superuser.Shell
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val logBuilder = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateRootStatus()

        binding.btnBoost.setOnClickListener {
            performBoost()
        }
    }

    private fun updateRootStatus() {
        binding.tvStatus.text = getString(R.string.status_checking)
        Shell.cmd("id").submit { result ->
            runOnUiThread {
                val hasRoot = result.isSuccess && result.out.any { it.contains("uid=0") }
                binding.tvStatus.text = if (hasRoot) {
                    getString(R.string.status_root_granted)
                } else {
                    getString(R.string.status_root_denied)
                }
                if (hasRoot) {
                    addLog("✓ Root access granted")
                } else {
                    addLog("✗ Root access denied")
                }
            }
        }
    }

    private fun performBoost() {
        setBoosting(true)
        clearLog()
        addLog("=== KasumiBoots Started ===")
        addLog("Loading boost script...")

        // Load and execute boost script with output capture
        val input = resources.openRawResource(R.raw.boost)
        Shell.cmd(input).submit { result ->
            runOnUiThread {
                setBoosting(false)
                
                // Display command outputs
                if (result.out.isNotEmpty()) {
                    result.out.forEach { line ->
                        if (line.isNotBlank()) addLog(line)
                    }
                }
                
                // Display errors if any
                if (result.err.isNotEmpty()) {
                    result.err.forEach { line ->
                        if (line.isNotBlank()) addLog("⚠ $line")
                    }
                }

                if (result.isSuccess) {
                    binding.tvStatus.text = getString(R.string.status_done)
                    addLog("✓ Boost completed successfully")
                } else {
                    binding.tvStatus.text = "Boost failed (code: ${result.code})"
                    addLog("✗ Boost failed with exit code ${result.code}")
                }
                
                addLog("=== Finished ===")
            }
        }
    }

    private fun setBoosting(inProgress: Boolean) {
        binding.progress.visibility = if (inProgress) View.VISIBLE else View.GONE
        binding.btnBoost.isEnabled = !inProgress
        if (inProgress) {
            binding.tvStatus.text = getString(R.string.status_boosting)
        }
    }

    private fun addLog(message: String) {
        val timestamp = dateFormat.format(Date())
        logBuilder.append("[$timestamp] $message\n")
        binding.tvLog.text = logBuilder.toString()
        
        // Auto scroll to bottom
        binding.tvLog.post {
            val scrollView = binding.tvLog.parent as? android.widget.ScrollView
            scrollView?.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun clearLog() {
        logBuilder.clear()
        binding.tvLog.text = getString(R.string.log_empty)
    }
}
