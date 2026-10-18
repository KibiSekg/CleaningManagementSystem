package com.southpoint.crms.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.southpoint.crms.data.CrmsRepository
import com.southpoint.crms.data.Roles
import com.southpoint.crms.data.SessionManager
import com.southpoint.crms.data.friendlyError
import com.southpoint.crms.databinding.ActivityLoginBinding
import com.southpoint.crms.ui.BaseActivity
import com.southpoint.crms.ui.clearErrorWhenTyped
import com.southpoint.crms.ui.requireEmail
import com.southpoint.crms.ui.requirePassword
import com.southpoint.crms.ui.value
import kotlinx.coroutines.launch

/** One login screen used for both students and cleaners (switched with EXTRA_CLEANER). */
class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isCleaner = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isCleaner = intent.getBooleanExtra(EXTRA_CLEANER, false)
        bindTexts()

        binding.layoutEmail.clearErrorWhenTyped()
        binding.layoutPassword.clearErrorWhenTyped()

        intent.getStringExtra(EXTRA_INFO)?.let { showMessage(binding.tvInfo, it, isError = false) }

        binding.btnLogin.setOnClickListener { login() }

        binding.btnGoogle.setOnClickListener {
            signInWithGoogle(
                if (isCleaner) Roles.CLEANER else Roles.STUDENT,
                binding.btnGoogle, "Continue with Google", binding.tvError
            )
        }

        binding.btnGoRegister.setOnClickListener {
            open(if (isCleaner) RegisterCleanerActivity::class.java else RegisterActivity::class.java)
        }
        binding.btnSwitchLogin.setOnClickListener {
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .putExtra(EXTRA_CLEANER, !isCleaner)
            )
            finish()
        }
    }

    private fun bindTexts() {
        if (isCleaner) {
            binding.tvTitle.text = "Cleaner Login"
            binding.btnGoRegister.text = "Need a cleaner account? Register"
            binding.btnSwitchLogin.text = "Login as Student"
        } else {
            binding.tvTitle.text = "Welcome Back!"
            binding.btnGoRegister.text = "Don't have an account? Register"
            binding.btnSwitchLogin.text = "Login as Cleaner"
        }
    }

    private fun login() {
        val emailOk = binding.layoutEmail.requireEmail()
        val passwordOk = binding.layoutPassword.requirePassword()
        if (!emailOk || !passwordOk) return

        val email = binding.layoutEmail.value().trim()
        val password = binding.layoutPassword.value()
        val role = if (isCleaner) Roles.CLEANER else Roles.STUDENT

        showMessage(binding.tvError, null)
        binding.tvInfo.visibility = View.GONE
        setBusy(binding.btnLogin, true, "Login")

        lifecycleScope.launch {
            try {
                val profile = CrmsRepository.login(email, password, role)
                SessionManager.start(this@LoginActivity, profile)
                goHome(profile.role)
            } catch (e: Exception) {
                showMessage(binding.tvError, friendlyError(e))
            } finally {
                setBusy(binding.btnLogin, false, "Login")
            }
        }
    }

    companion object {
        const val EXTRA_CLEANER = "extra_cleaner"
        const val EXTRA_INFO = "extra_info"
    }
}