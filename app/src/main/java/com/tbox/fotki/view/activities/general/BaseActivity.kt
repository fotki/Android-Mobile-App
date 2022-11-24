package com.tbox.fotki.view.activities.general

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.VolleyError

import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.model.web_providers.web_manager.WebManagerInterface
import com.tbox.fotki.util.PermissionAdapter

import org.json.JSONObject


open class BaseActivity : AppCompatActivity(),
    WebManagerInterface {

    private lateinit var permissionAdapter: PermissionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //checkPermissions()
    }

    fun checkPermissions() {
            permissionAdapter = PermissionAdapter(object : PermissionAdapter.Action {
                override fun todo() {}
            }, object : PermissionAdapter.Action {
                override fun todo() {
                    finish()
                }
            }).addPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET
            )
        if (!permissionAdapter.testPermissions(this)) {
            permissionAdapter.requestMultiplePermission(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionAdapter.check(requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //WebManagerInteface functions
    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {}

    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {}

    override fun sendNetworkFailure(isInterNetAvailableFlag: Boolean, apiRequestType: ApiRequestType) {}
}
