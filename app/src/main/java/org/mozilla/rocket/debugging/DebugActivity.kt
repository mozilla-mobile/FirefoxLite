package org.mozilla.rocket.debugging

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_debug.debug_locale_layout
import kotlinx.android.synthetic.main.activity_debug.debug_locale_text
import kotlinx.android.synthetic.main.activity_debug.toolbar
import org.json.JSONArray
import org.mozilla.focus.R
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.preference.stringLiveData

class DebugActivity : AppCompatActivity() {

    private lateinit var preference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        preference = getSharedPreferences(PREF_NAME_DEBUG, Context.MODE_PRIVATE)
        initDebugLocale()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun initDebugLocale() {
        val debugLocales = getDebugLocales()
        getDebugLocaleLiveData().observe(this, Observer {
            debug_locale_text.text = it
        })
        debug_locale_layout.setOnClickListener {
            showDropDownListDialog(debugLocales)
        }
    }

    private fun getDebugLocales(): List<String> =
            parseDebugLocalesJson(FirebaseHelper.getFirebase().getRcString(STR_RC_DEBUG_LOCALES))

    private fun getDebugLocaleLiveData(): LiveData<String> =
            preference.stringLiveData(SHARED_PREF_KEY_DEBUG_LOCALE, DEBUG_DEFAULT_LOCALE)

    private fun saveDebugLocale(locale: String) {
        preference.edit().putString(SHARED_PREF_KEY_DEBUG_LOCALE, locale).apply()
    }

    private fun showDropDownListDialog(data: List<String>) {
        val appContext = applicationContext
        val adapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice, data)
        AlertDialog.Builder(this)
                .setAdapter(adapter) { _, which ->
                    val selectedLocale = data[which]
                    FirebaseHelper.setUserProperty(appContext, "debug_locale", selectedLocale)
                    saveDebugLocale(selectedLocale)
                    refreshAppConfigs()
                }
                .create().show()
    }

    private fun refreshAppConfigs() {
        FirebaseHelper.refreshRemoteConfig { isSuccess, exception ->
            if (isSuccess) {
                Toast.makeText(this, "Refresh successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Refresh failed: $exception", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val STR_RC_DEBUG_LOCALES = "str_debug_locales"

        private const val PREF_NAME_DEBUG = "debug_pref"
        private const val SHARED_PREF_KEY_DEBUG_LOCALE = "shared_pref_key_debug_locale"
        private const val DEBUG_DEFAULT_LOCALE = "Default"

        fun getStartIntent(context: Context) = Intent(context, DebugActivity::class.java)
    }
}

private fun parseDebugLocalesJson(jsonStr: String): List<String> {
    if (jsonStr.isEmpty()) {
        return emptyList()
    }
    val jsonArray = JSONArray(jsonStr)
    return (0 until jsonArray.length())
            .map { jsonArray.getJSONObject(it).getString("country") }
}