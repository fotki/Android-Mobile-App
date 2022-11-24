@file:Suppress("DEPRECATION")

package com.tbox.fotki.util.upload_files

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.android.volley.*
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.model.entities.ApiRequestType.NEW_SESSION_API
import com.tbox.fotki.model.entities.Session
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.CrashLogger
import com.tbox.fotki.util.Utility
import com.tbox.fotki.model.web_providers.web_manager.WebManagerInterface
import com.tbox.fotki.R
import com.tbox.fotki.refactoring.screens.upload_files.UploadMultipleFilesService
import com.tbox.fotki.refactoring.service.UploadFilesService
import com.tbox.fotki.util.L
import org.json.JSONException
import org.json.JSONObject
import java.util.*

@Suppress("DEPRECATION", "VARIABLE_WITH_REDUNDANT_INITIALIZER", "NAME_SHADOWING")
/**
 * Created by Junaid on 5/12/17.
 */

class FileUploader() :
    WebManagerInterface {

    private var mSessionId: String? = null
    var mAlbumId: Long? = null
    var mFileTypes = ArrayList<FileType>()
    private var mItemCounter: Int = 0
    var mContext: Context? = null
    private var mRetryHitCount: Int = 0
    var isUploading = false
    var isCompressionAllow = false
    var mAlbumName = " "
    internal var mUtility = Utility.instance
    //var myService: UploadFilesService? = null
    var myService: UploadMultipleFilesService? = null

    var isDelete:Boolean = false

    fun startFileUploadingTask() {
        Log.d("UploadFilesViewModel","FileUploader startFileUploadingTask")
        mItemCounter = 0
        mRetryHitCount = 0
        mSessionId = Session.getInstance(this.mContext!!).mSessionId
        (mContext as FotkiTabActivity).uploadThreadManager
            .startUploadService(mAlbumName,mFileTypes,mSessionId!!,mAlbumId!!,
            PreferenceManager.getDefaultSharedPreferences(mContext)
                .getInt(Constants.CURRENT_LOAD_ITEM,0),isDelete,isCompressionAllow)
    }

    fun retryFileUploading() {
        Log.d("UploadFilesViewModel","FileUploader retryFileUploading")
        L.print(this,"MEDIA Restart uploading")
        mSessionId = Session.getInstance(this.mContext!!).mSessionId
        (mContext as FotkiTabActivity).uploadThreadManager
            .startUploadService(mAlbumName,mFileTypes,mSessionId!!,mAlbumId!!,
            PreferenceManager.getDefaultSharedPreferences(mContext)
                .getInt(Constants.CURRENT_LOAD_ITEM,0),isDelete,isCompressionAllow)
    }

    fun cancelFileUploading() {
        L.print(this,"MEDIA Uploading try to stop!!")
        isUploading = false
        (mContext as FotkiTabActivity).uploadThreadManager.stopUploadService()
    }

    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {

        var response = response
        if (apiRequestType === NEW_SESSION_API) {
            try {
                if (response.getInt(Constants.OK) == 1) {
                    val mSession = Session.getInstance(mContext!!)
                    mSession.mIsSessionId = true
                    response = response.getJSONObject(Constants.DATA)
                    mSession.mSessionId = response.getString(Constants.SESSION_ID)
                    mSession.saveSession(mContext!!)
                    retryFileUploading()
                }
            } catch (e: JSONException) {
                isUploading = false
                CrashLogger.sendCrashLog(CrashLogger.FILE_UPLOADER_LEVEL, e.message!!)
                Utility.instance.showAlertDialog(mContext!!, e.message!!) {
                    //isUploading = true
                    retryFileUploading() }
                e.printStackTrace()
            }
        }
    }

    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {
       // isUploading = false
        var message = " "
        mUtility.dismissProgressDialog()
        message = when (error) {
            is NetworkError -> mContext!!.getString(R.string.network_not_found)
            is ServerError -> mContext!!.getString(R.string.server_error)
            is AuthFailureError -> mContext!!.getString(R.string.network_not_found)
            is ParseError -> mContext!!.getString(R.string.parse_error)
            is TimeoutError -> mContext!!.getString(R.string.time_out_error)
            else -> mContext!!.getString(R.string.network_not_found)
        }
        mUtility.showAlertDialog(mContext!!, message)
        CrashLogger.sendCrashLog(CrashLogger.FILE_UPLOADER_LEVEL, message)
        Utility.instance.showAlertDialog(mContext!!, message) { retryFileUploading() }
    }

    override fun sendNetworkFailure(
        isInterNetAvailableFlag: Boolean,
        apiRequestType: ApiRequestType
    ) {
        isUploading = false
        CrashLogger.sendCrashLog(CrashLogger.FILE_UPLOADER_LEVEL, "Network failure")
        Utility.instance.showAlertDialog(mContext!!, "Network failure") { retryFileUploading() }
    }



    companion object {
        private var sFileUploader: FileUploader? = null
        val instance: FileUploader
            get() {
                if (sFileUploader == null) {
                    sFileUploader = session
                }
                return sFileUploader as FileUploader
            }

        private val session: FileUploader
            get() = FileUploader()
    }
}