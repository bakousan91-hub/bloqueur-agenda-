package com.bloqueur.agenda

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshStatus()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(40))
        }

        root.addView(TextView(this).apply {
            text = "Bloqueur Agenda"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Quand un événement de ton agenda contient un des mots-clés, les applis cochées sont bloquées."
            setPadding(0, dp(8), 0, dp(16))
        })

        statusView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(statusView)

        root.addView(Button(this).apply {
            text = "1. Autoriser l'accès à l'agenda"
            setOnClickListener {
                requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 1)
            }
        })
        root.addView(Button(this).apply {
            text = "2. Activer le blocage (Accessibilité)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        root.addView(Switch(this).apply {
            text = "Blocage activé"
            isChecked = prefs.enabled
            setPadding(0, dp(16), 0, dp(8))
            setOnCheckedChangeListener { _, checked ->
                prefs.enabled = checked
                refreshStatus()
            }
        })
        root.addView(Switch(this).apply {
            text = "Mode strict (bloque aussi les Paramètres pendant le travail)"
            isChecked = prefs.strict
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, checked -> prefs.strict = checked }
        })

        root.addView(TextView(this).apply {
            text = "Mots-clés dans le titre de l'événement (séparés par des virgules) :"
            setPadding(0, dp(16), 0, dp(4))
        })
        root.addView(EditText(this).apply {
            setText(prefs.keywords)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    prefs.keywords = s?.toString() ?: ""
                    refreshStatus()
                }
            })
        })

        root.addView(TextView(this).apply {
            text = "Applis à bloquer :"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(20), 0, dp(8))
        })

        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            .distinctBy { it.first }
            .filter { it.first != packageName }
            .sortedBy { it.second.lowercase() }

        for ((pkg, label) in apps) {
            root.addView(CheckBox(this).apply {
                text = label
                isChecked = pkg in prefs.blocked
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setOnCheckedChangeListener { _, checked ->
                    val updated = prefs.blocked.toMutableSet()
                    if (checked) updated.add(pkg) else updated.remove(pkg)
                    prefs.blocked = updated
                }
            })
        }

        return ScrollView(this).apply { addView(root) }
    }

    private fun accessibilityOn(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(packageName)
    }

    private fun refreshStatus() {
        val calendarOk = CalendarChecker.hasPermission(this)
        val serviceOk = accessibilityOn()
        val workNow = CalendarChecker.isWorkTimeNow(this, prefs.keywordList())

        statusView.text = buildString {
            append("Agenda : ").append(if (calendarOk) "autorisé" else "NON autorisé").append("\n")
            append("Blocage : ").append(if (serviceOk) "actif" else "PAS activé").append("\n")
            append("Mode travail en ce moment : ").append(if (workNow) "OUI" else "non")
        }
    }
}
