package com.tbox.fotki.view.fragments.folder_fragment

import android.view.View
import androidx.lifecycle.MutableLiveData
import com.tbox.fotki.view_model.BaseViewModel

class FolderFragmentViewModel: BaseViewModel() {
    val folderName = MutableLiveData<String>()
    val folderDescription = MutableLiveData<String>()
    val folderDescriptionVisibility = MutableLiveData<Int>()
    val internetNoAvailableVisibility = MutableLiveData<Int>()
    val emptyFolderVisibility = MutableLiveData<Int>()

    init {
        internetNoAvailableVisibility.value = View.GONE
        emptyFolderVisibility.value = View.GONE
    }

}