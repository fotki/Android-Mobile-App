package com.tbox.fotki.view.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.tbox.fotki.R

open class BaseFragment : Fragment() {
    private lateinit  var mButton: Button
    internal lateinit var mFragmentNavigation: FragmentNavigation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater!!.inflate(R.layout.fragment_main, container, false)
        mButton = view.findViewById(R.id.button)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentNavigation) {
            mFragmentNavigation = context
        }
    }

    interface FragmentNavigation {
        fun pushFragment(fragment: Fragment)
        fun popFragment()
    }

    companion object {
        val ARGS_INSTANCE = "com.tbox.fotki.argsInstance"
    }

}
