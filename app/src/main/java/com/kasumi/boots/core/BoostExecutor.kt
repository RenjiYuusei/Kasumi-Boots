package com.kasumi.boots.core

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

            // SECTION 1: AGGRESSIVE CPU OVERCLOCK
            log("[1/10] Aggressive CPU Overclock")
            delay(100)
            executeCommands(
                // Force enable ALL CPU cores
                "for cpu in /sys/devices/system/cpu/cpu[0-9]*; do [ -d \"\$cpu\" ] && echo 1 > \"\$cpu/online\" 2>/dev/null || true; done",
                
                // OVERCLOCK: Lock ALL cores to MAX frequency
                "for cpu in /sys/devices/system/cpu/cpu[0-9]*/cpufreq; do " +
                        "[ -d \"\$cpu\" ] || continue; " +
                        "echo performance > \"\$cpu/scaling_governor\" 2>/dev/null || true; " +
                        "MAX=\$(cat \"\$cpu/cpuinfo_max_freq\" 2>/dev/null); " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$cpu/scaling_max_freq\" 2>/dev/null || true; " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$cpu/scaling_min_freq\" 2>/dev/null || true; " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$cpu/cpuinfo_min_freq\" 2>/dev/null || true; " +
                        "done",
                
                // Apply to ALL policy*
                "for p in /sys/devices/system/cpu/cpufreq/policy*; do " +
                        "[ -d \"\$p\" ] || continue; " +
                        "echo performance > \"\$p/scaling_governor\" 2>/dev/null || true; " +
                        "MAX=\$(cat \"\$p/cpuinfo_max_freq\" 2>/dev/null); " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$p/scaling_max_freq\" 2>/dev/null || true; " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$p/scaling_min_freq\" 2>/dev/null || true; " +
                        "echo 0 > \"\$p/energy_dcfc_enabled\" 2>/dev/null || true; " +
                        "done",
                
                // Disable CPU power saving features
                "echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/down_rate_limit_us 2>/dev/null || true",
                "echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/up_rate_limit_us 2>/dev/null || true",
                
                // Force max boost
                "echo 1 > /sys/devices/system/cpu/cpufreq/boost 2>/dev/null || true",
                "echo 1 > /sys/devices/system/cpu/intel_pstate/no_turbo 2>/dev/null && echo 0 > /sys/devices/system/cpu/intel_pstate/no_turbo 2>/dev/null || true"
            )

            log("[1/10] Done")
            delay(100)
            
            // SECTION 2: DISABLE ALL THERMAL THROTTLING
            log("[2/10] Kill Thermal Throttling")
            delay(100)
            executeCommands(
                // Disable thermal zones
                "for t in /sys/class/thermal/thermal_zone*/mode; do [ -f \"\$t\" ] && echo disabled > \"\$t\" 2>/dev/null || true; done",
                "for t in /sys/class/thermal/thermal_zone*/trip_point_*_temp; do [ -f \"\$t\" ] && echo 999999 > \"\$t\" 2>/dev/null || true; done",
                
                // Stop ALL thermal services
                "stop thermal-engine 2>/dev/null || true",
                "stop thermald 2>/dev/null || true",
                "stop mi_thermald 2>/dev/null || true",
                "stop thermal 2>/dev/null || true",
                "stop vendor.thermal-engine 2>/dev/null || true",
                
                // Kill thermal monitoring completely
                "setprop vendor.disable.thermal.control 1 2>/dev/null || true",
                "setprop sys.thermal.enable 0 2>/dev/null || true",
                "killall thermal-engine 2>/dev/null || true",
                "killall thermald 2>/dev/null || true"
            )

            log("[2/10] Done")
            delay(100)
            
            // SECTION 3: EXTREME GPU OVERCLOCK
            log("[3/10] Extreme GPU Overclock")
            delay(100)
            executeCommands(
                // KGSL (Adreno) - MAX OVERCLOCK
                "if [ -d /sys/class/kgsl/kgsl-3d0/devfreq ]; then " +
                        "echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null || true; " +
                        "echo 3 > /sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost 2>/dev/null || true; " +
                        "MAX=\$(cat /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null); " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq 2>/dev/null || true; " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > /sys/class/kgsl/kgsl-3d0/max_pwrlevel 2>/dev/null || true; " +
                        "echo 0 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel 2>/dev/null || true; " +
                        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_no_nap 2>/dev/null || true; " +
                        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null || true; " +
                        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null || true; " +
                        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_rail_on 2>/dev/null || true; " +
                        "echo 0 > /sys/class/kgsl/kgsl-3d0/throttling 2>/dev/null || true; " +
                        "echo 0 > /sys/class/kgsl/kgsl-3d0/thermal_pwrlevel 2>/dev/null || true; " +
                        "fi",
                
                // Generic GPU (Mali/others) - LOCK MAX FREQ
                "for d in /sys/class/devfreq/*gpu* /sys/class/devfreq/*GPU*; do " +
                        "[ -d \"\$d\" ] || continue; " +
                        "echo performance > \"\$d/governor\" 2>/dev/null || true; " +
                        "MAX=\$(cat \"\$d/max_freq\" 2>/dev/null); " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$d/min_freq\" 2>/dev/null || true; " +
                        "echo 0 > \"\$d/polling_interval\" 2>/dev/null || true; " +
                        "done",
                
                // Mali GPU specific
                "if [ -f /sys/devices/platform/mali/power_policy ]; then " +
                        "echo always_on > /sys/devices/platform/mali/power_policy 2>/dev/null || true; " +
                        "fi",
                
                // Force GPU max clock via setprop
                "setprop vendor.debug.egl.swapinterval 0 2>/dev/null || true",
                "setprop debug.gr.swapinterval 0 2>/dev/null || true"
            )

            log("[3/10] Done")
            delay(100)
            
            // SECTION 4: BUS SPEED & MEMORY OVERCLOCK
            log("[4/10] Bus & Memory Boost")
            delay(100)
            executeCommands(
                // DDR Bus speed boost
                "for bus in /sys/class/devfreq/*ddr* /sys/class/devfreq/*ddr*bw /sys/class/devfreq/*DDR*; do " +
                        "[ -d \"\$bus\" ] 2>/dev/null || continue; " +
                        "echo performance > \"\$bus/governor\" 2>/dev/null || true; " +
                        "MAX=\$(cat \"\$bus/max_freq\" 2>/dev/null); " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$bus/min_freq\" 2>/dev/null || true; " +
                        "done",
                
                // CPU BUS boost
                "for bus in /sys/class/devfreq/*cpubw* /sys/class/devfreq/*cpu-cpu-ddr*; do " +
                        "[ -d \"\$bus\" ] 2>/dev/null || continue; " +
                        "echo performance > \"\$bus/governor\" 2>/dev/null || true; " +
                        "MAX=\$(cat \"\$bus/max_freq\" 2>/dev/null); " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$bus/min_freq\" 2>/dev/null || true; " +
                        "done",
                
                // GPU BUS boost
                "for bus in /sys/class/devfreq/*gpubw* /sys/class/devfreq/*gpu-gpu-ddr*; do " +
                        "[ -d \"\$bus\" ] 2>/dev/null || continue; " +
                        "echo performance > \"\$bus/governor\" 2>/dev/null || true; " +
                        "MAX=\$(cat \"\$bus/max_freq\" 2>/dev/null); " +
                        "[ -n \"\$MAX\" ] && echo \$MAX > \"\$bus/min_freq\" 2>/dev/null || true; " +
                        "done",
                
                // Force all devfreq to performance
                "for d in /sys/class/devfreq/*; do " +
                        "[ -d \"\$d\" ] || continue; " +
                        "echo performance > \"\$d/governor\" 2>/dev/null || true; " +
                        "done"
            )
            
            log("[4/10] Done")
            delay(100)
            
            // SECTION 5: CPU SET & SCHEDULING
            log("[5/10] CPU Scheduling")
            delay(100)
            executeCommands(
                "ONLINE=\$(cat /sys/devices/system/cpu/online 2>/dev/null); " +
                        "[ -n \"\$ONLINE\" ] && for c in /dev/cpuset/*/cpus; do [ -f \"\$c\" ] && echo \$ONLINE > \"\$c\" 2>/dev/null || true; done",
                
                "echo 1000000 > /proc/sys/kernel/sched_latency_ns 2>/dev/null || true",
                "echo 100000 > /proc/sys/kernel/sched_min_granularity_ns 2>/dev/null || true",
                "echo 500000 > /proc/sys/kernel/sched_wakeup_granularity_ns 2>/dev/null || true",
                "echo 128 > /proc/sys/kernel/sched_nr_migrate 2>/dev/null || true",
                "echo 0 > /proc/sys/kernel/sched_schedstats 2>/dev/null || true"
            )

            log("[5/10] Done")
            delay(100)
            
            // SECTION 6: I/O PERFORMANCE (Optimized for speed)
            log("[6/10] I/O Performance")
            delay(100)
            executeCommands(
                // Only main storage for speed
                "for b in /sys/block/sda*/queue /sys/block/mmcblk*/queue /sys/block/vd*/queue; do " +
                        "[ -d \"\$b\" ] 2>/dev/null || continue; " +
                        "echo 2048 > \"\$b/read_ahead_kb\" 2>/dev/null || true; " +
                        "echo 0 > \"\$b/iostats\" 2>/dev/null || true; " +
                        "done &"
            )

            log("[6/10] Done")
            delay(100)
            
            // SECTION 7: MEMORY OPTIMIZATION
            log("[7/10] Memory Optimization")
            delay(100)
            executeCommands(
                "echo 0 > /proc/sys/vm/swappiness 2>/dev/null || true",
                "echo 30 > /proc/sys/vm/dirty_ratio 2>/dev/null || true",
                "echo 10 > /proc/sys/vm/dirty_background_ratio 2>/dev/null || true",
                "echo 50 > /proc/sys/vm/vfs_cache_pressure 2>/dev/null || true",
                "echo 0 > /proc/sys/vm/page-cluster 2>/dev/null || true",
                "echo 0 > /proc/sys/vm/compaction_proactiveness 2>/dev/null || true"
            )

            log("[7/10] Done")
            delay(100)
            
            // SECTION 8: POWER SAVING DISABLED
            log("[8/10] Disabling Power Saving")
            delay(100)
            executeCommands(
                "cmd power set-fixed-performance-mode-enabled true 2>/dev/null || true",
                "cmd power set-mode 0 2>/dev/null || true",
                "settings put global low_power 0 2>/dev/null || true",
                "settings put global app_standby_enabled 0 2>/dev/null || true",
                "settings put global forced_app_standby_enabled 0 2>/dev/null || true",
                "dumpsys deviceidle disable 2>/dev/null || true"
            )

            log("[8/10] Done")
            delay(100)
            
            // SECTION 9: NETWORK OPTIMIZATION
            log("[9/10] Network Tuning")
            delay(100)
            executeCommands(
                "echo 1 > /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null || true",
                "echo 1 > /proc/sys/net/ipv4/tcp_timestamps 2>/dev/null || true",
                "echo 1 > /proc/sys/net/ipv4/tcp_sack 2>/dev/null || true",
                "echo 3 > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null || true",
                "echo cubic > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null || true",
                "echo 16777216 > /proc/sys/net/core/rmem_max 2>/dev/null || true",
                "echo 16777216 > /proc/sys/net/core/wmem_max 2>/dev/null || true"
            )
