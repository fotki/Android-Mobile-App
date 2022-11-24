package com.tbox.fotki.refactoring.auth_providers

import android.content.Context
import android.content.Intent
import com.facebook.login.LoginManager
import com.tbox.fotki.model.entities.Session
import com.tbox.fotki.view.activities.LoginActivity

object SessionManager {

    fun setSessionExpired(context: Context){
        val session = Session.getInstance(context)
        if (session.mSignInWithFacebook) {
            LoginManager.getInstance().logOut()
        }
        session.removeSessionInfo(context)

        val intent = Intent(
            context,
            LoginActivity::class.java
        )
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}