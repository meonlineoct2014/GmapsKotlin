package com.afour.gmapsdemo

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import kotlinx.android.synthetic.main.activity_edit_text_test.*
import java.util.*

class EditTextTestActivity : AppCompatActivity() {

    private lateinit var testEditText: EditText
    private lateinit var placesClient: PlacesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_text_test)
        setUpAutoCompleteFragment()
    }

    private fun setUpAutoCompleteFragment() {
        Places.initialize(applicationContext, "AIzaSyCtWsDUTFC72cDdrfsqCPAtyEmstBgRW9w")
        placesClient = Places.createClient(this@EditTextTestActivity)

        val placeSelectionHandler = object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.e("placeSelectionHandler", "Place: " + place.name + ", "
                            + place.id)
            }

            override fun onError(error: Status) {
                Log.e("placeSelectionHandler", error.statusMessage)
            }

        }

        val autoCompleteFragment = supportFragmentManager
            .findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autoCompleteFragment.setHint("Search for city or place here...")
        autoCompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME))
        autoCompleteFragment.setOnPlaceSelectedListener(placeSelectionHandler)
    }
}
