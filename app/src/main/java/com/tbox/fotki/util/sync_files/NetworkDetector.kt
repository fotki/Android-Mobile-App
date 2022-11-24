package com.tbox.fotki.util.sync_files

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager

object NetworkDetector {

    const val NOT_CONNECTED = 0
    const val TYPE_WIFI = 1
    const val TYPE_NETWORK = 2
    const val TYPE_ROAMING = 3

    fun getNetworkConnect(context:Context): Int {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT < 23) {
            val ni = cm.activeNetworkInfo
            if (ni != null) {
                return when{
                    ni.type == ConnectivityManager.TYPE_WIFI -> TYPE_WIFI
                    ni.type == ConnectivityManager.TYPE_MOBILE -> TYPE_NETWORK
                    !ni.isConnected -> NOT_CONNECTED
                    else -> NOT_CONNECTED
                }
            }
        } else {
            val n = cm.activeNetwork
            if (n != null) {
                val nc = cm.getNetworkCapabilities(n)
                return when {
                    nc!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> TYPE_NETWORK
                    nc!!.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TYPE_WIFI
                    else -> NOT_CONNECTED
                }
            }
        }
        return NOT_CONNECTED
    }

    fun testIsRoaming(context: Context)
            = (context.getSystemService(Context.TELEPHONY_SERVICE)
            as TelephonyManager).isNetworkRoaming

}