package com.ffsens.pro.data

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.sqrt

data class DeviceSpecs(
    // Device identity
    val brand: String,
    val model: String,
    val manufacturer: String,
    val deviceName: String,

    // CPU info
    val cpuAbi: String,
    val cpuCores: Int,
    val cpuMaxFreqMHz: Long,
    val cpuHardware: String,
    val cpuBoard: String,

    // RAM
    val totalRamMB: Long,
    val availableRamMB: Long,
    val ramUsagePercent: Float,

    // Screen
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val densityDpi: Int,
    val xdpi: Float,
    val ydpi: Float,
    val scaledDensity: Float,
    val screenInches: Float,
    val refreshRateHz: Float,

    // Android
    val androidVersion: String,
    val sdkVersion: Int,

    // Free Fire
    val ffInstalled: Boolean,
    val ffVersionName: String,
    val ffVersionCode: Long,
    val ffPackageValid: Boolean,
    val ffDataFolderExists: Boolean,
    val ffObbFolderExists: Boolean,
    val ffSuspectedMod: Boolean,
    val ffInstallSource: String
)

object DeviceDataCollector {

    fun collect(context: Context): DeviceSpecs {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem / (1024 * 1024)
        val availRam = memInfo.availMem / (1024 * 1024)
        val ramUsage = ((totalRam - availRam).toFloat() / totalRam.toFloat()) * 100f

        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels
        val xdpi = metrics.xdpi
        val ydpi = metrics.ydpi
        val diagonalPx = sqrt((screenW * screenW + screenH * screenH).toDouble()).toFloat()
        val avgDpi = (xdpi + ydpi) / 2f
        val screenInches = diagonalPx / avgDpi

        val refreshRate = try {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.refreshRate
        } catch (e: Exception) { 60f }

        val cpuMaxFreq = getCpuMaxFreqMHz()
        val cpuCores = Runtime.getRuntime().availableProcessors()

        val ffInfo = getFreeFireInfo(context)

        return DeviceSpecs(
            brand = Build.BRAND ?: "Unknown",
            model = Build.MODEL ?: "Unknown",
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            cpuAbi = Build.SUPPORTED_ABIS?.firstOrNull() ?: "Unknown",
            cpuCores = cpuCores,
            cpuMaxFreqMHz = cpuMaxFreq,
            cpuHardware = Build.HARDWARE ?: "Unknown",
            cpuBoard = Build.BOARD ?: "Unknown",
            totalRamMB = totalRam,
            availableRamMB = availRam,
            ramUsagePercent = ramUsage,
            screenWidthPx = screenW,
            screenHeightPx = screenH,
            densityDpi = metrics.densityDpi,
            xdpi = xdpi,
            ydpi = ydpi,
            scaledDensity = metrics.scaledDensity,
            screenInches = screenInches,
            refreshRateHz = refreshRate,
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkVersion = Build.VERSION.SDK_INT,
            ffInstalled = ffInfo.installed,
            ffVersionName = ffInfo.versionName,
            ffVersionCode = ffInfo.versionCode,
            ffPackageValid = ffInfo.packageValid,
            ffDataFolderExists = ffInfo.dataFolderExists,
            ffObbFolderExists = ffInfo.obbFolderExists,
            ffSuspectedMod = ffInfo.suspectedMod,
            ffInstallSource = ffInfo.installSource
        )
    }

    private fun getCpuMaxFreqMHz(): Long {
        return try {
            val cores = Runtime.getRuntime().availableProcessors()
            var maxFreq = 0L
            for (i in 0 until cores) {
                val file = java.io.File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                if (file.exists()) {
                    val freq = file.readText().trim().toLongOrNull() ?: 0L
                    if (freq > maxFreq) maxFreq = freq
                }
            }
            if (maxFreq > 0) maxFreq / 1000 else 0L
        } catch (e: Exception) { 0L }
    }

    private data class FFInfo(
        val installed: Boolean,
        val versionName: String,
        val versionCode: Long,
        val packageValid: Boolean,
        val dataFolderExists: Boolean,
        val obbFolderExists: Boolean,
        val suspectedMod: Boolean,
        val installSource: String
    )

    private fun getFreeFireInfo(context: Context): FFInfo {
        val packages = listOf(
            "com.dts.freefireth",    // Free Fire global
            "com.dts.freefiremax",   // Free Fire MAX
            "com.dts.freefiremaxth"  // Free Fire MAX TH
        )

        for (pkg in packages) {
            try {
                val pm = context.packageManager
                val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }

                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pi.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pi.versionCode.toLong()
                }

                // Check install source to detect sideloaded / modded APKs
                val installSource = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        pm.getInstallSourceInfo(pkg).installingPackageName ?: "Unknown"
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getInstallerPackageName(pkg) ?: "Unknown"
                    }
                } catch (e: Exception) { "Unknown" }

                val suspectedMod = installSource !in listOf(
                    "com.android.vending",      // Google Play
                    "com.sec.android.app.samsungapps", // Samsung Galaxy Store
                    "com.xiaomi.market",
                    "com.huawei.appmarket",
                    "com.oppo.market",
                    "com.vivo.appstore",
                    null
                )

                // Check data / obb folders
                val dataFolder = java.io.File("/sdcard/Android/data/$pkg")
                val obbFolder = java.io.File("/sdcard/Android/obb/$pkg")

                return FFInfo(
                    installed = true,
                    versionName = pi.versionName ?: "Unknown",
                    versionCode = versionCode,
                    packageValid = true,
                    dataFolderExists = dataFolder.exists(),
                    obbFolderExists = obbFolder.exists(),
                    suspectedMod = suspectedMod,
                    installSource = installSource
                )
            } catch (e: Exception) {
                // Package not found, try next
            }
        }

        return FFInfo(
            installed = false,
            versionName = "Not installed",
            versionCode = 0,
            packageValid = false,
            dataFolderExists = false,
            obbFolderExists = false,
            suspectedMod = false,
            installSource = "N/A"
        )
    }
}
