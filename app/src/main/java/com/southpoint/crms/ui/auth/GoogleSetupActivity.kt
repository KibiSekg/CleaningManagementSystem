package com.southpoint.crms.ui.auth

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.southpoint.crms.data.CrmsRepository
import com.southpoint.crms.data.Options
import com.southpoint.crms.data.RoomTakenException
import com.southpoint.crms.data.Roles
import com.southpoint.crms.data.SessionManager
import com.southpoint.crms.data.friendlyError
import com.southpoint.crms.databinding.ActivityGoogleSetupBinding
import com.southpoint.crms.ui.BaseActivity
import com.southpoint.crms.ui.clearErrorWhenTyped
import com.southpoint.crms.ui.requireValue
import com.southpoint.crms.ui.setOptions
import com.southpoint.crms.ui.value
import kotlinx.coroutines.launch

/** Second step for a first-time Google user: confirm name and (for students) choose a free room. */
class GoogleSetupActivity : BaseActivity() {

    private lateinit var binding: ActivityGoogleSetupBinding
    private var isStudent = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uid = CrmsRepository.currentUid
        if (uid == null) {
            logout()
            return
        }
        binding = ActivityGoogleSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isStudent = (intent.getStringExtra(EXTRA_ROLE) ?: Roles.STUDENT) == Roles.STUDENT

        binding.etName.setText(CrmsRepository.currentDisplayName.orEmpty())
        binding.tvSubtitle.text = if (isStudent) {
            "Signed in as ${CrmsRepository.currentEmail.orEmpty()}. Choose your room to finish registering."
        } else {
            "Signed in as ${CrmsRepository.currentEmail.orEmpty()}. Confirm your name to finish registering."
        }

        binding.layoutRoom.visibility = if (isStudent) View.VISIBLE else View.GONE
        if (isStudent) loadAvailableRooms()

        binding.layoutName.clearErrorWhenTyped()
        binding.layoutRoom.clearErrorWhenTyped()

        binding.btnContinue.setOnClickListener { save(uid) }
        binding.btnCancel.setOnClickListener { logout() }
    }

    private fun loadAvailableRooms() {
        lifecycleScope.launch {
            try {
                val taken = CrmsRepository.loadTakenRooms()
                val available = Options.allRooms.filter { it !in taken }
                binding.ddRoom.setOptions(available)
                binding.layoutRoom.helperText =
                    if (available.isEmpty()) "All rooms are taken" else "${available.size} rooms available"
            } catch (e: Exception) {
                binding.ddRoom.setOptions(Options.allRooms) // the database will still stop duplicates
            }
        }
    }

    private fun save(uid: String) {
        val nameOk = binding.layoutName.requireValue("Name is required")
        val roomOk = if (isStudent) binding.layoutRoom.requireValue("Please choose a room number") else true
        if (!(nameOk && roomOk)) return

        val name = binding.layoutName.value().trim()
        val email = CrmsRepository.currentEmail.orEmpty()
        val room = binding.layoutRoom.value()

        showMessage(binding.tvError, null)
        setBusy(binding.btnContinue, true, "Finish registration")

        lifecycleScope.launch {
            try {
                val profile = if (isStudent) {
                    CrmsRepository.createStudentProfile(uid, email, name, room)
                } else {
                    CrmsRepository.createCleanerProfile(uid, email, name)
                }
                SessionManager.start(this@GoogleSetupActivity, profile)
                showConfirmation(
                    title = "Registration successful",
                    message = if (isStudent) "Welcome, ${profile.name}! Room ${profile.room} is now registered to you."
                    else "Welcome, ${profile.name}! Your cleaner account is ready.",
                    buttonText = "Continue"
                ) { goHome(profile.role) }
            } catch (e: Exception) {
                showMessage(binding.tvError, friendlyError(e))
                if (e is RoomTakenException) {
                    binding.ddRoom.setText("", false)
                    loadAvailableRooms()
                }
            } finally {
                setBusy(binding.btnContinue, false, "Finish registration")
            }
        }
    }

    companion object {
        const val EXTRA_ROLE = "extra_role"
    }
}