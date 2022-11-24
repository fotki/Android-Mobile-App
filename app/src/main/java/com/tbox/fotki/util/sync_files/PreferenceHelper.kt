package com.tbox.fotki.util.sync_files

import android.content.Context
import androidx.preference.PreferenceManager

class PreferenceHelper(val context: Context) {
    val sp = PreferenceManager.getDefaultSharedPreferences(context)

    fun applyPrefs(hashMapOf: HashMap<String, Any>) {
        for ((key, value) in hashMapOf) {
            when(value){
                is String -> sp.edit().putString(key,value).apply()
                is Int -> sp.edit().putInt(key,value).apply()
                is Long -> sp.edit().putLong(key,value).apply()
                is Boolean -> sp.edit().putBoolean(key,value).apply()
            }
        }
    }

    fun getLong(key:String) = sp.getLong(key,0)
    fun getString(key:String) = sp.getString(key,"")
    fun getInt(key:String) = sp.getInt(key,0)
    fun getBoolean(key:String) = sp.getBoolean(key,false)
    fun getBooleanTrue(key: String) = sp.getBoolean(key,true)
    fun getIntNeg(key: String)= sp.getInt(key,-1)
}