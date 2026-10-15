package com.bloqueur.agenda

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Service d'accessibilité : dès qu'une appli bloquée s'ouvre pendant un
 * créneau de travail de l'agenda, on renvoie à l'écran d'accueil.
 */
class BlockerService : AccessibilityService() {

    private var lastCheck = 0L
    private var lastResult = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        val prefs = Prefs(this)
        if (!prefs.enabled) return

        val shouldBlock = pkg in prefs.blocked ||
            (prefs.strict && pkg == Prefs.SETTINGS_PACKAGE)
        if (!shouldBlock) return

        if (!isWorkTime(prefs)) return

        performGlobalAction(GLOBAL_ACTION_HOME)
        Toast.makeText(this, "Bloqué : c'est l'heure de travailler", Toast.LENGTH_SHORT).show()
    }

    /** On interroge l'agenda au maximum toutes les 15 secondes. */
    private fun isWorkTime(prefs: Prefs): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastCheck > 15_000) {
            lastResult = CalendarChecker.isWorkTimeNow(this, prefs.keywordList())
            lastCheck = now
        }
        return lastResult
    }

    override fun onInterrupt() {}
}
