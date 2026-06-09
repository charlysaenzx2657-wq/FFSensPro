package com.ffsens.pro.logic

import com.ffsens.pro.data.DeviceSpecs
import kotlin.math.*

data class SensitivityResult(
    // General sensitivity (100-200 range)
    val general: Int,
    val redDot: Int,
    val scope2x: Int,
    val scope4x: Int,
    val sniperScope: Int,
    val freeRecoil: Int,

    // Advanced
    val lookJoystick: Int,
    val fireButton: Int,

    // Meta info
    val deviceTier: DeviceTier,
    val screenProfile: ScreenProfile,
    val calibrationScore: Int, // 0-100 how well calibrated
    val tips: List<String>
)

enum class DeviceTier {
    LOW,     // <3GB RAM, slow CPU
    MID,     // 3-6GB RAM, mid CPU
    HIGH,    // 6-8GB RAM, fast CPU
    ULTRA    // 8GB+ RAM, flagship CPU
}

enum class ScreenProfile {
    SMALL_HD,    // <5.5" 720p
    MID_FHD,     // 5.5-6.3" 1080p
    LARGE_FHD,   // >6.3" 1080p
    HIGH_RES     // 1440p+
}

object SensitivityEngine {

    // ── PUBLIC ENTRY POINT ──────────────────────────────────────────────────
    fun calculate(specs: DeviceSpecs): SensitivityResult {
        val tier = classifyDeviceTier(specs)
        val screenProfile = classifyScreen(specs)

        // Base sensitivity derived from physical screen size + DPI
        // More DPI  → lower sensitivity needed (more precise movement per mm)
        // Bigger screen → higher sensitivity needed (finger travels more)
        val dpiNorm = normalizeDpi(specs.densityDpi)
        val sizeNorm = normalizeScreenSize(specs.screenInches)
        val rateNorm = normalizeRefreshRate(specs.refreshRateHz)
        val ramNorm = normalizeRam(specs.totalRamMB)
        val cpuNorm = normalizeCpu(specs.cpuMaxFreqMHz, specs.cpuCores)

        // Physical precision factor: how accurately the screen translates touch
        val physicalPrecision = computePhysicalPrecision(specs.xdpi, specs.ydpi)

        // Aspect ratio factor — taller/narrower screens need tuning
        val aspectFactor = computeAspectFactor(specs.screenWidthPx, specs.screenHeightPx)

        // ── GENERAL SENSITIVITY (100–200) ───────────────────────────────────
        // Formula: weighted blend of all device factors
        val generalBase = 150.0 // midpoint of 100-200
        val generalRaw = generalBase +
                (sizeNorm   *  28.0) +   // bigger screen → higher
                (dpiNorm    * -22.0) +   // more DPI → lower
                (rateNorm   *  12.0) +   // higher Hz → slightly higher
                (cpuNorm    *   8.0) +   // faster CPU → slightly higher
                (ramNorm    *  -5.0) +   // more RAM → slightly lower
                (physicalPrecision * -10.0) +
                (aspectFactor *  5.0)

        val general = clamp(generalRaw.roundToInt(), 100, 200)

        // ── RED DOT / IRON SIGHT ────────────────────────────────────────────
        // Red dot needs to be 8-15% higher than general for fast target acquisition
        val redDotFactor = 1.10 + (dpiNorm * 0.05) - (sizeNorm * 0.03)
        val redDot = clamp((general * redDotFactor).roundToInt(), 100, 200)

        // ── 2x SCOPE ────────────────────────────────────────────────────────
        // Medium zoom: ~85-95% of general
        val scope2xFactor = 0.90 - (dpiNorm * 0.03) + (rateNorm * 0.02)
        val scope2x = clamp((general * scope2xFactor).roundToInt(), 100, 200)

        // ── 4x SCOPE ────────────────────────────────────────────────────────
        // Long range: ~70-85% of general
        val scope4xFactor = 0.77 - (dpiNorm * 0.04) + (rateNorm * 0.015)
        val scope4x = clamp((general * scope4xFactor).roundToInt(), 100, 200)

        // ── SNIPER SCOPE (8x) ────────────────────────────────────────────────
        // Very low movement, precision: ~55-68% of general
        val sniperFactor = 0.61 - (dpiNorm * 0.05)
        val sniper = clamp((general * sniperFactor).roundToInt(), 100, 200)

        // ── FREE RECOIL (hipfire control) ────────────────────────────────────
        // Needs to compensate recoil, slightly lower than general
        val freeRecoilFactor = 0.95 - (ramNorm * 0.02) + (cpuNorm * 0.03)
        val freeRecoil = clamp((general * freeRecoilFactor).roundToInt(), 100, 200)

        // ── LOOK JOYSTICK ────────────────────────────────────────────────────
        val lookJoystick = clamp(general - 10 + (rateNorm * 8).roundToInt(), 100, 200)

        // ── FIRE BUTTON ─────────────────────────────────────────────────────
        // Fire reaction should be very responsive
        val fireButton = clamp(general + 5 + (cpuNorm * 5).roundToInt(), 100, 200)

        // ── CALIBRATION SCORE ────────────────────────────────────────────────
        val calScore = computeCalibrationScore(specs, tier)

        // ── TIPS ─────────────────────────────────────────────────────────────
        val tips = generateTips(specs, tier, screenProfile)

        return SensitivityResult(
            general = general,
            redDot = redDot,
            scope2x = scope2x,
            scope4x = scope4x,
            sniperScope = sniper,
            freeRecoil = freeRecoil,
            lookJoystick = lookJoystick,
            fireButton = fireButton,
            deviceTier = tier,
            screenProfile = screenProfile,
            calibrationScore = calScore,
            tips = tips
        )
    }

