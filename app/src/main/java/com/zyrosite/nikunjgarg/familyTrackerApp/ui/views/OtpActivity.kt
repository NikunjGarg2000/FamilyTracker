package com.zyrosite.nikunjgarg.familyTrackerApp.ui.views

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.zyrosite.nikunjgarg.familyTrackerApp.R.*
import com.zyrosite.nikunjgarg.familyTrackerApp.data.datastore.DataStoreManager
import com.zyrosite.nikunjgarg.familyTrackerApp.databinding.ActivityOtpBinding
import com.zyrosite.nikunjgarg.familyTrackerApp.utils.CommonUtils
import com.zyrosite.nikunjgarg.familyTrackerApp.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class OtpActivity : AppCompatActivity(), View.OnClickListener {

    private var _binding: ActivityOtpBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var mMaterialDialog: AlertDialog
    private var mCounterDown: CountDownTimer? = null
    private lateinit var phoneNumber: String
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var mVerificationId: String? = null
    private var mResendToken: PhoneAuthProvider.ForceResendingToken? = null
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityOtpBinding.inflate(layoutInflater)

        dataStoreManager = DataStoreManager(this)

        binding.btnVerify.setOnClickListener(this)
        binding.btnResend.setOnClickListener(this)

        initViews()
        startVerify()

        setContentView(binding.root)
    }

    private fun initViews() {
        phoneNumber = intent.getStringExtra(Constants.PHONE_NO).toString()
        binding.tvVerify.text = getString(string.textVerify, phoneNumber)

        setSpannableString()

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {

                if (mMaterialDialog.isShowing) {
                    mMaterialDialog.dismiss()
                }

                binding.etOtp.setText(credential.smsCode)
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(Constants.TAG, "onVerificationFailed", e)

                if (mMaterialDialog.isShowing) {
                    mMaterialDialog.dismiss()
                }

                Toast.makeText(this@OtpActivity, e.message.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {

                if (mMaterialDialog.isShowing) {
                    mMaterialDialog.dismiss()
                }

                Toast.makeText(
                    this@OtpActivity,
                    "Code Sent! \nPlease enter if not automatically verified!",
                    Toast.LENGTH_SHORT
                ).show()

                mVerificationId = verificationId
                mResendToken = token
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    if (mMaterialDialog.isShowing) {
                        mMaterialDialog.dismiss()
                    }

                    Toast.makeText(
                        this@OtpActivity,
                        "Sign in successful!",
                        Toast.LENGTH_SHORT
                    ).show()

                    saveNumber()

                } else {
                    Log.w(Constants.TAG, "signInWithCredential:failure", task.exception)

                    if (mMaterialDialog.isShowing) {
                        mMaterialDialog.dismiss()
                    }

                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(
                            this@OtpActivity,
                            "Code is invalid! Please try again!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    private fun setSpannableString() {
        val spannable = SpannableString(getString(string.textWaiting, phoneNumber))
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(p0: View) {
                startActivity(Intent(this@OtpActivity, LoginActivity::class.java))
                finish()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color = ds.linkColor
            }
        }

        spannable.setSpan(
            clickableSpan,
            spannable.length - 13,
            spannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.tvWaitingOrWrong.movementMethod = LinkMovementMethod.getInstance()
        binding.tvWaitingOrWrong.text = spannable
    }

    private fun startVerify() {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        startTimer(60000)
        mMaterialDialog = CommonUtils.showDialog(this, "Verification In Progress!", false).create()
        mMaterialDialog.show()
        mMaterialDialog.window?.setGravity(Gravity.BOTTOM)
    }

    private fun startTimer(milliSecsInFuture: Long) {
        binding.btnResend.isEnabled = false
        mCounterDown = object : CountDownTimer(milliSecsInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvCounter.isVisible = true
                binding.tvCounter.text =
                    getString(string.millisUntilFinished, millisUntilFinished / 1000)
            }

            override fun onFinish() {
                binding.btnResend.isEnabled = true
                binding.tvCounter.visibility = View.INVISIBLE
            }
        }.start()
    }

    private fun saveNumber() {
        CoroutineScope(Dispatchers.IO).launch {
            dataStoreManager.storePhoneNo(phoneNumber)

            // save to database
            val databaseRef = FirebaseDatabase.getInstance().reference
            databaseRef.child("Users").child(phoneNumber).child("request")
                .setValue(CommonUtils.getDateAndTime())
            databaseRef.child("Users").child(phoneNumber).child("Finders")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.children.count() == 0) {
                            databaseRef.child("Users").child(phoneNumber).child("Finders")
                                .setValue(CommonUtils.getDateAndTime())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.i(Constants.TAG, error.message)
                    }

                })

            startActivity(Intent(this@OtpActivity, MainActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mCounterDown != null) {
            mCounterDown!!.cancel()
        }
    }

    override fun onClick(view: View?) {
        CommonUtils.closeKeyboard(this)
        when (view) {
            binding.btnVerify -> {
                val code = binding.etOtp.text.toString()
                if (code.isNotEmpty() && !mVerificationId.isNullOrEmpty()) {
                    mMaterialDialog = CommonUtils.showDialog(this, "Please wait...", false).create()
                    mMaterialDialog.show()
                    mMaterialDialog.window?.setGravity(Gravity.BOTTOM)

                    val credential = PhoneAuthProvider.getCredential(mVerificationId!!, code)
                    signInWithPhoneAuthCredential(credential)
                }
            }

            binding.btnResend -> {
                if (mResendToken != null) {
                    startTimer(60000)
                    mMaterialDialog = CommonUtils.showDialog(this, "Sending a verification code!", false).create()
                    mMaterialDialog.show()
                    mMaterialDialog.window?.setGravity(Gravity.BOTTOM)
                    val options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
                        .setForceResendingToken(mResendToken!!)
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)
                }
            }
        }
    }
}