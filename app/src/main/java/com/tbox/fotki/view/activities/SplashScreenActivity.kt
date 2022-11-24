package com.tbox.fotki.view.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.Session
import com.tbox.fotki.util.L
import com.tbox.fotki.util.sync_files.PreferenceHelper
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivityViewModel

class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        Log.d("plhcheck", "starting splash" )
        L.print(this,"Start splash screen")
        supportActionBar?.hide()
        val history = PreferenceHelper(this).getString(FotkiTabActivityViewModel.STORED_ALBUMS)
        L.print(this, "History1 - $history")

       /* if (history.isEmpty()){
            val SPLASH_DISPLAY_LENGTH = 2000
            Handler().postDelayed({
                startLoginActivity()
            }, SPLASH_DISPLAY_LENGTH.toLong())
        } else{
            startLoginActivity()
        }*/
        checkIfSession()
    }

    private fun startLoginActivity() {
        L.print(this,"Start login activity")
        Log.d("plhcheck", "starting login" )
        startActivity(Intent(this, LoginActivity::class.java))
        this@SplashScreenActivity.finish()
    }

    private fun checkIfSession() {
        //mSession = Session.getInstance(this)
        if (Session.getInstance(this).mIsSessionId) {
            Log.d("plhcheck", "starting tbbb" )
            //if (BuildConfig.DEBUG) Log.v(LoginActivity.TAG, R.string.text_log_user_already_login.toString() + "")
            startActivity(Intent(this,
                FotkiTabActivity::class.java))
            finish()
        } else {
            startLoginActivity()
        }
    }

}
