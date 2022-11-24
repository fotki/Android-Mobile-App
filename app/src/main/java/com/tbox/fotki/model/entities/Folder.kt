package com.tbox.fotki.model.entities

import java.util.*

/**
* Created by Junaid on 4/12/17.
*/

class Folder {

    var mFolderIdEnc: Long? = null
    var mFolderName: String
    var mDescription: String
    var mShareUrl :String
    var mAlbums: ArrayList<ParcelableAlbum>
    var mFolders: ArrayList<Folder>
    var mNumberOfFolders: Long? = null
    var mNumberOfAlbumbs: Long? = null

    //default constructor
    init {
        mFolderIdEnc = 0L
        mNumberOfAlbumbs = 0L
        mNumberOfFolders = 0L
        mFolderName = ""
        mDescription = ""
        mShareUrl =" "
        mFolders = ArrayList()
        mAlbums = ArrayList()
    }

    fun setData(folderIdEnc: Long?, numberOfFol: Long?, numberOfAlbum: Long?, description: String, folderName: String,
                albums: ArrayList<ParcelableAlbum>, folders: ArrayList<Folder>, shareUrl: String) {
        this.mFolderIdEnc = folderIdEnc
        this.mNumberOfFolders = numberOfFol
        this.mNumberOfAlbumbs = numberOfAlbum
        this.mDescription = description
        this.mFolderName = folderName
        this.mAlbums = albums
        this.mFolders = folders
        this.mShareUrl = shareUrl
    }

    override fun toString(): String {
        return "Folder(mFolderIdEnc=$mFolderIdEnc, mFolderName='$mFolderName', mDescription='$mDescription', mShareUrl='$mShareUrl', mAlbums=$mAlbums, mFolders=$mFolders, mNumberOfFolders=$mNumberOfFolders, mNumberOfAlbumbs=$mNumberOfAlbumbs)"
    }


    companion object{
        const val ALBUM_TEMPLATE = "yyyy-MM-dd"
        const val FOLDER_YEAR_TEMPLATE = "yyyy"
        const val BACKUP_FOLDER_NAME = "Background upload"
        const val LAST_CREATED_ALBUM = "last_created_album"
        const val SHARED_ID = "id_folder"
        const val LAST_FOLDER = "last_folder"
        const val LAST_FOLDER_ID = "last_folder_id"
    }
}
