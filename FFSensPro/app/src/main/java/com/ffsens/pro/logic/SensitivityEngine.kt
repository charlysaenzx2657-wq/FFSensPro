package com.ffsens.pro.logic

import com.ffsens.pro.data.DeviceSpecs
import kotlin.math.*

data class SensitivityResult(
    val general: Int,
    val redDot: Int,
    val scope2x: Int,
    val scope4x: Int,
    val sniperScope: Int,
    val freeRecoil: Int,
    val lookJoystick: Int,
    val fireButton: Int,
    val deviceTier: DeviceTier,
    val screenProfile: ScreenProfile,
    val calibrationScore: Int,
    val tips: List<String>
)

enum class DeviceTier { LOW, MID, HIGH, ULTRA }
enum class ScreenProfile { SMALL_HD, MID_FHD, LARGE_FHD, HIGH_RES }

object SensitivityEngine {

    fun calculate(specs: DeviceSpecs): SensitivityResult {
        val tier = classifyDeviceTier(specs)
        val screenProfile = classifyScreen(specs)

        val dpiNorm   = normalizeDpi(specs.densityDpi)
        val sizeNorm  = normalizeScreenSize(specs.screenInches)
        val rateNorm  = normalizeRefreshRate(specs.refreshRateHz)
        val ramNorm   = normalizeRam(specs.totalRamMB)
        val cpuNorm   = normalizeCpu(specs.cpuMaxFreqMHz, specs.cpuCores)
        val physPrec  = computePhysicalPrecision(specs.xdpi, specs.ydpi)
        val aspectFac = computeAspectFactor(specs.screenWidthPx, specs.screenHeightPx)

        val generalRaw = 150.0 +
                (sizeNorm  *  28.0) +
                (dpiNorm   * -22.0) +
                (rateNorm  *  12.0) +
                (cpuNorm   *   8.0) +
                (ramNorm   *  -5.0) +
                (physPrec  * -10.0) +
                (aspectFac *   5.0)

        val general    = generalRaw.roundToIntSafe().coerceIn(100, 200)
        val redDot     = (general * (1.10 + dpiNorm * 0.05 - sizeNorm * 0.03)).roundToIntSafe().coerceIn(100, 200)
        val scope2x    = (general * (0.90 - dpiNorm * 0.03 + rateNorm * 0.02)).roundToIntSafe().coerceIn(100, 200)
        val scope4x    = (general * (0.77 - dpiNorm * 0.04 + rateNorm * 0.015)).roundToIntSafe().coerceIn(100, 200)
        val sniper     = (general * (0.61 - dpiNorm * 0.05)).roundToIntSafe().coerceIn(100, 200)
        val freeRecoil = (general * (0.95 - ramNorm * 0.02 + cpuNorm * 0.03)).roundToIntSafe().coerceIn(100, 200)
        val lookJoy    = (general - 10 + (rateNorm * 8).roundToIntSafe()).coerceIn(100, 200)
        val fireButton = (general + 5 + (cpuNorm * 5).roundToIntSafe()).coerceIn(100, 200)
        val calScore   = computeCalibrationScore(specs, tier)
        val tips       = generateTips(specs, tier, screenProfile)

        return SensitivityResult(
            general, redDot, scope2x, scope4x, sniper, freeRecoil,
            lookJoy, fireButton, tier, screenProfile, calScore, tips
        )
    }

    private fun Double.roundToIntSafe(): Int = kotlin.math.round(this).toInt()

    private fun classifyDeviceTier(specs: DeviceSpecs): DeviceTier {
        val ramGB    = specs.totalRamMB / 1024f
        val cpuScore = specs.cpuMaxFreqMHz + (specs.cpuCores * 200L)
        return when {
            ramGB >= 8f && cpuScore >= 3000 -> DeviceTier.ULTRA
            ramGB >= 6f && cpuScore >= 2200 -> DeviceTier.HIGH
            ramGB >= 3f && cpuScore >= 1600 -> DeviceTier.MID
            else -> DeviceTier.LOW
        }
    }

