package com.southpoint.crms.ui

import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.southpoint.crms.R
import com.southpoint.crms.data.CrmsRepository
import com.southpoint.crms.data.Roles
import com.southpoint.crms.data.SessionManager
import com.southpoint.crms.data.UserProfile
import com.southpoint.crms.data.friendlyError
import com.southpoint.crms.ui.auth.GoogleSetupActivity
import com.southpoint.crms.ui.auth.LoginActivity
import com.southpoint.crms.ui.cleaner.CleanerHomeActivity
import com.southpoint.crms.ui.student.StudentHomeActivity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

/** Shared helpers used by every screen. */
abstract class BaseActivity : AppCompatActivity() {

    /**
     * Returns the signed-in profile. If nobody is signed in (or it is the wrong role, e.g. after
     * Android restarted the app) the user is sent back to the splash screen and null is returned.
     * Pass null as role to allow both students and cleaners.
     */
    protected fun requireSession(role: String?): UserProfile? {
        val profile = SessionManager.profile
        if (profile == null || (role != null && profile.role != role)) {
            startActivity(
                Intent(this, SplashActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
            return null
        }
        return profile
    }

    /** Back arrow + optional text action (Notifications / Bookings) in the blue top bar. */
    protected fun setupToolbar(
        toolbar: MaterialToolbar,
        showBack: Boolean,
        menuRes: Int? = null,
        onMenuClick: (() -> Unit)? = null
    ) {
        if (showBack) {
            toolbar.setNavigationOnClickListener { finish() }
        }
        if (menuRes != null) {
            toolbar.inflateMenu(menuRes)
            toolbar.setOnMenuItemClickListener {
                onMenuClick?.invoke()
                true
            }
        }
    }

    protected fun open(target: Class<*>) {
        startActivity(Intent(this, target))
    }

    /** Opens the right home screen and clears everything behind it. */
    protected fun goHome(role: String) {
        val target: Class<*> =
            if (role == Roles.CLEANER) CleanerHomeActivity::class.java else StudentHomeActivity::class.java
        startActivity(Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    protected fun logout() {
        CrmsRepository.logout()
        SessionManager.clear(this)
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    /** Pop-up confirmation (used after registering). */
    protected fun showConfirmation(title: String, message: String, buttonText: String, onOk: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(buttonText) { dialog, _ ->
                dialog.dismiss()
                onOk()
            }
            .show()
    }

    /**
     * Google single sign-on for both login and register screens.
     * Existing users go straight to their home screen; new Google users finish setting up first
     * (choose a room, confirm name) on GoogleSetupActivity.
     */
    protected fun signInWithGoogle(role: String, button: Button, idleText: String, errorView: TextView) {
        showMessage(errorView, null)
        setBusy(button, true, idleText)

        lifecycleScope.launch {
            try {
                val existing = CrmsRepository.signInWithGoogle(this@BaseActivity)
                when {
                    existing == null -> {
                        startActivity(
                            Intent(this@BaseActivity, GoogleSetupActivity::class.java)
                                .putExtra(GoogleSetupActivity.EXTRA_ROLE, role)
                        )
                    }
                    existing.role != role -> {
                        CrmsRepository.logout()
                        showMessage(
                            errorView,
                            if (role == Roles.STUDENT) "This Google account is a cleaner account. Use the cleaner login."
                            else "This Google account is a student account. Use the student login."
                        )
                    }
                    else -> {
                        SessionManager.start(this@BaseActivity, existing)
                        goHome(existing.role)
                    }
                }
            } catch (e: Exception) {
                showMessage(errorView, friendlyError(e))
            } finally {
                setBusy(button, false, idleText)
            }
        }
    }

    /** Shows a message in a TextView (hidden when message is null/empty). */
    protected fun showMessage(view: TextView, message: String?, isError: Boolean = true) {
        if (message.isNullOrEmpty()) {
            view.visibility = View.GONE
            return
        }
        view.text = message
        view.setTextColor(ContextCompat.getColor(this, if (isError) R.color.sp_error else R.color.sp_blue))
        view.visibility = View.VISIBLE
    }

    /** Disables a button and changes its label while a request is running. */
    protected fun setBusy(button: Button, busy: Boolean, idleText: String) {
        button.isEnabled = !busy
        button.text = if (busy) "Please wait..." else idleText
    }

    /** Opens the Material date picker and returns the date as ISO text (yyyy-MM-dd). */
    protected fun pickDate(onPicked: (String) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val iso = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
            onPicked(iso)
        }
        picker.show(supportFragmentManager, "datePicker")
    }
}