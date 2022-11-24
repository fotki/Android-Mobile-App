package com.tbox.fotki.view.adapters

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.opengl.Visibility
import android.util.Log
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.ParcelableAlbum
import com.tbox.fotki.model.entities.ParcelableItem
import com.tbox.fotki.refactoring.screens.slider.SliderActivity
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import com.tbox.fotki.view.view_holders.ItemAlbumViewHolder
import java.util.*
import javax.sql.DataSource

class AlbumRecyclerAdapter(
    @get:JvmName("getContext_") private val context: Context,
    var album: ParcelableAlbum?,
    private var mAlbumItemCount: Int,
    private var mPageCount: Int
) :
    RecyclerView.Adapter<ItemAlbumViewHolder>() {

    private var mAlbums = ArrayList<ParcelableItem>()

    fun reloadAlbums(album: ParcelableAlbum?, mAlbumItemCount: Int, mPageCount: Int) {
            this.album = album
            if (album != null) {
                this.mAlbums = album.mitem
            }
            this.mAlbumItemCount = mAlbumItemCount
            this.mPageCount = mPageCount
            notifyDataSetChanged()

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemAlbumViewHolder {
        return ItemAlbumViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_album,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return mAlbums.size
    }

    override fun onBindViewHolder(holder: ItemAlbumViewHolder, position: Int) {
        album?.let {

            val isMedia =  mAlbums[position].mShortUrl.endsWith(".jpg")||
                    mAlbums[position].mShortUrl.endsWith(".jpeg")||
                    mAlbums[position].mShortUrl.endsWith(".png")||
                    mAlbums[position].mShortUrl.endsWith(".JPG")||
                    mAlbums[position].mShortUrl.endsWith(".JPEG")||
                    mAlbums[position].mShortUrl.endsWith(".PNG") ||
                    mAlbums[position].mIsVideo ||
                    mAlbums[position].mShortUrl.endsWith(".gif")||
                    mAlbums[position].mShortUrl.endsWith(".GIF")


            val callBackLoad = object: Callback{
                override fun onSuccess() {
                    holder.progressBar.visibility = GONE
                }
                override fun onError(e: Exception?) {}
            }
            if( mAlbums[position].mShortUrl.endsWith(".gif")||
                mAlbums[position].mShortUrl.endsWith(".GIF")){
                Glide.with(context).load(mAlbums[position].mOriginalUrl)
                    .listener(object : RequestListener<Drawable> {
                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Drawable>?,
                            dataSource: com.bumptech.glide.load.DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                           holder.progressBar.visibility = GONE
                            return false
                        }

                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.d("aa","===${e}")
                            return false
                        }
                    }).into(holder.imageViewMain)
            }
            else if (mAlbums[position].mThumbnailUrl != "null") {
                    Picasso.get()
                        .load(Uri.parse(mAlbums[position].mThumbnailUrl))
                        .into(holder.imageViewMain, callBackLoad)
                if(isMedia){
                    holder.imageViewMain.scaleType = ImageView.ScaleType.CENTER_CROP
                } else {
                    holder.imageViewMain.scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
            } else {
                Picasso.get()
                    .load(R.drawable.ic_album)
                    .into(holder.imageViewMain, callBackLoad)
                holder.imageViewMain.scaleType = ImageView.ScaleType.CENTER_INSIDE
            }

            if(isMedia){
                holder.rlTitle.visibility = GONE
            } else {
                holder.rlTitle.visibility = VISIBLE
                holder.tvTitle.text = mAlbums[position].mTitle
            }

            holder.itemView.setOnClickListener{ view->
//                if(mAlbums[position].mShortUrl.endsWith(".GIF")||
//                    mAlbums[position].mShortUrl.endsWith(".gif")){
//                    val selectedUri = Uri.parse(mAlbums[position].mOriginalUrl)
//                    val intent = Intent(Intent.ACTION_VIEW)
//                    intent.setDataAndType(selectedUri, "*/*")
//                    context.startActivity(intent)
//
//                }
//                else
                if(isMedia){
                    L.print(this, "album - $album")
                    //val intent = Intent(context, ImageSliderActivity::class.java)
                    val intent = Intent(context, SliderActivity::class.java)
                    intent.putExtra(Constants.PARCEABLE_ALBUM, album)
                    intent.putExtra(Constants.SELECTED_POSITION, position)
                    intent.putExtra(Constants.ALBUM_ITEM_COUNT, album!!.mitem.size)
                    intent.putExtra(Constants.ITEM_COUNT, mAlbumItemCount)
                    intent.putExtra(Constants.REQUEST_PAGE, mPageCount)
                    context.startActivity(intent)
                } else {
                    val selectedUri = Uri.parse(mAlbums[position].mOriginalUrl)
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(selectedUri, "*/*")
                    context.startActivity(intent)
                }
            }
        }
    }
}