package com.tbox.fotki.util.upload_files

/**
 * Created by Junaid on 5/16/17.
 */
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FileType(
    @SerializedName("file_path") var mFilePath: String,
    @SerializedName("mime_type") var mFileMimeType: String,
    @SerializedName("album_id") var albumId: Long,
    @SerializedName("album_name") var albumName: String,
    @SerializedName("sha1") val sha1: String
) : Parcelable {
    constructor(mFilePath: String, mFileMimeType: String) : this(
        mFilePath,
        mFileMimeType,
        0L,
        "",
        ""
    )

    constructor(mFilePath: String) : this(mFilePath, "")
}
