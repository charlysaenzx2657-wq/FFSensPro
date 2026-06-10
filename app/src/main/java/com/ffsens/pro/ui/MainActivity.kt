package com.ffsens.pro.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ffsens.pro.R
import com.ffsens.pro.data.DeviceDataCollector
import com.ffsens.pro.data.DeviceSpecs
import com.ffsens.pro.databinding.ActivityMainBinding
import com.ffsens.pro.logic.SensitivityEngine
import com.ffsens.pro.utils.LanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PERM_REQUEST = 101

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguageManager.applyToBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        requestPermissionsIfNeeded()
    }

    private fun setupUI() {
        binding.cardDeviceInfo.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))
        binding.btnScan.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_scale))

        binding.btnScan.setOnClickListener { startScan() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun requestPermissionsIfNeeded() {
        val perms = arrayOf(Manifest.permission.READ_PHONE_STATE)
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERM_REQUEST)
        } else {
            loadDevicePreview()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        loadDevicePreview()
    }

    private fun loadDevicePreview() {
        lifecycleScope.launch(Dispatchers.IO) {
            val specs = DeviceDataCollector.collect(this@MainActivity)
            withContext(Dispatchers.Main) { updateDevicePreview(specs) }
        }
    }

    private fun updateDevicePreview(specs: DeviceSpecs) {
        binding.tvDeviceName.text = specs.deviceName
        binding.tvDeviceDetails.text = buildString {
            append("CPU: ${specs.cpuAbi}")
            if (specs.cpuMaxFreqMHz > 0) append(" @ ${specs.cpuMaxFreqMHz}MHz")
            append(" · ${specs.cpuCores} núcleos\n")
            append("RAM: ${specs.totalRamMB / 1024}GB total · ${specs.availableRamMB}MB libre\n")
            append("Pantalla: ${specs.screenWidthPx}×${specs.screenHeightPx} · ${specs.densityDpi}dpi\n")
            append("Display: ${"%.1f".format(specs.screenInches)}\" · ${specs.refreshRateHz.toInt()}Hz")
        }

        if (specs.ffInstalled) {
            binding.chipFfStatus.text = if (specs.ffSuspectedMod)
                getString(R.string.ff_unofficial) + specs.ffVersionName
            else
                getString(R.string.ff_ok) + specs.ffVersionName
            binding.chipFfStatus.setChipBackgroundColorResource(
                if (specs.ffSuspectedMod) R.color.ff_warning else R.color.ff_success
            )
        } else {
            binding.chipFfStatus.text = getString(R.string.ff_not_installed)
            binding.chipFfStatus.setChipBackgroundColorResource(R.color.ff_error)
        }

        binding.cardDeviceInfo.visibility = View.VISIBLE
    }

    private fun startScan() {
        binding.btnScan.isEnabled = false
        binding.progressScan.visibility = View.VISIBLE
        binding.tvScanStatus.visibility = View.VISIBLE

        val scanSteps = listOf(
            getString(R.string.scan_cpu),
            getString(R.string.scan_screen),
            getString(R.string.scan_ram),
            getString(R.string.scan_ff),
            getString(R.string.scan_engine),
            getString(R.string.scan_final)
        )

        lifecycleScope.launch {
            scanSteps.forEachIndexed { index, step ->
                withContext(Dispatchers.Main) {
                    binding.tvScanStatus.text = step
                    binding.progressScan.progress = ((index + 1) * 100 / scanSteps.size)
                }
                delay(420)
            }

            val specs = withContext(Dispatchers.IO) {
                DeviceDataCollector.collect(this@MainActivity)
            }
            val result = withContext(Dispatchers.Default) {
                SensitivityEngine.calculate(specs)
            }

            withContext(Dispatchers.Main) {
                binding.progressScan.visibility = View.GONE
                binding.tvScanStatus.visibility = View.GONE
                binding.btnScan.isEnabled = true

                val intent = Intent(this@MainActivity, ResultActivity::class.java).apply {
                    putExtra("GENERAL",           result.general)
                    putExtra("RED_DOT",           result.redDot)
                    putExtra("SCOPE_2X",          result.scope2x)
                    putExtra("SCOPE_4X",          result.scope4x)
                    putExtra("SNIPER",            result.sniperScope)
                    putExtra("FREE_RECOIL",       result.freeRecoil)
                    putExtra("LOOK_JOYSTICK",     result.lookJoystick)
                    putExtra("FIRE_BUTTON",       result.fireButton)
                    putExtra("DEVICE_TIER",       result.deviceTier.name)
                    putExtra("CAL_SCORE",         result.calibrationScore)
                    putStringArrayListExtra("TIPS", ArrayList(result.tips))
                    putExtra("DEVICE_NAME",       specs.deviceName)
                    putExtra("FF_VERSION",        specs.ffVersionName)
                    putExtra("FF_SUSPECTED_MOD",  specs.ffSuspectedMod)
                    putExtra("SCREEN_PROFILE",    result.screenProfile.name)
                }
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }
}
