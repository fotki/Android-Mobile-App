package com.tbox.fotki.util

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AlertDialog
import com.tbox.fotki.util.upload_files.ReturnToAlbumInterface
import com.tbox.fotki.BuildConfig
import com.tbox.fotki.view.fragments.album_fragment.AlbumFragment
import com.tbox.fotki.R

@Suppress("DEPRECATION")
class Utility {

    private var progressDialog: ProgressDialog? = null

    fun showProgressDialog(context: Context, text: String) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(context)
        }
        progressDialog!!.setMessage(text)
        progressDialog!!.setCancelable(false)
        progressDialog!!.show()
    }

    fun showProgressDialog(context: Context, textID: Int) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(context)
        }
        progressDialog!!.setMessage(context.getString(textID))
        progressDialog!!.setCancelable(false)
        progressDialog!!.show()
    }

    fun dismissProgressDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
    }
    //    public void makeAndShowAlert(Context context,int title,String messge, int negative)

    fun showAlertDialog(context: Context, text: String) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.myDialog))
                .setTitle(R.string.app_name)
                .setMessage(text)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }

    fun showAlertDialog(context: Context, text: String, action: () -> Unit) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.myDialog))
            .setTitle(R.string.app_name)
            .setMessage(text)
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .setNegativeButton(R.string.retry) { _: DialogInterface, _: Int -> action()}
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    fun showConfirmDialog(context: Context, text: String, action: () -> Unit) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.myDialog))
            .setTitle(R.string.app_name)
            .setMessage(text)
            .setPositiveButton(android.R.string.ok) {_, _ -> action()}
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    fun showOkDialog(context: Context, text: String, action: () -> Unit) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.myDialog))
            .setTitle(R.string.app_name)
            .setMessage(text)
            .setPositiveButton(android.R.string.ok) {_, _ -> action()}
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }


    fun showResumeAlertDialog(context: Context, text: String, action: () -> Unit) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.myDialog))
            .setMessage(text)
            .setPositiveButton(R.string.btn_no_thanks) { _, _ -> }
            .setNegativeButton(R.string.bt_resume) { _: DialogInterface, _: Int -> action()}
            .show()
    }

    fun showAlertDialog(context: Context, text: Int) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.myDialog))
                .setTitle(R.string.app_name)
                .setMessage(text)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }

    fun showUploadSuccessAlert(context: Context,
                               returnToAlbumInterface: ReturnToAlbumInterface, message: String) {
        val dialog = AlertDialog.Builder(ContextThemeWrapper(context, R.style.myDialog))
            //.setMessage(R.string.text_upload_successful_title)
            .setMessage(message)
            .setPositiveButton(R.string.text_ok) { _, _ -> returnToAlbumInterface.returnToAlbumScreen(true) }
            .setCancelable(false)
            .create()

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    fun showAlreadyUploadInProgress(context: Context, albumFragment: AlbumFragment) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.myDialog))
                .setMessage(R.string.upload_inProgress)
                .setNegativeButton(R.string.ok) { _, _ -> }
                .setPositiveButton(R.string.open_uploadScreen) { _, _ -> albumFragment.openUploaderScreen() }
                .setNeutralButton(R.string.cancel,{_,_  -> albumFragment.cancelUpload()})
                .show()
    }

    fun isConnectingToInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val networks = connectivityManager.allNetworks
            var networkInfo: NetworkInfo
            for (mNetwork in networks) {
                networkInfo = connectivityManager.getNetworkInfo(mNetwork)!!
                if (networkInfo.state == NetworkInfo.State.CONNECTED) {
                    return true
                }
            }
        } else {

            val info = connectivityManager.allNetworkInfo
            info?.filter { it.state == NetworkInfo.State.CONNECTED }?.forEach { return true }
        }
        return false
    }

    fun fotkiAppDebugLogs(tag: String, appLog: String) {
        if (BuildConfig.DEBUG) Log.d(tag, appLog)
    }

    fun fotkiAppErrorLogs(tag: String, appLog: String) {
        Log.e(tag, appLog)
    }

    companion object {
        val instance: Utility
            get() = Utility()
    }
}
