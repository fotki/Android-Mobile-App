package com.tbox.fotki.model.entities

import android.content.Context
import android.os.Parcelable
import android.preference.PreferenceManager
import com.tbox.fotki.util.upload_files.FileType
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

@Parcelize
data class UploadProperty(
    var albumName: String,
    var fileTypes: ArrayList<FileType>,
    var sessionId: String,
    var albumId: Long,
    var itemCounter: Int,
    var isDelete: Boolean,
    var loadedSize: Long,
    var isUploading: Boolean,
    var isFinished: Boolean,
    var isCompressionAllowed:Boolean
) : Parcelable {

    constructor() :
            this("", ArrayList<FileType>(), "", 0L, 0,
                false, 0L, false, true, false)

    fun toPreferences(context: Context) {
        val spEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        spEditor.putString("album_name", albumName).commit()
        val filesBuffer = JSONArray()
        for (fileType in fileTypes) {
            val obj = JSONObject()
            obj.put("path", fileType.mFilePath)
            obj.put("type", fileType.mFileMimeType)
            filesBuffer.put(obj)
        }

        spEditor.putLong("uploaded_size", loadedSize)
        spEditor.putString("files", filesBuffer.toString()).commit()
        spEditor.putString("session_id", sessionId).commit()
        spEditor.putLong("album_id", albumId).commit()
        spEditor.putInt("item_counter", itemCounter).commit()
        spEditor.putBoolean("is_delete", isDelete).commit()
        spEditor.putBoolean("is_uploading", isUploading).commit()
        spEditor.putBoolean("is_finished", isFinished).commit()
        spEditor.putBoolean("is_compress",isCompressionAllowed).commit()
    }

    fun fromPreferences(context: Context) {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        albumName = sp.getString("album_name", "")!!
        fileTypes = ArrayList()
        val filesBuffer = JSONArray(sp.getString("files", "[]"))
        for (i in 0 until filesBuffer.length()) {
            val obj = filesBuffer.get(i) as JSONObject
            val fileType = FileType(obj.getString("path"), obj.getString("type"))
            fileTypes.add(fileType)
        }
        loadedSize = sp.getLong("uploaded_size", 0L)
        sessionId = sp.getString("session_id", "")!!
        albumId = sp.getLong("album_id", 0)
        itemCounter = sp.getInt("item_counter", 0)
        isDelete = sp.getBoolean("is_delete", false)
        isUploading = sp.getBoolean("is_uploading", false)
        isFinished = sp.getBoolean("is_finished", true)
        isCompressionAllowed = sp.getBoolean("is_compress", false)
    }

    override fun toString(): String {
        return "UploadProperty(albumName='$albumName', sessionId='$sessionId', albumId=$albumId," +
                " itemCounter=$itemCounter, isDelete=$isDelete, loadedSize=$loadedSize, " +
                "isUploading=$isUploading, isFinished=$isFinished, isCompress=$isCompressionAllowed)"
    }


}