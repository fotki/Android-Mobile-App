package com.tbox.fotki.view.adapters

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View.*
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.Folder
import com.tbox.fotki.model.entities.ParcelableAlbum
import com.tbox.fotki.util.L
import com.tbox.fotki.view.fragments.folder_fragment.FolderFragment
import com.tbox.fotki.view.view_holders.ItemFolderViewHolder
import java.lang.Exception
import java.util.ArrayList

class FolderRecyclerAdapter(val fragment: FolderFragment, var folder: Folder?, var column:Int):
    RecyclerView.Adapter<ItemFolderViewHolder>() {

    private val mFolderItems = ArrayList<Any>()
    var albumstart: Int=0;

    fun reloadFolder(folder: Folder?, column: Int){
            this.column = column
            this.folder = folder
            initFolderItems()
            notifyDataSetChanged()
    }

    private fun initFolderItems() {
        var mFolderCount = folder!!.mFolders.size
        mFolderItems.clear()
        mFolderItems.addAll(folder!!.mFolders)
        if(mFolderCount!=0&&column!=0){
            while (mFolderCount % column != 0) {
                Log.d("paddingError","folder count "+mFolderCount+" not equal to column "+column);
                mFolderItems.add(mFolderCount, "DummyObject")
                mFolderCount++
            }
        }
        mFolderItems.addAll(folder!!.mAlbums)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemFolderViewHolder {
        return ItemFolderViewHolder(LayoutInflater.from(parent.context).
             inflate(R.layout.item_folder,parent, false))
    }

    override fun getItemCount(): Int {
        return mFolderItems.size
    }

    override fun onBindViewHolder(holder: ItemFolderViewHolder, position: Int) {
        folder?.let {
            when (mFolderItems[position]) {
                is ParcelableAlbum -> {
                    showAlbum(holder, position)
                }
                is Folder -> {
                    albumstart =0;
                    showFolder(holder, position)
                }
                is String ->{
                    holder.llAlbum.visibility = GONE
                    holder.llFolder.visibility = INVISIBLE
                    holder.itemView.setOnClickListener(null)
                }
            }
        }
    }
    private fun showFolder(holder: ItemFolderViewHolder, position: Int) {
        holder.llAlbum.visibility = GONE
        holder.llFolder.visibility = VISIBLE

        val folder = mFolderItems[position] as Folder
        val folderCount = folder.mNumberOfFolders
        val albumCount = folder.mNumberOfAlbumbs
        holder.tvCountFolders.text = "$folderCount ${if (folderCount!! < 2) "Folder" else "Folders"}"
        holder.tvCountAlbums.text = "$albumCount ${if (albumCount!! < 2) "Album" else "Albums"}"
        if (position>0){
            holder.itemView.setPadding(0,5,0,0)
        }
        holder.tvFolderName.text = folder.mFolderName
        holder.itemView.setOnClickListener {
            fragment.handleOnClickListner(folder)
        }
    }

    private fun showAlbum(holder: ItemFolderViewHolder, position: Int) {
        holder.llAlbum.visibility = VISIBLE
        holder.llFolder.visibility = GONE
        if( albumstart < column){
            holder.llAlbum.setPadding(0,20,0,0);
            albumstart++;
        }

        val album = mFolderItems[position] as ParcelableAlbum
        holder.tvAlbumName.text = album.mName
        L.print(this,"photo - ${album.mCoverUrl}")

        val callBackLoad = object: Callback {
            override fun onSuccess() {
                holder.progressBar.visibility = GONE
            }
            override fun onError(e: Exception?) {}
        }
        if (album.mCoverUrl!="null"){
            Picasso.get()
                .load(Uri.parse(album.mCoverUrl))
                .into(holder.icAlbumImage, callBackLoad)
            holder.icAlbumImage.scaleType = ImageView.ScaleType.CENTER_CROP
        } else{
            Picasso.get()
                .load(R.drawable.ic_album)
                .into(holder.icAlbumImage, callBackLoad)
            holder.icAlbumImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        holder.itemView.setOnClickListener { fragment.handleOnClickListner(album) }
    }
}