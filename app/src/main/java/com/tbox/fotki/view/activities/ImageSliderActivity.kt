package com.tbox.fotki.view.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import com.android.volley.*
import com.tbox.fotki.R
import com.tbox.fotki.view.adapters.ImageSliderAdapter
import com.tbox.fotki.view.adapters.SizeReductionDialogAdapter
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.model.entities.ApiRequestType.GET_ALBUM_CONTENT
import com.tbox.fotki.model.entities.ParcelableAlbum
import com.tbox.fotki.model.entities.ParcelableItem
import com.tbox.fotki.model.entities.Session
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.refactoring.auth_providers.SessionManager
import com.tbox.fotki.util.*
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import com.tbox.fotki.view.activities.general.BaseActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_image_slider.*
import org.json.JSONException
import org.json.JSONObject
import java.io.File

@Suppress("NAME_SHADOWING", "VARIABLE_WITH_REDUNDANT_INITIALIZER")
class ImageSliderActivity : BaseActivity(), View.OnClickListener {
    private var mAlbumCount: Int = 0
    private var mAlbum: ParcelableAlbum? = null
    private var mSelectedPosition: Int = 0
    private var mCurrentPostion: Int = 0
    private var mSlideFlag = true
    private var mPageCount: Int = 0
    private var mAlbumItemCount: Int = 0
    private var mUtility = Utility.instance
    private var mApiBeingCalledFlag = false
    private var mApiBeingCalledThreshold = false
    private lateinit var mHandler: Handler
    private var mIsSharedButtonError = false
    private var isAllowCompress = false
    private var listChoice = ArrayList<String>()
    private var mAdapter: ImageSliderAdapter? = null
    private var mWakeLock: PowerManager.WakeLock? = null
    private lateinit var mDialog: Dialog
    private lateinit var choiceAlert: ListView


