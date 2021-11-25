package com.zyrosite.nikunjgarg.familyTrackerApp.data.alarm

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.zyrosite.nikunjgarg.familyTrackerApp.utils.CommonUtils
import com.zyrosite.nikunjgarg.familyTrackerApp.utils.Constants

class MyAlarmBroadcastReceiver : BroadcastReceiver() {

    var databaseRef = FirebaseDatabase.getInstance().reference
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var myPhone: String

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            myPhone = intent.extras?.getString(Constants.PHONE_NO).toString()
        }
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context!!)
        if (CommonUtils.isLocationEnabled(context)) {
            if (CommonUtils.checkForInternet(context)) {
                getLastLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastLocation() {
        fusedLocationProviderClient.lastLocation
            .addOnCompleteListener { task ->
                val location = task.result
                if (location == null) {
                    requestNewLocationData()
                } else {
                    saveLocation(location)
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {

        // Initializing LocationRequest
        // object with appropriate methods
        val mLocationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 15
            fastestInterval = 0
            numUpdates = 1
        }

        // setting LocationRequest
        // on FusedLocationClient
        fusedLocationProviderClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.getMainLooper()
        )
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation = locationResult.lastLocation
            saveLocation(mLastLocation)
        }
    }

    private fun saveLocation(location: Location) {
        databaseRef.child("Users").child(myPhone).child("location").child("latitude")
            .setValue(
                location.latitude
            )
        databaseRef.child("Users").child(myPhone).child("location").child("longitude")
            .setValue(
                location.longitude
            )
        databaseRef.child("Users").child(myPhone).child("location").child("lastOnline")
            .setValue(
                CommonUtils.getDateAndTime()
            )

        databaseRef.child("Users").child(myPhone).child("request")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    databaseRef.child("Users").child(myPhone).child("location").child("latitude")
                        .setValue(
                            location.latitude
                        )
                    databaseRef.child("Users").child(myPhone).child("location").child("longitude")
                        .setValue(
                            location.longitude
                        )
                    databaseRef.child("Users").child(myPhone).child("location").child("lastOnline")
                        .setValue(
                            CommonUtils.getDateAndTime()
                        )
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.i(Constants.TAG, error.message)
                }

            })
    }
}