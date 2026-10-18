package com.southpoint.crms.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.southpoint.crms.R
import com.southpoint.crms.data.CrmsRepository
import com.southpoint.crms.data.Roles
import com.southpoint.crms.data.UserProfile
import com.southpoint.crms.data.friendlyError
import com.southpoint.crms.databinding.ActivityProfileBinding
import com.southpoint.crms.ui.student.NotificationsActivity
import kotlinx.coroutines.launch

/**
 * Profile page shared by students and cleaners.
 * Firebase never lets an app read a password back, so instead of showing it you type your
 * current password to confirm an email / password change.
 */
class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var profile: UserProfile
    private var passwordSectionOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profile = requireSession(null) ?: return
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isStudent = profile.role == Roles.STUDENT
        if (isStudent) {
            setupToolbar(binding.toolbar, showBack = true, menuRes = R.menu.menu_notifications) {
                open(NotificationsActivity::class.java)
            }
        } else {
            setupToolbar(binding.toolbar, showBack = true)
        }

        binding.tvName.text = profile.name
        binding.etEmail.setText(profile.email)

        if (isStudent) {
            binding.cardExternalId.visibility = View.VISIBLE
            binding.tvExternalId.text = profile.externalBookingId
        }

        listOf(
            binding.layoutEmail, binding.layoutNewPassword,
            binding.layoutConfirmPassword, binding.layoutCurrentPassword
        ).forEach { it.clearErrorWhenTyped() }

        binding.btnTogglePassword.setOnClickListener { togglePasswordSection() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnLogout.setOnClickListener { logout() }
    }

    private fun togglePasswordSection() {
        passwordSectionOpen = !passwordSectionOpen
        binding.groupNewPassword.visibility = if (passwordSectionOpen) View.VISIBLE else View.GONE
        binding.btnTogglePassword.text = if (passwordSectionOpen) "Cancel password change" else "Change Password"
        if (!passwordSectionOpen) {
            binding.etNewPassword.setText("")
            binding.etConfirmPassword.setText("")
        }
    }

    private fun save() {
        val emailOk = binding.layoutEmail.requireEmail()
        val newEmail = binding.layoutEmail.value().trim()
        val newPassword = binding.layoutNewPassword.value()
        val wantsPasswordChange = passwordSectionOpen && newPassword.isNotEmpty()

        var passwordOk = true
        var confirmOk = true
        if (wantsPasswordChange) {
            passwordOk = binding.layoutNewPassword.requireNewPassword()
            confirmOk = binding.layoutConfirmPassword.requireMatch(binding.layoutNewPassword)
        }

        val emailChanged = !newEmail.equals(profile.email, ignoreCase = true)
        val currentPassword = binding.layoutCurrentPassword.value()
        var currentOk = true
        if ((emailChanged || wantsPasswordChange) && currentPassword.isEmpty()) {
            binding.layoutCurrentPassword.error = "Enter your current password to save changes"
            currentOk = false
        }

        if (!(emailOk && passwordOk && confirmOk && currentOk)) return

        if (!emailChanged && !wantsPasswordChange) {
            showMessage(binding.tvMessage, "Nothing to update.", isError = false)
            return
        }

        showMessage(binding.tvMessage, null)
        setBusy(binding.btnSave, true, "Save Changes")

        lifecycleScope.launch {
            try {
                val result = CrmsRepository.updateCredentials(
                    currentPassword = currentPassword,
                    newEmail = if (emailChanged) newEmail else "",
                    newPassword = if (wantsPasswordChange) newPassword else ""
                )
                showMessage(binding.tvMessage, result, isError = false)
                binding.etCurrentPassword.setText("")
                binding.etNewPassword.setText("")
                binding.etConfirmPassword.setText("")
            } catch (e: Exception) {
                showMessage(binding.tvMessage, friendlyError(e))
            } finally {
                setBusy(binding.btnSave, false, "Save Changes")
            }
        }
    }
}
