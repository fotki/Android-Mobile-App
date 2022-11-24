package com.tbox.fotki.util

import android.util.Log

object L {

    private const val ENABLE = true
    private const val TAG = "Fotki"

    fun print(obj:Any, message:String){
        if (ENABLE) Log.d(TAG, "${obj::javaClass.name}: $message")
    }
}