package com.southpoint.crms.ui.cleaner

import android.os.Bundle
import com.southpoint.crms.data.Roles
import com.southpoint.crms.databinding.ActivityCleanerHomeBinding
import com.southpoint.crms.ui.BaseActivity
import com.southpoint.crms.ui.ProfileActivity

class CleanerHomeActivity : BaseActivity() {

    private lateinit var binding: ActivityCleanerHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = requireSession(Roles.CLEANER) ?: return
        binding = ActivityCleanerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, showBack = false)

        binding.tvGreeting.text = "Hello ${profile.name}"

        binding.cardRequests.setOnClickListener { open(CleanerRequestsActivity::class.java) }
        binding.cardBookings.setOnClickListener { open(CleanerBookingsActivity::class.java) }
        binding.cardProfile.setOnClickListener { open(ProfileActivity::class.java) }
        binding.btnLogout.setOnClickListener { logout() }
    }
}
