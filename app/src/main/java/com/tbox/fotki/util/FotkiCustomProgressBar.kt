package com.tbox.fotki.util

import android.graphics.Canvas
import android.widget.ImageView
import android.widget.ProgressBar

import com.facebook.drawee.drawable.ProgressBarDrawable
import com.tbox.fotki.model.entities.ParcelableItem

/**
* Created by Junaid on 4/26/17.
*/

class FotkiCustomProgressBar(internal var mFoktiImageLoaderInterface: FoktiImageLoaderInterface,
                             private val mProgressBar: ProgressBar, private val mImageView: ImageView, internal var mItem: ParcelableItem
) : ProgressBarDrawable() {

    override fun onLevelChange(level: Int): Boolean {
        invalidateSelf()
        if (level == 10000) {
            mFoktiImageLoaderInterface.sendImageLoadingSuccess(mProgressBar, mImageView, this.mItem)
        }
        return true
    }

    override fun draw(canvas: Canvas) {

    }
}

