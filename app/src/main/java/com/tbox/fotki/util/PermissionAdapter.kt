package com.tbox.fotki.util

import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.tbox.fotki.R

class PermissionAdapter {

    private var permissions: MutableList<String>? = null
    var requestCode: Int = 0
    private var actionSuccess: Action? = null
    private var actionError: Action? = null

    constructor(vararg permissions: String) {
        this.permissions = ArrayList()
        addPermission(*permissions)
    }

    constructor(actionSuccess: Action, actionError: Action) {
        permissions = ArrayList()
        this.actionSuccess = actionSuccess
        this.actionError = actionError
    }

    fun setActionSuccess(actionSuccess: Action): PermissionAdapter {
        this.actionSuccess = actionSuccess
        return this
    }

    fun setActionError(actionError: Action): PermissionAdapter {
        this.actionError = actionError
        return this
    }

    fun setRequestCode(requestCode: Int): PermissionAdapter {
        this.requestCode = requestCode
        return this
    }

    fun addPermission(vararg names: String): PermissionAdapter {
        for (name in names) permissions!!.add(name)
        return this
    }

    fun requestMultiplePermission(activity: Activity) {
        AlertDialog.Builder(activity).setMessage(R.string.test_permissions_history)
            .setPositiveButton(android.R.string.ok
            ) { _, _ ->
                ActivityCompat.requestPermissions(activity, permissions!!.toTypedArray(), requestCode)
            }.show()
    }

    fun check(requestCode: Int, grantResults: IntArray) {
        if (requestCode == this.requestCode && grantResults.size > 0) {
            var answerPermission = true
            for (res in grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) answerPermission = false
            }
            if (answerPermission) {
                actionSuccess?.todo()
            } else {
                actionError?.todo()
            }
        }
    }

    fun testPermissions(activity: Activity): Boolean {
        var answerPermission = true
        for (permission in permissions!!) {
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                answerPermission = false
            }
        }
        return answerPermission
    }

    interface Action {
        fun todo()
    }

    companion object {
        fun checkingPermissionIsEnabledOrNot(activity: Activity, permission: String): Boolean {
            val AnswerPhoneResult = ContextCompat.checkSelfPermission(activity, permission)
            return AnswerPhoneResult == PackageManager.PERMISSION_GRANTED
        }
    }
}