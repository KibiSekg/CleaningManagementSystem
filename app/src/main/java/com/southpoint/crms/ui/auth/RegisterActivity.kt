package com.southpoint.crms.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.southpoint.crms.data.CrmsRepository
import com.southpoint.crms.data.Options
import com.southpoint.crms.data.RoomTakenException
import com.southpoint.crms.data.Roles
import com.southpoint.crms.data.friendlyError
import com.southpoint.crms.databinding.ActivityRegisterBinding
import com.southpoint.crms.ui.BaseActivity
import com.southpoint.crms.ui.clearErrorWhenTyped
import com.southpoint.crms.ui.requireEmail
import com.southpoint.crms.ui.requireMatch
import com.southpoint.crms.ui.requireNewPassword
import com.southpoint.crms.ui.requireValue
import com.southpoint.crms.ui.setOptions
import com.southpoint.crms.ui.value
import kotlinx.coroutines.launch

/** Student registration - the first screen for new users (like Home/Register in the original). */
class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ddRoom.setOptions(Options.allRooms)
        loadAvailableRooms()

        listOf(
            binding.layoutRoom, binding.layoutName, binding.layoutEmail,
            binding.layoutPassword, binding.layoutConfirm
        ).forEach { it.clearErrorWhenTyped() }

        binding.btnRegister.setOnClickListener { register() }
        binding.btnGoogle.setOnClickListener {
            signInWithGoogle(Roles.STUDENT, binding.btnGoogle, "Continue with Google", binding.tvError)
        }
        binding.btnGoLogin.setOnClickListener { open(LoginActivity::class.java) }
        binding.btnGoRegisterCleaner.setOnClickListener { open(RegisterCleanerActivity::class.java) }
    }

    /** Hides rooms that already belong to a registered student. */
    private fun loadAvailableRooms() {
        lifecycleScope.launch {
            try {
                val taken = CrmsRepository.loadTakenRooms()
                val available = Options.allRooms.filter { it !in taken }
                binding.ddRoom.setOptions(available)
                if (binding.layoutRoom.value() in taken) binding.ddRoom.setText("", false)
                binding.layoutRoom.helperText =
                    if (available.isEmpty()) "All rooms are taken" else "${available.size} rooms available"
            } catch (e: Exception) {
                // Keep the full list; the database still refuses a room that is already taken.
            }
        }
    }

    private fun register() {
        val roomOk = binding.layoutRoom.requireValue("Please choose a room number")
        val nameOk = binding.layoutName.requireValue("Name is required")
        val emailOk = binding.layoutEmail.requireEmail()
        val passwordOk = binding.layoutPassword.requireNewPassword()
        val confirmOk = binding.layoutConfirm.requireMatch(binding.layoutPassword)
        if (!(roomOk && nameOk && emailOk && passwordOk && confirmOk)) return

        val room = binding.layoutRoom.value()
        val name = binding.layoutName.value().trim()
        val email = binding.layoutEmail.value().trim()
        val password = binding.layoutPassword.value()

        showMessage(binding.tvError, null)
        setBusy(binding.btnRegister, true, "Register")

        lifecycleScope.launch {
            try {
                CrmsRepository.registerStudent(room, name, email, password)
                showConfirmation(
                    title = "Registration successful",
                    message = "Welcome, $name! Your account has been created and room $room is now registered to you. " +
                            "Log in to continue.",
                    buttonText = "Go to login"
                ) {
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                val message = friendlyError(e)
                showMessage(binding.tvError, message)
                if (e is RoomTakenException) {
                    binding.layoutRoom.error = message
                    binding.ddRoom.setText("", false)
                    loadAvailableRooms()
                }
            } finally {
                setBusy(binding.btnRegister, false, "Register")
            }
        }
    }
}