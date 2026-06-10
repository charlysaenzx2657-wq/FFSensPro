package com.ffsens.pro.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LanguageManager {

    private const val PREF_NAME = "ff_sens_prefs"
    private const val KEY_LANG  = "selected_language"

    val LANGUAGES = listOf(
        Language("es",    "Español"),
        Language("en",    "English"),
        Language("fr",    "Français"),
        Language("pt",    "Português (Portugal)"),
        Language("pt-BR", "Português (Brasil)"),
        Language("zh",    "中文 (Chino)"),
        Language("ja",    "日本語 (Japonés)"),
        Language("es-AR", "Español (Argentina)"),
        Language("es-MX", "Español (México)"),
        Language("es-CO", "Español (Colombia)"),
        Language("es-PE", "Español (Perú)"),
        Language("es-CL", "Español (Chile)"),
        Language("es-VE", "Español (Venezuela)")
    )

    data class Language(val code: String, val displayName: String)

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, "es") ?: "es"
    }

    fun saveLanguage(context: Context, langCode: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, langCode).apply()
    }

    fun applyLanguage(context: Context, langCode: String): Context {
        val locale = if (langCode.contains("-")) {
            val parts = langCode.split("-")
            Locale(parts[0], parts[1])
        } else {
            Locale(langCode)
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    fun applyToBaseContext(base: Context): Context {
        val lang = getSavedLanguage(base)
        return applyLanguage(base, lang)
    }
}
