package com.tbox.fotki.refactoring.screens

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.tbox.fotki.R
import com.tbox.fotki.refactoring.screens.auth.LoginFragment
import com.tbox.fotki.refactoring.screens.upload_files.UploadFilesFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction()
            .add(
                R.id.fragment_container_view,
                UploadFilesFragment()
            ).commit()
    }

    override fun onStop() {
        //supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        super.onStop()
    }
}
