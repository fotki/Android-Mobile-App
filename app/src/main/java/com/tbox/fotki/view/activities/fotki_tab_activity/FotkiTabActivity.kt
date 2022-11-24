package com.tbox.fotki.view.activities.fotki_tab_activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.navigation.NavigationView
import com.roughike.bottombar.BottomBarTab
import com.tbox.fotki.util.Constants
import com.tbox.fotki.BuildConfig
import com.tbox.fotki.Config
import com.tbox.fotki.R
import com.tbox.fotki.databinding.ActivityMainTabBinding
import com.tbox.fotki.model.entities.Folder
import com.tbox.fotki.model.entities.FragmentType
import com.tbox.fotki.model.web_providers.FolderProvider
import com.tbox.fotki.refactoring.screens.MainActivity
import com.tbox.fotki.util.Utility
import com.tbox.fotki.util.L
import com.tbox.fotki.util.destroyBroadcasts
import com.tbox.fotki.util.registerBroadcasts
import com.tbox.fotki.util.sync_files.BackupProperties
import com.tbox.fotki.util.sync_files.PreferenceHelper
import com.tbox.fotki.util.upload_files.UploadThreadManager
import com.tbox.fotki.view.activities.backup_settings_activity.BackupSettingsActivity
import com.tbox.fotki.view.activities.general.FragmentsManagerActivity
import kotlinx.android.synthetic.main.activity_main_tab.*
import kotlinx.android.synthetic.main.activity_main_tab.drawer_layout
import kotlinx.android.synthetic.main.activity_main_tab.nav_view
import kotlinx.android.synthetic.main.activity_main_tab.toolbar
import kotlinx.android.synthetic.main.activity_main_tab.tvVersion

@Suppress("NAME_SHADOWING", "DEPRECATION", "VARIABLE_WITH_REDUNDANT_INITIALIZER")
class FotkiTabActivity : FragmentsManagerActivity() {

    private lateinit var binding: ActivityMainTabBinding
    lateinit var viewModel: FotkiTabActivityViewModel
    //lateinit var uploadThreadManager: UploadThreadManager
    lateinit var uploadThreadManager: UploadThreadManager

    lateinit var utility: Utility


