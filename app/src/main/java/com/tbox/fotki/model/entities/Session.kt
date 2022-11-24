package com.tbox.fotki.model.entities

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.tbox.fotki.util.Constants

/**
 * Created by Junaid on 4/7/17.
 */

class Session {

    var mSessionId: String? = null
    var mIsSessionId: Boolean = false
    var mSignInWithGoogle: Boolean = false
    var mSignInWithFacebook: Boolean = false
    var mUser: User? = null


    fun saveSession(context: Context) {
        this.mIsSessionId = true
        val prefs = context.getSharedPreferences(
            Constants.SESSION_ID_FILE,
            MODE_PRIVATE
        )
        val prefsEditor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(this)
        prefsEditor.putString(Constants.SESSION_ID_KEY, json)
        prefsEditor.apply()
    }

    fun removeSessionInfo(context: Context) {
        val prefs = context.getSharedPreferences(
            Constants.SESSION_ID_FILE,
            MODE_PRIVATE
        )
        this.mIsSessionId = false
        this.mSessionId = ""
        val gson = Gson()
        val json = gson.toJson(this)
        val prefsEditor = prefs.edit()
        prefsEditor.putString(Constants.SESSION_ID_KEY, json)
        prefsEditor.apply()
    }

    fun addUser(user: User) {
        this.mUser = user
    }

    companion object {
        private var sSession: Session? = null
        fun getInstance(context: Context): Session {
            if (sSession == null) {
                sSession =
                    getSession(
                        context
                    )
            }
            return sSession as Session
        }

        private fun getSession(context: Context): Session {
            val gson = Gson()
            val prefs = context.getSharedPreferences(
                Constants.SESSION_ID_FILE,
                MODE_PRIVATE
            )
            val json = prefs.getString(Constants.SESSION_ID_KEY, null)
            try {
                if (json != null) {
                    val session = gson.fromJson(json, Session::class.java)
                    if (session.mSessionId != null && session.mSessionId!!.isNotEmpty()) {
                        session.mIsSessionId = true
                    }
                    return session
                } else {
                    return Session()
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                context.getSharedPreferences(
                    Constants.SESSION_ID_FILE,
                    MODE_PRIVATE
                ).edit().clear().apply()
                return Session()
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
                context.getSharedPreferences(Constants.SESSION_ID_FILE, MODE_PRIVATE).edit().clear()
                    .apply()
                return Session()
            }

        }
    }

}
