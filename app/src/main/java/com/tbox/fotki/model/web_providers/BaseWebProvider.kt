package com.tbox.fotki.model.web_providers

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.volley.*
import com.tbox.fotki.BuildConfig
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.Utility
import com.tbox.fotki.model.web_providers.web_manager.WebManagerInterface
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import org.json.JSONObject

open abstract class BaseWebProvider(val context: Context):
    WebManagerInterface {

    val mUtility = Utility.instance
    val res = MutableLiveData<JSONObject>()

    abstract fun start()

    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
        mUtility.dismissProgressDialog()
        if (response.has(Constants.OK)&&response[Constants.OK]!=1){
            mUtility.dismissProgressDialog()
            mUtility.fotkiAppErrorLogs(FotkiTabActivity.TAG, response.getString(Constants.API_MESSAGE))
            return
        } else {
            res.value = response
        }
    }

    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {
        if (BuildConfig.DEBUG) Log.d(FotkiTabActivity.TAG, error.toString())
        var message = " "
        mUtility.dismissProgressDialog()
        message = when (error) {
            is NetworkError -> context.getString(R.string.network_not_found)
            is ServerError -> context.getString(R.string.server_error)
            is AuthFailureError -> context.getString(R.string.network_not_found)
            is ParseError -> context.getString(R.string.parse_error)
            is TimeoutError -> context.getString(R.string.time_out_error)
            else -> context.getString(R.string.network_not_found)
        }
        mUtility.showAlertDialog(context, message)
    }

    override fun sendNetworkFailure(
        isInterNetAvailableFlag: Boolean,
        apiRequestType: ApiRequestType
    ) {
        res.value = null
    }
}