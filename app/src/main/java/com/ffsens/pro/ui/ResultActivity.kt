package com.ffsens.pro.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ffsens.pro.R
import com.ffsens.pro.databinding.ActivityResultBinding
import com.ffsens.pro.utils.LanguageManager

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguageManager.applyToBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadResults()
        setupButtons()
    }

    private fun loadResults() {
        val general    = intent.getIntExtra("GENERAL", 150)
        val redDot     = intent.getIntExtra("RED_DOT", 155)
        val scope2x    = intent.getIntExtra("SCOPE_2X", 130)
        val scope4x    = intent.getIntExtra("SCOPE_4X", 115)
        val sniper     = intent.getIntExtra("SNIPER", 100)
        val freeRecoil = intent.getIntExtra("FREE_RECOIL", 145)
        val lookJoy    = intent.getIntExtra("LOOK_JOYSTICK", 140)
        val fireBut    = intent.getIntExtra("FIRE_BUTTON", 155)
        val tier       = intent.getStringExtra("DEVICE_TIER") ?: "MID"
        val calScore   = intent.getIntExtra("CAL_SCORE", 75)
        val deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Tu Dispositivo"
        val ffVersion  = intent.getStringExtra("FF_VERSION") ?: ""
        val ffMod      = intent.getBooleanExtra("FF_SUSPECTED_MOD", false)
        val tips       = intent.getStringArrayListExtra("TIPS") ?: arrayListOf()

        binding.tvDeviceName.text = deviceName
        binding.tvCalScore.text   = "$calScore${getString(R.string.calibrated)}"
        binding.tvTierBadge.text  = when (tier) {
            "ULTRA" -> getString(R.string.tier_ultra)
            "HIGH"  -> getString(R.string.tier_high)
            "MID"   -> getString(R.string.tier_mid)
            else    -> getString(R.string.tier_low)
        }
        binding.barCalibration.progress = calScore

        when {
            ffMod -> {
                binding.tvFfVersion.text = "${getString(R.string.ff_unofficial)}$ffVersion"
                binding.tvFfVersion.setBackgroundResource(R.drawable.badge_warning)
            }
            ffVersion.isEmpty() || ffVersion == "Not installed" -> {
                binding.tvFfVersion.text = getString(R.string.ff_not_installed)
                binding.tvFfVersion.setBackgroundResource(R.drawable.badge_info)
            }
            else -> {
                binding.tvFfVersion.text = "${getString(R.string.ff_ok)}$ffVersion"
                binding.tvFfVersion.setBackgroundResource(R.drawable.badge_success)
            }
        }

        binding.tvGeneral.text    = general.toString()
        binding.tvRedDot.text     = redDot.toString()
        binding.tvScope2x.text    = scope2x.toString()
        binding.tvScope4x.text    = scope4x.toString()
        binding.tvSniper.text     = sniper.toString()
        binding.tvFreeRecoil.text = freeRecoil.toString()
        binding.tvLookJoy.text    = lookJoy.toString()
        binding.tvFireBut.text    = fireBut.toString()

        if (tips.isNotEmpty()) {
            binding.tvTips.text = tips.joinToString("\n\n")
            binding.cardTips.visibility = View.VISIBLE
        }

        binding.cardHeader.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))
        binding.cardSensitivities.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_up_delay))

        binding.btnCopy.tag = buildShareText(
            general, redDot, scope2x, scope4x, sniper, lookJoy, fireBut, deviceName)
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCopy.setOnClickListener {
            val text = binding.btnCopy.tag as? String ?: return@setOnClickListener
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("FF Sensibilidad", text))
            Toast.makeText(this, getString(R.string.copied_ok), Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildShareText(
        general: Int, redDot: Int, scope2x: Int, scope4x: Int,
        sniper: Int, lookJoy: Int, fireBut: Int, device: String
    ) = """
${getString(R.string.share_title)}
${getString(R.string.share_device)}$device
━━━━━━━━━━━━━━━━━━━━
🎯 ${getString(R.string.label_general)}:              $general
🔴 ${getString(R.string.label_red_dot)}:   $redDot
🔭 ${getString(R.string.label_scope2x)}:              $scope2x
🔭 ${getString(R.string.label_scope4x)}:              $scope4x
🎯 ${getString(R.string.label_sniper)}: $sniper
🕹️  ${getString(R.string.label_look)}:          $lookJoy
🔫 ${getString(R.string.label_fire)}:    $fireBut
━━━━━━━━━━━━━━━━━━━━
${getString(R.string.share_footer)}
    """.trimIndent()

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
