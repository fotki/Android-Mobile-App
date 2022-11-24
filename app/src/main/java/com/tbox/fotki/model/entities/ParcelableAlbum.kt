package com.tbox.fotki.model.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Created by Junaid on 8/29/17.
 */
@Parcelize
data class ParcelableAlbum(
    var mAlbumIdEnc: Long,
    var mdescription: String,
    var mName: String,
    var mCoverUrl: String,
    var mitem: ArrayList<ParcelableItem>,
    var mShareUrl: String
) : Parcelable