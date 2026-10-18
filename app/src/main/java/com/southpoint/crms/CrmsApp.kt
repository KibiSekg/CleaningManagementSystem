package com.southpoint.crms

import android.app.Application
import com.southpoint.crms.data.FirebaseConfig

class CrmsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Keep a copy of the data on the phone so the app opens instantly and works briefly offline.
        try {
            FirebaseConfig.database().setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Already initialised, or the database URL isn't configured yet (reported on first use).
        }
    }
}