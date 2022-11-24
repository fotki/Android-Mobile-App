package com.tbox.fotki.refactoring.screens.slider

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.volley.VolleyError
import com.simplemobiletools.commons.extensions.copyToClipboard
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.model.entities.ParcelableAlbum
import com.tbox.fotki.model.entities.Session
import com.tbox.fotki.model.web_providers.AlbumProvider
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.model.web_providers.web_manager.WebManagerInterface
import com.tbox.fotki.refactoring.SessionExpiredBroadcastReceiver
import com.tbox.fotki.refactoring.SharingErrorBroadcastReceiver
import com.tbox.fotki.refactoring.SharingFileBroadcastReceiver
import com.tbox.fotki.refactoring.SharingProgressBroadcastReceiver
import com.tbox.fotki.refactoring.api.toast
import com.tbox.fotki.refactoring.service.PhotoUploader
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.DownloadTask
import com.tbox.fotki.util.L
import com.tbox.fotki.util.Utility
import com.tbox.fotki.util.sync_files.PreferenceHelper
import com.tbox.fotki.view.adapters.ImageSliderAdapter
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class SliderViewModel : ViewModel() {

    var mAlbumCount: Int = 0
    val mAlbum: MutableLiveData<ParcelableAlbum> = MutableLiveData()
    var mSelectedPosition: Int = 0
    var mCurrentPostion: Int = 0
    private var mPageCount: Int = 0
    var mAlbumItemCount: Int = 0

    var mIsSharedButtonError = false
    var isAllowCompress = false

    var mApiBeingCalledFlag = false
    private var mApiBeingCalledThreshold = false

    lateinit var mAdapter: ImageSliderAdapter
    lateinit var broadcasts: HashMap<String, BroadcastReceiver>

    lateinit var albumProvider: AlbumProvider
    private var downloadTask:DownloadTask? = null

    fun initBroadcasts(
        activity: Activity,
        progressBar: ProgressBar,
        rlProgressBar: RelativeLayout
    ) {
        broadcasts = hashMapOf(
            Constants.SESSION_EXPIRED to SessionExpiredBroadcastReceiver(),
            Constants.SHARING_FILE_PROGRESS to SharingProgressBroadcastReceiver(progressBar),
            Constants.SHARING_FILE_DOWNLOADED_SHARE to SharingFileBroadcastReceiver(
                activity,
                rlProgressBar
            ),
            Constants.SHARING_FILE_ERROR to SharingErrorBroadcastReceiver(rlProgressBar)
        )
    }

    fun getCurrentItem() = mAlbum.value!!.mitem[mCurrentPostion]

    private var isShowSharing = false

    fun startSharing(context: Activity, rlProgressBarLayout: RelativeLayout) {
        val item = mAlbum.value!!.mitem[mCurrentPostion]
        Log.d("shareError","SliderViemodel startSharing Error")

        L.print(this,"Start sharing!!!! $item")

        if (mCurrentPostion >= mAlbum.value!!.mitem.size) return

        //val item = mAlbum.value!!.mitem[mCurrentPostion - 1]
        /*val url = if (item.mIsVideo) {
            item.mOriginalUrl
        } else {
            item.mViewUrl
        }*/
        val url = if(item.mOriginalUrl == "") item.mViewUrl else item.mOriginalUrl
        downloadTask = DownloadTask.getInstance()
        downloadTask?.let { downloadTask ->
            downloadTask.mContext = context
            downloadTask.url = url
            if (item.mTitle.isNotEmpty()&&isNameAllowed(item.mTitle)) {
                downloadTask.originalUrl = item.mTitle
            } else {
                downloadTask.originalUrl = ""
            }
            downloadTask.mFolder = "FotkiShare"
            downloadTask.isCompression = isAllowCompress
            downloadTask.isCompressionFailed = false
            downloadTask.finishAction = DownloadTask.ActionCall { type ->
                downloadTask.isCompressionFailed = false
                downloadTask.isCompression = false
                if(!isShowSharing){
                    isShowSharing = true
                    rlProgressBarLayout.visibility = View.GONE
                    PhotoUploader.shareFile(context, type)
                }
            }
            L.print(this,"Start sharing downloadTask - $downloadTask")
            isShowSharing = false
            downloadTask.startDownloadTask()
            rlProgressBarLayout.visibility = View.VISIBLE

        }
    }

    private fun isNameAllowed(mTitle: String): Boolean {
        if (mTitle.length>100) return false
        val allowSymbols = "_ ()"
        var res = true
        mTitle.forEach { ch ->
            if (!Character.isAlphabetic(ch.toInt())
                &&!Character.isDigit(ch.toInt())
                &&!allowSymbols.contains(ch)){
                    res = false
            }
        }
        return res
    }


    fun tryToDelete(context: Context, deletedSuccess: () -> Unit) {
        if (mCurrentPostion >= mAlbum.value!!.mitem.size) return

        L.print(
            this, "try to delete - session id - " +
                    "${Session.getInstance(context).mSessionId} " +
                    "album id - ${mAlbum.value!!.mAlbumIdEnc} " +
                    "image id - ${mAlbum.value!!.mitem[mCurrentPostion].mId} " +
                    "selected position - $mSelectedPosition " +
                    "current position - $mCurrentPostion"
        )
        Utility.instance.showConfirmDialog(context, "Do you really want to delete this item?") {
            WebManager.instance.removeImage(
                context,
                object :
                    WebManagerInterface {
                    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
                        L.print(this, "Deleted!!!")
                        PreferenceHelper(context).applyPrefs(hashMapOf("deleted_item" to true))
                        TimeUnit.SECONDS.sleep(1)
                        deletedSuccess()
                    }

                    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {
                        L.print(this, "Failure!")
                    }

                    override fun sendNetworkFailure(
                        isInterNetAvailableFlag: Boolean,
                        apiRequestType: ApiRequestType
                    ) {
                        L.print(this, "Error!")
                    }
                },
                mAlbum.value!!.mAlbumIdEnc,
                mAlbum.value!!.mitem[mCurrentPostion].mId
            )
        }
    }

    fun initFromIntent(data: Intent) {
        mAlbum.value = data.extras?.getParcelable<ParcelableAlbum>(Constants.PARCEABLE_ALBUM)
        mAlbumCount = data.getIntExtra(Constants.ALBUM_ITEM_COUNT, 0)
        mSelectedPosition = data.getIntExtra(Constants.SELECTED_POSITION, 0)
        mPageCount = data.getIntExtra(Constants.REQUEST_PAGE, 0)
        mPageCount--
        mAlbumItemCount = data.getIntExtra(Constants.ITEM_COUNT, 0)
    }

    fun registerAdapter(context: Context) {
        mAdapter = ImageSliderAdapter(context, mAlbum.value!!.mitem.size, mAlbum.value!!)
    }

    fun callPaginationIfNeeded(context: Context) {
        mAlbum.value.let { album ->
            albumProvider = AlbumProvider(context, album!!, mPageCount)

            val pos100 = mCurrentPostion % 100
            if (pos100 in 96..99) {
                val pageNeeded = ceil(mAlbumItemCount.toFloat() / 100.0)
                val mrange = ceil(mCurrentPostion.toFloat() / 100.0)
                if (mrange >= mPageCount) {
                    if (mPageCount < pageNeeded) {
                        if (!mApiBeingCalledFlag && !mApiBeingCalledThreshold) {
                            mApiBeingCalledFlag = true
                            mApiBeingCalledThreshold = true
                            albumProvider.start()
                        }
                    }
                }
            } else {
                mApiBeingCalledThreshold = false
            }
        }
    }

    fun saveImage(appCompatActivity: AppCompatActivity, rlProgressBarLayout: RelativeLayout) {
        /*val chooser =
            StorageChooser.Builder() // Specify context of the dialog
                .withActivity(appCompatActivity)
                .withFragmentManager(appCompatActivity.fragmentManager)
                .withMemoryBar(true)
                .allowCustomPath(true) // Define the mode as the FILE CHOOSER
                .setType(StorageChooser.DIRECTORY_CHOOSER)
                .build()
        chooser.setOnSelectListener { path -> // e.g /storage/emulated/0/Documents/file.txt
            L.print(this, "Path - $path")*/
        val item = mAlbum.value!!.mitem[mCurrentPostion]
        L.print(this,"file for loading - $item")
        /*val url = if (item.mIsVideo) {
            item.mOriginalUrl
        } else {
            item.mViewUrl
        }*/
        val url = if(item.mOriginalUrl == "") item.mViewUrl else item.mOriginalUrl
        downloadTask = DownloadTask.getInstance()
        downloadTask?.let {downloadTask ->
            downloadTask.mContext = appCompatActivity
            downloadTask.url = url
            if (item.mTitle.isNotEmpty()) {
                downloadTask.originalUrl = item.mTitle
            }
            downloadTask.isCompression = isAllowCompress
            downloadTask.isCompressionFailed = false
            downloadTask.mFolder = "Fotki"
            downloadTask.startDownloadTask()
            rlProgressBarLayout.visibility = View.VISIBLE

        }

        /*}
        chooser.show()*/
    }

    fun stopSharing() {
        L.print(this,"STOP!!!")
        downloadTask?.stopDownloadTask()
    }

    fun copyUrl(activity: Activity) {
        val item = mAlbum.value!!.mitem[mCurrentPostion]
        activity.copyToClipboard(item.mShortUrl)
        //activity.toast("Url copied to clipboard")
    }
}