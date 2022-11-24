package com.tbox.fotki.model.web_providers

import android.content.Context
import android.util.Log
import com.android.volley.VolleyError
import com.tbox.fotki.model.database.FilesEntity
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.model.entities.Folder
import com.tbox.fotki.util.Constants
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.util.sync_files.LogProvider
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat

class YearFolderTest(private val fileType: FilesEntity, val activity: Context) : BaseWebProvider(activity) {

    lateinit var successFunction: ()->Unit
    lateinit var errorFunction: ()->Unit

    private var yearFolderTitle:String =
        SimpleDateFormat(Folder.FOLDER_YEAR_TEMPLATE).format(File(fileType.fileName).lastModified())

    override fun start() {
        WebManager.instance.getAccountTree(context,this)
    }

    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
        Log.d("TAG_MEDIA", "res - $response")
        try {
            var respCurr = response.getJSONObject(Constants.DATA)
                .getJSONObject(Constants.ACCOUNT_TREE)
                .getJSONObject(Constants.PRIVATE_FOLDER)
                .getJSONArray(Constants.FOLDERS)

            for (i in 0 until respCurr.length()) {
                var currObject = respCurr.getJSONObject(i)
                if (currObject.getString(Constants.FOLDER_NAME) == Folder.BACKUP_FOLDER_NAME) {
                    //Log.d("TAG_MEDIA", "resp - $currObject folder - $folder")
                    val foldersArray = currObject.getJSONArray(Constants.FOLDERS)

                    var yearFolder: JSONObject? = null
                    for (i in 0 until foldersArray.length()) {
                        val folderCurr = foldersArray.getJSONObject(i)
                        if (folderCurr.getString(Constants.FOLDER_NAME) == yearFolderTitle) {
                            yearFolder = folderCurr
                        }
                    }

                    if (yearFolder == null) {
                        LogProvider.writeToFile("We did't find year folder. Try to create",
                            context
                        )
                        errorFunction.invoke()
                    } else {
                        LogProvider.writeToFile("We found year folder", context)
                        successFunction.invoke()
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {

    }

    override fun sendNetworkFailure(
        isInterNetAvailableFlag: Boolean,
        apiRequestType: ApiRequestType
    ) {
    }
}