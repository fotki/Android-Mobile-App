package com.tbox.fotki.view.fragments.upload_file_fragment

import android.view.View
import androidx.lifecycle.MutableLiveData
import com.tbox.fotki.view_model.BaseViewModel

class UploadFileFragmentViewModel:BaseViewModel() {

    val uploadNotInProgressVisibility = MutableLiveData<Int>()
    val uploadInProgressVisibility = MutableLiveData<Int>()
    val noInternetVisibility = MutableLiveData<Int>()

    val btnUploadVisibility = MutableLiveData<Int>()
    val btnUploadAndDelVisibility = MutableLiveData<Int>()
    val textTotalUpload = MutableLiveData<String>()
    val textFileUpload = MutableLiveData<String>()
    val textCurrentUpload = MutableLiveData<String>()

    val textDescription = MutableLiveData<String>()
    val textStatus = MutableLiveData<String>()
    val textStatusNoInternet = MutableLiveData<String>()


    fun showUploadInProgress(){
        uploadInProgressVisibility.value = View.VISIBLE
        uploadNotInProgressVisibility.value = View.GONE
        noInternetVisibility.value = View.GONE
    }

    fun showNotUploadInProgress(){
        uploadInProgressVisibility.value = View.GONE
        uploadNotInProgressVisibility.value = View.VISIBLE
        noInternetVisibility.value = View.GONE
    }

    fun showNoInternet(){
        uploadInProgressVisibility.value = View.GONE
        uploadNotInProgressVisibility.value = View.GONE
        noInternetVisibility.value = View.VISIBLE
    }
}