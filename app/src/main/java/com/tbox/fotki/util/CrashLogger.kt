package com.tbox.fotki.util

import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashLogger {

    const val LOGIN_ACTIVITY_LEVEL = "LoginActivity"
    const val IMAGE_SLIDER_LEVEL = "ImageSliderActivity"
    const val FILE_UPLOADER_LEVEL = "FileUploader"
    const val UPLOAD_JOB_SERVICE_LEVEL = "UploadJobService"

    fun sendCrashLog(level:String,message:String){
        FirebaseCrashlytics.getInstance().log(level+":"+message)
//        Crashlytics.log(1,level,message)
    }
}