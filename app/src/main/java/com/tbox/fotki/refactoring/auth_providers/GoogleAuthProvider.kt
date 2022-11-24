package com.tbox.fotki.refactoring.auth_providers

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.SocialLoginDetail
import com.tbox.fotki.model.entities.User
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.CrashLogger
import com.tbox.fotki.view.activities.LoginActivity
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import com.tbox.fotki.view.adapters.GoogleSignInDialogAdapter
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONException
import org.json.JSONObject

class GoogleAuthProvider(val activity: Activity){/*
    private fun registerGoogleSignUPDetails(btnSignInGoogle:SignInButton) {
        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(Scopes.PROFILE))
            .requestServerAuthCode(Constants.GOOGLE_AUTH_KEY_WEB_PRODUCTION, false)
            .requestIdToken(Constants.GOOGLE_AUTH_KEY_WEB_PRODUCTION)
            .requestEmail()
            .build()
        mGoogleApiClient = GoogleApiClient.Builder(activity)
            .enableAutoManage(activity, this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build()
        btnSignInGoogle.setSize(SignInButton.SIZE_STANDARD)
        btnSignInGoogle.setOnClickListener(this)
    }
    private fun signIn() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        activity.startActivityForResult(signInIntent, LoginActivity.RC_SIGN_IN)
    }
    private fun handleSignInResult(result: GoogleSignInResult) {
        Log.d(LoginActivity.TAG, "handleSignInResult:" + result.isSuccess)
        if (result.isSuccess) {
            val acct = result.signInAccount
            if (acct != null) {
                mUserGoogleEmail = acct.email
                RetrieveTokenTask().execute(mUserGoogleEmail)
            }
        } else {
            Log.e("googleError", "error")
        }
    }
    private fun getNewGoogleApiResponse(`object`: JSONObject) {
        var `object` = `object`
        mUtility.dismissProgressDialog()
        try {
            if (`object`.getInt(Constants.OK) == 1) {
                val numberOfAccount = `object`.optInt(Constants.ACCOUNTS_FOUND)
                `object` = `object`.getJSONObject(Constants.DATA)
                if (numberOfAccount == 1) {
                    //todo save user session and update session class
                    saveLoginWithGoogleSession(`object`)
                } else if (numberOfAccount >= 2) {
                    parseGoogleUser(`object`)
                }
            } else {
                var message = `object`.getString(Constants.API_MESSAGE)
                if (message == getString(R.string.no_account_found)) {
                    message = "We coudn't find any account associated with $mUserGoogleEmail on Fotki"
                    mUtility.showAlertDialog(this, message)
                }
            }
        } catch (e: JSONException) {
            CrashLogger.sendCrashLog(CrashLogger.LOGIN_ACTIVITY_LEVEL,e.message!!)
            e.printStackTrace()
        }

    }
    private fun getSelectedGoogleAccountResponse(`object`: JSONObject) {
        var `object` = `object`
        mUtility.dismissProgressDialog()
        try {
            if (`object`.getInt(Constants.OK) == 1) {
                `object` = `object`.getJSONObject(Constants.DATA)
                val sessionKey = `object`.getString(Constants.SESSION_ID)
                val login = `object`.getString(Constants.LOGIN)
                mSession!!.mIsSessionId = true
                mSession!!.mSessionId = sessionKey
                mSession!!.mSignInWithGoogle = true
                mSession!!.mSignInWithFacebook = false
                val user = User(login, "")
                mSession!!.addUser(user)
                mSession!!.saveSession(this)
                // TODO: 7/28/17
                //avoid memory leak
                startActivity(
                    Intent(this,
                        FotkiTabActivity::class.java)
                )

                this@LoginActivity.finish()
                WebManager.instance.getAccountInfo(baseContext, this)
            }
        } catch (e: JSONException) {
            CrashLogger.sendCrashLog(CrashLogger.LOGIN_ACTIVITY_LEVEL,e.message!!)
            e.printStackTrace()
        }

    }
    private fun saveLoginWithGoogleSession(jsonObject: JSONObject) {
        try {
            val userName = jsonObject.getString(Constants.LOGIN)
            val sessionKey = jsonObject.getString(Constants.SESSION_ID)
            mSession!!.mIsSessionId = true
            mSession!!.mSessionId = sessionKey
            mSession!!.mSignInWithGoogle = true
            mSession!!.mSignInWithFacebook = false
            val user = User(userName, "")
            mSession!!.addUser(user)
            mSession!!.saveSession(this)
            startActivity(
                Intent(this,
                    FotkiTabActivity::class.java)
            )
            this@LoginActivity.finish()
            WebManager.instance.getAccountInfo(baseContext, this)
        } catch (e: JSONException) {
            CrashLogger.sendCrashLog(CrashLogger.LOGIN_ACTIVITY_LEVEL,e.message!!)
            e.printStackTrace()
        }

    }
    private fun parseGoogleUser(jsonObject: JSONObject) {
        try {
            val jsonArray = jsonObject.getJSONArray(Constants.SOCIAL_LOGINS)
            mSocialLogins.clear()
            for (i in 0 until jsonArray.length()) {
                var `object` = jsonArray.getJSONObject(i)
                val login = `object`.getString(Constants.LOGIN)
                val avatar = `object`.getString(Constants.SOCIAL_AVATAR)
                val spaceUsed = `object`.getString(Constants.SPACE_USED)
                val active = `object`.getInt(Constants.ACTIVE)
                val suspended = `object`.getInt(Constants.SUSPENDED)
                `object` = `object`.getJSONObject(Constants.MEMBER_SINCE)
                val date = `object`.getString(Constants.DATE)
                val unix_Date = `object`.getString(Constants.UNIX_DATE)
                val googleLogin =
                    SocialLoginDetail(
                        login, avatar,
                        mUserGoogleEmail!!,
                        mGoogleAccessToken!!,
                        date,
                        unix_Date,
                        spaceUsed,
                        active,
                        suspended
                    )
                mSocialLogins.add(googleLogin)
            }
            //todo show custom alert dialog and handle cases
            showGoogleOptionAlertDialog(mSocialLogins)
        } catch (e: JSONException) {
            CrashLogger.sendCrashLog(CrashLogger.LOGIN_ACTIVITY_LEVEL,e.message!!)
            e.printStackTrace()
        }

    }
    @SuppressLint("InflateParams")
    private fun showGoogleOptionAlertDialog(googleLogins: ArrayList<SocialLoginDetail>) {
        alertDialog = Dialog(this@LoginActivity)
        val inflater = layoutInflater
        val convertView = inflater.inflate(R.layout.google_signin_dialog_view, null) as View
        val textView = convertView.findViewById<TextView>(R.id.text_selection)
        val str1 = "Here what we find with the "
        val str2 = mUserGoogleEmail
        val str3 = " email Please choose your fotki account you want to log in with"
        val str = SpannableStringBuilder(str1 + str2 + str3)
        str.setSpan(
            StyleSpan(Typeface.BOLD), str1.length, str1.length + str2!!.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        str.setSpan(
            UnderlineSpan(), str1.length, str1.length + str2.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = str
        alertDialog.setContentView(convertView)
        googleSignInListView = convertView.findViewById(R.id.googleSignIn)
        val googleSignInDialogAdapter = GoogleSignInDialogAdapter(this,
            R.layout.google_signin_dialog_view, googleLogins)
        googleSignInListView.adapter = googleSignInDialogAdapter
        alertDialog.show()
        setGoogleSignInListViewListner()
    }
    private fun setGoogleSignInListViewListner() {
        googleSignInListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val googleLogin = mSocialLogins[position]
            if (googleLogin.active == 1) {
                if (googleLogin.suspended == 0) {
                    //call api...
                    mUtility.showProgressDialog(this@LoginActivity,
                        this@LoginActivity.getString(R.string.text_progress_bar_wait))
                    WebManager.instance.makeGoogleLoginWithSelectedAccount(baseContext,
                        this@LoginActivity, googleLogin.mAccessToken,
                        googleLogin.mUserName)
                } else {
                    //show account is suspended
                    alertDialog.dismiss()
                    mUtility.showAlertDialog(this@LoginActivity, "Your account has been temporary suspended due to membership expiration. Please visit Fotki using internet browser to renew your subscription.")
                }

            } else {
                //show user is not active...
                alertDialog.dismiss()
                mUtility.showAlertDialog(this@LoginActivity, "You account has been deleted by Admin. Please contact with him.")
            }
        }
    }
    companion object {
        private var TAG = LoginActivity::class.java.name
        private val RC_SIGN_IN = 9001
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }*/
}