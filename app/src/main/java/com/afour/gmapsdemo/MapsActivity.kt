package com.afour.gmapsdemo

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import kotlinx.android.synthetic.main.activity_maps.*
import java.io.IOException
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var gpsImage: ImageView
    private lateinit var infoImage: ImageView
    private lateinit var placesClient: PlacesClient
    private lateinit var placesDetails : Place
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        gpsImage = image_gps
        infoImage = image_info
        enablegps()
        getLocationPermission()
        initWidgets()
        setUpAutoCompleteFragment()
    }

    private fun setUpAutoCompleteFragment() {
        Places.initialize(applicationContext, "AIzaSyCtWsDUTFC72cDdrfsqCPAtyEmstBgRW9w")
        placesClient = Places.createClient(this@MapsActivity)

        val placeSelectionHandler = object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.e(
                    "placeSelectionHandler", "Place: " + place.name + ", "
                            + place.id
                )
                Log.e("placeSelectionHandler", place.phoneNumber)
                placesDetails = place
                showLocationDetails(place)
            }

            override fun onError(error: Status) {
                Log.e("placeSelection-->", error.isSuccess.toString())
            }

        }

        val autoCompleteFragment = supportFragmentManager
            .findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autoCompleteFragment.setHint("Search for city or place")
        autoCompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.PHONE_NUMBER))
        autoCompleteFragment.setOnPlaceSelectedListener(placeSelectionHandler)
    }

    private fun initWidgets() {
        Log.e("initWidgets", "initializing widgets")

        gpsImage.setOnClickListener {
            getDeviceLocation()
        }

        infoImage.setOnClickListener {
            showLocationDetails(placesDetails)
        }
        hideSoftKeyboard()
    }

    private fun showLocationDetails(selectedLocation: Place) {
        placesDetails = selectedLocation
        var placeDetailsInfo = selectedLocation.name + selectedLocation.phoneNumber
        geoLocate(placeDetailsInfo)
    }

    private fun geoLocate(input: String?) {
        Log.e("geoLocate", "calling  geoLocate")
        var geocoder = Geocoder(this@MapsActivity)
        var listOfAddresses = arrayListOf<Address>()
        var searchString = input
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

    fun enablegps() {

        val mLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(2000)
            .setFastestInterval(1000)

        val settingsBuilder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest)
        settingsBuilder.setAlwaysShow(true)

        val result = LocationServices.getSettingsClient(this).checkLocationSettings(settingsBuilder.build())
        result.addOnCompleteListener { task ->

            //getting the status code from exception
            try {
                task.getResult(ApiException::class.java)
            } catch (ex: ApiException) {

                when (ex.statusCode) {

                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {

                        Toast.makeText(this, "GPS IS OFF", Toast.LENGTH_SHORT).show()

                        // Show the dialog by calling startResolutionForResult(), and check the result
                        // in onActivityResult().
                        val resolvableApiException = ex as ResolvableApiException
                        resolvableApiException.startResolutionForResult(
                            this, REQUEST_CHECK_SETTINGS
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        Toast.makeText(this, "PendingIntent unable to execute request.", Toast.LENGTH_SHORT).show()

                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {

                        Toast.makeText(
                            this,
                            "Something is wrong in your GPS",
                            Toast.LENGTH_SHORT
                        ).show()

                    }


                }
            }


        }
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
        if (isPermissionGranted) {
            getDeviceLocation()
            if (ContextCompat.checkSelfPermission(this.applicationContext, FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            )
                mMap.isMyLocationEnabled = true
        }
    }


    private fun hideSoftKeyboard() {
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    companion object {
        const val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
        const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        const val LOCATION_PERMISSION_REQUEST_CODE = 1234
        var isPermissionGranted = false
        const val DEFAULT_ZOOM = 15f
        const val REQUEST_CHECK_SETTINGS =1
    }
}
