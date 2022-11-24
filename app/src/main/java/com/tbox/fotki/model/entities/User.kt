package com.tbox.fotki.model.entities

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.google.gson.Gson
import com.tbox.fotki.util.Constants

/**
* Created by Junaid on 4/7/17.
*/

class User(var mUserName: String, var mPassword: String) {
    var mDisplayName: String? = null
    var mCoverUrl: String? = null
    var isLogin = false

    fun saveUserInfo(context: Context) {
        this.isLogin = true
        val prefs = context.getSharedPreferences(
            Constants.USER_INFO_FILE,
                MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(this)
        prefsEditor.putString(Constants.USER_INFO_IS_LOGIN, json)
        prefsEditor.apply()
    }

}
