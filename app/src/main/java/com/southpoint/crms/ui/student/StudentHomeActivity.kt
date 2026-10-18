package com.southpoint.crms.ui.student

import android.os.Bundle
import com.southpoint.crms.R
import com.southpoint.crms.data.Roles
import com.southpoint.crms.databinding.ActivityStudentHomeBinding
import com.southpoint.crms.ui.BaseActivity
import com.southpoint.crms.ui.ProfileActivity

class StudentHomeActivity : BaseActivity() {

    private lateinit var binding: ActivityStudentHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = requireSession(Roles.STUDENT) ?: return
        binding = ActivityStudentHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, showBack = false, menuRes = R.menu.menu_notifications) {
            open(NotificationsActivity::class.java)
        }

        binding.tvGreeting.text = "Hello ${profile.name}"

        binding.cardRequest.setOnClickListener { open(WhoForActivity::class.java) }
        binding.cardBookings.setOnClickListener { open(MyBookingsActivity::class.java) }
        binding.cardProfile.setOnClickListener { open(ProfileActivity::class.java) }
        binding.cardHelp.setOnClickListener { open(HelpSupportActivity::class.java) }
        binding.btnLogout.setOnClickListener { logout() }
    }
}