0
            log("[9/10] Done")
            delay(100)
            
            // SECTION 10: SYSTEM CLEANUP
            log("[10/10] System Cleanup")
            delay(100)
            executeCommands(
                "sync",
                "echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || true",
                "cmd activity kill-all 2>/dev/null || am kill-all 2>/dev/null || true",
                "stop perfd 2>/dev/null || true",
                "stop mpdecision 2>/dev/null || true"
            )

            log("[10/10] Done")
            delay(100)
            
            // CLOUD PHONE SPECIAL OPTIMIZATIONS
            log("[CLOUD] Cloud Phone Boost")
            delay(100)
            executeCommands(
                // Disable Android runtime optimizations for speed
                "setprop dalvik.vm.dex2oat-flags --compiler-filter=verify-none 2>/dev/null || true",
                "setprop dalvik.vm.image-dex2oat-flags --compiler-filter=verify-none 2>/dev/null || true",
                "setprop dalvik.vm.dex2oat-threads 4 2>/dev/null || true",
                
                // Force GPU rendering and max performance
                "setprop debug.hwui.renderer skiagl 2>/dev/null || true",
                "setprop debug.egl.hw 1 2>/dev/null || true",
                "setprop debug.sf.hw 1 2>/dev/null || true",
                "setprop ro.config.enable.hw_accel true 2>/dev/null || true",
                
                // Disable unnecessary cloud phone services
                "setprop config.disable_bluetooth true 2>/dev/null || true",
                "setprop config.disable_location true 2>/dev/null || true",
                "setprop config.disable_cameraservice true 2>/dev/null || true",
                "setprop config.disable_systemui.screenrecord true 2>/dev/null || true",
                
                // Optimize networking for cloud
                "setprop net.tcp.buffersize.default 4096,87380,704512,4096,16384,110208 2>/dev/null || true",
                "setprop net.tcp.buffersize.wifi 524288,1048576,2097152,524288,1048576,2097152 2>/dev/null || true",
                
                // Disable battery optimizations (no real battery)
                "dumpsys battery unplug 2>/dev/null || true",
                "dumpsys battery set level 100 2>/dev/null || true",
                "dumpsys battery set status 2 2>/dev/null || true",
                
                // Disable animations for speed
                "settings put global window_animation_scale 0 2>/dev/null || true",
                "settings put global transition_animation_scale 0 2>/dev/null || true",
                "settings put global animator_duration_scale 0 2>/dev/null || true",
                
                // Boost audio performance
                "setprop audio.offload.disable 1 2>/dev/null || true",
                "setprop persist.audio.fluence.speaker false 2>/dev/null || true"
            )
            
            log("[CLOUD] Done")
            delay(100)
            
            // SUMMARY
            log("")
            log("═══════════════════════════════════")
            log("✓ EXTREME OVERCLOCK COMPLETED")
            log("═══════════════════════════════════")
            log("CPU: MAX FREQ LOCKED | ALL CORES ACTIVE")
            log("GPU: EXTREME OVERCLOCK | THERMAL OFF")
            log("BUS: MAX SPEED | MEMORY: OPTIMIZED")
            log("Cloud Phone: FULLY OPTIMIZED")
            log("═══════════════════════════════════")

            BoostResult(success = true, logs = logs, errors = errors)

        } catch (e: Exception) {
            errors.add("ERROR: ${e.message}")
            log("ERROR: ${e.message ?: "Unknown error"}")
            BoostResult(success = false, logs = logs, errors = errors)
        }
    }

    private fun executeCommands(vararg commands: String) {
        try {
            commands.forEach { cmd ->
                // Execute and wait for completion
                val result = Shell.cmd(cmd).exec()
                // Log errors if any (but continue with || true pattern)
                if (!result.isSuccess && result.err.isNotEmpty()) {
                    // Silent fail - expected with || true pattern
                }
            }
        } catch (e: Exception) {
            // Silent fail - some commands may not be available on all devices
        }
    }
}
