package com.tbox.fotki.refactoring.screens.slider

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.codekidlabs.storagechooser.StorageChooser
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.ParcelableItem
import com.tbox.fotki.refactoring.api.replaceFragment
import com.tbox.fotki.refactoring.screens.ChoiceDialog
import com.tbox.fotki.util.L
import com.tbox.fotki.util.Utility
import com.tbox.fotki.util.registerBroadcasts
import com.tbox.fotki.util.sync_files.PreferenceHelper
import com.tbox.fotki.view.adapters.ImageSliderAdapter
import kotlinx.android.synthetic.main.activity_image_slider.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class SliderFragment : Fragment() {

    private val sliderViewModel: SliderViewModel by viewModel()

    private var mSlideFlag = true
    lateinit var mAdapter: ImageSliderAdapter
    private lateinit var mHandler: Handler
    private var pagerPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        L.print(this,"OnCreateView")
        return inflater.inflate(R.layout.activity_image_slider, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        L.print(this, "onActivityCreated")
        sliderViewModel.initFromIntent(requireActivity().intent)

        val currentPosition = PreferenceHelper(requireContext()).getIntNeg("mSelectedPosition")
        L.print(this, "current position - $currentPosition")
        when {
            currentPosition>=sliderViewModel.mAlbum.value!!.mitem.size -> {
                sliderViewModel.mSelectedPosition = sliderViewModel.mAlbum.value?.mitem?.size?:0
            }
            currentPosition > 0 -> {
                L.print(this, "AAAAAAAA")
                PreferenceHelper(requireContext()).applyPrefs(hashMapOf("mSelectedPosition" to -1))
                sliderViewModel.mSelectedPosition = currentPosition
            }
            else -> {
                L.print(this, "BBBBBBB")
            }
        }

        sliderViewModel.mCurrentPostion = sliderViewModel.mSelectedPosition
        sliderViewModel.initBroadcasts(requireActivity(), progressBar, rlProgressBarLayout)
        requireActivity().registerBroadcasts(sliderViewModel.broadcasts)

        sliderViewModel.callPaginationIfNeeded(requireContext())
        registerListners()
        sliderViewModel.mAlbum.observe(viewLifecycleOwner, Observer { mAlbum ->
            mAdapter = ImageSliderAdapter(requireContext(), mAlbum.mitem.size, mAlbum)
            setAdapter()
            setPagerListner()
            sliderViewModel.mAlbumCount = mAlbum!!.mitem.size
            sliderViewModel.mApiBeingCalledFlag = false
            mAdapter.setItemCount(mAlbum.mitem.size)
            mAdapter.notifyDataSetChanged()
            pager!!.adapter!!.notifyDataSetChanged()
        })

        mHandler = Handler()
        sliderViewModel.registerAdapter(requireContext())
    }

    private fun registerListners() {
        btnClose.setOnClickListener { requireActivity().onBackPressed() }
        btnShare.setOnClickListener {
            if (Utility().isConnectingToInternet(requireActivity())) {
                sliderViewModel.mIsSharedButtonError = false
                val dialog = ChoiceDialog()
                dialog.resizedCall = {
                    hideViews()
                    sliderViewModel.isAllowCompress = true
                    sliderViewModel.startSharing(requireActivity(), rlProgressBarLayout)
                }
                dialog.originalsCall = {
                    hideViews()
                    sliderViewModel.isAllowCompress = false
                    sliderViewModel.startSharing(requireActivity(), rlProgressBarLayout)
                }
                dialog.cancelCall = {
                    sliderViewModel.isAllowCompress = false
                }
                dialog.showChoiceDialog(requireContext())
            } else {
                rlInternetNotAvailable.visibility = View.VISIBLE
                sliderViewModel.mIsSharedButtonError = true
            }
        }
        btnLeft.setOnClickListener {
            L.print(this, "Tap on right - ${sliderViewModel.mCurrentPostion}")
            pager!!.currentItem = sliderViewModel.mCurrentPostion - 1
        }
        btnRight.setOnClickListener {
            L.print(this, "Tap on left - ${sliderViewModel.mCurrentPostion}")
            pager!!.currentItem = sliderViewModel.mCurrentPostion + 1
        }
        btnRetry.setOnClickListener {
            if (sliderViewModel.mIsSharedButtonError) {
                rlInternetNotAvailable.visibility = View.GONE
                sliderViewModel.mIsSharedButtonError = false
                btnShare.performClick()
            } else {
                sliderViewModel.albumProvider.getAblumContentViaRetry()
            }
        }

        btnDelete.setOnClickListener {
            sliderViewModel.tryToDelete(requireContext()){
                requireActivity().finish()
            }
        }
        rlProgressBarLayout.visibility = View.GONE
        progressBar.max = 100
        //setAdapter()
        //setPagerListner()
    }

    @SuppressLint("SetTextI18n")
    private fun setAdapter() {
        L.print(this, "setAdapter")
        pager.adapter = mAdapter
        pager.setCurrentItem(sliderViewModel.mSelectedPosition, true)
        tvPageIndex.text = (sliderViewModel.mSelectedPosition + 1).toString() + ""
        tvPageTotal.text = sliderViewModel.mAlbumItemCount.toString() + ""
    }

    private fun setPagerListner() {
        pager!!.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int, positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            @SuppressLint("SetTextI18n")
            override fun onPageSelected(position: Int) {
                pagerPosition = position
                pager!!.setCurrentItem(position, true)
                handlerTask(mSlideFlag)
                if (position >= sliderViewModel.mCurrentPostion) {
                    sliderViewModel.mCurrentPostion = position
                    sliderViewModel.callPaginationIfNeeded(requireContext())
                } else {
                    sliderViewModel.mCurrentPostion = position
                }
                tvPageIndex.text = (sliderViewModel.mCurrentPostion + 1).toString() + ""
                L.print(
                    this, "Page selected - $position " +
                            "sliderCurrentPosition = ${sliderViewModel.mCurrentPostion}"
                )

            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    var isStartedVideo = false

    fun singleTapDone(item: ParcelableItem) {
        if (mSlideFlag) {
            hideViews()
        } else {
            displaysViews()
            handlerTask(mSlideFlag)
        }

        if (item.mIsVideo) {
            L.print(this, "Current position - ${sliderViewModel.mCurrentPostion}")
            L.print(this, "Selected position - ${sliderViewModel.mSelectedPosition}")
            PreferenceHelper(requireContext()).applyPrefs(
                hashMapOf("mSelectedPosition" to sliderViewModel.mCurrentPostion)
            )
            L.print(this, "Selected before click - ${sliderViewModel.mCurrentPostion}")
            val videoFragment = VideoFragment()
            val bundle = Bundle()
            bundle.putParcelable("item", item)
            bundle.putInt("position",sliderViewModel.mCurrentPostion)
            bundle.putInt("size",sliderViewModel.mAlbumItemCount)
            videoFragment.arguments = bundle
            replaceFragment(R.id.fragment_container_view, videoFragment, true)
        }
    }

    fun hideViews() {
        try {
            grControllers.visibility = View.GONE
            toolbar.visibility = View.GONE
            mSlideFlag = false
        } catch (ex: Exception) {

        }
    }

    fun displaysViews() {
        mSlideFlag = true
        grControllers.visibility = View.VISIBLE
        toolbar.visibility = View.VISIBLE
    }

    fun handlerTask(slideFlag: Boolean) {
        mHandler.removeCallbacksAndMessages(null)
        if (slideFlag) {
            mHandler.postDelayed({ hideViews() }, 3000)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_slider_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share_image -> {
                if (Utility().isConnectingToInternet(requireActivity())) {

                    val item = sliderViewModel.getCurrentItem()
                    item?.let{
                        if(!it.mIsVideo){
                            sliderViewModel.mIsSharedButtonError = false
                            val dialog = ChoiceDialog()
                            dialog.resizedCall = {
                                sliderViewModel.isAllowCompress = true
                                sliderViewModel.startSharing(requireActivity(), rlProgressBarLayout)
                                hideViews()
                            }
                            dialog.originalsCall = {
                                sliderViewModel.isAllowCompress = false
                                sliderViewModel.startSharing(requireActivity(), rlProgressBarLayout)
                                hideViews()
                            }
                            dialog.cancelCall = {
                                sliderViewModel.isAllowCompress = false
                            }
                            dialog.showChoiceDialog(requireContext())
                        } else {
                            hideViews()
                            sliderViewModel.isAllowCompress = false
                            sliderViewModel.startSharing(requireActivity(), rlProgressBarLayout)
                        }
                    }
                } else {
                    rlInternetNotAvailable.visibility = View.VISIBLE
                    sliderViewModel.mIsSharedButtonError = true
                }
                true
            }
            R.id.remove_image -> {
                sliderViewModel.tryToDelete(requireContext()){
                    requireActivity().finish()
                }
                true
            }
            R.id.save_image -> {
                sliderViewModel.saveImage(requireActivity() as AppCompatActivity,rlProgressBarLayout)
                true
            }
            R.id.copy_url -> {
                sliderViewModel.copyUrl(requireActivity())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        sliderViewModel.stopSharing()
        super.onDestroyView()

    }
}