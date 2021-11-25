package com.zyrosite.nikunjgarg.familyTrackerApp.ui.views

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import com.zyrosite.nikunjgarg.familyTrackerApp.R
import com.zyrosite.nikunjgarg.familyTrackerApp.databinding.ActivityMapsBinding
import com.zyrosite.nikunjgarg.familyTrackerApp.utils.Constants

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var _binding: ActivityMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var mMap: GoogleMap
    private lateinit var databaseRef: DatabaseReference
    private var latLon = LatLng(0.0, 0.0)
    private var lastOnline = Constants.NOT_KNOWN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMapsBinding.inflate(layoutInflater)

        databaseRef = FirebaseDatabase.getInstance().reference

        val intent = intent
        val phoneNumber = intent.getStringExtra(Constants.PHONE_NO)
        val name = intent.getStringExtra(Constants.NAME)

        title = if (name == Constants.NOT_KNOWN) {
            "$phoneNumber's Location"
        } else {
            "$name's Location"
        }

        phoneNumber?.let { it ->
            databaseRef.child("Users").child(it).child("location").addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value != null) {
                        val latitude = snapshot.child("latitude").value.toString()
                        val longitude = snapshot.child("longitude").value.toString()
                        latLon = LatLng(latitude.toDouble(), longitude.toDouble())
                        lastOnline = snapshot.child("lastOnline").value.toString()

                        loadMap()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.i(Constants.TAG, error.message)
                }
            })
        }

        setContentView(binding.root)
    }

    private fun loadMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.clear()
        val marker = MarkerOptions().position(latLon).title("Last Online: $lastOnline")
        mMap.addMarker(marker)?.showInfoWindow()
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLon, 15f))
    }
}