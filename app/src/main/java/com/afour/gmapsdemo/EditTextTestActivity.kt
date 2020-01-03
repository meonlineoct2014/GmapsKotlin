package com.afour.gmapsdemo

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_edit_text_test.*

class EditTextTestActivity : AppCompatActivity() {

    private lateinit var testEditText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_text_test)
        testEditText = editText

        testEditText.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            Log.e("setOnEditor", "setOnEditorActionListener")
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                return@OnEditorActionListener true
            }
            false
        })
    }
}