    //-------------------------------------------------------------------------------------Lifecycle
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Config.IS_NEW_VERSION){
            startActivity(Intent(this,MainActivity::class.java))
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_tab)
        viewModel = ViewModelProviders.of(this).get(FotkiTabActivityViewModel::class.java)
        binding.viewModel = viewModel
        utility = Utility.instance

        setupUploadManager()
        registerUploadFragment()
        registerBroadcasts(viewModel.broadcasts)

        initToolbar()

        bottomBarLayout = bottomBar
        setBottomBar(savedInstanceState, viewModel.bottomBarTag, viewModel.bottomBarPosition)
        loadFoldersFromAccount()

        viewModel.bottomBarTag.observe(this, Observer { it?.let{ bottomBar.tag = it } })
        viewModel.bottomBarPosition.observe(this,
            Observer { it?.let { bottomBar.selectTabAtPosition(it) }  })

    }


    override fun onStart() {
        super.onStart()
        checkPermissions()
        //setupUploadManager()
        if (intent.getIntExtra(
                EXTRA_TYPE,
                TYPE_USUAL
            )== TYPE_BACKUP
        ){
            bottomBarLayout.visibility = GONE
        }
        if (!PreferenceHelper(this).getBoolean(Constants.SYNC_STARTED)){
            viewModel.isSyncInProgress.value = false
        }
        if (!PreferenceHelper(this).getBoolean(BackupProperties.PREFERENCE_IS_BACKUP_STARTED)&&
            viewModel.uploadingProgressVisibility.value==VISIBLE){
                viewModel.uploadingProgressVisibility.value = GONE
        }
    }

    override fun onDestroy() {
        uploadThreadManager.destroy()
        destroyBroadcasts(viewModel.broadcasts)
        super.onDestroy()
    }
    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (supportActionBar != null) {
                    if (supportActionBar!!.title != null && (
                                supportActionBar!!.title == getString(R.string.tab_type_public)
                                || supportActionBar!!.title == getString(R.string.tab_type_private)
                                        || supportActionBar!!.title == getString(R.string.tab_type_upload))
                    ) {
                        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                            drawer_layout.closeDrawer(GravityCompat.START)
                        } else {
                            drawer_layout.openDrawer(GravityCompat.START)
                        }
                    } else {
                        if (!mNavController!!.isRootFragment) {
                            mNavController!!.popFragment()
                        }
                    }
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setupUploadManager() {
        uploadThreadManager = UploadThreadManager(this)
        uploadThreadManager.testHasUploads{
            bottomBar.selectTabAtPosition(UPLOAD_TAB)
            mNavController!!.switchTab(UPLOAD_TAB)
            bottomBar.findViewById<BottomBarTab>(R.id.bb_menu_upload).tag = Constants.UPLOAD_TAB
        }
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)

        pbUploadSync.setOnClickListener {
            this.startActivity(Intent(this, BackupSettingsActivity::class.java))
        }

        attachDrawer(drawer_layout, nav_view, NavigationView.OnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_logout -> showAlertDialog(this@FotkiTabActivity)
                R.id.nav_settings -> switchToSettingsFragment()
                R.id.nav_albums -> {
                    if (intent.getIntExtra(
                            EXTRA_TYPE,
                            TYPE_USUAL
                        )== TYPE_BACKUP){
                        startActivity(Intent(this,
                            FotkiTabActivity::class.java))
                        finish()
                    }
                    else if( supportActionBar!!.title == getString(R.string.tab_type_upload)){
                        finish()
                    startActivity(Intent(this,
                            FotkiTabActivity::class.java))
                    }
                    else {
                        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                            drawer_layout.closeDrawer(GravityCompat.START)
                        }
                    }
//                    if (intent.getIntExtra(
//                            EXTRA_TYPE,
//                            TYPE_USUAL
//                        )== TYPE_BACKUP
//                    ){
//                        startActivity(Intent(this,
//                            FotkiTabActivity::class.java))
//                        finish()
//                    }
//                    else {
//                        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
//                            drawer_layout.closeDrawer(GravityCompat.START)
//                        }
//                    }
                }
            }
            true
        })
        tvVersion.text = BuildConfig.VERSION_NAME
        nav_view.menu.getItem(0).isChecked = true
    }

    fun loadFoldersFromAccount() {
        utility.showProgressDialog(this,R.string.text_loading_your_album)
        if (intent.getIntExtra(
                EXTRA_TYPE,
                TYPE_USUAL
            )== TYPE_BACKUP
        ){
            L.print(this,"TRY WITH BACKGROUND!!!")
            val provider = FolderProvider(this)
            provider.loadPrivate()
            provider.backupFolder.observe(this, Observer { res ->
                L.print(this,"res - $res")
                initilizeFotkiTabFragments(null,
                    null,
                    res, FragmentType.BACKUP_UPLOAD)
                utility.dismissProgressDialog()
            })
        } else {
            val provider = FolderProvider(this)
            provider.loadPublic()
            provider.publicFolder.observe(this, Observer { res ->
                L.print(this,"res - $res")
                initilizeFotkiTabFragments(res,
                    null,
                    null, FragmentType.ROOT_PUBLIC)
                utility.dismissProgressDialog()
            })
        }
    }

    fun hideToolbar() {
        supportActionBar!!.hide()
        topBar.visibility = View.GONE
    }

    fun showToolbar() {
        supportActionBar!!.show()
        topBar.visibility = View.VISIBLE
    }


    //-------------------------------------------------------------------------------------Companion
    interface FotkiTabInterface {
        fun sendFolderResponse(mFolder: Folder?)
    }
    interface FotkiApiCallingFailInterface {
        fun sendfailure(fragmentType: FragmentType)
    }

    companion object {
        internal var TAG = FotkiTabActivity::class.java.name
        val EXTRA_TYPE = "type"
        val TYPE_USUAL = 0
        val TYPE_BACKUP = 1
    }
}