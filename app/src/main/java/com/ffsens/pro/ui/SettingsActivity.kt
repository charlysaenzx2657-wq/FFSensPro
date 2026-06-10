package com.ffsens.pro.ui

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ffsens.pro.R
import com.ffsens.pro.databinding.ActivitySettingsBinding
import com.ffsens.pro.utils.LanguageManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguageManager.applyToBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val languages   = LanguageManager.LANGUAGES
        val displayNames = languages.map { it.displayName }
        val currentLang  = LanguageManager.getSavedLanguage(this)
        val currentIndex = languages.indexOfFirst { it.code == currentLang }.coerceAtLeast(0)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, displayNames)
        binding.listLanguages.adapter       = adapter
        binding.listLanguages.choiceMode    = android.widget.ListView.CHOICE_MODE_SINGLE
        binding.listLanguages.setItemChecked(currentIndex, true)
        binding.listLanguages.setSelection(currentIndex)

        // Entrance animation
        binding.cardSettings.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))

        binding.btnSaveLang.setOnClickListener {
            val selected = binding.listLanguages.checkedItemPosition
            if (selected >= 0) {
                val lang = languages[selected]
                LanguageManager.saveLanguage(this, lang.code)
                LanguageManager.applyLanguage(this, lang.code)
                Toast.makeText(this, "✅ ${lang.displayName}", Toast.LENGTH_SHORT).show()
                // Restart app to apply language
                val intent = Intent(this, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }

        binding.btnBackSettings.setOnClickListener { finish() }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
