package com.tbox.fotki.view.activities.backup_settings_activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.android.volley.VolleyError
import com.google.android.material.navigation.NavigationView
import com.tbox.fotki.BuildConfig
import com.tbox.fotki.R
import com.tbox.fotki.databinding.ActivityBackupSettingsBinding
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.model.entities.Folder
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.model.web_providers.web_manager.WebManagerInterface
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import com.tbox.fotki.util.destroyBroadcasts
import com.tbox.fotki.util.registerBroadcasts
import com.tbox.fotki.util.sync_files.*
import com.tbox.fotki.util.upload_files.UploadJobService
import com.tbox.fotki.view.activities.log_activity.LogActivity
import com.tbox.fotki.view.activities.NavigationActivity
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import com.tbox.fotki.view.dialogs.AlbumFoldersDialog
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.model.AlbumCollection
import kotlinx.android.synthetic.main.activity_backup_settings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

class BackupSettingsActivity : NavigationActivity(),
    AlbumCollection.AlbumCallbacks,
    WebManagerInterface {

    private lateinit var binding: ActivityBackupSettingsBinding
    lateinit var viewModel: BackupSettingsViewModel

    private val mSyncReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.statusSyncVisibility.value = View.GONE
            if (viewModel.isBackupStarted.value!!) {
                getInfoProgress()
                viewModel.isSyncInProgress.value = true
            } else viewModel.isSyncInProgress.value = false
        }
    }

    //-------------------------------------------------------------------------------------Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LogProvider.writeToFile("Open backup settings activity", this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_backup_settings)
        initToolbar()

        viewModel = ViewModelProviders.of(this).get(BackupSettingsViewModel::class.java)
        binding.viewModel = viewModel
        initBackupProperties(this)

        viewModel.getAlbumsInfo(this,this,savedInstanceState)
        getInfoProgress()

        viewModel.isBackupStarted.value?.let {

            L.print(this,"is  pause - $it")

            viewModel.tvStopText.value =
                resources.getString(
                    if (it) {
                        R.string.pause
                    } else {
                        R.string.resume
                    }
                )
        }
        WebManager.instance.getAccountInfo(this, this)

        viewModel.broadcasts[Constants.FILE_SYNC] = mSyncReceiver
        registerBroadcasts(viewModel.broadcasts)
        initSwitchEnable()

        swCellularBackupPhoto.setOnCheckedChangeListener(viewModel.photoCheckListener)
        swRoaming.setOnCheckedChangeListener(viewModel.roamingCheckListener)
    }

    override fun onStart() {
        super.onStart()
        checkPermissions()
    }

    private fun initSwitchEnable() {
        swEnableBackupPhoto.isChecked = viewModel.backupProperties.isBackgroungEnable.value!!
        swEnableBackupPhoto.setOnCheckedChangeListener { _, isChecked ->
            viewModel.checkChanged(isChecked)
            if (isChecked) {
                openFoldersDialog(true)
            } else {
                stopBackup(this)
                viewModel.tvStopText.value = resources.getString(R.string.pause)
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyBroadcasts(viewModel.broadcasts)
    }
    private fun initToolbar() {
        setSupportActionBar(toolbar)
        attachDrawer(drawer_layout, nav_view, navigationListener)
        tvVersion.text = BuildConfig.VERSION_NAME
        nav_view.menu.getItem(1).isChecked = true
    }

    //-------------------------------------------------------------------------------Navigation-menu
    private val navigationListener = NavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_logout -> {
                showAlertDialog(this)
            }
            R.id.nav_settings -> {
            }
            R.id.nav_albums -> {
                startActivity(Intent(this, FotkiTabActivity::class.java))
            }
        }
        true
    }

    //----------------------------------------------------------------------------------Open-methods
    fun openLogActivity(view: View) {
        startActivity(Intent(this, LogActivity::class.java))
    }
    fun openBackgroundFolder(view: View) {
        val intent = Intent(this, FotkiTabActivity::class.java)
        intent.putExtra(FotkiTabActivity.EXTRA_TYPE, FotkiTabActivity.TYPE_BACKUP)
        startActivity(intent)
    }
    fun onClickOpenFoldersDialog(view: View) {
        openFoldersDialog(false)
    }
    private fun openFoldersDialog(isStarted: Boolean) {
        val albumFoldersDialog = AlbumFoldersDialog()
        val bundle = Bundle()
        bundle.putParcelableArrayList(AlbumFoldersDialog.LIST_FOLDERS_EXTRA, viewModel.albums)
        bundle.putBoolean(AlbumFoldersDialog.IS_STARTED_EXTRA, isStarted)
        albumFoldersDialog.arguments = bundle
        albumFoldersDialog.show(supportFragmentManager, "FoldersDialog")
    }

    //-----------------------------------------------------------------------------Backup-management
    private fun initBackupProperties(context: Context) {
        viewModel.backupProperties = BackupProperties(context)
        viewModel.restoreFromBackupProperties()
    }
    private fun startBackup(context: Context, getInfo: () -> Unit) {
        L.print(this,"MEDIA Backup started!")
        try {
            viewModel.backupProperties.start()
            PreferenceHelper(context).applyPrefs(hashMapOf(UploadJobService.PREF_LAST_UPLOADED_TIME to Date().time / 1000))
            AlarmHelper().getInstance().startAlarms(context)
            getInfo.invoke()
        } catch (e: Exception) {
            LogProvider.writeToFile("Error start backup - $e", context)

        }
    }
    private fun stopBackup(context: Context) {
        viewModel.statusSyncVisibility.value = View.GONE
        viewModel.uploadingProgressVisibility.value = View.GONE
        AlarmHelper().getInstance().stopAlarms(context)
    }

    //------------------------------------------------------------------------------Binded-listeners
    fun pause(view: View?) {
        viewModel.isBackupStarted.value?.let {
            if (it) {
                stopBackup(this)
            } else {
                viewModel.statusSyncVisibility.value = View.VISIBLE
                startBackup(this) {
                    WebManager.instance.getAccountTree(this, this)
                }
            }

            viewModel.tvStopText.value = if (it) {
                resources.getString(R.string.resume)
            } else {
                resources.getString(R.string.pause)
            }
            viewModel.backupProperties.changeStatus(!it)
            //Toast.makeText(activity,"PAUSED FROM - $it",Toast.LENGTH_LONG).show()
        }

        viewModel.isBackupStarted.value = !viewModel.isBackupStarted.value!!
    }
    fun getInfoProgress() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val syncronizator = MediaSyncronizator(this@BackupSettingsActivity)
                val message = Message()
                message.what = syncronizator.getAllSyncFile().size
                message.arg1 = syncronizator.getNotLoadedFiles().size
                message.arg2 = syncronizator.getNotSyncFiles().size
                viewModel.handlerUpdateStatistic.sendMessage(message)
            } catch (e: Exception) {
            }
        }
    }

    //-----------------------------------------------------------------------------------Load-albums

    override fun onAlbumLoad(cursor: Cursor?) {
        viewModel.albums.clear()
        cursor?.let {
            it.moveToFirst()
            do {
                val album = Album.valueOf(cursor)
                viewModel.albums.add(album)
            } while (it.moveToNext())
            reloadAlbums(false)
        }
    }
    override fun onAlbumReset() {}
    fun startWithAlbums() {
        reloadAlbums(true)
    }
    fun reloadAlbums(isRestart: Boolean) {
        L.print(this,"TAG reloadAlbums ${viewModel.backupProperties.folderList.size}")
        viewModel.backupProperties.invalidate()

        L.print(this,"MEDIA reload before showSelected")
        val selectedStr =
            showSelectedAlbums(viewModel.backupProperties.folderList, viewModel.albums)
        if (selectedStr.isEmpty()) {
            viewModel.backupFolderMessage.value = resources.getString(R.string.none)
        } else {
            viewModel.backupFolderMessage.value = selectedStr
        }

        if (isRestart) {
            viewModel.isBackupStarted.value = true
            viewModel.tvStopText.value = resources.getString(R.string.pause)
            viewModel.statusSyncVisibility.value = View.VISIBLE
            MediaSyncronizator(this).readAndSync()
            startBackup(this) {
                WebManager.instance.getAccountTree(this, this)
            }
        }
    }
    private fun showSelectedAlbums(folderList: HashSet<String>, albums: ArrayList<Album>): String {
        var res = ""
        for (album in albums) {
            if (folderList.contains(album.id)) {
                res += ", " + album.getDisplayName(this)
            }
        }
        if (res.isNotEmpty()) res = res.substring(2)
        return res
    }

    //-----------------------------------------------------------------------------------Web-manager
    private fun checkIfBackupCreated(response: JSONObject) {
        if (response.getInt(Constants.OK) == 1) {
            val privateFolder = response.getJSONObject(Constants.DATA)
                .getJSONObject(Constants.ACCOUNT_TREE)
                .getJSONObject(Constants.PRIVATE_FOLDER)

            val foldersArray = privateFolder.getJSONArray(Constants.FOLDERS)
            var id = 0L
            for (i in 0 until foldersArray.length()) {
                val item = foldersArray.getJSONObject(i)
                if (Folder.BACKUP_FOLDER_NAME == item.getString(Constants.FOLDER_NAME)) {
                    id = item.getLong(Constants.FOLDER_ID_ENC)
                }
            }
            L.print(this,"MEDIA backup folder id - $id folder name - ${Folder.BACKUP_FOLDER_NAME}")

            if (id == 0L) {
                WebManager.instance.createFolder(
                    this, this,
                    privateFolder.getLong(Constants.FOLDER_ID_ENC), Folder.BACKUP_FOLDER_NAME
                )
            } else {
                BackupProperties.applyFolder(this, id)
            }
        }
    }
    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
        if (BuildConfig.DEBUG) Log.d(FotkiTabActivity.TAG, response.toString())
        when (apiRequestType) {
            ApiRequestType.GET_ACCOUNT_INFO_API -> viewModel.getAccountInfoFromResponse(response)
            ApiRequestType.GET_ACCOUNT_TREE_API -> checkIfBackupCreated(response)
            ApiRequestType.CREATE_FOLDER_API -> {
                L.print(this,"MEDIA CREATED ! $response")
                if (response.getInt(Constants.OK) == 1) {
                    BackupProperties.applyFolder(
                        this,
                        response.getJSONObject(Constants.DATA).getLong(
                            Constants.FOLDER_ID
                        )
                    )
                }
            }
            else -> {
            }
        }
    }
    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {}
    override fun sendNetworkFailure(isInterNetAvailableFlag: Boolean, apiRequestType: ApiRequestType
    ) {
    }
}