package com.bloqueur.agenda

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract

/** Regarde dans l'agenda du téléphone si un créneau de travail est en cours. */
object CalendarChecker {

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** Vrai si un événement en cours contient un des mots-clés dans son titre. */
    fun isWorkTimeNow(context: Context, keywords: List<String>): Boolean {
        if (keywords.isEmpty() || !hasPermission(context)) return false

        val now = System.currentTimeMillis()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, now)

        val cursor = runCatching {
            context.contentResolver.query(
                builder.build(),
                arrayOf(CalendarContract.Instances.TITLE),
                null,
                null,
                null
            )
        }.getOrNull() ?: return false

        cursor.use {
            while (it.moveToNext()) {
                val title = it.getString(0) ?: continue
                if (keywords.any { word -> title.contains(word, ignoreCase = true) }) {
                    return true
                }
            }
        }
        return false
    }
}
