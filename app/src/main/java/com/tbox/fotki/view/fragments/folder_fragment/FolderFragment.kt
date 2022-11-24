package com.tbox.fotki.view.fragments.folder_fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.VolleyError
import com.tbox.fotki.R
import com.tbox.fotki.databinding.FragmentFolderBinding
import com.tbox.fotki.model.entities.*
import com.tbox.fotki.util.FotkiUpdateFolderInterface
import com.tbox.fotki.util.Utility
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.model.web_providers.web_manager.WebManagerInterface
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import com.tbox.fotki.view.adapters.FolderRecyclerAdapter
import com.tbox.fotki.view.fragments.BaseFragment
import com.tbox.fotki.view.fragments.update_album_fragment.UpdateAlbumFragment
import com.tbox.fotki.view.fragments.update_folder_fragment.UpdateFolderFragment
import com.tbox.fotki.view.fragments.album_fragment.AlbumFragment
import com.tbox.fotki.view.view.SpacesItemDecoration
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


@Suppress("NAME_SHADOWING")
class FolderFragment : BaseFragment(), FotkiTabActivity.FotkiTabInterface,
    FotkiTabActivity.FotkiApiCallingFailInterface,
    FotkiUpdateFolderInterface,
    View.OnClickListener,
    WebManagerInterface, SwipeRefreshLayout.OnRefreshListener {


    private lateinit var rvMain:RecyclerView
    private var gridAdapter:FolderRecyclerAdapter? = null
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var viewModel: FolderFragmentViewModel


    private var mFolder: Folder? = null
    private var rootView: View? = null
    var folderTag: String? = null
    private var clickAction: String? = null
    private var mUtility = Utility.instance
    private var isApiFailure = false
    private var FRAGMENT_TYPE: Enum<*>? = null
    private val mReloadFolder = Folder()
    private var isLandscape = false

    private val screenConfiguration: Int
        get() {
            var gridColumn = 0
            if (activity != null) {
                gridColumn = if (requireActivity().resources.getBoolean(R.bool.is_landscape)) {
                    5
                } else {
                    3
                }
            }
            return gridColumn
        }

    private fun setFolder(folder: Folder) {
        this.mFolder = folder
    }
    //-------------------------------------------------------------------------------------Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        isLandscape = requireActivity().resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT
        L.print(this,"FolderFragment - ON CREATE! $isLandscape")
    }



    override fun onStart() {
        super.onStart()
        checkFolderRootType()

//        L.print(this,"FolderFragment - ON START! $isLandscape")
//        if( isLandscape != (requireActivity().resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT)){
//            rvMain.layoutManager = GridLayoutManager(activity, screenConfiguration)
//            rvMain.addItemDecoration(SpacesItemDecoration(2))
//            isLandscape = requireActivity().resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT
//            L.print(this,"FolderFragment - ON START CONFIGURATION CHANGED")
//        }
//        rvMain.addItemDecoration(SpacesItemDecoration(2))
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        L.print(this,"FolderFragment - ON CREATEVIEW! $rootView")

        return if (rootView == null) {
            viewModel = ViewModelProviders.of(requireActivity()).
                get(FolderFragmentViewModel::class.java)
            initBindingViewModel(inflater,container)

            viewModel.folderDescription.value = ""
            registerView()
            assignValuesToview()
            registerViewOnclick()
            checkIfApiFailed()
            //can be commented to ignore folder is empty
            //`checkIfFolderIsEmpty()
            if (mFolder!=null) {
                callGetFolderContentAPI()
            }
               // mUtility.showProgressDialog(activity!!, R.string.text_progress_bar_wait) }
            rootView
        } else {
            assignValuesToview()
            checkIfApiFailed()
            //can be commented to ignore folder is empty
            //checkIfFolderIsEmpty()
            rootView
        }
    }
    override fun onResume() {
        super.onResume()
        L.print(this,"FolderFragment - ON RESUME!")
        if (!(activity as AppCompatActivity).supportActionBar!!.isShowing) {
            (activity as AppCompatActivity).supportActionBar!!.show()
        }
        if (activity != null) {
            if (mFolder != null && (mFolder!!.mAlbums.size > 0 || mFolder!!.mFolders.size > 0)) {
                viewModel.emptyFolderVisibility.value = GONE
                val column = screenConfiguration
                //mGridViewView!!.numColumns = column
                var folderCurrent = mFolder
                for (folder in mFolder!!.mFolders) {
                    if (folder.mFolderName == Folder.BACKUP_FOLDER_NAME) {
                        folderCurrent = folder
                    }
                }

                if (requireActivity().intent.getIntExtra(
                        FotkiTabActivity.EXTRA_TYPE,
                        FotkiTabActivity.TYPE_USUAL
                    )
                    == FotkiTabActivity.TYPE_BACKUP
                ) {
                    viewModel.folderName.value = folderCurrent!!.mFolderName

                } else {
                    viewModel.folderName.value = mFolder!!.mFolderName
                }
                gridAdapter?.reloadFolder(mFolder,column)
            }
        }
        saveFragmentType()
    }
    private fun initBindingViewModel(inflater: LayoutInflater,
                                     container: ViewGroup?) {
        val binding = DataBindingUtil
            .inflate<FragmentFolderBinding>(inflater,R.layout.fragment_folder,container,false)
        rootView = binding.getRoot()
        binding.viewModel = viewModel
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        val column = screenConfiguration
        //rvMain.layoutManager = GridLayoutManager(activity, column)
        super.onConfigurationChanged(newConfig)
    }
    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_create -> {
                if (mUtility.isConnectingToInternet(requireActivity())) {
                    clickAction = null
                    creatNewAlbum()
                } else {
                    clickAction = Constants.ACTION_CREATE_ALBUM
                    hideMainView()
                }
            }
            R.id.btn_create_folder -> {
                if (mUtility.isConnectingToInternet(requireActivity())) {
                    clickAction = null
                    createNewFolder()
                } else {
                    clickAction = Constants.ACTION_CREATE_FOLDER
                    hideMainView()
                }
            }
            R.id.btn_retry -> {
                if (mUtility.isConnectingToInternet(requireActivity())) {
                    showMainView()
                    clickAction = Constants.ACTION_RELOAD_FOLDER
                    mUtility.showProgressDialog(requireActivity(), R.string.text_progress_bar_wait)
                    (activity as FotkiTabActivity).loadFoldersFromAccount()
                    //callGetFolderContentAPI()
                }
            }
            /*R.id.btn_retry -> if (clickAction != null) {
                if (clickAction == Constants.ACTION_CREATE_ALBUM) {
                    if (mUtility.isConnectingToInternet(activity!!)) {
                        showMainView()
                        clickAction = null
                        creatNewAlbum()
                    }
                } else if (clickAction == Constants.ACTION_CREATE_FOLDER) {
                    if (mUtility.isConnectingToInternet(activity!!)) {
                        showMainView()
                        clickAction = null
                        createNewFolder()
                    }
                } else if (clickAction == Constants.ACTION_UPDATE_FOLDER) {
                    if (mUtility.isConnectingToInternet(activity!!)) {
                        showMainView()
                        clickAction = null
                        updateFolder()
                    }
                } else if (clickAction == Constants.ACTION_RELOAD_FOLDER) {
                    if (mUtility.isConnectingToInternet(activity!!)) {
                        showMainView()
                        clickAction = Constants.ACTION_RELOAD_FOLDER
                        mUtility.showProgressDialog(activity!!, R.string.text_progress_bar_wait)
                        callGetFolderContentAPI()
                    }
                }

                //break
            } else if (FRAGMENT_TYPE != null) {
                if (FRAGMENT_TYPE === FragmentType.ROOT_PUBLIC) {
                    FRAGMENT_TYPE = null
                    val intent = Intent(Constants.RETRY_API_CALL)
                    intent.putExtra(Constants.FRAGMENTTYPEENUM, FragmentType.ROOT_PUBLIC)
                    LocalBroadcastManager.getInstance(activity!!).sendBroadcast(intent)
                } else if (FRAGMENT_TYPE === FragmentType.ROOT_PRIVATE) {
                    FRAGMENT_TYPE = null
                    val intent = Intent(Constants.RETRY_API_CALL)
                    intent.putExtra(Constants.FRAGMENTTYPEENUM, FragmentType.ROOT_PRIVATE)
                    LocalBroadcastManager.getInstance(activity!!).sendBroadcast(intent)
                }
            }*/
        }
    }

    //----------------------------------------------------------------------Lifecycle-helper-methods
    //onStart
    private fun checkFolderRootType() {
        val args = this.arguments
        if (args != null) {
            val tabType = args.getString(ARGS_INSTANCE)
            if (tabType == "public") {
                (activity as AppCompatActivity).supportActionBar!!.setTitle(
                    R.string.tab_type_public
                )
            } else {
                (activity as AppCompatActivity).supportActionBar!!.setTitle(
                    R.string.tab_type_private
                )
            }
        } else {
            (activity as AppCompatActivity).supportActionBar!!.setTitle(
                R.string.tab_type_test
            )
        }
    }
    //onCreateView
    private fun registerView() {
        val column = screenConfiguration
        mDrawerLayout = requireActivity().findViewById(R.id.drawer_layout)
        swipeRefreshLayout = rootView!!.findViewById(R.id.swipe_refresh_layout)
        rvMain = rootView!!.findViewById(R.id.rvMain)
        if (requireActivity().getResources().configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            rvMain.layoutManager = GridLayoutManager(activity, 3)
        } else {
            rvMain.layoutManager = GridLayoutManager(activity, 5)
        }
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.spacing)
        rvMain.addItemDecoration(SpacesItemDecoration(spacingInPixels))
        gridAdapter = FolderRecyclerAdapter(this@FolderFragment,mFolder,column)
        rvMain.adapter = gridAdapter
        swipeRefreshLayout.setOnRefreshListener(this)
    }
    private fun assignValuesToview() {
        if (mFolder != null) {
            rvMain.visibility = VISIBLE
            viewModel.folderName.value = mFolder!!.mFolderName
            L.print(this,"folder description - ${mFolder!!.mDescription}")
            if (mFolder!!.mDescription.isEmpty()) {
                viewModel.folderDescriptionVisibility.value = GONE
                viewModel.folderDescription.value = ""
            } else {
                viewModel.folderDescriptionVisibility.value = VISIBLE
                viewModel.folderDescription.value = mFolder!!.mDescription
            }
        }
    }
    private fun registerViewOnclick() {
        rootView?.findViewById<Button>(R.id.btn_retry)?.setOnClickListener(this)
        rootView?.findViewById<Button>(R.id.btn_create_folder)?.setOnClickListener(this)
        rootView?.findViewById<Button>(R.id.btn_create)?.setOnClickListener(this)
    }
    private fun checkIfFolderIsEmpty() {
        if (mFolder == null) {
            Log.d("FolderFragmentNetErrChk","if mFolder = null from checkIfFolderIsEmpty")
            L.print(this,"Error")
            viewModel.internetNoAvailableVisibility.value = GONE
            viewModel.emptyFolderVisibility.value = VISIBLE
        } else if (mFolder!!.mFolders.size == 0 && mFolder!!.mAlbums.size == 0) {
            Log.d("FolderFragmentNetErrChk","if mFolder!!.mFolders.size == 0 && mFolder!!.mAlbums.size == 0 from checkIfFolderIsEmpty")

            viewModel.emptyFolderVisibility.value = VISIBLE
        } else {
            Log.d("FolderFragmentNetErrChk","ele from checkIfFolderIsEmpty")

            //callGetFolderContentAPI()
            viewModel.emptyFolderVisibility.value = GONE
        }
    }
    private fun checkIfApiFailed() {
        if (isApiFailure) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            hideMainView()
        } else {

        }
    }
    //onResume
    private fun saveFragmentType() {
        requireActivity().getSharedPreferences(Constants.DEFTYPE_FILE, Context.MODE_PRIVATE).edit()
            .putString(
                Constants.DEFTYPE, Constants.DEFTYPE_KEY_FOLDER
            ).apply()
    }

    //-----------------------------------------------------------------------------------Option-menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        if (this.folderTag != null) {
            if (this.folderTag == Constants.ROOT_FRAGMENT) {
                inflater.inflate(R.menu.dropdown_fotki_base_folder_fragment, menu)
            } else {
                inflater.inflate(R.menu.dropdown_fotki_folder_fragment, menu)
            }
        }
        super.onCreateOptionsMenu(menu, inflater)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        when (itemId) {
            R.id.create_folder -> {
                if (mUtility.isConnectingToInternet(requireActivity())) {
                    clickAction = null
                    createNewFolder()
                } else {
                    clickAction = Constants.ACTION_CREATE_FOLDER
                    hideMainView()
                }
                return true
            }
            R.id.create_album -> {
                if (mUtility.isConnectingToInternet(requireActivity())) {
                    clickAction = null
                    creatNewAlbum()
                } else {
                    clickAction = Constants.ACTION_CREATE_ALBUM
                    hideMainView()
                }
                return true
            }
            R.id.update_folder -> {
                if (mUtility.isConnectingToInternet(requireActivity())) {
                    clickAction = null
                    updateFolder()
                } else {
                    clickAction = Constants.ACTION_UPDATE_FOLDER
                    hideMainView()
                }
                return true
            }
            R.id.copy_toClipBoard -> {
                if (mUtility.isConnectingToInternet(requireActivity())) {
                    clickAction = null
                    copyToClipBoard()
                }
                return true
            }
            R.id.share_via -> {
                if (mUtility.isConnectingToInternet(requireActivity())) {
                    clickAction = null
                    sendLinkThroughImplicitIntent()
                }
                return true
            }
            R.id.reload_folder -> {
                if (mUtility.isConnectingToInternet(requireActivity())) {
                    mUtility.showProgressDialog(requireActivity(), R.string.text_progress_bar_wait)
                    clickAction = Constants.ACTION_RELOAD_FOLDER
                    callGetFolderContentAPI()
                } else {
                    clickAction = Constants.ACTION_RELOAD_FOLDER
                    hideMainView()
                }
                return true
            }
            else -> {
            }
        }
        return false
    }

    private fun createNewFolder() {
        val createFolderFragment =
            UpdateFolderFragment.newInstance()
        createFolderFragment.setFolderID(mFolder!!.mFolderIdEnc)
        createFolderFragment.setFragmentType(FragmentType.CREATE_FOLDER_FRAGMENT)
        createFolderFragment.setFotkiUpdateFolderListner(this)
        mFragmentNavigation.pushFragment(createFolderFragment)
    }
    private fun creatNewAlbum() {
        val createAlbumFragment =
            UpdateAlbumFragment.newInstance()
        createAlbumFragment.setFolderID(mFolder!!.mFolderIdEnc)
        createAlbumFragment.setFragmentType(FragmentType.CREATE_ALBUM_FRAGMENT)
        createAlbumFragment.setFotkiUpdateFolderListner(this)
        mFragmentNavigation.pushFragment(createAlbumFragment)
    }
    private fun hideMainView() {
        Log.d("FolderFragmentNetErrChk"," from hideMainView")
        mUtility.dismissProgressDialog()
        viewModel.internetNoAvailableVisibility.value = VISIBLE
        rootView?.findViewById<RecyclerView>(R.id.rvMain)?.visibility = GONE
        L.print(this,"Network error")
    }

    private fun copyToClipBoard() {
        val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("fotki_label", mFolder!!.mShareUrl)
        //TODO fix clipboard
        //clipboard.primaryClip = clip
        Toast.makeText(activity, "Link copied to clipboard", Toast.LENGTH_LONG).show()
    }
    private fun sendLinkThroughImplicitIntent() {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_TEXT, mFolder!!.mShareUrl)
        shareIntent.type = "text/plain"
        startActivity(shareIntent)
    }

    // onclick of adapter
    fun handleOnClickListner(item: Any) {
        if (item is Folder) {
            val folderFragment =
                newInstance()
            folderFragment.setFolder(item)
            folderFragment.folderTag = Constants.NEXT_FRAGMENT
            mFragmentNavigation.pushFragment(folderFragment)
        } else if (item is ParcelableAlbum) {
            Log.d("albumCoverError","album items "+item.mCoverUrl+"item "+item)
            L.print(this, "Album fragment pushed")
            pushAlbumFragment(item)
        }
    }

    private fun pushAlbumFragment(item: ParcelableAlbum) {
        val albumFragment = AlbumFragment.newInstance()
        albumFragment.setAlbum(item.mAlbumIdEnc)
        albumFragment.setFolderId(mFolder!!.mFolderIdEnc)
        albumFragment.setFotkiUpdateFolderListner(this)
        L.print(this, "Pushed - $albumFragment")
        mFragmentNavigation.pushFragment(albumFragment)
    }

    //---------------------------------------------------------------------------------Web-interface
    private fun callGetFolderContentAPI() {
        Log.d("FolderFragmentNetErrChk","fun callGetFolderContentAPI")

        //showMainView()
        clickAction = Constants.ACTION_RELOAD_FOLDER
        mFolder?.mFolders?.clear()
        mFolder?.mAlbums?.clear()
        //madapter?.setFolder(mFolder!!, 3)
        //madapter?.notifyDataSetChanged()
        WebManager.instance.getFolderContent(
            requireActivity().baseContext, this,
            mFolder!!.mFolderIdEnc
        )
        //(activity as FotkiTabActivity).loadFoldersFromAccount()
    }

    // interface implementation
    override fun sendFolderResponse(mFolder: Folder?) {
        try {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            //mRelativeLayoutEmptyFolder.visibility = View.GONE
            viewModel.emptyFolderVisibility.value = GONE
            if (mFolder==null){
                this.mFolder = null
                throw Exception()
            }
            this.mFolder = mFolder
            L.print(this,"folder - $mFolder")
            Log.d("FolderFragmentNetErrChk","fun sendFolderResponse")
            showMainView()
            assignValuesToview()
            val column = screenConfiguration

            gridAdapter?.reloadFolder(mFolder,column)

            mUtility.dismissProgressDialog()
        } catch (e: Exception) {
            mUtility.dismissProgressDialog()
        }
    }
    override fun sendSuccess(folder: Folder) {
        clearFolderData()
        viewModel.emptyFolderVisibility.value = GONE
        populateUpdateFolderData(folder)
        val column = screenConfiguration
        gridAdapter?.reloadFolder(mFolder,column)
        mUtility.dismissProgressDialog()
    }
    override fun sendSuccess(name: String, descrip: String) {
        mFolder!!.mDescription = descrip
        mFolder!!.mFolderName = name
        assignValuesToview()
        val column = screenConfiguration
        viewModel.emptyFolderVisibility.value = GONE
        gridAdapter?.reloadFolder(mFolder,column)
        mUtility.dismissProgressDialog()
    }
    override fun sendfailure(fragmentType: FragmentType) {
        clickAction = null
        isApiFailure = true
        this.FRAGMENT_TYPE = fragmentType
    }
    override fun sendNetworkFailure(
        isInterNetAvailableFlag: Boolean,
        apiRequestType: ApiRequestType
    ) {
        swipeRefreshLayout.isRefreshing = false
        mUtility.dismissProgressDialog()
        hideMainView()

    }
    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {
        swipeRefreshLayout.isRefreshing = false
        mUtility.dismissProgressDialog()

    }
    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
        when {
            apiRequestType === ApiRequestType.GET_FOLDER_CONTENT -> {
                getFolderContentFomResponse(response)

            }
        }
    }
    override fun onRefresh() {
        fetchReloadData()
    }


    // parse updated album from response
    private fun parseAlbum(jsonArray: JSONArray): ArrayList<ParcelableAlbum> {
        val mAlbumArray = ArrayList<ParcelableAlbum>()
        for (i in 0 until jsonArray.length()) {
            val newList = ArrayList<ParcelableItem>()
            val album: ParcelableAlbum
            album = ParcelableAlbum(
                0,
                "",
                "",
                "",
                newList,
                ""
            )
            try {
                album.mAlbumIdEnc = jsonArray.getJSONObject(i).getLong(Constants.ALBUM_ID_ENC)
                album.mName = jsonArray.getJSONObject(i).getString(Constants.ALBUM_NAME)
                album.mdescription = jsonArray.getJSONObject(i).getString(Constants.DESCRICTION)
                album.mCoverUrl = jsonArray.getJSONObject(i).getString(Constants.COVER_PHOTO_URL)
                album.mShareUrl = jsonArray.getJSONObject(i).optString(Constants.SHARE_URL)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            mAlbumArray.add(album)
        }
        return mAlbumArray
    }

    // parse updated folder from respone
    private fun parseFolder(jsonArray: JSONArray): ArrayList<Folder> {
        val mFoldersArray = ArrayList<Folder>()
        for (i in 0 until jsonArray.length()) {
            val mFolder = Folder()
            try {
                mFolder.setData(
                    jsonArray.getJSONObject(i).getLong(
                        Constants.FOLDER_ID_ENC
                    ),/*
                    0,
                    0,*/
                    jsonArray.getJSONObject(i).getLong(
                        Constants.NUMBER_OF_FOLDER
                    ),
                    jsonArray.getJSONObject(i).getLong(
                        Constants.NUMBER_OF_ALBUMS
                    ),
                    jsonArray.getJSONObject(i).getString(Constants.DESCRICTION),
                    jsonArray.getJSONObject(i).getString(Constants.FOLDER_NAME),
                    /*parseAlbum(
                        jsonArray.getJSONObject(i).getJSONArray(
                            Constants.ALBUMS
                        )
                    ),*/
                    ArrayList<ParcelableAlbum>(),
                    /*parseFolder(
                        jsonArray.getJSONObject(i).getJSONArray(
                            Constants.FOLDERS
                        )
                    )*/ArrayList<Folder>(),
                    jsonArray.getJSONObject(i).optString(Constants.SHARE_URL)
                )
                mFoldersArray.add(mFolder)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        return mFoldersArray
    }

    // load new data from respone
    private fun loadFolder(folder: Folder, `object`: JSONObject) {
        var `object` = `object`

        mUtility.dismissProgressDialog()
        try {
            if (`object`.getInt(Constants.OK) == 1) {
                `object` = `object`.getJSONObject(Constants.DATA)
                folder.setData(
                    `object`.getLong(Constants.FOLDER_ID_ENC),
                    //0,
                    //0,
                    `object`.getLong(Constants.NUMBER_OF_FOLDER),
                    `object`.getLong(Constants.NUMBER_OF_ALBUMS),
                    `object`.getString(Constants.DESCRICTION),
                    `object`.getString(Constants.FOLDER_NAME),
                    //ArrayList<ParcelableAlbum>(),
                    //ArrayList<Folder>(),
                    parseAlbum(`object`.getJSONArray(Constants.ALBUMS)),
                    parseFolder(`object`.getJSONArray(Constants.FOLDERS)),
                    `object`.optString(Constants.SHARE_URL)
                )
            } else {
                mUtility.dismissProgressDialog()
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    // get update folder content from response and notify listner
    private fun getFolderContentFomResponse(response: JSONObject) {

        loadFolder(mReloadFolder, response)
        clearFolderData()
        populateUpdateFolderData(mReloadFolder)
        checkIfFolderIsEmpty()
        val column = screenConfiguration
        gridAdapter?.reloadFolder(mFolder,column)
        mUtility.dismissProgressDialog()
        clickAction = null
        swipeRefreshLayout.isRefreshing = false
    }

    //clear folder data so old memeory references can be use again
    private fun clearFolderData() {
        mFolder!!.mFolders.clear()
        mFolder!!.mAlbums.clear()
        mFolder!!.mFolderIdEnc = 0L
        mFolder!!.mDescription = ""
        mFolder!!.mFolderName = ""
    }

    // populate updated folder data on old refrences
    private fun populateUpdateFolderData(folder: Folder) {
        mFolder!!.mFolders.addAll(folder.mFolders)
        mFolder!!.mAlbums.addAll(folder.mAlbums)
        mFolder!!.mFolderIdEnc = folder.mFolderIdEnc
        mFolder!!.mDescription = folder.mDescription
        mFolder!!.mFolderName = folder.mFolderName
    }

    private fun updateFolder() {
        val updateFragment =
            UpdateFolderFragment.newInstance()
        updateFragment.setFolderID(mFolder!!.mFolderIdEnc)
        updateFragment.setFragmentType(FragmentType.UPDATE_FOLDER_FRAGMENT)
        updateFragment.setFolderNameWithDescription(
            mFolder!!.mFolderName,
            mFolder!!.mDescription
        )
        updateFragment.setFotkiUpdateFolderListner(this)
        mFragmentNavigation.pushFragment(updateFragment)
    }

    private fun showMainView() {
        Log.d("FolderFragmentNetErrChk","fun showMainView")

        viewModel.internetNoAvailableVisibility.value = GONE
        rootView?.findViewById<RecyclerView>(R.id.rvMain)?.visibility = VISIBLE
        checkIfFolderIsEmpty()

    }

    private fun fetchReloadData() {
        try {
            if (mUtility.isConnectingToInternet(requireActivity())) {
                clickAction = Constants.ACTION_RELOAD_FOLDER
                swipeRefreshLayout.isRefreshing = true
                callGetFolderContentAPI()
            } else {
                swipeRefreshLayout.isRefreshing = false
                clickAction = Constants.ACTION_RELOAD_FOLDER
                hideMainView()
            }
        } catch (ex: Exception) {
            swipeRefreshLayout.isRefreshing = false
            clickAction = Constants.ACTION_RELOAD_FOLDER
            hideMainView()
        }
    }

    companion object {
        fun newInstance(tabType: String): FolderFragment {
            val fragment =
                FolderFragment()
            val args = Bundle()
            args.putString(ARGS_INSTANCE, tabType)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(): FolderFragment =
            FolderFragment()
    }
}