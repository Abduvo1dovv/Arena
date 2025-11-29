package com.example.arena.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

object LocaleHelper {

    private const val PREF_NAME = "arena_prefs"
    private const val KEY_LANGUAGE = "key_language"

    // Tilni o'rnatish va saqlash
    fun setLocale(context: Context, languageCode: String) {
        saveLanguage(context, languageCode)
        updateResources(context, languageCode)

        // Ilovani qayta yuklash (Restart) - o'zgarish darhol ko'rinishi uchun
        if (context is Activity) {
            context.recreate()
        }
    }

    // Ilova ochilganda tilni yuklash
    fun loadLocale(context: Context) {
        val language = getSavedLanguage(context)
        updateResources(context, language)
    }

    private fun saveLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_LANGUAGE, language) }
    }

    private fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    private fun updateResources(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    // Hozirgi tilni olish (UI da ko'rsatish uchun)
    fun getCurrentLanguage(context: Context): String {
        return when(getSavedLanguage(context)) {
            "uz" -> "O'zbekcha"
            "ru" -> "Русский"
            else -> "English"
        }
    }
}