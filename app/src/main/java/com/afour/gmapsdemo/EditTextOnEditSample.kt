package com.afour.gmapsdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_edit_text_on_edit_sample.*

class EditTextOnEditSample : AppCompatActivity() {

    private lateinit var editText : EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_text_on_edit_sample)

        editText = edit_sample_text

        editText.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                Log.e("EditTextOnEditSample", "setOnEditorActionListener")
                true
            } else {
                false
            }
        }

    }
}
