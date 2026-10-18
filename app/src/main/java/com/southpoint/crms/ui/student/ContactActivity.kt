package com.southpoint.crms.ui.student

import android.os.Bundle
import com.southpoint.crms.data.Roles
import com.southpoint.crms.databinding.ActivityContactBinding
import com.southpoint.crms.ui.BaseActivity

class ContactActivity : BaseActivity() {

    private lateinit var binding: ActivityContactBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireSession(Roles.STUDENT) ?: return
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, showBack = true)
    }
}