    private fun classifyScreen(specs: DeviceSpecs): ScreenProfile {
        val maxPx = maxOf(specs.screenWidthPx, specs.screenHeightPx)
        return when {
            maxPx >= 2560            -> ScreenProfile.HIGH_RES
            specs.screenInches > 6.3f -> ScreenProfile.LARGE_FHD
            specs.screenInches > 5.5f -> ScreenProfile.MID_FHD
            else                      -> ScreenProfile.SMALL_HD
        }
    }

    private fun normalizeDpi(dpi: Int)       = ((dpi - 160).toDouble() / 320.0).coerceIn(-1.0, 1.0)
    private fun normalizeScreenSize(in_: Float) = ((in_ - 4.5f).toDouble() / 3.0).coerceIn(-1.0, 1.0)
    private fun normalizeRefreshRate(hz: Float) = ((hz - 60f).toDouble() / 105.0).coerceIn(0.0, 1.0)
    private fun normalizeRam(ramMB: Long)     = ((ramMB - 2048).toDouble() / 10240.0).coerceIn(0.0, 1.0)
    private fun normalizeCpu(maxFreqMHz: Long, cores: Int): Double {
        if (maxFreqMHz <= 0) return 0.0
        val score = maxFreqMHz.toDouble() + (cores * 150.0)
        return ((score - 1500.0) / 3000.0).coerceIn(0.0, 1.0)
    }

    private fun computePhysicalPrecision(xdpi: Float, ydpi: Float): Double {
        val ratio      = if (ydpi > 0) xdpi / ydpi else 1.0f
        val uniformity = 1.0 - (ratio - 1.0).toDouble().absoluteValue.coerceAtMost(0.5)
        val avgDpi     = ((xdpi + ydpi) / 2.0)
        return (uniformity * 0.5 + (avgDpi / 600.0).coerceIn(0.0, 1.0) * 0.5)
    }

    private fun computeAspectFactor(w: Int, h: Int): Double {
        val ratio = maxOf(w, h).toDouble() / minOf(w, h).toDouble()
        return ((ratio - 1.78) / 0.5).coerceIn(-1.0, 1.0)
    }

    private fun computeCalibrationScore(specs: DeviceSpecs, tier: DeviceTier): Int {
        var score = 60
        if (specs.cpuMaxFreqMHz > 0) score += 8
        if (specs.xdpi > 0 && specs.ydpi > 0) score += 10
        if (specs.refreshRateHz > 60) score += 7
        if (specs.totalRamMB > 0) score += 5
        if (specs.screenInches > 0) score += 5
        if (tier == DeviceTier.ULTRA) score += 5 else if (tier == DeviceTier.HIGH) score += 3
        return score.coerceIn(0, 100)
    }

    private fun generateTips(specs: DeviceSpecs, tier: DeviceTier, screen: ScreenProfile): List<String> {
        val tips = mutableListOf<String>()
        if (specs.refreshRateHz >= 90f)
            tips.add("✅ Your ${specs.refreshRateHz.toInt()}Hz display gives you a huge advantage. Set Free Fire to 'Ultra' frame rate.")
        if (specs.refreshRateHz == 60f)
            tips.add("⚡ Your screen runs at 60Hz. Set FF graphics to 'High' for best performance.")
        if (tier == DeviceTier.LOW || tier == DeviceTier.MID)
            tips.add("🔧 Close all background apps before playing to free up RAM.")
        if (specs.availableRamMB < 1024)
            tips.add("⚠️ Low available RAM (${specs.availableRamMB}MB). Free up memory for consistent sensitivity.")
        if (screen == ScreenProfile.SMALL_HD)
            tips.add("📱 Small screen detected. Use 3-finger claw grip for best control.")
        if (screen == ScreenProfile.LARGE_FHD || screen == ScreenProfile.HIGH_RES)
            tips.add("📱 Large screen detected. 4-finger claw or gyroscope layout recommended.")
        if (specs.ffSuspectedMod)
            tips.add("⚠️ WARNING: Your Free Fire may not be from an official store (${specs.ffInstallSource}). Modded APKs can cause bans.")
        if (!specs.ffInstalled)
            tips.add("ℹ️ Free Fire not detected. Settings calculated from your device specs.")
        if (specs.densityDpi >= 400)
            tips.add("🎯 High-DPI screen detected. Red dot sensitivity is tuned lower for accuracy.")
        return tips
    }
}
