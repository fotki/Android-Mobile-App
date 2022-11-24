package com.tbox.fotki.model.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Created by Junaid on 8/29/17.
 */
@Parcelize
data class ParcelableItem(
    var mAlbumIdEnc: Long,
    var mThumbnailUrl: String,
    var mCreated: String,
    var mViewUrl: String,
    var mOriginalUrl: String,
    var mTitle: String,
    var mIsVideo: Boolean,
    var mVideoUrl:String,
    var mId: Long,
    var mInaccessable:Int = 0,
    var mOriginalFilename:String = "",
    var mShortUrl:String = ""
) : Parcelable
