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
* Created by Junaid on 8/1/17.
*/

class FaceBookSignInDialogAdapter(@get:JvmName("getContext_") private val context: Context, resource: Int,
                                  facebookLogins: ArrayList<SocialLoginDetail>?) : ArrayAdapter<SocialLoginDetail>(context, resource) {
    private var mFaceBookLogins = ArrayList<SocialLoginDetail>()


    init {
        if (facebookLogins != null) {
            mFaceBookLogins.clear()
            this.mFaceBookLogins = facebookLogins
        }
    }

    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val viewHolder: ViewHolder
        if (view == null) {
            val inflater = context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.item_facebook_logins, null, true)
            viewHolder = ViewHolder()
            viewHolder.mDisplayName = view!!.findViewById(R.id.tvUserName)
            viewHolder.mUserDate = view.findViewById(R.id.tvSince)
            viewHolder.mSpaceUsed = view.findViewById(R.id.tvSpace)
            viewHolder.mdraweeViewItem = view.findViewById(R.id.avatar_image)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }
        val facebookLogin = mFaceBookLogins[position]
        val uri = Uri.parse(facebookLogin.mAvatar)
        viewHolder.mdraweeViewItem!!.setImageURI(uri)
        viewHolder.mDisplayName!!.text = facebookLogin.mUserName
        val date = "since : " + facebookLogin.date
        viewHolder.mUserDate!!.setText(date)
        val double_size = java.lang.Double.parseDouble(facebookLogin.space_used)
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

    override fun getCount(): Int = mFaceBookLogins.size

    // viewholder class
    private class ViewHolder {
        internal var mDisplayName: TextView? = null
        internal var mUserDate: TextView? = null
        internal var mSpaceUsed: TextView? = null
        internal var mdraweeViewItem: SimpleDraweeView? = null
    }
}
