package com.tbox.fotki.model.web_providers

import android.app.Activity
import com.tbox.fotki.model.web_providers.web_manager.WebManager

class AccountProvider(activity:Activity):BaseWebProvider(activity){
    init {
        start()
    }
    override fun start() {
        WebManager.instance.getAccountInfo(context, this)
    }
}