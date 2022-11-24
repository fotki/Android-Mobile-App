package com.tbox.fotki.view.activities.fotki_tab_activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.tbox.fotki.R
import com.tbox.fotki.model.web_providers.FolderProvider
import com.tbox.fotki.model.entities.Folder
import com.tbox.fotki.model.entities.FragmentType
import com.tbox.fotki.model.entities.Session
import com.tbox.fotki.util.Utility
import com.tbox.fotki.view.fragments.fragment_navigation.FragNavController
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import com.tbox.fotki.util.sync_files.PreferenceHelper
import com.tbox.fotki.view.activities.LoginActivity
import com.tbox.fotki.view_model.ProgressNavigationViewModel

class FotkiTabActivityViewModel:
    ProgressNavigationViewModel() {

    val broadcasts: HashMap<String, BroadcastReceiver>

    private var mUtility = Utility.instance
    var mFragmentTypeResult: FragmentType? = null

    var mPublicFolder: Folder? =
        Folder()
    var mPrivateFolder: Folder? =
        Folder()

    val bottomBarTag = MutableLiveData<String>()
    val bottomBarPosition = MutableLiveData<Int>()
    lateinit var fProvider:FolderProvider

    private val mSaveAlbumFragmentPosition = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (bottomBarTag.value == Constants.PUBLIC_TAB) {
                PreferenceHelper(context!!).applyPrefs(
                    hashMapOf(Constants.SWITCH_TAB_STATE to bottomBarTag.value!!)
                )
            } else if (bottomBarTag.value == Constants.PRIVATE_TAB) {
                PreferenceHelper(context!!).applyPrefs(
                    hashMapOf(Constants.SWITCH_TAB_STATE to bottomBarTag.value!!)
                )
            }
        }

    }
    private val mSwitchTab = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
           val isNewUpload = intent.getBooleanExtra(Constants.NEW_UPLOAD, false)
            L.print(this,"Switch tab with $isNewUpload")
            if (isNewUpload) {
                L.print(this,"current bottomBar tag - ${bottomBarTag.value}")
                if (bottomBarTag.value == Constants.PUBLIC_TAB ||
                    bottomBarTag.value == Constants.PRIVATE_TAB
                ) {
                    PreferenceHelper(context!!).applyPrefs(
                        hashMapOf(Constants.SWITCH_TAB_STATE to bottomBarTag.value!!)
                    )
                }
                bottomBarPosition.value = FragNavController.TAB3
                bottomBarTag.value = Constants.UPLOAD_TAB
                //(activity as FotkiTabActivity).uplo
            } else {
                val tag = PreferenceHelper(context!!).getString(Constants.SWITCH_TAB_STATE)
                if (tag == Constants.PUBLIC_TAB) {
                    bottomBarPosition.value = FragNavController.TAB1
                } else {
                    bottomBarPosition.value = FragNavController.TAB2
                }
                bottomBarTag.value = tag
                //(activity as FragmentsManagerActivity).popFragment()
            }
        }
    }
    private val mSwitchTabDuringUploading = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isUploading = intent.getBooleanExtra(
                Constants.SWITCHSCREEN_WHILE_UPLOADING,
                false
            )
            if (isUploading) {
                bottomBarPosition.value = FragNavController.TAB3
            }
        }
    }
    private val mSessionExpired = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val session = Session.getInstance(context)
            session.removeSessionInfo(context)
//            LoginActivity.launchAffinity(context)

            val intent=Intent(context,
                LoginActivity::class.java)
            intent.flags=Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

        }
    }
    private val mRetryApiCall = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            mFragmentTypeResult = intent.getSerializableExtra(
                Constants.FRAGMENTTYPEENUM
            ) as FragmentType
            //getAccountTree(context, successLoadFolder)
        }
    }
    private val mUploadingStopped = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            uploadingComplete()
        }
    }
    private val mSyncReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (intent.hasExtra(Constants.SYNC_STARTED)){
                L.print(this, "Sync received - "+intent.getBooleanExtra(Constants.SYNC_STARTED, false))
                isSyncInProgress.value = intent.getBooleanExtra(Constants.SYNC_STARTED, false)
            } else
                isSyncInProgress.value = false
        }
    }

    init {
        broadcasts = hashMapOf(
            Constants.UPLOAD_STARTED to mUploadingStarted,
            Constants.UPDATE_CURRENT_PROGRESS to mMessageReceiver,
            Constants.ALL_UPLOADS_DONE to mAllUploadDone,
            Constants.FILE_SYNC to mSyncReceiver,
            Constants.UPLOADINGSTOPPED to mUploadingStopped,
            Constants.RETRY_API_CALL to mRetryApiCall,
            Constants.SESSION_EXPIRED to mSessionExpired,
            Constants.UPLOADING_IN_PROGRESS to mSwitchTabDuringUploading,
            Constants.PREF_ALBUM_POSITION to mSaveAlbumFragmentPosition,
            Constants.SWITCH_TAB to mSwitchTab
        )
    }

    fun getAccountTree(context: Context) {

        /*val history = PreferenceHelper(context).getString(STORED_ALBUMS)
        L.print(this, "History2 - $history")

        if (history.isEmpty())*/
        mUtility.showProgressDialog(context, R.string.text_loading_your_album)

        //fProvider = if (history.isEmpty()){
        fProvider = FolderProvider(context)
        /*} else {
            FolderProvider(context,history)
        }*/
    }

    fun updateFolder(res: Folder, afterLoading: () -> Unit){
        L.print(this, "Changed public folder - $res")
        if (res == null)
            networkFailure()
        else {
            mPublicFolder = res
            mPrivateFolder = fProvider.privateFolder.value
            mUtility.dismissProgressDialog()
            afterLoading.invoke()
        }
    }

    private fun networkFailure() {
        mUtility.dismissProgressDialog()
        bottomBarPosition.value = FragNavController.TAB1

        if (mFragmentTypeResult != null) {
            if (mFragmentTypeResult === FragmentType.ROOT_PRIVATE) {
                mFragmentTypeResult = null
                bottomBarPosition.value = FragNavController.TAB2
            } else {
                mFragmentTypeResult = null
                bottomBarPosition.value = FragNavController.TAB1
            }
        }
    }

    companion object {
        const val STORED_ALBUMS = "stored_albums"
    }
}