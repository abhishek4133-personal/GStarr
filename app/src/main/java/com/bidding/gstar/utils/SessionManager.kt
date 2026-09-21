package com.bidding.gstar.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "khela_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"
        private const val SESSION_DURATION_MS = 12 * 60 * 60 * 1000L // 12 hours in milliseconds
    }
    
    fun saveLoginSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }
    
    fun isSessionValid(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) {
            return false
        }

        val loginTimestamp = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0)
        val timeDifference = System.currentTimeMillis() - loginTimestamp
        val isValid = timeDifference in 0 until SESSION_DURATION_MS
        if (!isValid) {
            clearSession()
        }
        return isValid
    }
    
    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putLong(KEY_LOGIN_TIMESTAMP, 0)
            apply()
        }
    }
    
    fun getRemainingSessionTime(): Long {
        if (!isSessionValid()) {
            return 0
        }
        
        val loginTimestamp = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - loginTimestamp
        return SESSION_DURATION_MS - elapsed
    }
}
