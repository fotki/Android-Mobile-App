package com.tbox.fotki.util

import android.text.TextWatcher
import android.view.View
import android.view.View.GONE
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.roughike.bottombar.BottomBar

@BindingAdapter("adapter")
fun setAdapter(view: RecyclerView, adapter: MutableLiveData<RecyclerView.Adapter<*>>) {
    val parentActivity:AppCompatActivity? = view.getParentActivity()
    if(parentActivity != null) {
        //adapter.observe(parentActivity, Observer { value ->  })
        view.adapter = adapter.value
    }
}

@BindingAdapter("mutableVisibility")
fun setMutableVisibility(view: View,  visibility: MutableLiveData<Int>?) {
    val parentActivity: AppCompatActivity? = view.getParentActivity()
    if(parentActivity != null && visibility != null) {
        visibility.observe(parentActivity, Observer {
                value -> view.visibility = value?:View.VISIBLE})
    }
}

@BindingAdapter("mutableBoolVisibility")
fun setMutableBoolVisibility(view: View,  isVisible: MutableLiveData<Boolean>?) {
    val parentActivity: AppCompatActivity? = view.getParentActivity()
    if(parentActivity != null && isVisible != null) {
        isVisible.observe(parentActivity, Observer {
                value -> view.visibility = if(value) View.VISIBLE else GONE})
    }
}

@BindingAdapter("mutableText")
fun setMutableText(view: TextView,  text: MutableLiveData<String>?) {
    val parentActivity:AppCompatActivity? = view.getParentActivity()
    if(parentActivity != null && text != null) {
        text.observe(parentActivity, Observer { value -> view.text = value?:""})
    }
}

@BindingAdapter("mutableEditText")
fun setMutableEditText(view: EditText,  text: MutableLiveData<String>?) {
    val parentActivity:AppCompatActivity? = view.getParentActivity()
    if(parentActivity != null && text != null) {
        text.observe(parentActivity, Observer { value ->
            view.setText(value)
        })
    }
}

@BindingAdapter("style")
fun setStyle(view: TextView,  style: Int?) {
    val parentActivity:AppCompatActivity? = view.getParentActivity()
    if(parentActivity != null && style != null) {
        view.setTypeface(null,style)
    }
}

@BindingAdapter("mutableTitle")
fun setMutableTitle(view: Toolbar,  text: MutableLiveData<String>?) {
    val parentActivity:AppCompatActivity? = view.getParentActivity()
    if(parentActivity != null && text != null) {
        text.observe(parentActivity, Observer { value -> view.title = value?:""})
    }
}

@BindingAdapter("textColorMutable")
fun setTextColorCircular(view: TextView, text: MutableLiveData<Int>?) {
    val parentActivity:AppCompatActivity? = view.getParentActivity()
    if(parentActivity != null && text != null) {
        text.observe(parentActivity, Observer { value -> view.setTextColor(value)})
    }
}

@BindingAdapter("textChangedListener")
fun onTextChanged(et: EditText, textWatcher:TextWatcher) {
    et.addTextChangedListener(textWatcher)
}

@BindingAdapter("mutableMax")
fun setMutableMax(pb:ProgressBar, max:MutableLiveData<Int>) {
    val parentActivity:AppCompatActivity? = pb.getParentActivity()
    if(parentActivity != null && max != null) {
        max.observe(parentActivity, Observer { value -> pb.max = value})
    }
}

@BindingAdapter("mutableProgress")
fun setMutableProgress(pb:ProgressBar, progress:MutableLiveData<Int>) {
    val parentActivity:AppCompatActivity? = pb.getParentActivity()
    if(parentActivity != null && progress != null) {
        progress.observe(parentActivity, Observer { value -> pb.progress = value})
    }
}

@BindingAdapter("mutableChecked")
fun setMutableChecked(switchCompat: SwitchCompat, checked:MutableLiveData<Boolean>) {
    val parentActivity:AppCompatActivity? = switchCompat.getParentActivity()
    if(parentActivity != null && checked != null) {
        checked.observe(parentActivity, Observer { value -> switchCompat.isChecked = value})
    }
}

@BindingAdapter("mutableRefreshing")
fun setMutableRefreshing(switchCompat: SwipeRefreshLayout, checked:MutableLiveData<Boolean>) {
    val parentActivity:AppCompatActivity? = switchCompat.getParentActivity()
    if(parentActivity != null && checked != null) {
        checked.observe(parentActivity, Observer { value -> switchCompat.isRefreshing = value})
    }
}

@BindingAdapter("viewMutableTag")
fun setViewTag(view: View, data:MutableLiveData<String>) {
    val parentActivity:AppCompatActivity? = view.getParentActivity()
    if(parentActivity != null && data != null) {
        data.observe(parentActivity, Observer { value -> view.setTag(data)})
    }
}

@BindingAdapter("clickLIstener")
fun clickListener(view: View, listener:View.OnClickListener) {
    view.setOnClickListener(listener)
}

@BindingAdapter("tabPosition")
fun setTabPosition(view: BottomBar, position:MutableLiveData<Int>) {
    val parentActivity:AppCompatActivity? = view.getParentActivity()
    if(parentActivity != null) {
        position.observe(parentActivity, Observer { value -> view.selectTabAtPosition(value)})
    }
}