    // ── DEVICE TIER ─────────────────────────────────────────────────────────
    private fun classifyDeviceTier(specs: DeviceSpecs): DeviceTier {
        val ramGB = specs.totalRamMB / 1024f
        val cpuScore = specs.cpuMaxFreqMHz + (specs.cpuCores * 200)
        return when {
            ramGB >= 8f && cpuScore >= 3000 -> DeviceTier.ULTRA
            ramGB >= 6f && cpuScore >= 2200 -> DeviceTier.HIGH
            ramGB >= 3f && cpuScore >= 1600 -> DeviceTier.MID
            else -> DeviceTier.LOW
        }
    }

    // ── SCREEN PROFILE ──────────────────────────────────────────────────────
    private fun classifyScreen(specs: DeviceSpecs): ScreenProfile {
        val maxPx = maxOf(specs.screenWidthPx, specs.screenHeightPx)
        return when {
            maxPx >= 2560 -> ScreenProfile.HIGH_RES
            specs.screenInches > 6.3f -> ScreenProfile.LARGE_FHD
            specs.screenInches > 5.5f -> ScreenProfile.MID_FHD
            else -> ScreenProfile.SMALL_HD
        }
    }

    // ── NORMALIZERS (all return -1.0 to +1.0) ────────────────────────────────
    private fun normalizeDpi(dpi: Int): Double {
        // 160 dpi = -1.0, 480 dpi = +1.0
        return ((dpi - 160).toDouble() / 320.0).coerceIn(-1.0, 1.0)
    }

    private fun normalizeScreenSize(inches: Float): Double {
        // 4.5" = -1.0, 7.5" = +1.0
        return ((inches - 4.5f).toDouble() / 3.0).coerceIn(-1.0, 1.0)
    }

    private fun normalizeRefreshRate(hz: Float): Double {
        // 60Hz = 0.0, 120Hz = 0.5, 165Hz = 1.0
        return ((hz - 60f).toDouble() / 105.0).coerceIn(0.0, 1.0)
    }

    private fun normalizeRam(ramMB: Long): Double {
        // 2GB = 0.0, 12GB = 1.0
        return ((ramMB - 2048).toDouble() / 10240.0).coerceIn(0.0, 1.0)
    }

