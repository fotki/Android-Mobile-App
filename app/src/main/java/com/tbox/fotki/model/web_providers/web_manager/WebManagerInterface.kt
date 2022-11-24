package com.tbox.fotki.model.web_providers.web_manager

import com.android.volley.VolleyError

import com.tbox.fotki.model.entities.ApiRequestType

import org.json.JSONObject

/**
* Created by Junaid on 4/6/17.
*/

interface WebManagerInterface {

    fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType)

    fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType)

    fun sendNetworkFailure(isInterNetAvailableFlag: Boolean, apiRequestType: ApiRequestType)
}
