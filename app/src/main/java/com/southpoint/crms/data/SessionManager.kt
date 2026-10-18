package com.southpoint.crms.data

import android.content.Context

/**
 * Holds the signed-in user's profile while the app runs, and keeps a small copy on the phone
 * so the user stays logged in (even offline) until they press Logout.
 */
object SessionManager {
    var profile: UserProfile? = null

    private const val PREFS = "crms_session"

    /** Call after a successful login / registration. */
    fun start(context: Context, p: UserProfile) {
        profile = p
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("uid", p.uid)
            .putString("role", p.role)
            .putString("name", p.name)
            .putString("email", p.email)
            .putString("room", p.room)
            .putString("externalBookingId", p.externalBookingId)
            .apply()
    }

    /** The saved copy for this Firebase user, or null if there is none. */
    fun cached(context: Context, uid: String): UserProfile? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString("uid", null) != uid) return null
        return UserProfile(
            uid = uid,
            role = prefs.getString("role", "") ?: "",
            name = prefs.getString("name", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            room = prefs.getString("room", "") ?: "",
            externalBookingId = prefs.getString("externalBookingId", "") ?: ""
        )
    }

    /** Call on Logout. */
    fun clear(context: Context) {
        profile = null
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}