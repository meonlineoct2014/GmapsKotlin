package com.afour.gmapsdemo

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.android.synthetic.main.activity_showallmaps.*
import java.util.*

class ShowAllMapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var searchText: EditText
    private lateinit var filterResults : ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_showallmaps)
        enableGPS()
        getLocationPermission()
        init()
    }


    private fun init() {
        Places.initialize(applicationContext, "AIzaSyCtWsDUTFC72cDdrfsqCPAtyEmstBgRW9w")
        placesClient = Places.createClient(this@ShowAllMapsActivity)

        filterResults = image_filter
        searchText = edit_input_text

        filterResults.setOnClickListener {
            googleMap.clear()
        }


        searchText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_SEARCH
            ) {
                Log.e("init", "setOnEditorActionListener called")
                //geoLocatePlace(searchText.text.toString())
                getPlacePredictions(searchText.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun getPlacePredictions(query: String) {
        googleMap.clear()
        var token = AutocompleteSessionToken.newInstance()
        var request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener {
                for (prediction in it.autocompletePredictions) {
                    Log.e("addOnSuccessListener", prediction.placeId)
                    Log.e("addOnSuccessListener", prediction.getPrimaryText(null).toString())
                    geoLocatePlace(prediction.placeId)
                }
            }
            .addOnFailureListener {
                Log.e("addOnFailureListener", "Place not found: " + it as ApiException)
            }
    }

    private fun geoLocatePlace(input: String) {
        Log.e("geoLocatePlace", "geoLocatePlace called")
        var placeId = input
        Log.e("geoLocatePlace", "Place ID is $placeId")
        var listOfPlaces = Arrays.asList(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )
        val fetchPlaceRequest = FetchPlaceRequest.newInstance(placeId, listOfPlaces)

        placesClient.fetchPlace(fetchPlaceRequest)
            .addOnSuccessListener {
                val place = it.place

                Log.e("geoLocatePlace", "Place found: " + place.name)
                drawMarkerOnMap(
                    LatLng(place.latLng!!.latitude, place.latLng!!.longitude),
                    place.address.toString()
                )
                /*moveCamera(
                    LatLng(place.latLng!!.latitude, place.latLng!!.longitude), DEFAULT_ZOOM,
                    place.address.toString()
                )*/
            }.addOnFailureListener {
                Log.e("geoLocatePlace failure", it.message)
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
                            this@ShowAllMapsActivity, "Could not found location",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

        } catch (exception: SecurityException) {
            exception.printStackTrace()
        }
    }

    private fun drawMarkerOnMap(latLong: LatLng,title: String) {
        if (!title.run { equals("You Are Here") }) {
            var markerOption = MarkerOptions()
                .position(latLong)
                .title(title)

            googleMap.addMarker(markerOption)
        }
        hideSoftKeyboard()
    }

    private fun moveCamera(latLong: LatLng, zoom: Float, title: String) {
        Log.e("moveCamera", "moving camera to lat = ${latLong.latitude} with lan = ${latLong.longitude}")
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, zoom))

        if (!title.run { equals("You Are Here") }) {
            var markerOption = MarkerOptions()
                .position(latLong)
                .title(title)

            googleMap.addMarker(markerOption)
        }
        hideSoftKeyboard()
    }

    private fun enableGPS() {

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
            .findFragmentById(R.id.showAllMaps) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        Log.e("MapsActivity", "### onMapReady")
        if (isPermissionGranted) {
            getDeviceLocation()
            if (ContextCompat.checkSelfPermission(this.applicationContext, FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            )
                this.googleMap.isMyLocationEnabled = true
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
        const val REQUEST_CHECK_SETTINGS = 1
    }
}
