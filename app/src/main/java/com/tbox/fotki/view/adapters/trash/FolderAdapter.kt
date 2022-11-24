package com.tbox.fotki.view.adapters.trash

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import com.tbox.fotki.model.entities.Folder
import com.tbox.fotki.model.entities.ParcelableAlbum
import com.tbox.fotki.view.fragments.folder_fragment.FolderFragment
import com.tbox.fotki.R
import java.util.*

@Suppress("DEPRECATION")
/**
* Created by Junaid on 7/14/17.
*/

class FolderAdapter(@get:JvmName("getContext_") private val context: Context, private val mFolderFragment: FolderFragment, resource: Int, folder: Folder?, columns: Int) : ArrayAdapter<Any>(context, resource) {
    private val mFolderItems = ArrayList<Any>()
    private var mFolder: Folder? = null

    init {
        if (folder != null) {
            this.setFolder(folder, columns)
        }
    }

    @SuppressLint("ViewHolder", "InflateParams", "SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val viewHolder: ViewHolder
        if (view == null) {
            val inflater = context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.item_folder, null, true)
            viewHolder =
                ViewHolder(view)
            view!!.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }
        if (mFolderItems[position] is Folder) {
            val folder = mFolderItems[position] as Folder
            viewHolder.mFolderName.text = folder.mFolderName
            val folder_count = folder.mNumberOfFolders
            val album_count = folder.mNumberOfAlbumbs
            if (folder_count!! < 2){
                viewHolder.mFolderCount.text = "$folder_count Folder"
            } else{
                viewHolder.mFolderCount.text = "$folder_count Folders"
            }
            if (album_count!! < 2){
                viewHolder.mAlbumCount.text = "$album_count Album"
            } else{
                viewHolder.mAlbumCount.text = "$album_count Albums"
            }
            viewHolder.mLinearLayout.visibility = View.VISIBLE
            viewHolder.mRelativeLayout.visibility = View.GONE
            view.isClickable = false
        } else if (mFolderItems[position] is String) {
            viewHolder.mLinearLayout.visibility = View.INVISIBLE
            viewHolder.mRelativeLayout.visibility = View.GONE
            view.isClickable = true
        } else {
            if (mFolderItems[position] is ParcelableAlbum) {
                viewHolder.mLinearLayout.visibility = View.GONE
                viewHolder.mRelativeLayout.visibility = View.VISIBLE
                view.isClickable = false
                val album = mFolderItems[position] as ParcelableAlbum
                val uri = Uri.parse(album.mCoverUrl)
                viewHolder.mAlbumName.text = album.mName
                //Picasso.get().load(uri).centerCrop().into(viewHolder.mdraweeViewItem)

                viewHolder.mdraweeViewItem.setImageURI(uri)
            }
        }

        view.setOnClickListener { mFolderFragment.handleOnClickListner(mFolderItems[position]) }
        return view
    }

    override fun getCount(): Int {
        return if (mFolder == null) {
            0
        } else {
            3
            //mFolderItems.size
        }
    }

    fun setFolder(folder: Folder, column: Int) {
        this.mFolder = folder
        var mFolderCount = mFolder!!.mFolders.size
        mFolderItems.clear()
        mFolderItems.addAll(mFolder!!.mFolders)
        while (mFolderCount % column != 0) {
            mFolderItems.add(mFolderCount, "DummyObject")
            mFolderCount++
        }
        mFolderItems.addAll(mFolder!!.mAlbums)
    }


    // viewholder class
    private class ViewHolder internal constructor(view: View) {

        internal var mFolderName: TextView = view.findViewById(R.id.folderName)
        internal var mFolderCount: TextView = view.findViewById(R.id.number_of_folders)
        internal var mAlbumCount: TextView = view.findViewById(R.id.number_of_albums)
        internal var mdraweeViewItem: SimpleDraweeView = view.findViewById(R.id.ic_Album_item)
        internal var mAlbumName: TextView = view.findViewById(R.id.album_name)
        internal var mRelativeLayout: RelativeLayout = view.findViewById(R.id.rel_album_item)
        internal var mLinearLayout: LinearLayout = view.findViewById(R.id.linearLayoutFolder_item)
    }
}
