package com.southpoint.crms.ui.student

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.southpoint.crms.R
import com.southpoint.crms.data.CrmsRepository
import com.southpoint.crms.data.Options
import com.southpoint.crms.data.Roles
import com.southpoint.crms.data.friendlyError
import com.southpoint.crms.databinding.ActivityRequestMeBinding
import com.southpoint.crms.ui.BaseActivity
import com.southpoint.crms.ui.clearErrorWhenTyped
import com.southpoint.crms.ui.formatDate
import com.southpoint.crms.ui.requireValue
import com.southpoint.crms.ui.setOptions
import com.southpoint.crms.ui.value
import kotlinx.coroutines.launch

/** "For Myself": the room number comes from the student's profile. */
class RequestForMeActivity : BaseActivity() {

    private lateinit var binding: ActivityRequestMeBinding
    private var selectedDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = requireSession(Roles.STUDENT) ?: return
        binding = ActivityRequestMeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, showBack = true, menuRes = R.menu.menu_notifications) {
            open(NotificationsActivity::class.java)
        }

        binding.tvRoomNumber.text = profile.room

        binding.ddRoomType.setOptions(Options.roomTypes)
        binding.ddTime.setOptions(Options.timeSlots)
        binding.ddCleaningType.setOptions(Options.cleaningTypes)

        listOf(binding.layoutRoomType, binding.layoutTime, binding.layoutCleaningType)
            .forEach { it.clearErrorWhenTyped() }

        binding.etDate.setOnClickListener { chooseDate() }
        binding.layoutDate.setEndIconOnClickListener { chooseDate() }

        binding.btnSubmit.setOnClickListener { submit(profile.uid, profile.room) }
    }

    private fun chooseDate() {
        pickDate { iso ->
            selectedDate = iso
            binding.etDate.setText(formatDate(iso))
            binding.layoutDate.error = null
        }
    }

    private fun submit(studentId: String, room: String) {
        val dateOk = if (selectedDate.isEmpty()) {
            binding.layoutDate.error = "Please select a date"
            false
        } else {
            true
        }
        val typeOk = binding.layoutRoomType.requireValue()
        val timeOk = binding.layoutTime.requireValue()
        val cleaningOk = binding.layoutCleaningType.requireValue()
        if (!(dateOk && typeOk && timeOk && cleaningOk)) return

        showMessage(binding.tvError, null)
        setBusy(binding.btnSubmit, true, "Submit")

        lifecycleScope.launch {
            try {
                CrmsRepository.createBooking(
                    targetStudentId = studentId,
                    onBehalfOfSomeoneElse = false,
                    room = room,
                    date = selectedDate,
                    roomType = binding.layoutRoomType.value(),
                    timeSlot = binding.layoutTime.value(),
                    cleaningType = binding.layoutCleaningType.value(),
                    instructions = binding.layoutInstructions.value()
                )
                open(MyBookingsActivity::class.java)
                finish()
            } catch (e: Exception) {
                showMessage(binding.tvError, friendlyError(e))
            } finally {
                setBusy(binding.btnSubmit, false, "Submit")
            }
        }
    }
}
