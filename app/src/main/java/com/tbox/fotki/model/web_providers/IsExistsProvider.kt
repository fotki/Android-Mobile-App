package com.tbox.fotki.model.web_providers

import android.content.Context
import android.util.Log
import com.android.volley.VolleyError
import com.tbox.fotki.model.database.FilesEntity
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.util.Constants
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.util.L
import com.tbox.fotki.util.sync_files.LogProvider
import com.tbox.fotki.util.sync_files.MediaSyncronizator
import com.tbox.fotki.util.sync_files.PreferenceHelper
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class IsExistsProvider(context: Context, var afterSync: () -> Unit,
                       var files: ArrayList<FilesEntity>) : BaseWebProvider(context) {

    private var tryToLoad = 0

    init {
        start()
    }

    override fun start() {
        WebManager.instance.isPhotoExists(context, this, files)
    }

    override fun sendSuccess(
        response: JSONObject,
        apiRequestType: ApiRequestType
    ) {
        L.print(this, "response isExists - $response")
        LogProvider.writeToFile("response from isPhotoExists - $response", context)
        val data = response.getJSONObject(Constants.DATA)
        val missing = data.getJSONArray(Constants.MISSING)
        for (i in 0 until missing.length()) {
            MediaSyncronizator(context)
                .updateSuccessSyncedSHA(
                    missing.getString(i),
                    FilesEntity.STATUS_NOT_CHECK_FOLDER
                )
            TimeUnit.MILLISECONDS.sleep(100)
        }
        val exist = data.getJSONArray(Constants.EXIST)
        for (i in 0 until exist.length()) {
            MediaSyncronizator(context)
                .updateSuccessSyncedSHA(exist.getString(i), FilesEntity.LOADED)
            TimeUnit.MILLISECONDS.sleep(100)
        }

        Log.d("MEDIA_TAG", "resp from is exists - $response")
        afterSync.invoke()
    }

    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {
        Log.d("MEDIA_TAG", "$tryToLoad error from isPhotoExists - $error")
        LogProvider.writeToFile(
            "$tryToLoad error from isPhotoExists - $error"
            , context
        )
        tryToLoad++
        if (tryToLoad < 5) {
            TimeUnit.SECONDS.sleep(10)
            start()
        } else {
            PreferenceHelper(context).applyPrefs(hashMapOf(Constants.SYNC_STARTED to false))
            tryToLoad = 0
        }
    }

    override fun sendNetworkFailure(
        isInterNetAvailableFlag: Boolean,
        apiRequestType: ApiRequestType
    ) {
        PreferenceHelper(context).applyPrefs(hashMapOf(Constants.SYNC_STARTED to false))
        Log.d("MEDIA_TAG", "network failure")
        LogProvider.writeToFile("network failure", context)
    }
}