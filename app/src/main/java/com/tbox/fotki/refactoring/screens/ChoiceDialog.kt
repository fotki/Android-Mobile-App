package com.tbox.fotki.refactoring.screens

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import com.tbox.fotki.R
import com.tbox.fotki.view.adapters.SizeReductionDialogAdapter

class ChoiceDialog {

    lateinit var mDialog:Dialog
    lateinit var choiceAlert:ListView
    lateinit var listChoice:ArrayList<String>

    var resizedCall = {}
    var originalsCall = {}
    var cancelCall = {}

    @SuppressLint("InflateParams")
    fun showChoiceDialog(context: Context) {
        mDialog = Dialog(context)
        listChoice = makeList()

        val inflater = LayoutInflater.from(context)
        val convertView = inflater.inflate(R.layout.share_file_size_reduction_dialog, null) as View
        choiceAlert = convertView.findViewById(R.id.choiceDialog)
        val sizeReductionDialogAdapter = SizeReductionDialogAdapter(
            context, R.layout.share_file_size_reduction_dialog, listChoice
        )

        choiceAlert.adapter = sizeReductionDialogAdapter
        mDialog.setContentView(convertView)
        mDialog.show()
        setChoiceListViewListner()
    }
    private fun makeList():ArrayList<String> {
        val listChoice = ArrayList<String>()
        listChoice.add("Resized")
        listChoice.add(" Originals")
        listChoice.add("Cancel")
        return listChoice
    }
    private fun setChoiceListViewListner() {
        choiceAlert.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            when (listChoice[position]) {
                "Resized" -> {
                    mDialog.dismiss()
                    resizedCall.invoke()
                }
                " Originals" -> {
                    originalsCall.invoke()
                    mDialog.dismiss()
                }
                "Cancel" -> {
                    cancelCall.invoke()
                    mDialog.dismiss()
                }
                else -> {
                    mDialog.dismiss()
                }
            }
        }
    }
}