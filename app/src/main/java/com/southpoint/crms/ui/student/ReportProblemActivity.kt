package com.southpoint.crms.ui.student

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.southpoint.crms.R
import com.southpoint.crms.data.CrmsRepository
import com.southpoint.crms.data.Options
import com.southpoint.crms.data.Roles
import com.southpoint.crms.data.friendlyError
import com.southpoint.crms.databinding.ActivityReportProblemBinding
import com.southpoint.crms.ui.BaseActivity
import com.southpoint.crms.ui.clearErrorWhenTyped
import com.southpoint.crms.ui.requireValue
import com.southpoint.crms.ui.setOptions
import com.southpoint.crms.ui.value
import kotlinx.coroutines.launch

/** Issue + room + photo are all required (as in the original). The photo is checked but not uploaded. */
class ReportProblemActivity : BaseActivity() {

    private lateinit var binding: ActivityReportProblemBinding
    private var photoUri: Uri? = null

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            photoUri = uri
            binding.tvPhotoStatus.setTextColor(getColor(R.color.sp_blue))
            binding.tvPhotoStatus.text = "Photo selected \u2713"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireSession(Roles.STUDENT) ?: return
        binding = ActivityReportProblemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, showBack = true)

        binding.ddRoom.setOptions(Options.allRooms)
        binding.layoutIssue.clearErrorWhenTyped()
        binding.layoutRoom.clearErrorWhenTyped()

        binding.btnChoosePhoto.setOnClickListener { photoPicker.launch("image/*") }
        binding.btnSubmit.setOnClickListener { submit() }
    }

    private fun submit() {
        val issueOk = binding.layoutIssue.requireValue("Please describe the problem.")
        val roomOk = binding.layoutRoom.requireValue("Please select a room number.")
        val photoOk = photoUri != null
        if (!photoOk) {
            binding.tvPhotoStatus.setTextColor(getColor(R.color.sp_error))
            binding.tvPhotoStatus.text = "Please upload a photo."
        }
        if (!(issueOk && roomOk && photoOk)) return

        showMessage(binding.tvMessage, null)
        setBusy(binding.btnSubmit, true, "Submit Report")

        lifecycleScope.launch {
            try {
                CrmsRepository.submitProblemReport(
                    issue = binding.layoutIssue.value(),
                    room = binding.layoutRoom.value(),
                    photoAttached = true
                )
                showMessage(binding.tvMessage, "Report successfully submitted.", isError = false)
                binding.etIssue.setText("")
                binding.ddRoom.setText("", false)
                binding.layoutIssue.error = null
                binding.layoutRoom.error = null
                photoUri = null
                binding.tvPhotoStatus.text = ""
            } catch (e: Exception) {
                showMessage(binding.tvMessage, friendlyError(e))
            } finally {
                setBusy(binding.btnSubmit, false, "Submit Report")
            }
        }
    }
}
