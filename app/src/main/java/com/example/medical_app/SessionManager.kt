package com.example.medical_app

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        "medical_app_session",
        Context.MODE_PRIVATE
    )

    fun saveSession(
        token: String,
        email: String,
        userId: Int
    ) {
        prefs.edit()
            .putString("token", token)
            .putString("email", email)
            .putInt("user_id", userId)
            .apply()
    }

    fun getToken(): String? =
        prefs.getString("token", null)

    fun getEmail(): String? =
        prefs.getString("email", null)

    fun getUserId(): Int? =
        if (prefs.contains("user_id")) prefs.getInt("user_id", -1) else null

    fun isLoggedIn(): Boolean =
        getToken() != null && getUserId() != null

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
