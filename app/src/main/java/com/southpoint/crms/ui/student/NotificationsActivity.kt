package com.southpoint.crms.ui.student

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.southpoint.crms.R
import com.southpoint.crms.data.AppNotification
import com.southpoint.crms.data.CrmsRepository
import com.southpoint.crms.data.Roles
import com.southpoint.crms.data.friendlyError
import com.southpoint.crms.databinding.ActivityNotificationsBinding
import com.southpoint.crms.databinding.ItemNotificationBinding
import com.southpoint.crms.ui.BaseActivity
import com.southpoint.crms.ui.formatDate
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class NotificationsActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = requireSession(Roles.STUDENT) ?: return
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, showBack = true, menuRes = R.menu.menu_bookings) {
            open(MyBookingsActivity::class.java)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CrmsRepository.observeNotifications(profile.uid)
                    .catch { showMessage(binding.tvError, friendlyError(it)) }
                    .collect { render(it) }
            }
        }
    }

    private fun render(notifications: List<AppNotification>) {
        binding.containerNotifications.removeAllViews()
        binding.tvEmpty.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE

        notifications.forEach { note ->
            val item = ItemNotificationBinding.inflate(layoutInflater, binding.containerNotifications, false)
            item.tvMessage.text = note.message

            if (!note.cleanerName.isNullOrEmpty()) {
                item.tvCleaner.text = "Cleaner: ${note.cleanerName}"
                item.tvCleaner.visibility = View.VISIBLE
            }
            if (!note.bookingDate.isNullOrEmpty()) {
                item.tvDate.text = "Date: ${formatDate(note.bookingDate)}"
                item.tvDate.visibility = View.VISIBLE
            }
            if (!note.passkey.isNullOrEmpty()) {
                item.tvPasskey.text = "Passkey: ${note.passkey}"
                item.tvPasskey.visibility = View.VISIBLE
            }
            binding.containerNotifications.addView(item.root)
        }
    }
}
