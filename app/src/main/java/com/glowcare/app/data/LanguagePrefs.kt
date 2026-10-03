package com.glowcare.app.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.langDataStore by preferencesDataStore(name = "glowcare_lang")

/**
 * Language preference ("en" / "hi"), persisted via DataStore and applied
 * process-wide with per-app locales (AppCompat), so values-hi resources
 * resolve automatically on every API level we support.
 */
object LanguagePrefs {
    private val KEY = stringPreferencesKey("language")

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val flow: Flow<String>
        get() = appContext.langDataStore.data.map { it[KEY] ?: "en" }

    fun current(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) locales[0]?.language ?: "en" else "en"
    }

    suspend fun set(language: String) {
        appContext.langDataStore.edit { it[KEY] = language }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }
}
