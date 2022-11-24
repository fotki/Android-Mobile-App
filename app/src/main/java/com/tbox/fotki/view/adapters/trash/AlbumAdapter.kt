package com.tbox.fotki.view.adapters.trash

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.tbox.fotki.model.entities.ParcelableAlbum
import com.tbox.fotki.model.entities.ParcelableItem
import com.tbox.fotki.util.FoktiImageLoaderInterface
import com.tbox.fotki.util.FotkiCustomProgressBar
import com.tbox.fotki.R
import java.util.*

@Suppress("DEPRECATION")
/**
* Created by Junaid on 4/17/17.
*/

class AlbumAdapter(@get:JvmName("getContext_")private val context: Context, private val layoutResourceId: Int, album: ParcelableAlbum?, albumCount: Int) : ArrayAdapter<Any>(context, layoutResourceId),
    FoktiImageLoaderInterface {
    private var mAlbumCount: Int = 0
    private var mAlbum: ParcelableAlbum? = null
    private val mAlbums = ArrayList<ParcelableItem>()
    private var holder: ViewHolder? = null

    init {
        if (album != null) {
            this.setAlbum(album)
            this.setAbumCount(albumCount)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row = convertView
        if (row == null) {
            val inflater = (context as Activity).layoutInflater
            row = inflater.inflate(layoutResourceId, parent, false)
            holder =
                ViewHolder(row)
            row!!.tag = holder
        } else {
            holder = row.tag as ViewHolder
        }
        holder!!.mProgressBar.visibility = View.VISIBLE
        holder!!.mImageView_vidPlay.visibility = View.GONE
        if (position < mAlbums.size) {
            val item = mAlbums[position]
            val uri = Uri.parse(item.mThumbnailUrl)
            val request = ImageRequestBuilder.newBuilderWithSource(uri)
                    .setRequestPriority(Priority.HIGH)
                    .setProgressiveRenderingEnabled(false)
                    .build()

            holder!!.mdraweeViewItem.hierarchy.setProgressBarImage(
                FotkiCustomProgressBar(
                    this,
                    holder!!.mProgressBar,
                    holder!!.mImageView_vidPlay,
                    item
                )
            )
            val controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(request)
                    .setTapToRetryEnabled(true)
                    .setOldController(holder!!.mdraweeViewItem.controller)
                    .build()
            holder!!.mdraweeViewItem.controller = controller
        } else {
            holder!!.mdraweeViewItem.setImageResource(R.color.colorWhite)
            holder!!.mProgressBar.visibility = View.VISIBLE
            holder!!.mImageView_vidPlay.visibility = View.GONE
        }
        return row
    }

    override fun getCount(): Int {
        return if (mAlbum == null) {
            0
        } else {
            mAlbumCount
        }
    }

    fun setAlbum(album: ParcelableAlbum) {
        this.mAlbum = album
        mAlbums.clear()
        mAlbums.addAll(mAlbum!!.mitem)
    }

    fun setAbumCount(count: Int) {
        this.mAlbumCount = count
    }

    // interface method
    override fun sendImageLoadingSuccess(progressBar: ProgressBar, imageView: ImageView, item: ParcelableItem) {
        progressBar.visibility = View.GONE
        if (item.mIsVideo) {
            imageView.visibility = View.VISIBLE
        } else {
            imageView.visibility = View.GONE
        }
    }

    // view holder class
    private class ViewHolder internal constructor(view: View) {

        internal var mdraweeViewItem: SimpleDraweeView = view.findViewById(R.id.drawee_item)
        internal var mProgressBar: ProgressBar = view.findViewById(R.id.pbLoading)
        internal var mImageView_vidPlay: ImageView = view.findViewById(R.id.imageView_Video)
    }
}
