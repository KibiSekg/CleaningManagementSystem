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
import com.southpoint.crms.data.UserProfile
import com.southpoint.crms.data.friendlyError
import com.southpoint.crms.databinding.ActivityCleanerRequestsBinding
import com.southpoint.crms.databinding.ItemRequestBinding
import com.southpoint.crms.ui.BaseActivity
import com.southpoint.crms.ui.formatDate
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Every cleaner sees the same pool of Pending requests; accepting one makes it theirs. */
class CleanerRequestsActivity : BaseActivity() {

    private lateinit var binding: ActivityCleanerRequestsBinding
    private lateinit var profile: UserProfile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profile = requireSession(Roles.CLEANER) ?: return
        binding = ActivityCleanerRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, showBack = true)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CrmsRepository.observeOpenRequests()
                    .catch { showMessage(binding.tvMessage, friendlyError(it)) }
                    .collect { render(it) }
            }
        }
    }

    private fun render(requests: List<Booking>) {
        binding.containerRequests.removeAllViews()
        binding.tvEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE

        requests.forEach { booking ->
            val item = ItemRequestBinding.inflate(layoutInflater, binding.containerRequests, false)
            item.tvRoom.text = "Room ${booking.roomNumber}"
            item.tvDateTime.text = "${formatDate(booking.bookingDate)} \u00B7 ${booking.timeSlot}"
            item.tvType.text = "${booking.cleaningType} Cleaning (${booking.roomType})"
            if (booking.specialInstructions.isNotEmpty()) {
                item.tvInstructions.text = "\"${booking.specialInstructions}\""
                item.tvInstructions.visibility = View.VISIBLE
            }
            item.btnAccept.setOnClickListener { accept(booking, item.btnAccept) }
            binding.containerRequests.addView(item.root)
        }
    }

    private fun accept(booking: Booking, button: Button) {
        button.isEnabled = false
        showMessage(binding.tvMessage, null)

        lifecycleScope.launch {
            try {
                val accepted = CrmsRepository.acceptBooking(booking, profile.uid, profile.name)
                if (!accepted) {
                    showMessage(binding.tvMessage, "Another cleaner has already accepted that request.")
                }
            } catch (e: Exception) {
                showMessage(binding.tvMessage, friendlyError(e))
                button.isEnabled = true
            }
        }
    }
}
