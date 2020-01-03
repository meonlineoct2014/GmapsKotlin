package com.afour.gmapsdemo

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var searchCityEditText: EditText
    private lateinit var searchImage: ImageView
    private lateinit var gpsImage: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        searchCityEditText = editText
        searchImage = img_search_places
        gpsImage = image_gps
        getLocationPermission()
        initWidgets()
    }

    private fun initWidgets() {
        Log.e("initWidgets", "initializing widgets")
        editText.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            Log.e("setOnEditor", "setOnEditorActionListener")
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                Log.e("actionId", "setOnEditorActionListener")
                geoLocate()
                return@OnEditorActionListener true
            }
            false
        })
        gpsImage.setOnClickListener {
            getDeviceLocation()
        }
        hideSoftKeyboard()
    }

    private fun geoLocate() {
        Log.e("geoLocate", "calling  geoLocate")
        var geocoder = Geocoder(this@MapsActivity)
        var listOfAddresses = arrayListOf<Address>()
        var searchString = searchCityEditText.text.toString()
        Log.e("geoLocate", "searchString is $searchString")
        try {
            listOfAddresses = geocoder.getFromLocationName(searchString, 1) as ArrayList<Address>
            if (listOfAddresses.size > 0) {
                var address = listOfAddresses[0]
                Log.e("geoLocate", "Address is == $address")

                moveCamera(
                    LatLng(address.latitude, address.longitude), DEFAULT_ZOOM,
                    address.getAddressLine(0)
                )
            }
        } catch (exception: IOException) {
            exception.printStackTrace()
        }

    }

    private fun getDeviceLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            if (isPermissionGranted) {
                var locations = fusedLocationClient.lastLocation
                locations.addOnCompleteListener {
                    if (it.isSuccessful) {
                        Log.e("getDeviceLocation", "location found")

                        var currentLocation = it.result
                        if (currentLocation != null) {
                            moveCamera(
                                LatLng(currentLocation.latitude, currentLocation.longitude),
                                DEFAULT_ZOOM, "You Are Here"
                            )

                        }

                    } else {
                        Toast.makeText(
                            this@MapsActivity, "Could not found location",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

        } catch (exception: SecurityException) {
            exception.printStackTrace()
        }
    }

    private fun moveCamera(latLong: LatLng, zoom: Float, title: String) {
        Log.e("moveCamera", "moving camera to lat = ${latLong.latitude} with lan = ${latLong.longitude}")
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, zoom))

        if (!title.run { equals("You Are Here") }) {
            var markerOption = MarkerOptions()
                .position(latLong)
                .title(title)

            mMap.addMarker(markerOption)
        }

        hideSoftKeyboard()
    }


    private fun getLocationPermission() {
        var locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (ContextCompat.checkSelfPermission(this.applicationContext, FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (ContextCompat.checkSelfPermission(this.applicationContext, COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                isPermissionGranted = true
                initMap()
            } else {
                ActivityCompat.requestPermissions(
                    this, locationPermissions,
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                this, locationPermissions,
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        isPermissionGranted = false
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    for (i in grantResults) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            return
                        }
                    }
                    isPermissionGranted = true
                    initMap()
                }
            }
        }
    }

    private fun initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Log.e("MapsActivity", "### onMapReady")
        // Add a marker in Sydney and move the camera

        if (isPermissionGranted) {
            getDeviceLocation()
            if (ContextCompat.checkSelfPermission(this.applicationContext, FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            )
                mMap.isMyLocationEnabled = true
        }
        /*val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))*/
    }


    fun hideSoftKeyboard() {
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)


    }

    companion object {
        const val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
        const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        const val LOCATION_PERMISSION_REQUEST_CODE = 1234
        var isPermissionGranted = false
        const val DEFAULT_ZOOM = 15f
    }
}
