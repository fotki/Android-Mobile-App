package com.tbox.fotki.view.activities.general

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.roughike.bottombar.BottomBar
import com.roughike.bottombar.BottomBarTab
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.Folder
import com.tbox.fotki.model.entities.FragmentType
import com.tbox.fotki.model.web_providers.FolderProvider
import com.tbox.fotki.refactoring.screens.upload_files.UploadFilesFragment
import com.tbox.fotki.util.Constants
import com.tbox.fotki.view.fragments.fragment_navigation.FragNavController
import com.tbox.fotki.util.L
import com.tbox.fotki.util.Utility
import com.tbox.fotki.view.activities.NavigationActivity
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import com.tbox.fotki.view.fragments.BaseFragment
import com.tbox.fotki.view.fragments.folder_fragment.FolderFragment
import com.tbox.fotki.view.fragments.upload_file_fragment.UploadFileFragment
import kotlinx.android.synthetic.main.activity_main_tab.*

open class FragmentsManagerActivity : NavigationActivity(),
    BaseFragment.FragmentNavigation,
    FragNavController.TransactionListener, FragNavController.RootFragmentListener {

    val PUBLIC_TAB = FragNavController.TAB1
    val PRIVATE_TAB = FragNavController.TAB2
    val UPLOAD_TAB = FragNavController.TAB3

    lateinit var bottomBarLayout: BottomBar

    protected var mNavController: FragNavController? = null
    protected var mPublicFotkiTabInterface: FotkiTabActivity.FotkiTabInterface? = null
    protected var mPrivateFotkiTabInterface: FotkiTabActivity.FotkiTabInterface? = null

    private lateinit var mUploadFileFragment: UploadFilesFragment

    var savedInstanceState: Bundle? = null
    private var startedMode = FragmentType.ROOT_PUBLIC

    override fun onBackPressed() {
        if (intent.getIntExtra(
                FotkiTabActivity.EXTRA_TYPE,
                FotkiTabActivity.TYPE_USUAL) == FotkiTabActivity.TYPE_BACKUP){
            super.onBackPressed()

        } else if (!mNavController!!.isRootFragment) {
            mNavController!!.popFragment()
        } else if (mNavController!!.isRootFragment) {
            if (mNavController!!.currentFrag != null && mNavController!!.currentFrag != mUploadFileFragment) {
                moveTaskToBack(true)
            }
        } else {
            moveTaskToBack(true)
            bottomBar.visibility = View.VISIBLE
        }
    }

    protected fun initilizeFotkiTabFragments(
        publicFolder: Folder?, privateFolder: Folder?, backupFolder: Folder?,
        fragmentType: FragmentType?
    ) {
        mNavController!!.clearStack()
        startedMode = fragmentType!!
        L.print(
            this, "initialize - $publicFolder " +
                    "$privateFolder $mPublicFotkiTabInterface")

        if (mPublicFotkiTabInterface == null) {
            getRootFragment(PUBLIC_TAB)
        }

        if (fragmentType === FragmentType.BACKUP_UPLOAD){
            L.print(this,"folder - ${backupFolder!!.mFolderName}")
            mPublicFotkiTabInterface!!.sendFolderResponse(backupFolder)
            bottomBarLayout.visibility = GONE
        } else {
            publicFolder?.let { mPublicFotkiTabInterface!!.sendFolderResponse(it) }
            bottomBarLayout.selectTabAtPosition(PUBLIC_TAB)
            if (fragmentType === FragmentType.ROOT_PRIVATE) {
                bottomBarLayout.selectTabAtPosition(PRIVATE_TAB)
            } else {
                bottomBarLayout.selectTabAtPosition(PUBLIC_TAB)
            }
        }
    }

    protected fun registerUploadFragment() {
        mUploadFileFragment = if (savedInstanceState == null) {
            //UploadFileFragment.newInstance(getString(R.string.tab_type_public))
            UploadFilesFragment()
        } else {
            this@FragmentsManagerActivity.supportFragmentManager
                .findFragmentByTag("your_fragment_tag") as UploadFilesFragment
        }
    }


    protected fun setBottomBar(savedInstanceState: Bundle?,bottomBarTag:MutableLiveData<String>
            ,bottomBarPosition:MutableLiveData<Int> ) {

        bottomBarLayout.selectTabAtPosition(PUBLIC_TAB)

        mNavController = FragNavController.newBuilder(
            savedInstanceState,
            supportFragmentManager, R.id.container
        )
            .transactionListener(this)
            .rootFragmentListener(this, 3)
            .build()

        bottomBarLayout.setOnTabSelectListener { tabId ->
            when (tabId) {
                R.id.bb_menu_recents -> {
                    //bottomBarPosition.value = 0
                    bottomBarTag.value = Constants.PUBLIC_TAB

                    mNavController!!.switchTab(PUBLIC_TAB)
                    val bottomBarTab1 = bottomBarLayout.findViewById<BottomBarTab>(
                        R.id.bb_menu_recents
                    )
                    bottomBarTab1.tag = Constants.PUBLIC_TAB
                }
                R.id.bb_menu_favorites -> {
                    //bottomBarPosition.value = 1
                    if (mPrivateFotkiTabInterface == null) {
                        val utility = Utility.instance
                        utility.showProgressDialog(this,R.string.text_loading_your_album)
                        getRootFragment(PRIVATE_TAB)
                        val provider = FolderProvider(this)
                        provider.loadPrivate()
                        provider.privateFolder.observe(this, Observer {
                            res ->
                                bottomBarLayout.selectTabAtPosition(PRIVATE_TAB)
                                mPrivateFotkiTabInterface!!.sendFolderResponse(res)
                                utility.dismissProgressDialog()
                        })
                    }

                    bottomBarTag.value = Constants.PRIVATE_TAB

                    mNavController!!.switchTab(PRIVATE_TAB)
                    val bottomBarTab2 = bottomBarLayout.findViewById<BottomBarTab>(
                        R.id.bb_menu_favorites
                    )
                    bottomBarTab2.tag = Constants.PRIVATE_TAB
                }
                R.id.bb_menu_upload -> {
                    //bottomBarPosition.value = 2
                    bottomBarTag.value = Constants.UPLOAD_TAB

                    mNavController!!.switchTab(UPLOAD_TAB)
                    val bottomBarTab3 = bottomBarLayout.findViewById<BottomBarTab>(
                        R.id.bb_menu_upload
                    )
                    bottomBarTab3.tag = Constants.UPLOAD_TAB
                }
            }
        }
        bottomBarLayout.setOnTabReselectListener { mNavController!!.clearStack() }
    }

    override fun pushFragment(fragment: Fragment) {
        if (mNavController != null) {
            mNavController!!.pushFragment(fragment)
        }
    }

    override fun popFragment() {
        if (mNavController != null) {
            mNavController!!.popFragment()
        }
    }

    override fun onTabTransaction(fragment: Fragment, index: Int) {
        if (supportActionBar != null && mNavController != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(!mNavController!!.isRootFragment)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.menu_back_icon)
        }
        if (supportActionBar != null && mNavController != null
            && mNavController!!.isRootFragment
        ) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.menu_hamburger)
        }
    }

    override fun onFragmentTransaction(
        fragment: Fragment,
        transactionType: FragNavController.TransactionType
    ) {
        if (supportActionBar != null && mNavController != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(!mNavController!!.isRootFragment)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.menu_back_icon)
            bottomBarLayout.visibility = GONE
        }
        if (supportActionBar != null && mNavController != null
            && mNavController!!.isRootFragment
        ) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.menu_hamburger)

            if (startedMode!== FragmentType.BACKUP_UPLOAD)
                bottomBarLayout.visibility = VISIBLE
        }
    }

    override fun getRootFragment(index: Int): Fragment {
        when (index) {
            PUBLIC_TAB -> {
                val publicFolderFragment =
                    FolderFragment.newInstance(
                        getString(
                            R
                                .string.tab_type_public
                        )
                    )
                publicFolderFragment.folderTag = Constants.ROOT_FRAGMENT
                this.mPublicFotkiTabInterface = publicFolderFragment
                return publicFolderFragment
            }
            PRIVATE_TAB -> {
                val privateFolderFragment =
                    FolderFragment.newInstance(
                        getString(
                            R
                                .string.tab_type_private
                        )
                    )
                privateFolderFragment.folderTag = Constants.ROOT_FRAGMENT
                this.mPrivateFotkiTabInterface = privateFolderFragment
                return privateFolderFragment
            }
            UPLOAD_TAB -> return mUploadFileFragment
        }
        throw IllegalStateException(R.string.text_execption_illegal.toString() + "")
    }
}