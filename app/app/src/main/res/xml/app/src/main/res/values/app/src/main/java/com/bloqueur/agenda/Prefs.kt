package com.bloqueur.agenda

import android.content.Context

/** Réglages de l'appli, enregistrés sur le téléphone. */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("bloqueur", Context.MODE_PRIVATE)

    /** Interrupteur général. */
    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(value) = sp.edit().putBoolean("enabled", value).apply()

    /** Mode strict : bloque aussi les Paramètres pendant le travail. */
    var strict: Boolean
        get() = sp.getBoolean("strict", false)
        set(value) = sp.edit().putBoolean("strict", value).apply()

    /** Mots-clés (séparés par des virgules) cherchés dans le titre des événements. */
    var keywords: String
        get() = sp.getString("keywords", DEFAULT_KEYWORDS) ?: DEFAULT_KEYWORDS
        set(value) = sp.edit().putString("keywords", value).apply()

    /** Applis bloquées (noms de paquets). */
    var blocked: Set<String>
        get() = (sp.getStringSet("blocked", DEFAULT_BLOCKED) ?: DEFAULT_BLOCKED).toSet()
        set(value) = sp.edit().putStringSet("blocked", value.toSet()).apply()

    fun keywordList(): List<String> =
        keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    companion object {
        const val DEFAULT_KEYWORDS = "devoirs,révision,lecture"

        val DEFAULT_BLOCKED: Set<String> = setOf(
            "com.zhiliaoapp.musically",   // TikTok
            "com.instagram.android",      // Instagram
            "com.snapchat.android",       // Snapchat
            "com.google.android.youtube", // YouTube
            "com.facebook.katana",        // Facebook
            "com.twitter.android"         // X
        )

        const val SETTINGS_PACKAGE = "com.android.settings"
    }
}
