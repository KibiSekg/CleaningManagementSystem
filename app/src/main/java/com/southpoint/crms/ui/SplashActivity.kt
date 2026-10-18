package com.southpoint.crms.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.southpoint.crms.R
import com.southpoint.crms.data.CrmsRepository
import com.southpoint.crms.data.Roles
import com.southpoint.crms.data.SessionManager
import com.southpoint.crms.data.UserProfile
import com.southpoint.crms.ui.auth.LoginActivity
import com.southpoint.crms.ui.auth.RegisterActivity
import com.southpoint.crms.ui.cleaner.CleanerHomeActivity
import com.southpoint.crms.ui.student.StudentHomeActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Decides where to go when the app opens. A signed-in user stays signed in (until they press Logout):
 * their profile is loaded from the Realtime Database, or from the copy saved on the phone if the
 * network is slow or offline.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            var target: Class<*> = RegisterActivity::class.java
            val uid = CrmsRepository.currentUid

            if (uid != null) {
                var profile: UserProfile? = null
                var lookupFailed = false
                try {
                    profile = withTimeout(8000) { CrmsRepository.loadProfile(uid) }
                } catch (e: Exception) {
                    lookupFailed = true // offline / slow: fall back to the saved copy
                }
                if (lookupFailed) {
                    profile = SessionManager.cached(this@SplashActivity, uid)
                } else if (profile == null) {
                    CrmsRepository.logout() // signed in but never finished setting up
                    SessionManager.clear(this@SplashActivity)
                }

                if (profile != null) {
                    SessionManager.start(this@SplashActivity, profile)
                    target = if (profile.role == Roles.CLEANER) {
                        CleanerHomeActivity::class.java
                    } else {
                        StudentHomeActivity::class.java
                    }
                } else if (lookupFailed) {
                    target = LoginActivity::class.java
                }
            }
            startActivity(Intent(this@SplashActivity, target))
            finish()
        }
    }
}