package com.tbox.fotki.refactoring.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.tbox.fotki.R
import kotlinx.android.synthetic.main.fragment_home.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeFragment: Fragment() {

    private val viewModel: MainViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.eventsLiveData.observe(viewLifecycleOwner, Observer {
/*
            when (it) {
                is LoadSubjectsList -> {
                    stopLoadClear()
                    checkMySubjectAndLoadList(it.list)
                }
                is NoSubjectsFound -> {
                    stopLoadClear()
                    contentView.setGone()
                    emptyView.setVisible()
                }
                is Logout -> {
                    stopLoadClear()
                    userAccount.logout()
                    parentFragmentManager.clearStack()
                    replaceFragment(
                        R.id.fragment_container_view,
                        SplashScreenFragment(),
                        false)
                }
            }
*/
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnGetFolder.setOnClickListener {
            viewModel.loadFolder(4294967294L,1)
        }
    }
}