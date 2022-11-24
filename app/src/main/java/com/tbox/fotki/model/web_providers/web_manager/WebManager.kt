package com.tbox.fotki.model.web_providers.web_manager

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.tbox.fotki.view.activities.LoginActivity
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.model.entities.ApiRequestType.*
import com.tbox.fotki.model.entities.Session
import com.tbox.fotki.BuildConfig
import com.tbox.fotki.R
import com.tbox.fotki.model.database.FilesEntity
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import org.json.JSONArray
import org.json.JSONException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

@Suppress("DEPRECATION", "NAME_SHADOWING")
/**
* Created by Junaid on 4/6/17.
*/

class WebManager {
    private var mWebManagerInterface: WebManagerInterface?=null
    private var mUserName: String?=null
    private var mPassword: String?=null
    private var mAlbumID: Long?=0L
    private var mFolderID: Long?=0L
    private var mPhotoId: Long? = 0L
    private var mFolderName: String?=null
    private var mAlbumName: String?=null
    private var mPage=0
    private var apiCallingCount=0
    private var REQUEST_URL: Enum<*>?=null
    private var mFolderDesc: String?=null
    private var mAlbumDesc: String?=null
    private var mAccessToken: String?=null


    private fun isConnectingToInternet(context: Context): Boolean {
        val connectivityManager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw      = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                //for check internet over Bluetooth
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val networks=connectivityManager.allNetworks
            var networkInfo: NetworkInfo
            for (mNetwork in networks) {
                networkInfo= connectivityManager.getNetworkInfo(mNetwork)!!
                if (networkInfo.state == NetworkInfo.State.CONNECTED) {
                    return true
                }
            }
        } else {

            val info=connectivityManager.allNetworkInfo
            info?.filter { it.state == NetworkInfo.State.CONNECTED }?.forEach { return true }
        }
        return false
    }

    private fun parseToUtf8Format(chunk: String?): String? {
        var chunk=chunk
        val mTemp=chunk
        chunk = try {
            URLEncoder.encode(chunk, Constants.UTF_8_FORMAT)
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            mTemp
        }
        return chunk
    }

    private fun checkApiToCall(context: Context, apiRequestType: ApiRequestType) {
        when (apiRequestType) {
            NEW_SESSION_API -> makeLogin(context, this.mWebManagerInterface, this.mUserName, this.mPassword)
            GET_ACCOUNT_INFO_API -> getAccountInfo(context, this.mWebManagerInterface)
            GET_ACCOUNT_TREE_API -> getAccountTree(context, this.mWebManagerInterface)
            GET_ALBUM_CONTENT -> getAlbumContentWithPage(context, this.mWebManagerInterface, this.mAlbumID,
                    this.mPage)
            GET_ALBUM_ITEM_COUNT -> getAlbumItemCount(context, this.mWebManagerInterface, this.mAlbumID)
            CREATE_FOLDER_API -> createFolder(context, this.mWebManagerInterface, this.mFolderID, this.mFolderName)
            GET_FOLDER_CONTENT -> getFolderContent(context, this.mWebManagerInterface, this.mFolderID)
            UPDATE_FOLDER -> updateFolder(context, this.mWebManagerInterface, this.mFolderID, this.mFolderName,
                    this.mFolderDesc)
            CREATE_ALBUM_API -> createAlbum(context, this.mWebManagerInterface, this.mFolderID, this.mAlbumName)
            UPDATE_ALBUM -> updateAlbum(context, this.mWebManagerInterface, this.mAlbumID, this.mAlbumName,
                    this.mAlbumDesc)
            GOOGLE_SIGNIN_API -> makeGoogleLogin(context, mWebManagerInterface, mAccessToken)
            GOOGLE_SIGIN_LOGIN_API -> makeGoogleLoginWithSelectedAccount(context, mWebManagerInterface, mAccessToken,
                    mUserName)
            FACEBOOK_SIGNIN_API -> makeFaceBookLogin(context, mWebManagerInterface, mAccessToken)
            FACEBOOK_SIGIN_LOGIN_API -> makeFacebookLoginWithSelectedAccount(context, mWebManagerInterface, mAccessToken,
                    mUserName)
            else -> {
            }
        }
    }

    //make login web api calling method
    fun makeLogin(context: Context, wMInterface: WebManagerInterface?, username: String?,
                  password: String?) {
        this.mWebManagerInterface=wMInterface
        this.mUserName=username
        this.mPassword=password
        val url=
            Constants.BASE_URL + Constants.NEW_SESSION + Constants.LOGIN + "=" + username + "&"+ Constants.PASSWORD + "=" + password
        makeWebRequest(context, url, NEW_SESSION_API)
    }

    fun makeGoogleLogin(context: Context, wManagerInterface: WebManagerInterface?,
                        accesToken: String?) {
        this.mWebManagerInterface=wManagerInterface
        this.mAccessToken=accesToken
        val url=
            Constants.BASE_URL + Constants.GOOGLE_NEW_SESSION + "&" + Constants.ACCESS_TOKEN+"=" + accesToken
        makeWebRequest(context, url, GOOGLE_SIGNIN_API)
    }

    fun makeGoogleLoginWithSelectedAccount(context: Context,
                                           wManagerInterface: WebManagerInterface?,
                                           accessToken: String?, userName: String?) {
        this.mWebManagerInterface=wManagerInterface
        this.mAccessToken=accessToken
        this.mUserName=userName
        val url=
            Constants.BASE_URL + Constants.GOOGLE_NEW_SESSION + "&" + Constants.ACCESS_TOKEN+"=" + accessToken + "&" + Constants.SOCIAL_MEDIA_LOGIN_KEY + "="+userName
        makeWebRequest(context, url, GOOGLE_SIGIN_LOGIN_API)
    }

    fun makeFaceBookLogin(context: Context, wManagerInterface: WebManagerInterface?,
                          accesToken: String?) {
        this.mWebManagerInterface=wManagerInterface
        this.mAccessToken=accesToken
        val url=
            Constants.BASE_URL + Constants.FACEBOOK_NEW_SESSION + "&" + Constants.ACCESS_TOKEN+"=" + accesToken
        makeWebRequest(context, url, FACEBOOK_SIGNIN_API)
    }

    fun makeFacebookLoginWithSelectedAccount(context: Context,
                                             wManagerInterface: WebManagerInterface?,
                                             accessToken: String?, userName: String?) {
        this.mWebManagerInterface=wManagerInterface
        this.mAccessToken=accessToken
        this.mUserName=userName
        val url=
            Constants.BASE_URL + Constants.FACEBOOK_NEW_SESSION + "&" + Constants.ACCESS_TOKEN+"=" + accessToken + "&" + Constants.SOCIAL_MEDIA_LOGIN_KEY + "="+userName
        makeWebRequest(context, url, FACEBOOK_SIGIN_LOGIN_API)
    }

    //get account information web api calling method
    fun getAccountInfo(context: Context, wMInterface: WebManagerInterface?) {
        this.mWebManagerInterface=wMInterface
        val sessionId=
            Session.getInstance(context).mSessionId
        val url=
            Constants.BASE_URL + Constants.GET_ACCOUNT_INFO + Constants.SESSION_ID + "="+sessionId
        makeWebRequest(context, url, GET_ACCOUNT_INFO_API)
    }

    //get account tree method web api calling method
    fun getAccountTree(context: Context, wMInterface: WebManagerInterface?) {
        this.mWebManagerInterface=wMInterface
        val sessionId=
            Session.getInstance(context).mSessionId
        val url=
            Constants.BASE_URL + Constants.GET_ACCOUNT_TREE + Constants.SESSION_ID + "="+sessionId
        makeWebRequest(context, url, GET_ACCOUNT_TREE_API)
    }

    //get album item count api
    fun getAlbumItemCount(context: Context, wMInterface: WebManagerInterface?,
                          albumID: Long?) {
        this.mWebManagerInterface=wMInterface
        this.mAlbumID=albumID
        val sessionId=
            Session.getInstance(context).mSessionId
        val url=
            Constants.BASE_URL + Constants.GET_ALBUM_ITEM_COUNT + Constants.SESSION_ID +
                "=" + sessionId + "&" + Constants.ALBUM_ID_ENC + "=" + albumID

        L.print(this,"url getAlbumItemCount - $url")
        makeWebRequest(context, url, GET_ALBUM_ITEM_COUNT)
    }

    /*http://api.dev.fotki.com:8786/v3/is_photo_exists
    * session_id=<param>
    in_account=true
    sha1=<param>
    * */
    //get album content api
    fun isPhotoExists(context: Context, wMInterface: WebManagerInterface?,
                      sha1: String?) {
        this.mWebManagerInterface=wMInterface
        val sessionId=
            Session.getInstance(context).mSessionId
        val files = JSONArray()
        files.put(sha1)
        val params = HashMap<String,String>()
        params[Constants.SHA1] = files.toString()
        params[Constants.SESSION_ID] = sessionId!!

        val url=
            Constants.BASE_URL + Constants.IS_PHOTO_EXISTS+
                Constants.SESSION_ID+"="+sessionId
                "&" + Constants.SHA1 + "=" + files.toString()

        makePostWebRequest(context, url, params, IS_EXISTS)
    }

    fun isPhotoExists(context: Context, wMInterface: WebManagerInterface?,
                      shaArray: List<FilesEntity>?) {
        this.mWebManagerInterface=wMInterface
        val sessionId=
            Session.getInstance(context).mSessionId
        var files = ""
        for (sha in shaArray!!){
            files+="&sha1="+sha.hashSHA
        }

        val params = HashMap<String,String>()
        params[Constants.SHA1] = files
        params[Constants.SESSION_ID] = sessionId!!

        val url=
            Constants.BASE_URL + Constants.IS_PHOTO_EXISTS+
                Constants.SESSION_ID+"="+sessionId + files

        makePostWebRequest(context, url, params, IS_EXISTS)
    }

    //get album content api
    fun getAlbumContentWithPage(context: Context, wMInterface: WebManagerInterface?,
                                albumID: Long?, page: Int) {
        this.mWebManagerInterface=wMInterface
        this.mAlbumID=albumID
        this.mPage=page
        val sessionId=
            Session.getInstance(context).mSessionId
        val url=
            Constants.BASE_URL + Constants.GET_ALBUM_CONTENT + Constants.SESSION_ID +
                "=" + sessionId + "&" + Constants.ALBUM_ID_ENC + "=" + albumID + "&"+ Constants.CURRENT_PAGE + "=" + page
        L.print(this,"url getAlbumContentWithPage - $url")
        makeWebRequest(context, url, GET_ALBUM_CONTENT)
    }

    fun createFolder(context: Context, wMInterface: WebManagerInterface?, folderID: Long?,
                     folderName: String?) {
        this.mWebManagerInterface=wMInterface
        this.mFolderID=folderID
        this.mFolderName=folderName
        this.mFolderName=parseToUtf8Format(this.mFolderName)
        val sessionId=
            Session.getInstance(context).mSessionId
        val url=
            Constants.BASE_URL + Constants.CREATE_FOLDER_API + Constants.SESSION_ID +
                "=" + sessionId + "&" + Constants.FOLDER_ID_ENC + "=" + folderID + "&"+ Constants.CREATE_FOLDER_NAME + "=" + mFolderName
        makeWebRequest(context, url, CREATE_FOLDER_API)
    }

    fun removeImage(context: Context, wMInterface: WebManagerInterface?, albumId: Long?,
                     photoId: Long?) {
        this.mWebManagerInterface=wMInterface
        this.mAlbumID=albumId
        this.mPhotoId=photoId
        val sessionId=
            Session.getInstance(context).mSessionId
        val url=
            Constants.BASE_URL + Constants.DELETE_MEDIA_API + Constants.SESSION_ID +
                    "=" + sessionId + "&" + Constants.ALBUM_ID_ENC + "=" + albumId + "&"+
                    Constants.MEDIA_ID_ENC + "=" + mPhotoId
        makeWebRequest(context, url, DELETE_MEDIA_API)
    }

    fun getFolderContent(context: Context, wMInterface: WebManagerInterface?, folderID: Long?) {
        this.mWebManagerInterface=wMInterface
        this.mFolderID=folderID
        val sessionId=
            Session.getInstance(context).mSessionId
        val url=
            Constants.BASE_URL + Constants.GET_FOLDER_CONTENT + Constants.SESSION_ID +
                "=" + sessionId + "&" + Constants.FOLDER_ID_ENC + "=" + folderID +"&level=1"
        makeWebRequest(context, url, GET_FOLDER_CONTENT)
    }

    fun updateFolder(context: Context, wMInterface: WebManagerInterface?,
                     folderID: Long?, name: String?, folderDesc: String?) {
        this.mWebManagerInterface=wMInterface
        this.mFolderID=folderID
        this.mFolderName=name
        this.mFolderDesc=folderDesc
        this.mFolderName=parseToUtf8Format(mFolderName)
        this.mFolderDesc=parseToUtf8Format(mFolderDesc)
        val sessionId=
            Session.getInstance(context).mSessionId
        val url=
            Constants.BASE_URL + Constants.UPDATE_FOLDER + Constants.SESSION_ID +
                "=" + sessionId + "&" + Constants.FOLDER_ID_ENC + "=" + mFolderID + "&"+ Constants.UPDATE_ITEM_NAME + "=" + mFolderName + "&"+ Constants.UPDATE_FOLDER_DESC + "=" + mFolderDesc
        makeWebRequest(context, url, UPDATE_FOLDER)
    }

    fun createAlbum(context: Context, wMInterface: WebManagerInterface?, folderID: Long?,
                    albumName: String?) {
        this.mWebManagerInterface=wMInterface
        this.mFolderID=folderID
        this.mAlbumName=albumName
        this.mAlbumName=parseToUtf8Format(mAlbumName)
        val sessionId=
            Session.getInstance(context).mSessionId
        val url=
            Constants.BASE_URL + Constants.CREATE_ALBUM_API + Constants.SESSION_ID +
                "=" + sessionId + "&" + Constants.FOLDER_ID_ENC + "=" + folderID + "&"+ Constants.CREATE_ALBUM_NAME + "=" + mAlbumName
        makeWebRequest(context, url, CREATE_ALBUM_API)
    }

    fun updateAlbum(context: Context, wMInterface: WebManagerInterface?, albumID: Long?,
                    albumName: String?, albumDesc: String?) {
        this.mWebManagerInterface=wMInterface
        this.mAlbumID=albumID
        this.mAlbumName=albumName
        this.mAlbumDesc=albumDesc
        this.mAlbumName=parseToUtf8Format(mAlbumName)
        this.mAlbumDesc=parseToUtf8Format(mAlbumDesc)
        val sessionId=
            Session.getInstance(context).mSessionId
        val url=
            Constants.BASE_URL + Constants.UPDATE_ALBUM + Constants.SESSION_ID +
                "=" + sessionId + "&" + Constants.ALBUM_ID_ENC + "=" + mAlbumID + "&"+ Constants.UPDATE_ITEM_NAME + "=" + mAlbumName + "&"+ Constants.UPDATE_ALBUM_DESC + "=" + mAlbumDesc
        makeWebRequest(context, url, UPDATE_ALBUM)
    }

    //make web request should get method context, url, and api name to call
    private fun makeWebRequest(context: Context, url: String,
                               apiRequestType: ApiRequestType
    ) {
        if (BuildConfig.DEBUG) Log.d(TAG + " url is", url)
        if (isConnectingToInternet(context)) {
            val jsObjRequest=JsonObjectRequest(Request.Method.GET, url, null, Response.Listener { response ->
                try {
                    if (apiCallingCount <= 3) {
                        if (response.getInt(Constants.OK) == 1) {
                            mWebManagerInterface!!.sendSuccess(response,
                                    apiRequestType)
                        } else {
                            if (response.getString(Constants.API_MESSAGE) == context.getString(
                                    R.string.wrong_setting_id_message)) {
                                val session=
                                    Session.getInstance(context)
                                session.removeSessionInfo(context)

                                val intent=Intent(context,
                                    LoginActivity::class.java)
                                intent.flags=Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            } else {
                                apiCallingCount++
                                REQUEST_URL=apiRequestType
                                Log.e(TAG + "count", apiCallingCount.toString() + "")
                                checkApiToCall(context, (REQUEST_URL as ApiRequestType?)!!)
                            }
                        }
                    } else {
                        mWebManagerInterface!!.sendSuccess(response, apiRequestType)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }, Response.ErrorListener { error ->
                if (apiCallingCount < 3) {
                    apiCallingCount++
                    makeWebRequest(context, url, apiRequestType)
                } else {
                    mWebManagerInterface!!.sendFailure(error, apiRequestType)
                }
                Log.d("MEDIA_TAG","error - $error")

            })
            SingeltonRequestQueue.instance.addToRequestQueue(context, jsObjRequest)
        } else {
            mWebManagerInterface!!.sendNetworkFailure(false, apiRequestType)
        }
    }

    private fun makePostWebRequest(context: Context, url: String, jsonParams:Map<String, String>,
                               apiRequestType: ApiRequestType
    ) {
        if (BuildConfig.DEBUG)
            L.print(this, "$url $jsonParams")
        if (isConnectingToInternet(context)) {
           // val jsObjRequest=JsonObjectRequest(Request.Method.POST, url, JSONObject(jsonParams),
            val jsObjRequest=JsonObjectRequest(Request.Method.GET, url, null,

                Response.Listener { response ->
                try {
                    L.print(this,"res - $response")
                    if (apiCallingCount <= 3) {
                        if (response.getInt(Constants.OK) == 1) {
                            mWebManagerInterface!!.sendSuccess(response, apiRequestType)
                        } else {
                            if (response.getString(Constants.API_MESSAGE) == context.getString(
                                    R.string.wrong_setting_id_message)) {
                                val session=
                                    Session.getInstance(context)
                                session.removeSessionInfo(context)

                                val intent=Intent(context,
                                    LoginActivity::class.java)
                                intent.flags=Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)

                                //LoginActivity.launchAffinity(context)
                            } else {
                                apiCallingCount++
                                REQUEST_URL=apiRequestType
                                Log.e(TAG + "count", apiCallingCount.toString() + "")
                                checkApiToCall(context, (REQUEST_URL as ApiRequestType?)!!)
                            }
                        }
                    } else {
                        mWebManagerInterface!!.sendSuccess(response, apiRequestType)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }, Response.ErrorListener { error ->
                if (apiCallingCount < 3) {
                    apiCallingCount++
                    makeWebRequest(context, url, apiRequestType)
                } else {
                    mWebManagerInterface!!.sendFailure(error, apiRequestType)
                }
                Log.d("MEDIA_TAG","error - $error")

            })
            SingeltonRequestQueue.instance.addToRequestQueue(context, jsObjRequest)
        } else {
            mWebManagerInterface!!.sendNetworkFailure(false, apiRequestType)
        }
    }


    companion object {
        private val TAG=
            WebManager::class.java.name

        val instance: WebManager
            get()= WebManager()
    }
}


