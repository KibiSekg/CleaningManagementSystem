package com.southpoint.crms.ui.cleaner

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.southpoint.crms.data.Booking
import com.southpoint.crms.data.CrmsRepository
import com.southpoint.crms.data.Roles
import com.southpoint.crms.data.Status
import com.southpoint.crms.data.UserProfile
import com.southpoint.crms.data.friendlyError
import com.southpoint.crms.databinding.ActivityCleanerBookingsBinding
import com.southpoint.crms.databinding.ItemBookingBinding
import com.southpoint.crms.databinding.ItemJobBinding
import com.southpoint.crms.ui.BaseActivity
import com.southpoint.crms.ui.clearErrorWhenTyped
import com.southpoint.crms.ui.formatDate
import com.southpoint.crms.ui.requireValue
import com.southpoint.crms.ui.value
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Jobs this cleaner accepted. In-progress jobs are finished by typing the student's passkey. */
class CleanerBookingsActivity : BaseActivity() {

    private lateinit var binding: ActivityCleanerBookingsBinding
    private lateinit var profile: UserProfile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profile = requireSession(Roles.CLEANER) ?: return
        binding = ActivityCleanerBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, showBack = true)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CrmsRepository.observeCleanerBookings(profile.uid)
                    .catch { showMessage(binding.tvMessage, friendlyError(it)) }
                    .collect { render(it) }
            }
        }
    }

    private fun render(bookings: List<Booking>) {
        val inProgress = bookings.filter { it.status == Status.IN_PROGRESS }
        val completed = bookings.filter { it.status == Status.COMPLETED }

        binding.containerInProgress.removeAllViews()
        binding.tvEmptyInProgress.visibility = if (inProgress.isEmpty()) View.VISIBLE else View.GONE
        inProgress.forEach { booking ->
            val item = ItemJobBinding.inflate(layoutInflater, binding.containerInProgress, false)
            item.tvRoom.text = "Room ${booking.roomNumber}"
            item.tvDateTime.text = "${formatDate(booking.bookingDate)} \u00B7 ${booking.timeSlot}"
            item.tvType.text = "${booking.cleaningType} Cleaning"
            if (booking.specialInstructions.isNotEmpty()) {
                item.tvInstructions.text = "\"${booking.specialInstructions}\""
                item.tvInstructions.visibility = View.VISIBLE
            }
            item.layoutPasskey.clearErrorWhenTyped()
            item.btnComplete.setOnClickListener {
                if (item.layoutPasskey.requireValue("Passkey is required")) {
                    complete(booking, item.layoutPasskey.value(), item.btnComplete)
                }
            }
            binding.containerInProgress.addView(item.root)
        }

        binding.containerCompleted.removeAllViews()
        binding.tvEmptyCompleted.visibility = if (completed.isEmpty()) View.VISIBLE else View.GONE
        completed.forEach { booking ->
            val item = ItemBookingBinding.inflate(layoutInflater, binding.containerCompleted, false)
            item.tvRoom.text = "Room ${booking.roomNumber}"
            item.tvDateTime.text = "${formatDate(booking.bookingDate)} \u00B7 ${booking.timeSlot}"
            item.tvType.text = "${booking.cleaningType} Cleaning"
            item.tvFooter.visibility = View.GONE
            binding.containerCompleted.addView(item.root)
        }
    }

    private fun complete(booking: Booking, passkey: String, button: Button) {
        button.isEnabled = false
        showMessage(binding.tvMessage, null)

        lifecycleScope.launch {
            try {
                val done = CrmsRepository.completeBooking(booking, passkey, profile.name)
                if (!done) {
                    showMessage(binding.tvMessage, "Incorrect passkey \u2014 ask the student to confirm it.")
                    button.isEnabled = true
                }
            } catch (e: Exception) {
                showMessage(binding.tvMessage, friendlyError(e))
                button.isEnabled = true
            }
        }
    }
}
