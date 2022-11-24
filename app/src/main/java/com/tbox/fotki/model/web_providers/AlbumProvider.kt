package com.tbox.fotki.model.web_providers

import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import com.android.volley.*
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.model.entities.Folder
import com.tbox.fotki.model.entities.ParcelableAlbum
import com.tbox.fotki.model.entities.ParcelableItem
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.CrashLogger
import com.tbox.fotki.util.sync_files.LogProvider
import com.tbox.fotki.view.activities.ImageSliderActivity
import kotlinx.android.synthetic.main.activity_image_slider.*
import org.json.JSONException
import org.json.JSONObject

class AlbumProvider(context:Context,val requestedAlbum:ParcelableAlbum, val page:Int)
    :BaseWebProvider(context) {

    val album = MutableLiveData<ParcelableAlbum>()
    var success = {}


    override fun start() {
        WebManager.instance.getAlbumContentWithPage(
            context, this,
            requestedAlbum.mAlbumIdEnc, page
        )
    }

    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
        if (apiRequestType === ApiRequestType.GET_ALBUM_CONTENT) {
            mUtility.dismissProgressDialog()
            mUtility.fotkiAppDebugLogs(ImageSliderActivity.TAG, response.toString())
            populateAlbumFromGetAlbumContent(response)
        } else {
            mUtility.dismissProgressDialog()
            CrashLogger.sendCrashLog(CrashLogger.IMAGE_SLIDER_LEVEL, response.toString())
            mUtility.showAlertDialog(context, response.toString()) { getAblumContentViaRetry() }
        }
    }

    private fun populateAlbumFromGetAlbumContent(response: JSONObject?) {
        var response = response
        try {
            if (response != null) {
                if (response.getInt(Constants.OK) == 1) {
                    response = response.getJSONObject(Constants.DATA)
                    val mPhoto_json_array = response!!.getJSONArray(Constants.PHOTOS)
                    val album = ParcelableAlbum(requestedAlbum.mAlbumIdEnc,
                        requestedAlbum.mdescription,requestedAlbum.mName,requestedAlbum.mCoverUrl,
                        ArrayList(),requestedAlbum.mShareUrl)

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
                        item.mAlbumIdEnc = requestedAlbum.mAlbumIdEnc
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
                        item.mVideoUrl = mPhoto_json_array.getJSONObject(i).getString(
                            Constants.VIDEO_URL
                        )
                        item.mTitle = mPhoto_json_array.getJSONObject(i).getString(
                            Constants.TITTLE
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
                        val mIsVideo = mPhoto_json_array.getJSONObject(i).getInt(
                            Constants.VIDEO
                        )
                        item.mIsVideo = mIsVideo == 2
                        album.mitem.add(item)
                    }
                    this.album.value = album
                } else {
                    CrashLogger.sendCrashLog(
                        CrashLogger.IMAGE_SLIDER_LEVEL, response.toString())
                    mUtility.showAlertDialog(
                        context,
                        response.toString()
                    ) { getAblumContentViaRetry() }
                }
            }
        } catch (e: JSONException) {
            CrashLogger.sendCrashLog(CrashLogger.IMAGE_SLIDER_LEVEL, e.message!!)
            mUtility.showAlertDialog(context, e.message!!) { getAblumContentViaRetry() }
            e.printStackTrace()
        }

    }

    fun getAblumContentViaRetry() {
        //mPageCount++
        mUtility.showProgressDialog(context, R.string.text_progress_bar_wait)
        WebManager.instance.getAlbumContentWithPage(
            context, this,
            requestedAlbum.mAlbumIdEnc, page
        )
    }

}