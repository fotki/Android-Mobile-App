package com.tbox.fotki.view.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.ParcelableAlbum
import com.tbox.fotki.model.entities.ParcelableItem
import com.tbox.fotki.refactoring.api.setVisible
import com.tbox.fotki.refactoring.screens.slider.SliderActivity
import com.tbox.fotki.util.FoktiImageLoaderInterface
import com.tbox.fotki.util.FotkiCustomProgressBar
import com.tbox.fotki.view.activities.ImageSliderActivity
import com.tbox.fotki.view.view.zoomable_view.TapGestureListner
import com.tbox.fotki.view.view.zoomable_view.ZoomableDraweeView

/**
* Created by Junaid on 4/18/17.
*/
class ImageSliderAdapter(private val mContext: Context, private var mItemCount: Int, private val mAlbum: ParcelableAlbum) : PagerAdapter(),
    FoktiImageLoaderInterface {
    private val mItems = this.mAlbum.mitem
    private var mAllowSwipingWhileZoomed = true
    private var isFirstImageLoaded = false

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val index = position % 3
        val page = container.getChildAt(index) as FrameLayout
        val zoomableDraweeView = page.findViewById<ZoomableDraweeView>(R.id.zoomableView)
        val progressBar = page.findViewById<ProgressBar>(R.id.pbLoading)
        val mImageView_vidPlay = page.findViewById<ImageView>(R.id.imageView_Video)
        val gif_iv = page.findViewById<ImageView>(R.id.gif_iv)
        if(mItems[position % mItems.size].mShortUrl.endsWith(".gif")||
            mItems[position % mItems.size].mShortUrl.endsWith(".GIF")){

            gif_iv.visibility = View.VISIBLE
            zoomableDraweeView.visibility = View.GONE

            Glide.with(mContext).load(mItems[position % mItems.size].mOriginalUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        TODO("Not yet implemented")
                    }
                }).into(gif_iv)
            gif_iv.setOnClickListener {
                val tapCallback = mContext as SliderActivity
                tapCallback.singleTapDone(mItems[position % mItems.size])
            }
        }

        val viewVideo = page.findViewById<VideoView>(R.id.videoView)

        /*if ( mItems[position % mItems.size].mIsVideo) {

            viewVideo.visibility = View.VISIBLE
            mImageView_vidPlay.visibility = View.GONE

            val mediaControls = MediaController(mContext)
            mediaControls.setAnchorView(viewVideo)
            // set the media controller for video view
            viewVideo.setMediaController(mediaControls)
            // set the uri for the video view
            viewVideo.setVideoURI(Uri.parse(mItems[position % mItems.size].mOriginalUrl))
            // start a video
            viewVideo.start()

            // implement on completion listener on video view

            // implement on completion listener on video view
            viewVideo.setOnCompletionListener {
                Toast.makeText(
                    getApplicationContext(),
                    "Thank You...!!!",
                    Toast.LENGTH_LONG
                ).show() // display a toast when an video is completed
            }
            viewVideo.setOnErrorListener { mp, what, extra ->
                Toast.makeText(
                    getApplicationContext(),
                    "Oops An Error Occur While Playing Video...!!!",
                    Toast.LENGTH_LONG
                ).show() // display a toast when an error is occured while playing an video
                false
            }
        } else {
            viewVideo.visibility = View.GONE
            mImageView_vidPlay.visibility = View.VISIBLE*/

            zoomableDraweeView.setAllowTouchInterceptionWhileZoomed(mAllowSwipingWhileZoomed)
            // needed for double tap to zoom
            zoomableDraweeView.setIsLongpressEnabled(false)
            val tapGestureListner =
                TapGestureListner(
                    zoomableDraweeView
                )
            tapGestureListner.setTapCallback(this.mContext, mItems[position % mItems.size])
            zoomableDraweeView.setTapListener(tapGestureListner)
            progressBar.visibility = View.VISIBLE
            mImageView_vidPlay.visibility = View.GONE

            zoomableDraweeView.hierarchy.setProgressBarImage(
                FotkiCustomProgressBar(
                    this,
                    progressBar,
                    mImageView_vidPlay,
                    mItems[position % mItems.size]
                )
            )

        try {
            Log.d("GifError", "short url"+mItems[position % mItems.size].mShortUrl)

            val controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(ImageRequest.fromUri(mItems[position % mItems.size].mViewUrl))
                .setAutoPlayAnimations(true)
                .build()
            zoomableDraweeView.controller = null
            zoomableDraweeView.controller = controller
            page.requestLayout()
        }catch (e:Exception){
            Log.d("GifError", "Error: $e")
        }

        //}
        return page
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {

    }

    override fun getCount(): Int = mItemCount

    override fun isViewFromObject(arg0: View, arg1: Any): Boolean = arg0 === arg1

    override fun getItemPosition(`object`: Any): Int = // We want to create a new view when we call notifyDataSetChanged() to have the correct
            // behavior
            PagerAdapter.POSITION_NONE

    fun setItemCount(count: Int) {
        this.mItemCount = count
    }

    // interface implementation
    override fun sendImageLoadingSuccess(progressBar: ProgressBar, imageView: ImageView, item: ParcelableItem) {
        progressBar.visibility = View.GONE
        if (item.mIsVideo) {
            imageView.visibility = View.VISIBLE
        } else {
            imageView.visibility = View.GONE
        }
        if (!isFirstImageLoaded) {
            //val imageSliderActivity = mContext as SliderActivity
            //imageSliderActivity.handlerTask(true)
            isFirstImageLoaded = true
        }
    }
}
