package com.southpoint.crms.ui.student

import android.os.Bundle
import com.southpoint.crms.R
import com.southpoint.crms.data.Roles
import com.southpoint.crms.databinding.ActivityWhoForBinding
import com.southpoint.crms.ui.BaseActivity

/** "Who is this booking for?" - yourself or someone else. */
class WhoForActivity : BaseActivity() {

    private lateinit var binding: ActivityWhoForBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireSession(Roles.STUDENT) ?: return
        binding = ActivityWhoForBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, showBack = true, menuRes = R.menu.menu_notifications) {
            open(NotificationsActivity::class.java)
        }

        binding.cardMyself.setOnClickListener { open(RequestForMeActivity::class.java) }
        binding.cardSomeoneElse.setOnClickListener { open(RequestForElseActivity::class.java) }
    }
}
