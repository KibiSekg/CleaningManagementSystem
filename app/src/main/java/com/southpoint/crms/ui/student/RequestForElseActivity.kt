package com.southpoint.crms.ui.student

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.southpoint.crms.R
import com.southpoint.crms.data.CrmsRepository
import com.southpoint.crms.data.Options
import com.southpoint.crms.data.Roles
import com.southpoint.crms.data.friendlyError
import com.southpoint.crms.databinding.ActivityRequestElseBinding
import com.southpoint.crms.ui.BaseActivity
import com.southpoint.crms.ui.clearErrorWhenTyped
import com.southpoint.crms.ui.formatDate
import com.southpoint.crms.ui.requireValue
import com.southpoint.crms.ui.setOptions
import com.southpoint.crms.ui.value
import kotlinx.coroutines.launch

/** "Someone Else": needs the other student's External Booking ID (shown on their profile). */
class RequestForElseActivity : BaseActivity() {

    private lateinit var binding: ActivityRequestElseBinding
    private var selectedDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireSession(Roles.STUDENT) ?: return
        binding = ActivityRequestElseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, showBack = true, menuRes = R.menu.menu_notifications) {
            open(NotificationsActivity::class.java)
        }

        binding.ddRoom.setOptions(Options.allRooms)
        binding.ddRoomType.setOptions(Options.roomTypes)
        binding.ddTime.setOptions(Options.timeSlots)
        binding.ddCleaningType.setOptions(Options.cleaningTypes)

        listOf(
            binding.layoutRoom, binding.layoutRoomType, binding.layoutTime,
            binding.layoutCleaningType, binding.layoutExternalId
        ).forEach { it.clearErrorWhenTyped() }

        binding.etDate.setOnClickListener { chooseDate() }
        binding.layoutDate.setEndIconOnClickListener { chooseDate() }

        binding.btnSubmit.setOnClickListener { submit() }
    }

    private fun chooseDate() {
        pickDate { iso ->
            selectedDate = iso
            binding.etDate.setText(formatDate(iso))
            binding.layoutDate.error = null
        }
    }

    private fun submit() {
        val roomOk = binding.layoutRoom.requireValue()
        val dateOk = if (selectedDate.isEmpty()) {
            binding.layoutDate.error = "Please select a date"
            false
        } else {
            true
        }
        val typeOk = binding.layoutRoomType.requireValue()
        val timeOk = binding.layoutTime.requireValue()
        val cleaningOk = binding.layoutCleaningType.requireValue()
        val idOk = binding.layoutExternalId.requireValue(
            "Enter the External Booking ID of the student you're booking for"
        )
        if (!(roomOk && dateOk && typeOk && timeOk && cleaningOk && idOk)) return

        val externalId = binding.layoutExternalId.value().trim()

        showMessage(binding.tvError, null)
        setBusy(binding.btnSubmit, true, "Submit")

        lifecycleScope.launch {
            try {
                val targetId = CrmsRepository.findStudentByExternalId(externalId)
                if (targetId == null) {
                    binding.layoutExternalId.error = "That External Booking ID doesn't match any student."
                } else {
                    CrmsRepository.createBooking(
                        targetStudentId = targetId,
                        onBehalfOfSomeoneElse = true,
                        room = binding.layoutRoom.value(),
                        date = selectedDate,
                        roomType = binding.layoutRoomType.value(),
                        timeSlot = binding.layoutTime.value(),
                        cleaningType = binding.layoutCleaningType.value(),
                        instructions = binding.layoutInstructions.value()
                    )
                    open(MyBookingsActivity::class.java)
                    finish()
                }
            } catch (e: Exception) {
                showMessage(binding.tvError, friendlyError(e))
            } finally {
                setBusy(binding.btnSubmit, false, "Submit")
            }
        }
    }
}
