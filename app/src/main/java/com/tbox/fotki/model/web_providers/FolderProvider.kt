package com.tbox.fotki.model.web_providers

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.volley.VolleyError
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.model.entities.Folder
import com.tbox.fotki.model.entities.ParcelableAlbum
import com.tbox.fotki.model.entities.ParcelableItem
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.model.web_providers.web_manager.WebManagerInterface
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class FolderProvider(context: Context) : BaseWebProvider(context) {

    val publicFolder = MutableLiveData<Folder?>()
    val privateFolder = MutableLiveData<Folder?>()
    val backupFolder = MutableLiveData<Folder?>()
    var data = ""

    constructor(context: Context, data: String) : this(context) {
        this.data = data
    }

   /* init {
        //start()
        *//*if (data.isEmpty()) {
            start()
        } else {
            populateFoldersFromAccountTree(JSONObject(data))
        }*//*
    }*/

    override fun start() {
        loadPublic()
        //WebManager.instance.getAccountTree(context, this)
    }

    fun loadPublic() {
        WebManager.instance.getFolderContent(context,object:WebManagerInterface{
            override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
                L.print(this,"Public - $response")
                val folder = loadFolder(response.getJSONObject(Constants.DATA))
                L.print(this,"folder public - $folder")
                publicFolder.value = folder
                //loadPrivate()
            }
            override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {}
            override fun sendNetworkFailure(isInterNetAvailableFlag: Boolean, apiRequestType: ApiRequestType) {
                publicFolder.postValue(null)
            }
        }, 4294967294L)
    }

    fun loadPrivate() {
        WebManager.instance.getFolderContent(context,object:WebManagerInterface{
            override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
                val folder = loadFolder(response.getJSONObject(Constants.DATA))
                L.print(this,"resp - $response")
                L.print(this,"folder private - $folder")
                privateFolder.value = folder
                for (dir in folder.mFolders) {
                    if (dir.mFolderName == "Background upload" &&
                        response.getJSONObject("data").getString("url_p") == "private"
                    ) {
                        L.print(this,"Background founded!")
                        L.print(this,"folder backup - $dir")
                        loadBackup(dir)
                    }
                }
            }
            override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {}
            override fun sendNetworkFailure(isInterNetAvailableFlag: Boolean, apiRequestType: ApiRequestType) {
                publicFolder.postValue(null)
            }
        }, 4294967293L)
    }

    private fun loadBackup(dir:Folder) {
        WebManager.instance.getFolderContent(context,object:WebManagerInterface{
            override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
                val folder = loadFolder(response.getJSONObject(Constants.DATA))
                L.print(this,"resp - $response")
                L.print(this,"folder backup - $folder")
                backupFolder.value = folder
            }
            override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {}
            override fun sendNetworkFailure(isInterNetAvailableFlag: Boolean, apiRequestType: ApiRequestType) {
                publicFolder.postValue(null)
            }
        }, dir.mFolderIdEnc)
    }

    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
        super.sendSuccess(response, apiRequestType)

        /*res.value = response

        L.print(this, "History3 - $response")
        PreferenceHelper(context).applyPrefs(
            hashMapOf(FotkiTabActivityViewModel.STORED_ALBUMS to response.toString())
        )

        populateFoldersFromAccountTree(response)*/
    }

    override fun sendNetworkFailure(
        isInterNetAvailableFlag: Boolean,
        apiRequestType: ApiRequestType
    ) {
        super.sendNetworkFailure(isInterNetAvailableFlag, apiRequestType)
        publicFolder.value = null
        privateFolder.value = null
    }

    private fun parseAlbum(jsonArray: JSONArray): ArrayList<ParcelableAlbum> {
        val mAlbumArray = ArrayList<ParcelableAlbum>()
        for (i in 0 until jsonArray.length()) {
            val newList = ArrayList<ParcelableItem>()
            val album: ParcelableAlbum
            album = ParcelableAlbum(
                0,
                "",
                "",
                "",
                newList,
                ""
            )
            try {
                album.mAlbumIdEnc = jsonArray.getJSONObject(i).getLong(Constants.ALBUM_ID_ENC)
                album.mName = jsonArray.getJSONObject(i).getString(Constants.ALBUM_NAME)
                album.mdescription = jsonArray.getJSONObject(i).getString(Constants.DESCRICTION)
                album.mCoverUrl = jsonArray.getJSONObject(i).getString(Constants.COVER_PHOTO_URL)
                album.mShareUrl = jsonArray.getJSONObject(i).optString(Constants.SHARE_URL)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            mAlbumArray.add(album)
        }
        return mAlbumArray
    }

    private fun parseFolder(jsonArray: JSONArray): ArrayList<Folder> {
        val mFoldersArray = ArrayList<Folder>()
        for (i in 0 until jsonArray.length()) {
            val mFolder = Folder()
            try {
                mFolder.setData(
                    jsonArray.getJSONObject(i).getLong(Constants.FOLDER_ID_ENC),
                    jsonArray.getJSONObject(i).getLong(Constants.NUMBER_OF_FOLDER),
                    jsonArray.getJSONObject(i).getLong(Constants.NUMBER_OF_ALBUMS),
                    //0,
                    //0,
                    jsonArray.getJSONObject(i).getString(Constants.DESCRICTION),
                    jsonArray.getJSONObject(i).getString(Constants.FOLDER_NAME),
                    ArrayList<ParcelableAlbum>(),
                    ArrayList<Folder>(),
                    //parseAlbum(jsonArray.getJSONObject(i).getJSONArray(Constants.ALBUMS)),
                    //parseFolder(jsonArray.getJSONObject(i).getJSONArray(Constants.FOLDERS)),
                    jsonArray.getJSONObject(i).optString(Constants.SHARE_URL)
                )
                mFoldersArray.add(mFolder)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        return mFoldersArray
    }

    private fun loadFolder(response: JSONObject): Folder {
        val folder = Folder()
        try {
            folder.setData(
                response.getLong(Constants.FOLDER_ID_ENC),
                response.getLong(Constants.NUMBER_OF_FOLDER),
                response.getLong(Constants.NUMBER_OF_ALBUMS),
                response.getString(Constants.DESCRICTION),
                response.getString(Constants.FOLDER_NAME),
                parseAlbum(response.getJSONArray(Constants.ALBUMS)),
                parseFolder(response.getJSONArray(Constants.FOLDERS)),
                response.getString(Constants.SHARE_URL)
            )

            for (dir in folder.mFolders) {
                if (dir.mFolderName == "Background upload" &&
                    response.getString("url_p") == "private"
                ) {
                    L.print(this,"Background founded!")
                    backupFolder.value = dir
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return folder
    }

    private fun populateFoldersFromAccountTree(res: JSONObject?) {
        Log.d("TAG_API", "res - $res")
        res?.let {
            try {
                val response = it.getJSONObject(Constants.DATA).getJSONObject(
                    Constants.ACCOUNT_TREE
                )
                privateFolder.value = loadFolder(response.getJSONObject(Constants.PRIVATE_FOLDER))
                publicFolder.value = loadFolder(response.getJSONObject(Constants.PUBLIC_FOLDER))
                mUtility.dismissProgressDialog()
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
    }
}