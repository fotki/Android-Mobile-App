package com.tbox.fotki.view.fragments.update_album_fragment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tbox.fotki.view_model.BaseViewModel

class UpdateAlbumFragmentViewModel:BaseViewModel() {

    val albumETDescriptionVisibility = MutableLiveData<Int>()
    val albumTVDescriptionVisibility = MutableLiveData<Int>()
    val btnCreateVisibility = MutableLiveData<Int>()

    val albumETDescription = MutableLiveData<String>()
    val albumETName = MutableLiveData<String>()
    val btnSaveVisibility = MutableLiveData<Int>()

    val mainLayoutVisibility = MutableLiveData<Int>()
    val internetNotAvailableVisibility = MutableLiveData<Int>()
}