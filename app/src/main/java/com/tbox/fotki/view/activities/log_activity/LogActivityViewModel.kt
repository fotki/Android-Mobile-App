package com.tbox.fotki.view.activities.log_activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.tbox.fotki.R
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.sync_files.LogProvider
import com.tbox.fotki.view_model.BaseViewModel

class LogActivityViewModel: BaseViewModel()  {

    val broadcasts: HashMap<String, BroadcastReceiver>
    val text = MutableLiveData<String>()

    private val mLogReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            text.value =
                LogProvider.createLogMessage(intent.getStringExtra(Constants.MESSAGE)!!)+text.value
        }
    }

    init {
        broadcasts = hashMapOf(Constants.SERVICE_LOG_MESSAGE to mLogReceiver)
    }

    fun loadText(activity: AppCompatActivity){
        text.value = LogProvider.readFromFile(activity)!!
    }

    fun sendText(activity: AppCompatActivity) {
        LogProvider.sendToEmail(activity)
    }

    fun clearText(context: Context) {
        AlertDialog.Builder(context)
            .setMessage(R.string.clear_log_message)
            .setPositiveButton(R.string.confirm){_,_->
                LogProvider.clearFile(context)
                text.value = "" }
            .setNegativeButton(R.string.dismiss){_,_->}
            .show()
    }
}