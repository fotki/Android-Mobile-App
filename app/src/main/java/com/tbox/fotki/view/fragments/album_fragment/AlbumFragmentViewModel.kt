package com.tbox.fotki.view.fragments.album_fragment

import androidx.lifecycle.MutableLiveData
import com.tbox.fotki.view_model.BaseViewModel

class AlbumFragmentViewModel: BaseViewModel() {
    val topInfoVisibility = MutableLiveData<Int>()
    val emptyAlbumVisibility = MutableLiveData<Int>()
    val swipeRefreshVisibility = MutableLiveData<Int>()
    val swipeRefreshActive = MutableLiveData<Boolean>()
    val internetNotAvailableVisibility = MutableLiveData<Int>()

    val albumName = MutableLiveData<String>()
    val albumDescription = MutableLiveData<String>()
    val albumCount = MutableLiveData<String>()
    val albumDescriptionVisibility = MutableLiveData<Int>()

    val imFolderVisibility = MutableLiveData<Int>()

    var mPageCount: Int = 0
    var mAlbumItemCount: Int = 0
    var mAlbumPhotosCount = 0
    var mAlbumVideosCount = 0
    var mAlbumSize: Long = 0
    var mContentAPIFlag = false
    var mContentUploaded = false

    var listChoice = ArrayList<String>()
    var isAllowCompress = false
    var isReloadAlbum = false
    var isSwipeRefresh = false

    fun makeList() {
        listChoice.clear()
        listChoice.add("Resized")
        listChoice.add(" Originals")
        listChoice.add("Cancel")
    }
}