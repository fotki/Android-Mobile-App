package com.tbox.fotki.model.database

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity
@Parcelize
data class FilesEntity(
    val fileName:String,
    val mimeType:String,
    var folder:String,
    var albumId:Long,
    var loadedStatus:Int,
    val hashSHA:String
) : Parcelable {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    constructor():this("","","",0L, STATUS_NOT_LOADED,"")

    companion object{
        const val STATUS_NOT_CHECK_FOLDER = 4
        const val STATUS_ONLY_ADDED = 3
        const val DELETED = 2
        const val STATUS_NOT_LOADED = 0
        const val LOADED = 1
    }
}