package com.ffsens.pro.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.ffsens.pro.R
import com.ffsens.pro.databinding.ActivitySplashBinding
import com.ffsens.pro.utils.LanguageManager

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguageManager.applyToBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivLogo.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_scale))
        binding.tvAppName.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))
        binding.tvTagline.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_delay))

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(R.anim.fade_in_scale, android.R.anim.fade_out)
            finish()
        }, 2800)
    }
}
