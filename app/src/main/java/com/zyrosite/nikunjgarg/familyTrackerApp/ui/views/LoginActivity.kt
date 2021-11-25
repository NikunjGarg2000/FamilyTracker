package com.zyrosite.nikunjgarg.familyTrackerApp.ui.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.auth.api.credentials.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.zyrosite.nikunjgarg.familyTrackerApp.data.datastore.DataStoreManager
import com.zyrosite.nikunjgarg.familyTrackerApp.databinding.ActivityLoginBinding
import com.zyrosite.nikunjgarg.familyTrackerApp.utils.CommonUtils
import com.zyrosite.nikunjgarg.familyTrackerApp.utils.Constants

class LoginActivity : AppCompatActivity() {

    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataStoreManager: DataStoreManager
    private var mAuth: FirebaseAuth? = null
    private lateinit var countryCode: String
    private lateinit var phoneNumber: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)

        dataStoreManager = DataStoreManager(this)
        mAuth = FirebaseAuth.getInstance()

        phoneSelection()

        binding.etPhone.addTextChangedListener {
            binding.btnNext.isEnabled = !(it.isNullOrEmpty() || it.length < 10)
        }

        binding.btnNext.setOnClickListener {
            if (CommonUtils.checkForInternet(this)) {
                checkNumber()
            } else {
                Toast.makeText(this, "Please connect to Internet!!", Toast.LENGTH_SHORT).show()
            }
        }

        setContentView(binding.root)
    }

    private fun checkNumber() {
        countryCode = binding.ccp.selectedCountryCodeWithPlus
        phoneNumber = countryCode + binding.etPhone.text.toString()

        CommonUtils.closeKeyboard(this)
        notifyUser()
    }

    private fun notifyUser() {
        MaterialAlertDialogBuilder(this).apply {
            setMessage("We will be verifying phone Number $phoneNumber\nIs it ok or would you like to edit the number?")
            setPositiveButton("OK") { _, _ ->
                showOtpActivity()
            }
            setNegativeButton("Edit") { dialog, _ ->
                dialog.dismiss()
            }
            setCancelable(false)
            create()
            show()
        }
    }

    private fun showOtpActivity() {
        startActivity(
            Intent(this, OtpActivity::class.java).putExtra(
                Constants.PHONE_NO,
                phoneNumber.filter { !it.isWhitespace() }
            )
        )
        finish()
    }

    private fun phoneSelection() {
        // To retrieve the Phone Number hints, first, configure
        // the hint selector dialog by creating a HintRequest object.
        val hintRequest = HintRequest.Builder()
            .setPhoneNumberIdentifierSupported(true)
            .build()

        val options = CredentialsOptions.Builder()
            .forceEnableSaveDialog()
            .build()

        // Then, pass the HintRequest object to
        // credentialsClient.getHintPickerIntent()
        // to get an intent to prompt the user to
        // choose a phone number.
        val credentialsClient = Credentials.getClient(applicationContext, options)
        val intent = credentialsClient.getHintPickerIntent(hintRequest)
        try {
            startForResult.launch(IntentSenderRequest.Builder(intent).build())
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
        }
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (result.data != null) {
                    // get data from the dialog which is of type Credential
                    val credential: Credential? =
                        result.data?.getParcelableExtra(Credential.EXTRA_KEY)
                    if (credential?.id?.startsWith("+") == true) {
                        if (credential.id.length == 13) {
                            // eg: +911234567890
                            binding.etPhone.setText(credential.id.substring(3))
                        } else if (credential.id.length == 14) {
                            // eg: +91 1234567890
                            binding.etPhone.setText(credential.id.substring(4))
                        }
                    } else if (credential?.id?.startsWith("0") == true) {
                        // eg: 01234567890
                        binding.etPhone.setText(credential.id.substring(1))
                    } else {
                        binding.etPhone.setText(credential?.id)
                    }
                }
            } else if (result.resultCode == CredentialsApi.ACTIVITY_RESULT_NO_HINTS_AVAILABLE) {
                Toast.makeText(this, "No phone numbers found", Toast.LENGTH_LONG).show()
            }
        }
}