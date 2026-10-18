package com.southpoint.crms.ui.student

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.southpoint.crms.R
import com.southpoint.crms.data.Booking
import com.southpoint.crms.data.CrmsRepository
import com.southpoint.crms.data.Roles
import com.southpoint.crms.data.Status
import com.southpoint.crms.data.friendlyError
import com.southpoint.crms.databinding.ActivityMyBookingsBinding
import com.southpoint.crms.databinding.ItemBookingBinding
import com.southpoint.crms.ui.BaseActivity
import com.southpoint.crms.ui.formatDate
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Student's bookings, updated live from Firestore. */
class MyBookingsActivity : BaseActivity() {

    private lateinit var binding: ActivityMyBookingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = requireSession(Roles.STUDENT) ?: return
        binding = ActivityMyBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, showBack = true, menuRes = R.menu.menu_notifications) {
            open(NotificationsActivity::class.java)
        }
        binding.btnRequestCleaning.setOnClickListener { open(WhoForActivity::class.java) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CrmsRepository.observeStudentBookings(profile.uid)
                    .catch { showMessage(binding.tvError, friendlyError(it)) }
                    .collect { render(it) }
            }
        }
    }

    private fun render(bookings: List<Booking>) {
        fill(
            binding.containerPending, binding.tvEmptyPending,
            bookings.filter { it.status == Status.PENDING }
        ) { "Waiting for a cleaner to accept" }

        fill(
            binding.containerInProgress, binding.tvEmptyInProgress,
            bookings.filter { it.status == Status.IN_PROGRESS }
        ) { "Cleaner: ${it.cleanerName ?: "Not yet assigned"}" }

        fill(
            binding.containerCompleted, binding.tvEmptyCompleted,
            bookings.filter { it.status == Status.COMPLETED }
        ) { "Cleaner: ${it.cleanerName ?: "Not yet assigned"}" }
    }

    /** Inflates one item_booking card per booking into the container. */
    private fun fill(
        container: LinearLayout,
        emptyView: TextView,
        items: List<Booking>,
        footer: (Booking) -> String
    ) {
        container.removeAllViews()
        emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

        items.forEach { booking ->
            val item = ItemBookingBinding.inflate(layoutInflater, container, false)
            item.tvRoom.text = "Room ${booking.roomNumber}"
            item.tvDateTime.text = "${formatDate(booking.bookingDate)} \u00B7 ${booking.timeSlot}"
            item.tvType.text = "${booking.cleaningType} Cleaning"
            item.tvFooter.text = footer(booking)
            container.addView(item.root)
        }
    }
}