    //-------------------------------------------------------------------------------------Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_slider)
        supportActionBar?.hide()
        populateVariableWithIntent()
        registerViews()
        registerListners()
        registerLocalBroadCast()
        callPaginationIfNeeded()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btnShare) {
            val rxPermissions = RxPermissions(this)
            rxPermissions.request(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
                .subscribe(object : Observer<Boolean> {
                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onNext(t: Boolean) {
                        if (t) {
                            when (v.id) {
                                R.id.btnShare -> if (mUtility.isConnectingToInternet(
                                        this@ImageSliderActivity
                                    )
                                ) {
                                    mIsSharedButtonError = false
                                    //todo here show dialog screen and on its listners inflate compression login...
                                    showChoiceDialog()
                                    // startSharing()
                                } else {
                                    rlInternetNotAvailable.visibility = View.VISIBLE
                                    mIsSharedButtonError = true
                                }
                            }
                        } else {
                            Toast.makeText(
                                this@ImageSliderActivity,
                                R.string.permission_download_denied, Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }

                    override fun onError(e: Throwable) {

                    }

                    override fun onComplete() {

                    }
                })
        }
        when (v.id) {
            R.id.btnClose -> onBackPressed()
            R.id.btnLeft -> pager!!.currentItem = mCurrentPostion - 2
            R.id.btnRight -> pager!!.currentItem = mCurrentPostion
            R.id.btn_retry ->
                //todo call api and on response remove white screen
                if (mIsSharedButtonError) {
                    rlInternetNotAvailable.visibility = View.GONE
                    mIsSharedButtonError = false
                    btnShare.performClick()
                } else {
                    getAblumContentViaRetry()
                }
        }
    }
    override fun onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this@ImageSliderActivity).unregisterReceiver(
            mSessionExpired
        )
        LocalBroadcastManager.getInstance(this@ImageSliderActivity).unregisterReceiver(
            mSharingFileDownloaded
        )
        LocalBroadcastManager.getInstance(this@ImageSliderActivity).unregisterReceiver(
            mSharingError
        )
        super.onDestroy()
    }
    override fun onBackPressed() {
        if (mApiBeingCalledFlag) {
            mPageCount--
        }
        val intent = Intent(Constants.LOCAL_BROADCAST_ALBUM)
        intent.putExtra(Constants.PARCEABLE_ALBUM, mAlbum)
        intent.putExtra(Constants.ALBUM_ITEM_COUNT, mAlbum!!.mitem.size)
        intent.putExtra(Constants.ITEM_COUNT, mAlbumItemCount)
        intent.putExtra(Constants.REQUEST_PAGE, mPageCount)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        super.onBackPressed()
        finish()
    }

    //------------------------------------------------------------------------------OnCreate-methods
    private fun populateVariableWithIntent() {
        val data = intent.extras
        mAlbum = data!!.getParcelable(Constants.PARCEABLE_ALBUM)
        mAlbumCount = intent.getIntExtra(Constants.ALBUM_ITEM_COUNT, 0)
        mSelectedPosition = intent.getIntExtra(Constants.SELECTED_POSITION, 0)
        mPageCount = intent.getIntExtra(Constants.REQUEST_PAGE, 0)
        mPageCount--
        mAlbumItemCount = intent.getIntExtra(Constants.ITEM_COUNT, 0)
        mCurrentPostion = mSelectedPosition
        mCurrentPostion++
    }
    private fun registerViews() {
        mHandler = Handler()
        mAdapter = ImageSliderAdapter(this, mAlbum!!.mitem.size, mAlbum!!)
    }
    private fun registerListners() {
        btnClose.setOnClickListener(this)
        btnShare.setOnClickListener(this)
        btnLeft.setOnClickListener(this)
        btnRight.setOnClickListener(this)
        btnRetry.setOnClickListener(this)
        rlProgressBarLayout.visibility = View.GONE
        progressBar.max = 100
        setAdapter()
        setPagerListner()
    }

    //-------------------------------------------------------------------------------Local-broadcast
    private val mSessionExpired = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            SessionManager.setSessionExpired(context)
        }
    }
    private val mSharingProgress = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            progressBar.updateProgressbarStatus(intent.getIntExtra(
                Constants.SHARING_FILE_DOWNLOAD_STATUS, 0
            ))
        }
    }
    private val mSharingFileDownloaded = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            rlProgressBarLayout.visibility = View.GONE
            val downloadTask = DownloadTask.getInstance()
            if (downloadTask.isCompression && downloadTask.mFiletype.equals("jpg") && !downloadTask.isCompressionFailed) {
                downloadTask.isCompressionFailed = false
                downloadTask.isCompression = false
                val fotkiDir = File(
                    android.os.Environment.getExternalStorageDirectory().toString()
                            + java.io.File.separator + "FotkiResize"
                )
                fotkiDir.listFiles()?.let{ list ->
                    for (f in list) {
                        if (f.isFile) {
                            val shareIntent = Intent()
                            shareIntent.action = Intent.ACTION_SEND
                            val uri = Uri.parse("file://" + f.absolutePath)
                            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                            shareIntent.type = "*/*"
                            startActivity(Intent.createChooser(shareIntent, "send"))
                        }
                    }
                }
            } else {
                val fotkiDir = File(
                    android.os.Environment.getExternalStorageDirectory().toString()
                            + java.io.File.separator + "Fotki"
                )
                fotkiDir.listFiles()?.let{ list ->
                    for (f in list) {
                        if (f.isFile) {
                            val shareIntent = Intent()
                            shareIntent.action = Intent.ACTION_SEND
                            val uri = Uri.parse("file://" + f.absolutePath)
                            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                            shareIntent.type = "*/*"
                            startActivity(Intent.createChooser(shareIntent, "send"))
                        }
                    }
                }
            }
        }
    }
    private val mSharingError = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //todo hide progress dialog
            rlProgressBarLayout.visibility = View.GONE
        }
    }
    private fun registerLocalBroadCast() {
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mSessionExpired,
            IntentFilter(Constants.SESSION_EXPIRED)
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mSharingProgress,
            IntentFilter(Constants.SHARING_FILE_PROGRESS)
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mSharingFileDownloaded,
            IntentFilter(Constants.SHARING_FILE_DOWNLOADED)
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mSharingError,
            IntentFilter(Constants.SHARING_FILE_ERROR)
        )
    }

    //--------------------------------------------------------------------------------Adapter-images
    @SuppressLint("SetTextI18n")
    private fun setAdapter() {
        pager.adapter = mAdapter
        pager.setCurrentItem(mSelectedPosition, true)
        tvPageIndex.text = (mSelectedPosition + 1).toString() + ""
        tvPageTotal.text = mAlbumItemCount.toString() + ""
    }
    fun callPaginationIfNeeded() {
        //move to right case
        val `val` = mCurrentPostion % 100
        if (`val` == 96 || `val` == 97 || `val` == 98
            || `val` == 99
        ) {
            val pageNeeded = Math.ceil(mAlbumItemCount.toFloat() / 100.0)
            val mrange = Math.ceil(mCurrentPostion.toFloat() / 100.0)
            if (mrange >= mPageCount) {
                if (mPageCount < pageNeeded) {
                    if (!mApiBeingCalledFlag && !mApiBeingCalledThreshold) {
                        mApiBeingCalledFlag = true
                        mApiBeingCalledThreshold = true
                        getAlbumContent()
                    }
                }
            }
        } else {
            mApiBeingCalledThreshold = false
        }
    }
    private fun setPagerListner() {
        pager!!.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int, positionOffset: Float,
                positionOffsetPixels: Int
            ) {

            }

            @SuppressLint("SetTextI18n")
            override fun onPageSelected(position: Int) {
                pager!!.setCurrentItem(position, true)
                handlerTask(mSlideFlag)
                if (position >= mCurrentPostion) {
                    mCurrentPostion += 1
                    tvPageIndex.text = mCurrentPostion.toString() + ""
                    this@ImageSliderActivity.callPaginationIfNeeded()
                } else {
                    mCurrentPostion -= 1
                    tvPageIndex.text = mCurrentPostion.toString() + ""
                }
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
    }

    //---------------------------------------------------------------------------------Album-manager
    private fun getAlbumContent() {
        mPageCount++
        WebManager.instance.getAlbumContentWithPage(
            baseContext, this,
            mAlbum!!.mAlbumIdEnc, mPageCount
        )
    }
    private fun getAblumContentViaRetry() {
        mPageCount++
        mUtility.showProgressDialog(this@ImageSliderActivity, R.string.text_progress_bar_wait)
        WebManager.instance.getAlbumContentWithPage(
            baseContext, this,
            mAlbum!!.mAlbumIdEnc, mPageCount
        )
    }
    fun singleTapDone(item: ParcelableItem) {
        if (mSlideFlag) {
            hideViews()
        } else {
            displaysViews()
            handlerTask(mSlideFlag)
        }
        if (item.mIsVideo) {

            //TODO tap is here
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.mOriginalUrl))
            startActivity(browserIntent)
        }

    }
    private fun hideViews() {
        grControllers.visibility = View.GONE
        mSlideFlag = false
    }
    private fun displaysViews() {
        grControllers.visibility = View.VISIBLE
        mSlideFlag = true
    }
    fun handlerTask(slideFlag: Boolean) {
        mHandler.removeCallbacksAndMessages(null)
        if (slideFlag) {
            mHandler.postDelayed({ hideViews() }, 3000)
        }
    }

    //---------------------------------------------------------------------------------Web-interface
    private fun populateAlbumFromGetAlbumContent(response: JSONObject?) {
        var response = response
        try {
            if (response != null) {
                if (response.getInt(Constants.OK) == 1) {
                    response = response.getJSONObject(Constants.DATA)
                    val mPhoto_json_array = response!!.getJSONArray(Constants.PHOTOS)
                    for (i in 0 until mPhoto_json_array.length()) {
                        val item = ParcelableItem(
                            0,
                            "",
                            "",
                            "",
                            "",
                            "",
                            false,
                            "",
                            0
                        )
                        item.mAlbumIdEnc = mAlbum!!.mAlbumIdEnc
                        item.mId = mPhoto_json_array.getJSONObject(i).getLong(Constants.ID)
                        item.mViewUrl = mPhoto_json_array.getJSONObject(i).getString(
                            Constants.VIEW_URL
                        )
                        item.mCreated = mPhoto_json_array.getJSONObject(i).getString(
                            Constants.CREATED
                        )
                        item.mThumbnailUrl = mPhoto_json_array.getJSONObject(i).getString(
                            Constants.THUMBNAIL_URL
                        )
                        item.mOriginalUrl = mPhoto_json_array.getJSONObject(i).getString(
                            Constants.ORIGINAL_URL
                        )
                        item.mTitle = mPhoto_json_array.getJSONObject(i).getString(
                            Constants.TITTLE
                        )
                        val mIsVideo = mPhoto_json_array.getJSONObject(i).getInt(
                            Constants.VIDEO
                        )
                        item.mVideoUrl = mPhoto_json_array.getJSONObject(i).getString(
                            Constants.VIDEO_URL
                        )
                        item.mShortUrl = mPhoto_json_array.getJSONObject(i).getString(
                            Constants.SHORT_URL
                        )

                        if(mPhoto_json_array.getJSONObject(i).has(Constants.VIDEO_CONVERT_STATUS)){
                            item.mInaccessable = mPhoto_json_array.getJSONObject(i).getInt(
                                Constants.VIDEO_CONVERT_STATUS
                            )
                        }


                        if(mPhoto_json_array.getJSONObject(i).has(Constants.ORIGINAL_FILENAME)){
                            item.mOriginalFilename = mPhoto_json_array.getJSONObject(i).getString(
                                Constants.ORIGINAL_FILENAME
                            )
                        }

                        item.mIsVideo = mIsVideo == 2
                        mAlbum!!.mitem.add(item)
                    }
                    mAlbumCount = mAlbum!!.mitem.size
                    mApiBeingCalledFlag = false
                    mAdapter!!.setItemCount(mAlbum!!.mitem.size)
                    mAdapter!!.notifyDataSetChanged()
                    pager!!.adapter!!.notifyDataSetChanged()
                } else {
                    CrashLogger.sendCrashLog(
                        CrashLogger.IMAGE_SLIDER_LEVEL, response.toString())
                    mUtility.showAlertDialog(
                        this,
                        response.toString()
                    ) { getAblumContentViaRetry() }
                }
            }
        } catch (e: JSONException) {
            CrashLogger.sendCrashLog(CrashLogger.IMAGE_SLIDER_LEVEL, e.message!!)
            mUtility.showAlertDialog(this, e.message!!) { getAblumContentViaRetry() }
            e.printStackTrace()
        }

    }
    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
        if (apiRequestType === GET_ALBUM_CONTENT) {
            rlInternetNotAvailable.visibility = View.GONE
            mUtility.dismissProgressDialog()
            mUtility.fotkiAppDebugLogs(TAG, response.toString())
            populateAlbumFromGetAlbumContent(response)
        } else {
            mUtility.dismissProgressDialog()
            CrashLogger.sendCrashLog(CrashLogger.IMAGE_SLIDER_LEVEL, response.toString())
            mUtility.showAlertDialog(this, response.toString()) { getAblumContentViaRetry() }
        }
    }
    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {
        mUtility.dismissProgressDialog()

        var message = " "
        mUtility.dismissProgressDialog()
        message = when (error) {
            is NetworkError -> getString(R.string.network_not_found)
            is ServerError -> getString(R.string.server_error)
            is AuthFailureError -> getString(R.string.network_not_found)
            is ParseError -> getString(R.string.parse_error)
            is TimeoutError -> getString(R.string.time_out_error)
            else -> getString(R.string.network_not_found)
        }
        //mUtility.showAlertDialog(this, message)
        CrashLogger.sendCrashLog(CrashLogger.IMAGE_SLIDER_LEVEL, message)
        mUtility.showAlertDialog(this, message) { getAblumContentViaRetry() }
    }
    override fun sendNetworkFailure(isInterNetAvailableFlag: Boolean, apiRequestType: ApiRequestType
    ) {
        if (!isInterNetAvailableFlag) {
            mUtility.dismissProgressDialog()
            rlInternetNotAvailable.visibility = View.VISIBLE
            if (mApiBeingCalledFlag) {
                mPageCount--
            }
        }

    }

    //-----------------------------------------------------------------------------------------Utils
    private fun updateProgressbarStatus(value: Int) {
        progressBar.progress = value

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            javaClass.name
        )
        mWakeLock!!.acquire()
    }
    private fun startSharing() {
        val item = mAlbum!!.mitem[mCurrentPostion - 1]
        var url: String? = null
        url = if (item.mIsVideo) {
            item.mOriginalUrl
        } else {
            item.mViewUrl
        }
        val downloadTask = DownloadTask.getInstance()
        downloadTask.mContext = this@ImageSliderActivity
        downloadTask.url = url
        downloadTask.isCompression = isAllowCompress
        downloadTask.isCompressionFailed = false
        downloadTask.startDownloadTask()
        rlProgressBarLayout.visibility = View.VISIBLE
    }
    @SuppressLint("InflateParams")
    private fun showChoiceDialog() {
        makeList()
        mDialog = Dialog(this)

        val inflater = this.layoutInflater
        val convertView = inflater.inflate(R.layout.share_file_size_reduction_dialog, null) as View
        choiceAlert = convertView.findViewById(R.id.choiceDialog)
        val sizeReductionDialogAdapter = SizeReductionDialogAdapter(
            this,
            R.layout.share_file_size_reduction_dialog, listChoice
        )

        choiceAlert.adapter = sizeReductionDialogAdapter
        mDialog.setContentView(convertView)
        mDialog.show()
        setChoiceListViewListner()
    }
    private fun makeList() {
        listChoice.clear()
        listChoice.add("Resized")
        listChoice.add(" Originals")
        listChoice.add("Cancel")
    }
    private fun setChoiceListViewListner() {
        choiceAlert.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val choiceSelected = listChoice[position]
            when (choiceSelected) {
                "Resized" -> {
                    isAllowCompress = true
                    mDialog.dismiss()
                    startSharing()
                }
                " Originals" -> {
                    isAllowCompress = false
                    mDialog.dismiss()
                    startSharing()
                }
                "Cancel" -> {
                    isAllowCompress = false
                    mDialog.dismiss()
                }
                else -> {
                    mDialog.dismiss()
                }
            }
        }
    }

    companion object {
        var TAG = FotkiTabActivity::class.java.name
    }
}