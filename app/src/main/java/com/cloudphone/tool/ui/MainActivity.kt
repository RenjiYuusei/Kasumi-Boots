package com.kasumi.boots.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.kasumi.boots.R
import com.kasumi.boots.databinding.ActivityMainBinding
import com.topjohnwu.superuser.Shell

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateRootStatus()

        binding.btnBoost.setOnClickListener {
            setBoosting(true)
            // Load root boost script from raw resource and execute
            val input = resources.openRawResource(R.raw.boost)
            Shell.cmd(input).submit { result ->
                runOnUiThread {
                    setBoosting(false)
                    binding.tvStatus.text = if (result.isSuccess) getString(R.string.status_done) else "Boost failed (check root)"
                }
            }
        }
    }

    private fun updateRootStatus() {
        val res = Shell.cmd("id").exec()
        val ok = res.isSuccess
        binding.tvStatus.text = if (ok) getString(R.string.status_root_granted) else getString(R.string.status_root_denied)
    }

    private fun setBoosting(inProgress: Boolean) {
        binding.progress.visibility = if (inProgress) View.VISIBLE else View.GONE
        binding.btnBoost.isEnabled = !inProgress
        if (inProgress) binding.tvStatus.text = getString(R.string.status_boosting)
    }
}
