package com.kasumi.boots.core

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * BoostExecutor - Thực thi lệnh tối ưu hóa trực tiếp (không dùng script file)
 * Tất cả lệnh shell được thực thi trực tiếp với root
 */
class BoostExecutor {

    data class BoostResult(
        val success: Boolean,
        val logs: List<String>,
        val errors: List<String> = emptyList()
    )

    suspend fun execute(onProgress: (String) -> Unit): BoostResult = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        fun log(msg: String) {
            logs.add(msg)
            onProgress(msg)
        }

        try {

            // SECTION 1: CPU OPTIMIZATION
            log("✓ [1/9] CPU Optimization")
            executeCommands(
                // Enable all CPU cores
                "for cpu in /sys/devices/system/cpu/cpu[1-9]*; do [ -d \"\$cpu\" ] && echo 1 > \"\$cpu/online\" 2>/dev/null || true; done",
                
                // Set performance governor + lock max frequency
                "for cpu in /sys/devices/system/cpu/cpu[0-9]*/cpufreq; do " +
                        "[ -d \"\$cpu\" ] || continue; " +
                        "echo performance > \"\$cpu/scaling_governor\" 2>/dev/null || true; " +
                        "MAX=\$(cat \"\$cpu/cpuinfo_max_freq\" 2>/dev/null); " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$cpu/scaling_max_freq\" 2>/dev/null || true; " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$cpu/scaling_min_freq\" 2>/dev/null || true; " +
                        "done",
                
                // Apply to policy*
                "for p in /sys/devices/system/cpu/cpufreq/policy*; do " +
                        "[ -d \"\$p\" ] || continue; " +
                        "echo performance > \"\$p/scaling_governor\" 2>/dev/null || true; " +
                        "MAX=\$(cat \"\$p/cpuinfo_max_freq\" 2>/dev/null); " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$p/scaling_max_freq\" 2>/dev/null || true; " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$p/scaling_min_freq\" 2>/dev/null || true; " +
                        "done"
            )

            // SECTION 2: THERMAL CONTROL
            log("✓ [2/9] Thermal Control")
            executeCommands(
                "for t in /sys/class/thermal/thermal_zone*/mode; do [ -f \"\$t\" ] && echo disabled > \"\$t\" 2>/dev/null || true; done",
                "for t in /sys/class/thermal/thermal_zone*/trip_point_*_temp; do [ -f \"\$t\" ] && echo 999999 > \"\$t\" 2>/dev/null || true; done",
                "stop thermal-engine 2>/dev/null || true",
                "stop thermald 2>/dev/null || true",
                "stop mi_thermald 2>/dev/null || true"
            )

            // SECTION 3: GPU OPTIMIZATION
            log("✓ [3/9] GPU Optimization")
            executeCommands(
                // KGSL (Adreno)
                "if [ -d /sys/class/kgsl/kgsl-3d0/devfreq ]; then " +
                        "echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null || true; " +
                        "echo 3 > /sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost 2>/dev/null || true; " +
                        "MAX=\$(cat /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null); " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq 2>/dev/null || true; " +
                        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_no_nap 2>/dev/null || true; " +
                        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null || true; " +
                        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null || true; " +
                        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_rail_on 2>/dev/null || true; " +
                        "fi",
                
                // Generic GPU (Mali/others)
                "for d in /sys/class/devfreq/*gpu* /sys/class/devfreq/*GPU*; do " +
                        "[ -d \"\$d\" ] || continue; " +
                        "echo performance > \"\$d/governor\" 2>/dev/null || true; " +
                        "MAX=\$(cat \"\$d/max_freq\" 2>/dev/null); " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$d/min_freq\" 2>/dev/null || true; " +
                        "done"
            )

            // SECTION 4: CPU SET & SCHEDULING
            log("✓ [4/9] CPU Set & Scheduling")
            executeCommands(
                "ONLINE=\$(cat /sys/devices/system/cpu/online 2>/dev/null); " +
                        "[ -n \"\$ONLINE\" ] && for c in /dev/cpuset/*/cpus; do [ -f \"\$c\" ] && echo \$ONLINE > \"\$c\" 2>/dev/null || true; done",
                
                "echo 1000000 > /proc/sys/kernel/sched_latency_ns 2>/dev/null || true",
                "echo 100000 > /proc/sys/kernel/sched_min_granularity_ns 2>/dev/null || true",
                "echo 500000 > /proc/sys/kernel/sched_wakeup_granularity_ns 2>/dev/null || true",
                "echo 128 > /proc/sys/kernel/sched_nr_migrate 2>/dev/null || true",
                "echo 0 > /proc/sys/kernel/sched_schedstats 2>/dev/null || true"
            )

            // SECTION 5: I/O PERFORMANCE
            log("✓ [5/9] I/O Performance")
            executeCommands(
                "for b in /sys/block/*/queue; do " +
                        "[ -d \"\$b\" ] || continue; " +
                        "echo 2048 > \"\$b/read_ahead_kb\" 2>/dev/null || true; " +
                        "echo 512 > \"\$b/nr_requests\" 2>/dev/null || true; " +
                        "echo 2 > \"\$b/rq_affinity\" 2>/dev/null || true; " +
                        "echo 0 > \"\$b/add_random\" 2>/dev/null || true; " +
                        "echo 0 > \"\$b/iostats\" 2>/dev/null || true; " +
                        "done"
            )

            // SECTION 6: MEMORY OPTIMIZATION
            log("✓ [6/9] Memory Optimization")
            executeCommands(
                "echo 0 > /proc/sys/vm/swappiness 2>/dev/null || true",
                "echo 30 > /proc/sys/vm/dirty_ratio 2>/dev/null || true",
                "echo 10 > /proc/sys/vm/dirty_background_ratio 2>/dev/null || true",
                "echo 50 > /proc/sys/vm/vfs_cache_pressure 2>/dev/null || true",
                "echo 0 > /proc/sys/vm/page-cluster 2>/dev/null || true",
                "echo 0 > /proc/sys/vm/compaction_proactiveness 2>/dev/null || true"
            )

            // SECTION 7: POWER SAVING DISABLED
            log("✓ [7/9] Power Saving Disabled")
            executeCommands(
                "cmd power set-fixed-performance-mode-enabled true 2>/dev/null || true",
                "cmd power set-mode 0 2>/dev/null || true",
                "settings put global low_power 0 2>/dev/null || true",
                "settings put global app_standby_enabled 0 2>/dev/null || true",
                "settings put global forced_app_standby_enabled 0 2>/dev/null || true",
                "dumpsys deviceidle disable 2>/dev/null || true"
            )

            // SECTION 8: NETWORK OPTIMIZATION
            log("✓ [8/9] Network Tuning")
            executeCommands(
                "echo 1 > /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null || true",
                "echo 1 > /proc/sys/net/ipv4/tcp_timestamps 2>/dev/null || true",
                "echo 1 > /proc/sys/net/ipv4/tcp_sack 2>/dev/null || true",
                "echo 3 > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null || true",
                "echo cubic > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null || true",
                "echo 16777216 > /proc/sys/net/core/rmem_max 2>/dev/null || true",
                "echo 16777216 > /proc/sys/net/core/wmem_max 2>/dev/null || true"
            )

            // SECTION 9: CLEANUP
            log("✓ [9/9] Cleanup")
            executeCommands(
                "sync",
                "echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || true",
                "cmd activity kill-all 2>/dev/null || am kill-all 2>/dev/null || true",
                "stop perfd 2>/dev/null || true",
                "stop mpdecision 2>/dev/null || true"
            )

            // SUMMARY
            log("")
            log("🎉 HOÀN TẤT! Hệ thống đang chạy hiệu suất tối đa")

            BoostResult(success = true, logs = logs, errors = errors)

        } catch (e: Exception) {
            errors.add("ERROR: ${e.message}")
            log("❌ LỖI: ${e.message ?: "Unknown error"}")
            BoostResult(success = false, logs = logs, errors = errors)
        }
    }

    private fun executeCommands(vararg commands: String) {
        commands.forEach { cmd ->
            Shell.cmd(cmd).exec()
        }
    }
}
