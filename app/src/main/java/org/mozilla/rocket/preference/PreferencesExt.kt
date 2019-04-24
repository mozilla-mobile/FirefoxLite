package org.mozilla.rocket.preference

import org.mozilla.strictmodeviolator.StrictModeViolation

fun Preferences.bypassStrictMode(): Preferences {
    return object : Preferences {
        private var loaded = false

        override fun putString(key: String, value: String) {
            return ensureLoaded {
                this@bypassStrictMode.putString(key, value)
            }
        }

        override fun getString(key: String, defaultValue: String): String? {
            return ensureLoaded {
                this@bypassStrictMode.getString(key, defaultValue)
            }
        }

        override fun putInt(key: String, value: Int) {
            return ensureLoaded {
                this@bypassStrictMode.putInt(key, value)
            }
        }

        override fun getInt(key: String, defaultValue: Int): Int {
            return ensureLoaded {
                this@bypassStrictMode.getInt(key, defaultValue)
            }
        }

        override fun putLong(key: String, value: Long) {
            return ensureLoaded {
                this@bypassStrictMode.putLong(key, value)
            }
        }

        override fun getLong(key: String, defaultValue: Long): Long {
            return ensureLoaded {
                this@bypassStrictMode.getLong(key, defaultValue)
            }
        }

        override fun putFloat(key: String, value: Float) {
            return ensureLoaded {
                this@bypassStrictMode.putFloat(key, value)
            }
        }

        override fun getFloat(key: String, defaultValue: Float): Float {
            return ensureLoaded {
                this@bypassStrictMode.getFloat(key, defaultValue)
            }
        }

        override fun putBoolean(key: String, value: Boolean) {
            return ensureLoaded {
                this@bypassStrictMode.putBoolean(key, value)
            }
        }

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            return ensureLoaded {
                this@bypassStrictMode.getBoolean(key, defaultValue)
            }
        }

        private fun <T> ensureLoaded(op: () -> T): T {
            return if (loaded) {
                op.invoke()
            } else {
                StrictModeViolation.tempGrant({ it.permitDiskWrites().permitDiskReads() }) {
                    op.invoke().also { loaded = true }
                }
            }
        }
    }
}
