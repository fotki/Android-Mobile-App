package com.tbox.fotki.view.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import com.tbox.fotki.model.entities.SocialLoginDetail
import com.tbox.fotki.R
import java.util.*

@Suppress("DEPRECATION")
/**
 * Created by Junaid on 7/28/17.
 */

class GoogleSignInDialogAdapter(@get:JvmName("getContext_") private val context: Context, resource: Int,
                                googleLogins: ArrayList<SocialLoginDetail>?) : ArrayAdapter<SocialLoginDetail>(context, resource) {
    private var mGoogleLogins = ArrayList<SocialLoginDetail>()

    init {
        if (googleLogins != null) {
            mGoogleLogins.clear()
            this.mGoogleLogins = googleLogins
        }
    }

    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val viewHolder: ViewHolder
        if (view == null) {
            val inflater = context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.item_google_logins, null, true)
            viewHolder = ViewHolder()
            viewHolder.mDisplayName = view!!.findViewById(R.id.tvUserName)
            viewHolder.mUserDate = view.findViewById(R.id.tvSince)
            viewHolder.mSpaceUsed = view.findViewById(R.id.tvSpace)
            viewHolder.mdraweeViewItem = view.findViewById(R.id.avatar_image)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }
        val googleLogin = mGoogleLogins[position]
        val uri = Uri.parse(googleLogin.mAvatar)
        viewHolder.mdraweeViewItem!!.setImageURI(uri)
        viewHolder.mDisplayName!!.text = googleLogin.mUserName
        val date = "since : " + googleLogin.date
        viewHolder.mUserDate!!.setText(date)
        val double_size = java.lang.Double.parseDouble(googleLogin.space_used)
        val size = double_size.toLong()
        val albumSize = humanReadableByteCount(size, true)
        val spaceUsed = "space used : " + albumSize
        viewHolder.mSpaceUsed!!.setText(spaceUsed)
        return view
    }

    //calculate size of album
    private fun humanReadableByteCount(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024
        if (bytes < unit) return bytes.toString() + " B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }
    override fun getCount(): Int = mGoogleLogins.size

    // viewholder class
    private class ViewHolder {
        internal var mDisplayName: TextView? = null
        internal var mUserDate: TextView? = null
        internal var mSpaceUsed: TextView? = null
        internal var mdraweeViewItem: SimpleDraweeView? = null
    }
}
