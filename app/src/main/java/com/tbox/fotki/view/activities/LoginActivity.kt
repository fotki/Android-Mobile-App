package com.tbox.fotki.view.activities


import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.android.volley.*
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.tbox.fotki.view.adapters.FaceBookSignInDialogAdapter
import com.tbox.fotki.view.adapters.GoogleSignInDialogAdapter
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.model.entities.ApiRequestType.*
import com.tbox.fotki.model.entities.Session
import com.tbox.fotki.model.entities.SocialLoginDetail
import com.tbox.fotki.model.entities.User
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.CrashLogger
import com.tbox.fotki.util.Utility
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.BuildConfig
import com.tbox.fotki.R
import com.tbox.fotki.refactoring.auth_providers.GoogleAuthProvider
import com.tbox.fotki.util.L
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import com.tbox.fotki.view.activities.general.BaseActivity
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

@Suppress("NAME_SHADOWING", "VARIABLE_WITH_REDUNDANT_INITIALIZER", "DEPRECATION")
class LoginActivity : BaseActivity(), View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    private var mSession: Session? = null
    private var mUtility = Utility.instance
    private lateinit var mGoogleApiClient: GoogleApiClient
    private var mUserGoogleEmail: String? = null
    private var mUserFaceBookEmail: String? = null
    private var mGoogleAccessToken: String? = null
    private var mSocialLogins = ArrayList<SocialLoginDetail>()
    private lateinit var googleSignInListView: ListView
    private lateinit var facebookSignInListView: ListView
    private lateinit var alertDialog: Dialog
    private var callbackManager: CallbackManager? = null
    private var mFacebookAccessToken: String? = null


    //-------------------------------------------------------------------------------------Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()
        L.print(this, "Login activity started!")

        registerGoogleSignUPDetails()
        registerFaceBookSignUpDetails()
        registerListners()
        checkIfSession()

        tvVersion.text = "${BuildConfig.VERSION_NAME}"
        checkPermissions()

    }
    override fun onClick(view: View?) {
        var view = view
        when (view!!.id) {
            R.id.btnLogin -> {
                view = this.currentFocus
                if (view != null) {
                    val imm = getSystemService(
                            Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
                checkInputField()
            }
            R.id.btnLoginGoogle, R.id.btnSignInGoogle -> if (mUtility.isConnectingToInternet(this)) {
                signIn()
            } else {
                hideViews()
            }
            R.id.btnLoginFb -> if (mUtility.isConnectingToInternet(this)) {
                btnSignInFacebook.performClick()
            } else {
                hideViews()
            }
        }
    }
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleSignInResult(result)
        } else { // for facebook
            callbackManager!!.onActivityResult(requestCode, resultCode, data)
        }

    }

    //------------------------------------------------------------------------------Helper-functions
    private fun registerListners() {
        btnLogin.setOnClickListener(this)
        btnLoginGoogle.setOnClickListener(this)
        btnLoginFb.setOnClickListener(this)
        btnSignInFacebook.setReadPermissions(
            Arrays.asList(
                "public_profile", "email"))
    }
    private fun checkIfSession() {
        mSession = Session.getInstance(this)
        if (mSession!!.mIsSessionId) {
            if (BuildConfig.DEBUG) Log.v(TAG, R.string.text_log_user_already_login.toString() + "")
            startActivity(Intent(this,
                FotkiTabActivity::class.java))
            this@LoginActivity.finish()
        }
    }
    private fun checkInputField() {
        if (isFieldValidated) {
            mUtility.showProgressDialog(this, R.string.text_progress_bar_wait)
            WebManager.instance.makeLogin(baseContext, this,
                etUserName.text.toString(), etUserPassword.text.toString())
        } else {
            mUtility.showAlertDialog(this, R.string.text_alert_login)
        }
    }
    private val isFieldValidated: Boolean
        get() = !(etUserName.text.isEmpty() || etUserPassword.text.isEmpty())
    private inner class RetrieveTokenTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String): String? {
            val accountName = params[0]
            val scopes = "oauth2:profile email"
            var token: String? = null
            try {
                token = GoogleAuthUtil.getToken(applicationContext, accountName, scopes)
            } catch (e: Exception) {
                CrashLogger.sendCrashLog(CrashLogger.LOGIN_ACTIVITY_LEVEL,e.message!!)
                e.printStackTrace()
            }

            return token
        }

        override fun onPostExecute(accessToken: String) {
            super.onPostExecute(accessToken)
            mGoogleAccessToken = accessToken
            mUtility.showProgressDialog(this@LoginActivity,
                this@LoginActivity.getString(R.string.text_progress_bar_wait))
            WebManager.instance.makeGoogleLogin(baseContext, this@LoginActivity,
                accessToken)
        }
    }

    //--------------------------------------------------------------------------------------Facebook
    private fun registerFaceBookSignUpDetails() {
        callbackManager = CallbackManager.Factory.create()
        btnSignInFacebook.registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(loginResult: LoginResult) {
                        Log.d("fbError","registerFaceBookSignUpDetails onSuccess "+loginResult)
                        mFacebookAccessToken = loginResult.accessToken.token
                        val request = GraphRequest.newMeRequest(
                                loginResult.accessToken
                        ) { `object`, _ ->
                            try {
                                mUserFaceBookEmail = `object`.getString("email")
                                mUtility.showProgressDialog(this@LoginActivity,
                                        this@LoginActivity.getString(
                                                R.string.text_progress_bar_wait))
                                WebManager.instance.makeFaceBookLogin(
                                        baseContext, this@LoginActivity,
                                        mFacebookAccessToken)
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                        val parameters = Bundle()
                        parameters.putString("fields",
                                "id, first_name, last_name, email,gender, birthday, location")
                        request.parameters = parameters
                        request.executeAsync()
                    }

                    override fun onCancel() {
                        Log.d("fbError","registerFaceBookSignUpDetails onCancel")
                        // App code
                    }

                    override fun onError(exception: FacebookException) {
                        // App code
                        Log.d("fbError","registerFaceBookSignUpDetails onError")
                        Log.e("fb Exception", exception.toString())
                    }
                })
    }
    private fun getNewFaceBookApiResponse(`object`: JSONObject) {
        var `object` = `object`
        mUtility.dismissProgressDialog()
        try {
            if (`object`.getInt(Constants.OK) == 1) {
                LoginManager.getInstance().logOut()
                val numberOfAccount = `object`.optInt(Constants.ACCOUNTS_FOUND)
                `object` = `object`.getJSONObject(Constants.DATA)
                if (numberOfAccount == 1) {
                    //todo save user session and update session class
                    saveLoginWithFaceBookSession(`object`)
                } else if (numberOfAccount >= 2) {
                    parseFaceBookUser(`object`)
                }
            } else {
                LoginManager.getInstance().logOut()
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
    private fun getSelectedFaceBookAccountResponse(`object`: JSONObject) {
        var `object` = `object`
        mUtility.dismissProgressDialog()
        try {
            if (`object`.getInt(Constants.OK) == 1) {
                `object` = `object`.getJSONObject(Constants.DATA)
                val sessionKey = `object`.getString(Constants.SESSION_ID)
                val login = `object`.getString(Constants.LOGIN)
                mSession!!.mIsSessionId = true
                mSession!!.mSessionId = sessionKey
                mSession!!.mSignInWithGoogle = false
                mSession!!.mSignInWithFacebook = true
                val user = User(login, "")
                mSession!!.addUser(user)
                mSession!!.saveSession(this)
                // TODO: 7/28/17
                //avoid memory leak
                startActivity(Intent(this,
                    FotkiTabActivity::class.java))
                this@LoginActivity.finish()
                WebManager.instance.getAccountInfo(baseContext, this)
            }
        } catch (e: JSONException) {
            CrashLogger.sendCrashLog(CrashLogger.LOGIN_ACTIVITY_LEVEL,e.message!!)
            e.printStackTrace()
        }

    }
    private fun saveLoginWithFaceBookSession(jsonObject: JSONObject) {
        try {
            val userName = jsonObject.getString(Constants.LOGIN)
            val sessionKey = jsonObject.getString(Constants.SESSION_ID)
            mSession!!.mIsSessionId = true
            mSession!!.mSessionId = sessionKey
            mSession!!.mSignInWithGoogle = false
            mSession!!.mSignInWithFacebook = true
            val user = User(userName, "")
            mSession!!.addUser(user)
            mSession!!.saveSession(this)
            startActivity(Intent(this,
                FotkiTabActivity::class.java))

            this@LoginActivity.finish()
            WebManager.instance.getAccountInfo(baseContext, this)
        } catch (e: JSONException) {
            CrashLogger.sendCrashLog(CrashLogger.LOGIN_ACTIVITY_LEVEL,e.message!!)
            e.printStackTrace()
        }

    }
    private fun parseFaceBookUser(jsonObject: JSONObject) {
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
                val faceBookLogin =
                    SocialLoginDetail(
                        login, avatar,
                        mUserFaceBookEmail!!,
                        mFacebookAccessToken!!,
                        date,
                        unix_Date,
                        spaceUsed,
                        active,
                        suspended
                    )
                mSocialLogins.add(faceBookLogin)
            }
            //todo show custom alert dialog and handle cases
            showFaceBookOptionAlertDialog(mSocialLogins)
        } catch (e: JSONException) {
            CrashLogger.sendCrashLog(CrashLogger.LOGIN_ACTIVITY_LEVEL,e.message!!)
            e.printStackTrace()
        }

    }
    @SuppressLint("InflateParams")
    private fun showFaceBookOptionAlertDialog(faceboolLogins: ArrayList<SocialLoginDetail>) {
        alertDialog = Dialog(this@LoginActivity)
        val inflater = layoutInflater
        val convertView = inflater.inflate(R.layout.facebook_signin_dialog_view, null) as View
        val textView = convertView.findViewById<TextView>(R.id.text_selection)
        val str1 = "Here what we find with the "
        val str2 = mUserFaceBookEmail
        val str3 = " email Please choose your fotki account you want to log in with"
        val str = SpannableStringBuilder(str1 + str2 + str3)
        str.setSpan(StyleSpan(Typeface.BOLD), str1.length, str1.length + str2!!.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        str.setSpan(UnderlineSpan(), str1.length, str1.length + str2.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = str
        alertDialog.setContentView(convertView)
        facebookSignInListView = convertView.findViewById(R.id.facebookSignIn)
        val faceBookSignInDialogAdaptern = FaceBookSignInDialogAdapter(
            this,
            R.layout.facebook_signin_dialog_view, faceboolLogins)

        facebookSignInListView.adapter = faceBookSignInDialogAdaptern
        alertDialog.show()
        setFaceBookSignInListViewListner()
    }
    private fun setFaceBookSignInListViewListner() {
        facebookSignInListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val faceBookLogin = mSocialLogins[position]
            if (faceBookLogin.active == 1) {
                if (faceBookLogin.suspended == 0) {
                    //call api...
                    mUtility.showProgressDialog(this@LoginActivity,
                        this@LoginActivity.getString(R.string.text_progress_bar_wait))
                    WebManager.instance.makeFacebookLoginWithSelectedAccount(baseContext,
                        this@LoginActivity, faceBookLogin.mAccessToken,
                        faceBookLogin.mUserName)
                } else {
                    //show account is suspended
                    alertDialog.dismiss()
                    mUtility.showAlertDialog(this@LoginActivity, "You account has been suspended by Admin. Please contact with him.")
                }

            } else {
                //show user is not active...
                alertDialog.dismiss()
                mUtility.showAlertDialog(this@LoginActivity, "You account has been deleted by Admin. Please contact with him.")
            }
        }
    }

    //----------------------------------------------------------------------------------------Google
    private fun registerGoogleSignUPDetails() {
        val gso = GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(Scope(Scopes.PROFILE))
                .requestServerAuthCode(Constants.GOOGLE_AUTH_KEY_WEB_PRODUCTION, false)
                .requestIdToken(Constants.GOOGLE_AUTH_KEY_WEB_PRODUCTION)
                .requestEmail()
                .build()
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this@LoginActivity, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()
        btnSignInGoogle.setSize(SignInButton.SIZE_STANDARD)
        btnSignInGoogle.setOnClickListener(this)
    }
    private fun signIn() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
       // mGoogleApiClient.connect();
        Log.d("signinError","signIn ")
    }
    private fun handleSignInResult(result: GoogleSignInResult) {

        Log.d(TAG, "handleSignInResult:" + result.isSuccess)
        if (result.isSuccess) {
            val acct = result.signInAccount
            if (acct != null) {
                mUserGoogleEmail = acct.email
                RetrieveTokenTask().execute(mUserGoogleEmail)
            }
        } else {
            Log.d("signinError","handleSignInResult else ${result.status} rzlt  ")
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
                startActivity(Intent(this,
                    FotkiTabActivity::class.java))

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
            startActivity(Intent(this,
                FotkiTabActivity::class.java))
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
        Log.d("signinError","showGoogleOptionAlertDialog ")
        alertDialog = Dialog(this@LoginActivity)
        val inflater = layoutInflater
        val convertView = inflater.inflate(R.layout.google_signin_dialog_view, null) as View
        val textView = convertView.findViewById<TextView>(R.id.text_selection)
        val str1 = "Here what we find with the "
        val str2 = mUserGoogleEmail
        val str3 = " email Please choose your fotki account you want to log in with"
        val str = SpannableStringBuilder(str1 + str2 + str3)
        str.setSpan(StyleSpan(Typeface.BOLD), str1.length, str1.length + str2!!.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        str.setSpan(UnderlineSpan(), str1.length, str1.length + str2.length,
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
            Log.d("signinError","googleSignInListView.onItemClickListener  ")

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

    //---------------------------------------------------------------------------------Web-interface
    private fun getSessionFromResponse(`object`: JSONObject) {
        var `object` = `object`
        mUtility.dismissProgressDialog()
        try {
            if (`object`.getInt(Constants.OK) == 1) {
                `object` = `object`.getJSONObject(Constants.DATA)
                val sessionKey = `object`.getString(Constants.SESSION_ID)
                mSession!!.mIsSessionId = true
                mSession!!.mSessionId = sessionKey
                mSession!!.mSignInWithGoogle = false
                mSession!!.mSignInWithFacebook = false
                val user = User(
                    etUserName.text.toString(),
                    etUserPassword.text.toString()
                )
                mSession!!.addUser(user)
                mSession!!.saveSession(this)
                startActivity(Intent(this,
                    FotkiTabActivity::class.java))

                this@LoginActivity.finish()
                WebManager.instance.getAccountInfo(baseContext, this)
            } else {
                mUtility.fotkiAppErrorLogs(TAG, `object`.getString(Constants.API_MESSAGE))
                mUtility.showAlertDialog(this, `object`.getString(Constants.API_MESSAGE))
                CrashLogger.sendCrashLog(
                    CrashLogger.LOGIN_ACTIVITY_LEVEL,
                    `object`.getString(Constants.API_MESSAGE))
            }
        } catch (e: JSONException) {
            CrashLogger.sendCrashLog(CrashLogger.LOGIN_ACTIVITY_LEVEL,e.message!!)
            e.printStackTrace()
        }

    }
    private fun hideViews() {
        llInternetNotAvailable.visibility = View.VISIBLE
        mUtility.dismissProgressDialog()
    }
    override fun sendNetworkFailure(isInterNetAvailableFlag: Boolean, apiRequestType: ApiRequestType) {
        CrashLogger.sendCrashLog(CrashLogger.LOGIN_ACTIVITY_LEVEL,"Network error!")
        Utility.instance.showAlertDialog(this,"Network error!")
        hideViews()
    }
    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, response.toString())
        }
        when {
            apiRequestType === NEW_SESSION_API -> getSessionFromResponse(response)
            apiRequestType === GOOGLE_SIGNIN_API -> getNewGoogleApiResponse(response)
            apiRequestType === GOOGLE_SIGIN_LOGIN_API -> getSelectedGoogleAccountResponse(response)
            apiRequestType === FACEBOOK_SIGNIN_API -> getNewFaceBookApiResponse(response)
            apiRequestType === FACEBOOK_SIGIN_LOGIN_API -> getSelectedFaceBookAccountResponse(response)
        }
    }
    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {
        if (BuildConfig.DEBUG) Log.d(TAG, error.toString())
        var message = " "
        mUtility.dismissProgressDialog()
        message = when (error) {
            is NetworkError -> getString(R.string.network_not_found)
            is ServerError -> getString(R.string.server_error)
            is AuthFailureError -> getString(R.string.auth_failure)
            is ParseError -> getString(R.string.parse_error)
            is TimeoutError -> getString(R.string.time_out_error)
            else -> "" /*getString(R.string.network_not_found)*/
        }
        val errorStr = "\n Network response: ${error.networkResponse} message - ${error.message}"
        mUtility.showAlertDialog(this@LoginActivity, message+errorStr)
        CrashLogger.sendCrashLog(CrashLogger.LOGIN_ACTIVITY_LEVEL,message+errorStr)
        //Utility.instance.showAlertDialog(this,message)
    }
    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        val errorStr = "\n Network response: ${connectionResult.errorCode} message - " +
                "${connectionResult.errorMessage}"

        Log.d(TAG, "onConnectionFailed:$connectionResult")
        CrashLogger.sendCrashLog(
            CrashLogger.LOGIN_ACTIVITY_LEVEL,
            "onConnectionFailed:$connectionResult"
        )
        Utility.instance.showAlertDialog(this,
            "onConnectionFailed:$connectionResult $errorStr")
    }

    companion object {
        private var TAG = LoginActivity::class.java.name
        private val RC_SIGN_IN = 9001
    }

}

