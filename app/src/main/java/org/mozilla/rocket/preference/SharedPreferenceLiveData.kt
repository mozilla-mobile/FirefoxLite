package org.mozilla.rocket.preference

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

abstract class SharedPreferenceLiveData<T>(
    val sharedPrefs: SharedPreferences,
    val key: String,
    private val defValue: T
) : LiveData<T>() {

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == this.key) {
            value = getValueFromPreferences(key, defValue)
        }
    }

    abstract fun getValueFromPreferences(key: String, defValue: T): T

    override fun onActive() {
        super.onActive()
        value = getValueFromPreferences(key, defValue)
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onInactive() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        super.onInactive()
    }
}

class SharedPreferenceIntLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Int) :
        SharedPreferenceLiveData<Int>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Int): Int = sharedPrefs.getInt(key, defValue)
}

class SharedPreferenceStringLiveData(sharedPrefs: SharedPreferences, key: String, defValue: String) :
        SharedPreferenceLiveData<String>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: String): String = requireNotNull(sharedPrefs.getString(key, defValue))
}

class SharedPreferenceBooleanLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Boolean) :
        SharedPreferenceLiveData<Boolean>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Boolean): Boolean = sharedPrefs.getBoolean(key, defValue)
}

class SharedPreferenceFloatLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Float) :
        SharedPreferenceLiveData<Float>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Float): Float = sharedPrefs.getFloat(key, defValue)
}

class SharedPreferenceLongLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Long) :
        SharedPreferenceLiveData<Long>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Long): Long = sharedPrefs.getLong(key, defValue)
}

fun SharedPreferences.intLiveData(key: String, defValue: Int): SharedPreferenceLiveData<Int> =
        SharedPreferenceIntLiveData(this, key, defValue)

fun SharedPreferences.stringLiveData(key: String, defValue: String): SharedPreferenceLiveData<String> =
        SharedPreferenceStringLiveData(this, key, defValue)

fun SharedPreferences.booleanLiveData(key: String, defValue: Boolean): SharedPreferenceLiveData<Boolean> =
        SharedPreferenceBooleanLiveData(this, key, defValue)

fun SharedPreferences.floatLiveData(key: String, defValue: Float): SharedPreferenceLiveData<Float> =
        SharedPreferenceFloatLiveData(this, key, defValue)

fun SharedPreferences.longLiveData(key: String, defValue: Long): SharedPreferenceLiveData<Long> =
        SharedPreferenceLongLiveData(this, key, defValue)