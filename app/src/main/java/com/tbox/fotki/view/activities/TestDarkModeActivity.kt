package com.tbox.fotki.view.activities

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.tbox.fotki.R
import com.tbox.fotki.view.view.ThemeUtils

class TestDarkModeActivity : AppCompatActivity(), View.OnClickListener {

    var numb = 1
    val themeUtils = ThemeUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
/*
        if (themeUtils.isDarkTheme(this)) {
            setTheme(R.style.darkTheme)
            //  layout2.setBackgroundColor(Color.GRAY)


        } else {
            setTheme(R.style.lighTheme)
            // layout2.setBackgroundColor(Color.YELLOW)
            //  recreate()
        }*/



        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_test_dark_mode)
        initView()

        // val intent = Intent(this, NextActivity::class.java)
// To pass any data to next activity
        // intent.putExtra("keyIdentifier", value)
// start your next activity
        //     startActivity(intent)

    }

    private fun initView() {
        val button = findViewById<Button>(R.id.btnTestDarkModeActivity)
        button.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        val layout2 = findViewById<LinearLayout>(R.id.leaytMainTestDarkMode)
/*

        if (numb == 1) {
            //  layout2.setBackgroundColor(Color.GRAY)
            super.setTheme(R.style.darkTheme)
            // recreate()

            numb = 2
        } else if (numb == 2) {
            // layout2.setBackgroundColor(Color.GREEN)
           super.setTheme(R.style.lighTheme)
            //  recreate()
            numb = 1
        }
        Toast.makeText(
            applicationContext, "night mode - " + themeUtils.isDarkTheme(this), Toast.LENGTH_SHORT
        ).show()
*/


      /*  if (themeUtils.isDarkTheme(this)) {
            setTheme(R.style.darkTheme)
        } else {
            setTheme(R.style.lighTheme)
        }*/

    }


}