    private fun normalizeCpu(maxFreqMHz: Long, cores: Int): Double {
        if (maxFreqMHz <= 0) return 0.0
        val score = maxFreqMHz.toDouble() + (cores * 150.0)
        // ~1500 = low, ~4500 = flagship
        return ((score - 1500.0) / 3000.0).coerceIn(0.0, 1.0)
    }

    private fun computePhysicalPrecision(xdpi: Float, ydpi: Float): Double {
        // High xdpi/ydpi ratio close to 1.0 = more precise screen
        val ratio = if (ydpi > 0) xdpi / ydpi else 1.0
        val uniformity = 1.0 - abs(ratio - 1.0).coerceAtMost(0.5)
        val avgDpi = ((xdpi + ydpi) / 2.0)
        return (uniformity * 0.5 + (avgDpi / 600.0).coerceIn(0.0, 1.0) * 0.5)
    }

    private fun computeAspectFactor(w: Int, h: Int): Double {
        val ratio = maxOf(w, h).toDouble() / minOf(w, h).toDouble()
        // 16:9 ≈ 1.78, 18:9 ≈ 2.0, 20:9 ≈ 2.22
        return ((ratio - 1.78) / 0.5).coerceIn(-1.0, 1.0)
    }

    // ── CALIBRATION SCORE ────────────────────────────────────────────────────
    private fun computeCalibrationScore(specs: DeviceSpecs, tier: DeviceTier): Int {
        var score = 60 // base
        // More data = more precise calibration
        if (specs.cpuMaxFreqMHz > 0) score += 8
        if (specs.xdpi > 0 && specs.ydpi > 0) score += 10
        if (specs.refreshRateHz > 60) score += 7
        if (specs.totalRamMB > 0) score += 5
        if (specs.screenInches > 0) score += 5
        if (tier == DeviceTier.ULTRA) score += 5
        else if (tier == DeviceTier.HIGH) score += 3
        return score.coerceIn(0, 100)
    }

    // ── TIPS ─────────────────────────────────────────────────────────────────
    private fun generateTips(
        specs: DeviceSpecs,
        tier: DeviceTier,
        screen: ScreenProfile
    ): List<String> {
        val tips = mutableListOf<String>()

        if (specs.refreshRateHz >= 90f) {
            tips.add("✅ Your ${specs.refreshRateHz.toInt()}Hz display gives you a huge advantage. Make sure Free Fire is set to 'Ultra' frame rate.")
        }
        if (specs.refreshRateHz == 60f) {
            tips.add("⚡ Your screen runs at 60Hz. Set FF graphics to 'High' for best performance.")
        }
        if (tier == DeviceTier.LOW || tier == DeviceTier.MID) {
            tips.add("🔧 Close all background apps before playing to free up RAM and reduce input lag.")
        }
        if (specs.availableRamMB < 1024) {
            tips.add("⚠️ Low available RAM detected (${specs.availableRamMB}MB). Your sensitivity may feel inconsistent. Free up memory.")
        }
        if (screen == ScreenProfile.SMALL_HD) {
            tips.add("📱 Small screen detected. Use 3-finger claw grip for best control with these settings.")
        }
        if (screen == ScreenProfile.LARGE_FHD || screen == ScreenProfile.HIGH_RES) {
            tips.add("📱 Large screen detected. 4-finger claw or gyroscope layout recommended.")
        }
        if (specs.ffSuspectedMod) {
            tips.add("⚠️ WARNING: Your Free Fire installation may not be from an official store (${specs.ffInstallSource}). Modded APKs can cause bans and inconsistent performance.")
        }
        if (!specs.ffInstalled) {
            tips.add("ℹ️ Free Fire not detected. Sensitivity settings are pre-calculated from your device specs.")
        }
        if (specs.densityDpi >= 400) {
            tips.add("🎯 Your high-DPI screen is ideal for precise aiming. Red dot sensitivity is tuned lower for accuracy.")
        }

        return tips
    }

    private fun clamp(value: Int, min: Int, max: Int) = value.coerceIn(min, max)
    private fun Double.roundToInt() = kotlin.math.roundToInt()
}

private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
