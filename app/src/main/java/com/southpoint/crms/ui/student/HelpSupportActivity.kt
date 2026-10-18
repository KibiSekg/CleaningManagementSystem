package com.southpoint.crms.ui.student

import android.os.Bundle
import com.southpoint.crms.data.Roles
import com.southpoint.crms.databinding.ActivityHelpSupportBinding
import com.southpoint.crms.ui.BaseActivity

class HelpSupportActivity : BaseActivity() {

    private lateinit var binding: ActivityHelpSupportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireSession(Roles.STUDENT) ?: return
        binding = ActivityHelpSupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, showBack = true)

        binding.btnGuidelines.setOnClickListener { open(GuidelinesActivity::class.java) }
        binding.btnContact.setOnClickListener { open(ContactActivity::class.java) }
        binding.btnReport.setOnClickListener { open(ReportProblemActivity::class.java) }
    }
}
