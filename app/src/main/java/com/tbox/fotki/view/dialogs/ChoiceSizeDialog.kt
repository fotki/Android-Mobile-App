package com.tbox.fotki.view.dialogs

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.tbox.fotki.R
import com.tbox.fotki.view.adapters.SizeReductionDialogAdapter

class ChoiceSizeDialog:DialogFragment() {

    lateinit var onItemClick:AdapterView.OnItemClickListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.size_reduction_dialog_view,container,false)
        arguments?.let {
            val str = if (it.getBoolean("isDelete")){
                SpannableStringBuilder(getString(R.string.resize_photos_text_delete))
            } else {
                SpannableStringBuilder(getString(R.string.resize_photos_text))
            }
            str.setSpan(
                ForegroundColorSpan(
                ContextCompat.getColor(requireContext(), R.color.colorDarkGrey)), 142, 156, 0)
            str.setSpan(RelativeSizeSpan(0.9f), 142, 156, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            view.findViewById<TextView>(R.id.sharetext).text = str
            val choiceAlert = view.findViewById<ListView>(R.id.choiceDialog)
            val sizeReductionDialogAdapter = SizeReductionDialogAdapter(
                requireContext(),
                R.layout.size_reduction_dialog_view, arrayListOf("Resized","Originals","Cancel"))

            choiceAlert.adapter = sizeReductionDialogAdapter
            choiceAlert.onItemClickListener = onItemClick
        }
        return view
    }

}