package com.southpoint.crms.ui.student

import android.os.Bundle
import com.southpoint.crms.data.Roles
import com.southpoint.crms.databinding.ActivityGuidelinesBinding
import com.southpoint.crms.ui.BaseActivity

/** Static text - the content lives in activity_guidelines.xml and strings.xml. */
class GuidelinesActivity : BaseActivity() {

    private lateinit var binding: ActivityGuidelinesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireSession(Roles.STUDENT) ?: return
        binding = ActivityGuidelinesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, showBack = true)
    }
}
