package com.southpoint.crms.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.southpoint.crms.data.CrmsRepository
import com.southpoint.crms.data.Roles
import com.southpoint.crms.data.friendlyError
import com.southpoint.crms.databinding.ActivityRegisterCleanerBinding
import com.southpoint.crms.ui.BaseActivity
import com.southpoint.crms.ui.clearErrorWhenTyped
import com.southpoint.crms.ui.requireEmail
import com.southpoint.crms.ui.requireMatch
import com.southpoint.crms.ui.requireNewPassword
import com.southpoint.crms.ui.requireValue
import com.southpoint.crms.ui.value
import kotlinx.coroutines.launch

class RegisterCleanerActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterCleanerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterCleanerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listOf(
            binding.layoutName, binding.layoutEmail, binding.layoutPassword, binding.layoutConfirm
        ).forEach { it.clearErrorWhenTyped() }

        binding.btnRegister.setOnClickListener { register() }
        binding.btnGoogle.setOnClickListener {
            signInWithGoogle(Roles.CLEANER, binding.btnGoogle, "Continue with Google", binding.tvError)
        }
        binding.btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).putExtra(LoginActivity.EXTRA_CLEANER, true))
        }
        binding.btnGoStudentRegister.setOnClickListener { open(RegisterActivity::class.java) }
    }

    private fun register() {
        val nameOk = binding.layoutName.requireValue("Name is required")
        val emailOk = binding.layoutEmail.requireEmail()
        val passwordOk = binding.layoutPassword.requireNewPassword()
        val confirmOk = binding.layoutConfirm.requireMatch(binding.layoutPassword)
        if (!(nameOk && emailOk && passwordOk && confirmOk)) return

        val name = binding.layoutName.value().trim()
        val email = binding.layoutEmail.value().trim()
        val password = binding.layoutPassword.value()

        showMessage(binding.tvError, null)
        setBusy(binding.btnRegister, true, "Register")

        lifecycleScope.launch {
            try {
                CrmsRepository.registerCleaner(name, email, password)
                showConfirmation(
                    title = "Registration successful",
                    message = "Welcome, $name! Your cleaner account has been created. Log in to continue.",
                    buttonText = "Go to login"
                ) {
                    startActivity(
                        Intent(this@RegisterCleanerActivity, LoginActivity::class.java)
                            .putExtra(LoginActivity.EXTRA_CLEANER, true)
                    )
                    finish()
                }
            } catch (e: Exception) {
                showMessage(binding.tvError, friendlyError(e))
            } finally {
                setBusy(binding.btnRegister, false, "Register")
            }
        }
    }
}