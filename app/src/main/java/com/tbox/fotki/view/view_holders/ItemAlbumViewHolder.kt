package com.tbox.fotki.view.view_holders

import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import kotlinx.android.synthetic.main.item_album.view.*

class ItemAlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val imageViewMain:SimpleDraweeView = itemView.drawee_item
    val progressBar:ProgressBar = itemView.pbLoading
    val tvTitle:TextView = itemView.item_name
    val rlTitle:RelativeLayout = itemView.item_layout
}