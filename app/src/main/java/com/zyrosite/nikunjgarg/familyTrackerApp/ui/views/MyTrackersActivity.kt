package com.zyrosite.nikunjgarg.familyTrackerApp.ui.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.zyrosite.nikunjgarg.familyTrackerApp.R
import com.zyrosite.nikunjgarg.familyTrackerApp.data.datastore.DataStoreManager
import com.zyrosite.nikunjgarg.familyTrackerApp.data.model.UserContact
import com.zyrosite.nikunjgarg.familyTrackerApp.databinding.ActivityMyTrackersBinding
import com.zyrosite.nikunjgarg.familyTrackerApp.ui.adapter.ContactAdapter
import com.zyrosite.nikunjgarg.familyTrackerApp.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyTrackersActivity : AppCompatActivity() {

    private var _binding: ActivityMyTrackersBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var databaseRef: DatabaseReference
    private lateinit var myPhone: String

    private val contactList = ArrayList<UserContact>()
    private val contactAdapter = ContactAdapter(contactList) {
        DataStoreManager.myTrackers.remove(it.phoneNumber)
        lifecycleScope.launchWhenCreated {
            dataStoreManager.storeContactInfo()
            withContext(Dispatchers.Main) {
                refreshData()
            }
        }

        // remove from realtime database
        it.phoneNumber?.let { it1 ->
            databaseRef.child("Users").child(it1).child("Finders").child(
                myPhone
            ).removeValue()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMyTrackersBinding.inflate(layoutInflater)

        binding.rvTracker.apply {
            layoutManager = LinearLayoutManager(this@MyTrackersActivity)
            adapter = contactAdapter
        }

        if (contactList.size == 0) {
            contactList.add(UserContact(Constants.NO_USERS, Constants.NOTHING))
            contactAdapter.notifyItemInserted(0)
        }

        dataStoreManager = DataStoreManager(this)
        databaseRef = FirebaseDatabase.getInstance().reference

        CoroutineScope(Dispatchers.IO).launch {
            dataStoreManager.loadTrackersList()
            withContext(Dispatchers.Main) {
                refreshData()
            }
        }

        lifecycleScope.launchWhenCreated {
            dataStoreManager.getPhoneNo().collect {
                myPhone = it
            }
        }

        setContentView(binding.root)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.tracker_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.addContact -> {
                checkContactPermission()
            }
            R.id.finishActivity -> {
                finish()
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    private fun checkContactPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                Constants.CONTACT_CODE
            )
        } else {
            pickContact()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.CONTACT_CODE -> {
                if (grantResults.isEmpty()) {
                    return
                }

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickContact()
                } else {
                    Toast.makeText(this, "Cannot access Contacts!!", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun pickContact() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startForResult.launch(intent)
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val contactData = result.data?.data
                val c = contactData?.let { contentResolver.query(it, null, null, null, null) }

                if (c?.moveToFirst() == true) {
                    val id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val hasPhone =
                        c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))

                    if (hasPhone.equals("1")) {
                        val phones = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + id,
                            null,
                            null
                        )

                        phones!!.moveToFirst()
                        val phoneNumber = phones.getString(phones.getColumnIndex("data1"))
                            .filter { !it.isWhitespace() }
                        val name =
                            c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

                        DataStoreManager.myTrackers[phoneNumber] = name
                        lifecycleScope.launchWhenCreated {
                            dataStoreManager.storeContactInfo()
                            withContext(Dispatchers.Main) {
                                refreshData()
                            }
                        }

                        try {
                            // save to realtime database
                            databaseRef.child("Users").child(phoneNumber).child("Finders").child(
                                myPhone
                            ).setValue(true)
                        } catch (e: Exception) {
                            Log.i(Constants.TAG, e.message.toString())
                        }
                    }
                }
            }
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshData() {
        if (DataStoreManager.myTrackers.isEmpty()) {
            return
        }
        contactList.clear()
        // key is phone no, value is name
        for ((key, value) in DataStoreManager.myTrackers) {
            contactList.add(UserContact(value, key))
        }
        contactAdapter.notifyDataSetChanged()
    }

}