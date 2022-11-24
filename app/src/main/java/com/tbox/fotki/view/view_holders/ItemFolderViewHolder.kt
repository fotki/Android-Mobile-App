package com.tbox.fotki.view.view_holders

import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import kotlinx.android.synthetic.main.item_folder.view.*

class ItemFolderViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView) {

    val llFolder:LinearLayout = itemView.linearLayoutFolder_item
    var llAlbum:RelativeLayout = itemView.rel_album_item

    //-----------------------------------------------------------------------------------Album-views
    val tvAlbumName: TextView = itemView.album_name
    val icAlbumImage: SimpleDraweeView = itemView.ic_Album_item
    val progressBar: ProgressBar = itemView.pbLoading

    //----------------------------------------------------------------------------------Folder-views
    val tvFolderName:TextView = itemView.folderName
    val tvCountFolders:TextView = itemView.number_of_folders
    val tvCountAlbums:TextView = itemView.number_of_albums
}