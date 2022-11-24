package com.tbox.fotki.util

import android.widget.ImageView
import android.widget.ProgressBar
import com.tbox.fotki.model.entities.ParcelableItem
/**
* Created by Junaid on 4/24/17.
*/

interface FoktiImageLoaderInterface {

    fun sendImageLoadingSuccess(progressBar: ProgressBar, imageView: ImageView, item: ParcelableItem)
}
