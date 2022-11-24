package com.tbox.fotki.view.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.navigation.NavigationView
import com.tbox.fotki.util.Constants
import com.tbox.fotki.R
import com.tbox.fotki.model.web_providers.AccountProvider
import com.tbox.fotki.model.entities.Session
import com.tbox.fotki.util.L
import com.tbox.fotki.util.sync_files.BackupProperties
import com.tbox.fotki.view.activities.backup_settings_activity.BackupSettingsActivity
import com.tbox.fotki.view.activities.general.BaseActivity
import org.json.JSONException
import org.json.JSONObject

open class NavigationActivity : BaseActivity() {
    private lateinit var profileName: TextView
    private lateinit var avataImage: SimpleDraweeView
    private lateinit var drawerLayout: DrawerLayout

    fun attachDrawer(
        drawerLayout: DrawerLayout, navView: NavigationView,
        navigationListener: NavigationView.OnNavigationItemSelectedListener
    ) {
        this.drawerLayout = drawerLayout
        val mDrawerToggle = object : ActionBarDrawerToggle(
            this, drawerLayout,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        ) {}
        drawerLayout.addDrawerListener(mDrawerToggle)
        mDrawerToggle.syncState()
        val header = navView.inflateHeaderView(R.layout.nav_header_fotki)

        profileName = header.findViewById(R.id.profile_name)
        avataImage = header.findViewById(R.id.avatar_image)

        navView.setNavigationItemSelectedListener(navigationListener)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.menu_hamburger)

        getAccountInfo()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                L.print(this,"TAG OPEN!")
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun getAccountInfo() {
        AccountProvider(this).res.observe(this,
            Observer {
                it?.let { getAccountInfoFromResponse(it) }
            }
        )
    }

    private fun getAccountInfoFromResponse(response: JSONObject) {
        try {
            L.print(this,"TAG response - $response")
            val accInfo = response.getJSONObject(Constants.DATA)
                .getJSONObject(Constants.ACCOUNT_INFO)
            profileName.text = accInfo.getString(Constants.DISP_NAME)
            avataImage.setImageURI(Uri.parse(
                accInfo.getJSONObject(Constants.AVATAR).getString(
                    Constants.URL)))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun showAlertDialog(context: Context) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.myDialog))
            .setMessage(R.string.text_logout)
            .setNegativeButton(R.string.text_no) { _, _ -> }
            .setPositiveButton(R.string.text_yes) { _, _ ->
                onLogoutConfirmed()
            }
            .show()
    }

    private fun onLogoutConfirmed() {
        val session = Session.getInstance(this)
        session.removeSessionInfo(this)
        val backupProperties = BackupProperties(this)
        backupProperties.stop()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    fun switchToSettingsFragment() {
        startActivity(Intent(this, BackupSettingsActivity::class.java))
    }
}