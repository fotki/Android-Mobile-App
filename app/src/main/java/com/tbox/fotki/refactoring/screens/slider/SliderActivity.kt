package com.tbox.fotki.refactoring.screens.slider

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.ParcelableItem
import com.tbox.fotki.refactoring.general.PermissionActivity
import com.tbox.fotki.refactoring.screens.auth.LoginFragment
import com.tbox.fotki.util.L
import com.tbox.fotki.util.sync_files.PreferenceHelper


class SliderActivity : PermissionActivity() {
    val sliderFragment = SliderFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PreferenceHelper(this).applyPrefs(
            hashMapOf("mSelectedPosition" to 0))

        supportFragmentManager.beginTransaction()
            .add(
                R.id.fragment_container_view,
                sliderFragment
            ).commit()
    }

    override fun onStop() {
        testPermission()
        super.onStop()
    }

    fun singleTapDone(item: ParcelableItem) {
       sliderFragment.singleTapDone(item)
    }


}