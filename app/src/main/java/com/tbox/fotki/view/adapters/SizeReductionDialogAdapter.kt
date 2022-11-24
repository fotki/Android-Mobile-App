package com.tbox.fotki.view.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.tbox.fotki.R
import java.util.*

/**
* Created by Junaid on 8/23/17.
*/


class SizeReductionDialogAdapter(@get:JvmName("getContext_") private val context: Context, resource: Int,
                                 list: ArrayList<String>?) : ArrayAdapter<String>(context, resource) {
    private var mChoice = ArrayList<String>()


    init {
        if (list != null) {
            mChoice.clear()
            this.mChoice = list
        }
    }

    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val viewHolder: ViewHolder
        if (view == null) {
            val inflater = context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.item_choice_dialog, null, true)
            viewHolder = ViewHolder()
            viewHolder.mTvChoice = view!!.findViewById<TextView>(R.id.tvChoice)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }
        viewHolder.mTvChoice!!.text = mChoice[position]

        return view
    }

    override fun getCount(): Int = mChoice.size


    // viewholder class
    private class ViewHolder {
        internal var mTvChoice: TextView? = null

    }
}
