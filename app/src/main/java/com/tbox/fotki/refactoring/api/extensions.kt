package com.tbox.fotki.refactoring.api

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.gson.JsonObject
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference

fun JSONObject.getSafeString(filed: String): String {
    return if (has(filed)) {
        get(filed).toString()
    } else {
        ""
    }
}

fun JsonObject.getSafeString(filed: String): String {
    return if (has(filed)) {
        get(filed).toString()
    } else {
        ""
    }
}

fun JsonObject.getSafeJsonObject(filed: String): JsonObject? {
    return if (has(filed)) {
        get(filed).asJsonObject
    } else {
        null
    }
}

fun Fragment.replaceFragment(fragmentView: Int, fragment: Fragment, backStack: Boolean = true) {
    activity?.let { a ->
        val trans = a.supportFragmentManager.beginTransaction()
        trans.replace(fragmentView, fragment)
        if (backStack) {
            trans.addToBackStack(fragment::class.java.simpleName)
        }
        trans.commitAllowingStateLoss()
    }
}

fun Fragment.backFragment() {
    activity?.supportFragmentManager?.popBackStack()
}

fun View.setVisible(){
    visibility = View.VISIBLE
}

fun View.setGone(){
    visibility = View.GONE
}

fun View.setInvisible(){
    visibility = View.INVISIBLE
}

@Suppress("unused")
val Any?.unit: Unit get() = Unit

fun <T> T.weak() = WeakReference(this)

fun File.toRequestBody(type: MediaType): RequestBody = RequestBody.create(type, this)

fun String.capitalizeEachWord(): String {
    return split(' ').joinToString(" ") { it.capitalize() }
}

fun FragmentManager.clearStack() {
    try {
        for (i in 0 until backStackEntryCount) {
            popBackStack()
        }
    } catch (e: Exception) {
    }
}

fun Context.toast(message : String) {
    Toast.makeText(this,message, Toast.LENGTH_SHORT).show()
}