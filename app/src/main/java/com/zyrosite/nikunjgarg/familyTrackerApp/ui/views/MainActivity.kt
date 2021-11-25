package com.zyrosite.nikunjgarg.familyTrackerApp.ui.views

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.zyrosite.nikunjgarg.familyTrackerApp.R
import com.zyrosite.nikunjgarg.familyTrackerApp.data.alarm.MyAlarmBroadcastReceiver
import com.zyrosite.nikunjgarg.familyTrackerApp.data.datastore.DataStoreManager
import com.zyrosite.nikunjgarg.familyTrackerApp.data.model.UserContact
import com.zyrosite.nikunjgarg.familyTrackerApp.databinding.ActivityMainBinding
import com.zyrosite.nikunjgarg.familyTrackerApp.ui.adapter.ContactAdapter
import com.zyrosite.nikunjgarg.familyTrackerApp.utils.CommonUtils
import com.zyrosite.nikunjgarg.familyTrackerApp.utils.Constants
import kotlinx.coroutines.flow.collect
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var myPhone: String = Constants.NOT_AVAILABLE

    private val contactList = ArrayList<UserContact>()
    private val contactAdapter = ContactAdapter(contactList) {
        if (CommonUtils.checkForInternet(this)) {
            if (it.phoneNumber != Constants.NOTHING) {
                // save to database
                it.phoneNumber?.let { it1 ->
                    databaseRef.child("Users").child(it1).child("request")
                        .setValue(CommonUtils.getDateAndTime())
                }

                val intent = Intent(this, MapsActivity::class.java)
                intent.putExtra(Constants.PHONE_NO, it.phoneNumber)
                intent.putExtra(Constants.NAME, it.name)
                startActivity(intent)
            }
        } else {
            Toast.makeText(this, "Please connect to Internet!!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)

        binding.rvTracker.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = contactAdapter
        }

        if (contactList.size == 0) {
            contactList.add(UserContact(Constants.NO_USERS, Constants.NOTHING))
            contactAdapter.notifyItemChanged(0)
        }

        dataStoreManager = DataStoreManager(this)
        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        lifecycleScope.launchWhenCreated {
            dataStoreManager.getPhoneNo().collect {
                if (it == Constants.NOT_AVAILABLE && auth.currentUser == null) {
                    val i = Intent(this@MainActivity, LoginActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(i)
                    finish()
                }
                myPhone = it
                refreshUsers()
                if (checkLocationPermission()) {
                    startIt()
                }
            }
        }

        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        refreshUsers()
    }

    override fun onRestart() {
        super.onRestart()
        checkLocationPermission()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.addTracker -> {
                startActivity(Intent(this@MainActivity, MyTrackersActivity::class.java))
            }
            R.id.shareApp -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                val shareSub = "Track your family now!"
                val shareBody = getString(R.string.app_link)
                intent.putExtra(Intent.EXTRA_SUBJECT, shareSub)
                intent.putExtra(Intent.EXTRA_TEXT, shareBody)
                startActivity(
                    Intent.createChooser(
                        intent,
                        "Share With"
                    )
                )
            }
            R.id.logOut -> {
                auth.signOut()
                val i = Intent(this@MainActivity, LoginActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                finish()
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    private fun checkContactPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                Constants.CONTACT_CODE
            )
        } else {
            return true
        }
        return false
    }

    private fun checkLocationPermission(): Boolean {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                return checkLocationPermissionAPI30()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                return checkLocationPermissionAPI29()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                return checkLocationPermissionAPI28()
            }
        }
        return false
    }

    // FOR VERSION GREATER THAN OR EQUAL TO API 30 (ANDROID 11)
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkLocationPermissionAPI30(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                Constants.LOCATION_CODE
            )
        } else {
            return true
        }
        return false
    }

    // FOR VERSION GREATER THAN OR EQUAL TO API 29 (ANDROID 10)
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkLocationPermissionAPI29(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                Constants.LOCATION_CODE
            )
        } else {
            return true
        }
        return false
    }

    // FOR VERSION LESS THAN OR EQUAL TO API 28 (ANDROID 9)
    private fun checkLocationPermissionAPI28(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                Constants.LOCATION_CODE
            )
        } else {
            return true
        }
        return false
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
                    checkContactPermission()
                } else {
                    Toast.makeText(this, "Cannot access Contacts!!", Toast.LENGTH_SHORT).show()
                }
            }
            Constants.LOCATION_CODE -> {
                if (grantResults.isEmpty()) {
                    return
                }

                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_DENIED) {
                                AlertDialog.Builder(this)
                                    .setTitle("Background location permission")
                                    .setMessage("Allow location permission to get location updates in background")
                                    .setPositiveButton("Allow") { _, _ ->
                                        requestPermissions(
                                            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                            Constants.LOCATION_CODE
                                        )
                                    }
                                    .setNegativeButton("Cancel") { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .create()
                                    .show()
                            }
                        } else {
                            Toast.makeText(this, "Cannot find Location!!", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                            checkLocationPermission()
                        } else {
                            Toast.makeText(this, "Cannot find Location!!", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                            checkLocationPermission()
                        } else {
                            Toast.makeText(this, "Cannot find Location!!", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    @SuppressLint("Recycle", "Range")
    fun checkName(num: String): String {
        val curContacts: Cursor =
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                null
            )!!
        curContacts.moveToFirst()
        while (curContacts.moveToNext()) {
            val contactNumber: String =
                curContacts.getString(curContacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    .filter { !it.isWhitespace() }
            if (num == contactNumber) {
                return curContacts.getString(curContacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            }
        }
        return Constants.NOT_KNOWN
    }

    private fun refreshUsers() {
        if (myPhone == Constants.NOT_AVAILABLE) {
            return
        }
        databaseRef.child("Users").child(myPhone).child("Finders")
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.children.count() == 0) {
                        return
                    }

                    contactList.clear()

                    if (snapshot.children.count() != 0) {
                        snapshot.children.forEach {
                            if (checkContactPermission()) {
                                val name = it.key?.let { it1 -> checkName(it1) }
                                contactList.add(UserContact(name.toString(), it.key))
                            }
                        }
                    }
                    contactAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.i(Constants.TAG, error.message)
                }
            })
    }

    private fun startIt() {
        if (CommonUtils.isLocationEnabled(this)) {
            if (CommonUtils.checkForInternet(this)) {
                setAlarm()
            } else {
                Toast.makeText(this, "Please connect to Internet!!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG)
                .show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun setAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MyAlarmBroadcastReceiver::class.java)
        intent.putExtra(Constants.PHONE_NO, myPhone)
        val pendingIntent =
            PendingIntent.getBroadcast(
                this,
                Constants.ALARM_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        alarmManager.setRepeating(
            AlarmManager.RTC,
            5000,
            60000,
            pendingIntent
        )
    }
}