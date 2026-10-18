package com.southpoint.crms.data

import com.google.firebase.database.FirebaseDatabase

object FirebaseConfig {
    /**
     * Normally leave this empty: the URL is read from google-services.json.
     * If the app says it "can't determine the Firebase Database URL", paste it here, e.g.
     * "https://your-project-default-rtdb.firebaseio.com"
     */
    const val DATABASE_URL = ""

    fun database(): FirebaseDatabase =
        if (DATABASE_URL.isBlank()) FirebaseDatabase.getInstance() else FirebaseDatabase.getInstance(DATABASE_URL)
}