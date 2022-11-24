package com.tbox.fotki.view.fragments.album_fragment


//import kotlinx.android.synthetic.main.fragment_album.*
//import kotlinx.android.synthetic.main.item_folder.*
//import kotlinx.android.synthetic.main.item_folder.album_name
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.*
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.VolleyError
import com.simplemobiletools.commons.extensions.getRealPathFromURI
import com.tbox.fotki.R
import com.tbox.fotki.databinding.FragmentAlbumBinding
import com.tbox.fotki.model.entities.ApiRequestType
import com.tbox.fotki.model.entities.ApiRequestType.GET_ALBUM_CONTENT
import com.tbox.fotki.model.entities.ApiRequestType.GET_ALBUM_ITEM_COUNT
import com.tbox.fotki.model.entities.FragmentType
import com.tbox.fotki.model.entities.ParcelableAlbum
import com.tbox.fotki.model.entities.ParcelableItem
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.model.web_providers.web_manager.WebManagerInterface
import com.tbox.fotki.util.*
import com.tbox.fotki.util.AlbumHelper.browseMulipleItems
import com.tbox.fotki.util.sync_files.PreferenceHelper
import com.tbox.fotki.util.upload_files.FileType
import com.tbox.fotki.util.upload_files.FileUploader
import com.tbox.fotki.view.adapters.AlbumRecyclerAdapter
import com.tbox.fotki.view.dialogs.ChoiceSizeDialog
import com.tbox.fotki.view.fragments.BaseFragment
import com.tbox.fotki.view.fragments.folder_fragment.FolderFragment
import com.tbox.fotki.view.fragments.update_album_fragment.UpdateAlbumFragment
import com.tbox.fotki.refactoring.UriConverter
import com.tbox.fotki.view.view.SpacesItemDecoration
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.FileNotFoundException


@Suppress("NAME_SHADOWING", "UNREACHABLE_CODE")
class AlbumFragment : BaseFragment(),
    WebManagerInterface, View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private var rootView: View? = null
    private var mAlbumId: Long? = null
    private var mFolderId: Long? = null
    //private var mPageCount: Int = 0

    private lateinit var rvMain: RecyclerView
    private lateinit var adapter: AlbumRecyclerAdapter

    private val mUtility = Utility.instance
    private var mItems: ArrayList<ParcelableItem>? = null
    private var mAlbum: ParcelableAlbum? = null
    private lateinit var mFotkiUpdateFolderInterface: FotkiUpdateFolderInterface
    private lateinit var filePath: String
    private var mClickAction: String? = null
    private var REQUEST_URL: Enum<*>? = null
    private lateinit var mDialog: ChoiceSizeDialog
    private lateinit var choiceAlert: ListView

    private var fileTypeArrayList = ArrayList<FileType>()
    private val viewModel: AlbumFragmentViewModel by viewModel()
    //private lateinit var viewModel: AlbumFragmentViewModel

    fun setAlbum(albumId: Long?) {
        mAlbumId = albumId
    }

    fun setFolderId(folderID: Long?) {
        this.mFolderId = folderID
    }

    fun setFotkiUpdateFolderListner(fotkiUpdateFolderListner: FotkiUpdateFolderInterface) {
        mFotkiUpdateFolderInterface = fotkiUpdateFolderListner
    }

    //-------------------------------------------------------------------------------------Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        L.print(this, "ONCREATE AlbumFragment")
        setHasOptionsMenu(true)
//        throw RuntimeException("Test Crash")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        L.print(this, "ONATTACH AlbumFragment")
        //mActivity = activity!!
    }

    override fun onStart() {
        super.onStart()
        L.print(this, "ONSTART AlbumFragment")
        checkFolderRootType()
        val isDeleted = PreferenceHelper(requireContext()).getBoolean("deleted_item")
        if (isDeleted) {
            fetchReloadAlbum()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        L.print(this, "ONCREATEVIEW AlbumFragment")

        return return if (rootView === null) {
            setupViewModel(inflater, container)
            //activity?.let { mUtility.showProgressDialog(it, R.string.text_progress_bar_wait) }
            registerRecyclerView()
            registerListeners()
            registerGridViewOnScrollListner()
            registerLocalBroadCast()
            getAlbumItemCount()
            rootView
        } else {
            //getAlbumItemCount()
            rootView
        }
    }

    private fun setupViewModel(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) {
        /*viewModel = ViewModelProviders.of(requireActivity()).
            get(AlbumFragmentViewModel::class.java)*/
        initBindingViewModel(inflater, container)
        viewModel.albumName.value = ""
        viewModel.albumCount.value = ""
        viewModel.albumDescription.value = ""
    }

    override fun onResume() {
        super.onResume()
        L.print(this, "ONRESUME AlbumFragment")

        if (!(activity as AppCompatActivity).supportActionBar!!.isShowing) {
            (activity as AppCompatActivity).supportActionBar!!.show()
        }
        saveFragmentType()
        onRefresh()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            rvMain.layoutManager = GridLayoutManager(activity, 3)
        } else {
            rvMain.layoutManager = GridLayoutManager(activity, 5)
        }
        super.onConfigurationChanged(newConfig)
    }

    private fun getUris( data: Intent?):ArrayList<Uri>{
        val uris = ArrayList<Uri>()

        data?.data?.let { uri ->
            try {
                uris.add(uri)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        data?.clipData?.let { data ->
            L.print(this, "item count - ${data.itemCount} data - $data")
            for (i in 0 until data.itemCount) {
                val item = data.getItemAt(i)
                val uri = item.uri
                uris.add(uri)
            }
        }
        L.print(this,"Uris - $uris")
        return uris
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {

            val uris = getUris(data)

//            val configs = Configuration.Builder()
//                .setCheckPermission(true)
//                .setShowImages(true)
//                .setShowAudios(true)
//                .setShowFiles(true)
//                .setTitle("Select files")
//                //.enableImageCapture(true)
//                //.setMaxSelection(1000)
//                .setSkipZeroSizeFiles(true)
//                .build()

           /* val files: ArrayList<MediaFile> =
                data!!.getParcelableArrayListExtra(FilePickerActivity.MEDIA_FILES)
            val uris = ArrayList<Uri>()

            files.forEach { file->
                uris.add(file.uri)
            }*/

            L.print(this, "array files - $uris")
            //uris.addAll(Matisse.obtainResult(data))
            if (uris.size < 1) {
                return super.onActivityResult(requestCode, resultCode, data)
            }
            fileTypeArrayList.clear()
            var hasPhoto = false
            for (i in uris.indices) {
                val picUri = uris[i]
                val mimeType = requireActivity().contentResolver.getType(picUri)
                mimeType?.let {
                    if (mimeType.startsWith("image")) {
                        hasPhoto = true
                    }
                }
                val split =
                    mimeType!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                //filePath = AlbumHelper.getPath(activity, picUri)

                filePath = requireContext().getRealPathFromURI(picUri)?:""
                if(filePath.isEmpty()){
                    filePath = UriConverter(requireContext()).getPath(picUri)
                }
                //filePath = files[i].path
                //filePath = getPathFromURI(picUri)?:""
                L.print(this, "Path - $filePath")
                val fileType: FileType
                fileType = if (true && split[1] == "gif") FileType(filePath, split[1]) else {
                    FileType(filePath, split[0])
                }
                fileTypeArrayList.add(fileType)
            }
            if (mUtility.isConnectingToInternet(requireActivity())) {
                if (FileUploader.instance.isDelete) {
                    viewModel.isAllowCompress = false
                    if (mAlbum != null) {
                        startUploadingFiles(fileTypeArrayList, mAlbumId)
                    } else {
                        mUtility.showAlertDialog(requireActivity(), "Error! Please retry.")
                    }
                } else {
                    if (hasPhoto) {
                        showChoiceDialog()
                    } else {
                        startUploadingFiles(fileTypeArrayList, mAlbumId)
                    }
                }
            } else {
                mClickAction = Constants.ACTION_UPLOAD_Files
                setVisibiltyForNoInternetView()
            }
        }
    }

    private fun getPathFromURI(uri: Uri): String? {
        L.print(this, uri.path!!)
        val dataStr = arrayOf(MediaStore.Audio.Media.DATA)
        val loader = CursorLoader(requireActivity(), uri, dataStr, null, null, null)
        val cursor = loader.loadInBackground()
        cursor.moveToFirst()
        val fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
        L.print(this, "file is - $fileName")
        return fileName
    }

    override fun onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mMessageReceiver)
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mUploadReceiver)
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(
            mAlbumNameDescriptionUpdated
        )
        super.onDestroy()
    }

    //----------------------------------------------------------------------Options-menu-and-onClick
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.dropdown_fotki_album_fragment, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        when (itemId) {
            R.id.udpate_album -> {
                if (mUtility.isConnectingToInternet(requireActivity())) {
                    mClickAction = null
                    creatUpdateAlbumFragment()
                } else {
                    mClickAction = Constants.ACTION_UPDATE_ALBUM
                    setVisibiltyForNoInternetView()
                }
                return true
            }
            R.id.upload_files -> {
                L.print(this, "LOG UPLOAD from album - ${mAlbumId}")
                val fileUploader = FileUploader.instance

                if (!fileUploader.isUploading) {

                    fileUploader.mContext = activity

                    if (mUtility.isConnectingToInternet(requireActivity())) {
                        mClickAction = null
                        fileUploader.isDelete = false
                        L.print(this, "All ok, try to browse $fileUploader")
                        activity?.let { activity ->
                            AlbumHelper.browseMulipleItems(
                                activity,
                                this@AlbumFragment,
                                false
                            )
                        }
                    } else {
                        mClickAction = Constants.ACTION_UPLOAD_Files
                        setVisibiltyForNoInternetView()
                    }
                } else {
                    mUtility.showAlreadyUploadInProgress(requireActivity(), this@AlbumFragment)
                }
                return true
            }
            R.id.upload_and_delete_files -> {
                L.print(this, "LOG UPLOAD_AND_DELETE")
                val fileUploader = FileUploader.instance
                if (!fileUploader.isUploading) {
                    if (mUtility.isConnectingToInternet(requireActivity())) {
                        mClickAction = null
                        fileUploader.isDelete = true
                        activity?.let { browseMulipleItems(it, this@AlbumFragment, true) }
                    } else {
                        mClickAction = Constants.ACTION_UPLOAD_Files
                        setVisibiltyForNoInternetView()
                    }
                } else {
                    mUtility.showAlreadyUploadInProgress(requireActivity(), this@AlbumFragment)
                }
                return true
            }

            R.id.reload_Album -> {
                viewModel.isSwipeRefresh = false
                fetchReloadAlbum()
                return true
            }
            R.id.copy_toClipBoard -> {
                if (mUtility.isConnectingToInternet(requireActivity())) {
                    mClickAction = null
                    activity?.let { AlbumHelper.copyToClipBoard(it, mAlbum) }
                }
                return true
            }
            R.id.share_via -> {
                if (mUtility.isConnectingToInternet(requireActivity())) {
                    mClickAction = null
                    activity?.let { AlbumHelper.sendLinkThroughImplicitIntent(it, mAlbum) }
                }
                return true
            }
            else -> {
            }
        }
        return false
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_upload -> {
                val fileUploader = FileUploader.instance
                fileUploader.isDelete = false
                if (!fileUploader.isUploading) {
                    if (mUtility.isConnectingToInternet(requireActivity())) {
                        mClickAction = null
                        activity?.let { browseMulipleItems(it, this@AlbumFragment, false) }
                    } else {
                        mClickAction = Constants.ACTION_UPLOAD_Files
                        setVisibiltyForNoInternetView()
                    }
                } else {
                    mUtility.showAlreadyUploadInProgress(requireActivity(), this@AlbumFragment)
                }
            }
            R.id.btn_upload_and_delete -> {
                val fileUploader = FileUploader.instance
                fileUploader.isDelete = true
                if (!fileUploader.isUploading) {
                    if (mUtility.isConnectingToInternet(requireActivity())) {
                        mClickAction = null
                        activity?.let { browseMulipleItems(it, this@AlbumFragment, true) }
                    } else {
                        mClickAction = Constants.ACTION_UPLOAD_Files
                        setVisibiltyForNoInternetView()
                    }
                } else {
                    mUtility.showAlreadyUploadInProgress(requireActivity(), this@AlbumFragment)
                }
            }
            R.id.btn_retry ->
                if (mClickAction != null) {
                    if (mClickAction == Constants.ACTION_UPDATE_ALBUM) {
                        if (mUtility.isConnectingToInternet(requireActivity())) {
                            mClickAction = null
                            setVisibilityForView()
                            creatUpdateAlbumFragment()
                        }
                    } else if (mClickAction == Constants.ACTION_UPLOAD_Files) {
                        if (mUtility.isConnectingToInternet(requireActivity())) {
                            mClickAction = null
                            setVisibilityForView()
                            val uploader = FileUploader.instance
                            if (!uploader.isUploading) {
                                activity?.let {
                                    AlbumHelper.browseMulipleItems(
                                        it,
                                        this@AlbumFragment,
                                        uploader.isDelete
                                    )
                                }
                            } else {
                                mUtility.showAlreadyUploadInProgress(
                                    requireActivity(),
                                    this@AlbumFragment
                                )
                            }
                        }
                    }
                } else if (REQUEST_URL != null) {
                    if (REQUEST_URL === ApiRequestType.GET_ALBUM_ITEM_COUNT) {
                        if (viewModel.mContentUploaded) {
                            requestUpdatedAlbumCount()
                        } else {
                            getAlbumItemCount()
                        }
                    } else if (REQUEST_URL === ApiRequestType.GET_ALBUM_CONTENT) {
                        REQUEST_URL = null
                        getAlbumContent()
                    }
                }
        }
    }

    private fun creatUpdateAlbumFragment() {
        mAlbum?.let { album ->
            val updateAlbumFragment =
                UpdateAlbumFragment.newInstance()
            updateAlbumFragment.setFolderID(mFolderId)
            updateAlbumFragment.setFolderNameWithDescription(
                album.mName,
                album.mdescription
            )

            L.print(this, "Start album with description - ${album.mdescription}")
            updateAlbumFragment.setAlbumID(album.mAlbumIdEnc)
            updateAlbumFragment.setFragmentType(FragmentType.UPDATE_ALBUM_FRAGMENT)
            updateAlbumFragment.setFotkiUpdateFolderListner(mFotkiUpdateFolderInterface)
            mFragmentNavigation.pushFragment(updateAlbumFragment)
        }
    }

    private fun setVisibiltyForNoInternetView() {
        viewModel.topInfoVisibility.value = GONE
        viewModel.emptyAlbumVisibility.value = GONE
        viewModel.swipeRefreshVisibility.value = GONE
        viewModel.internetNotAvailableVisibility.value = VISIBLE
    }

    private fun fetchReloadAlbum() {
        try {
            if (mUtility.isConnectingToInternet(requireActivity())) {
                mClickAction = null
                REQUEST_URL = ApiRequestType.GET_ALBUM_ITEM_COUNT
                viewModel.isReloadAlbum = true
                mAlbum?.mitem?.clear()
                viewModel.mAlbumItemCount = 0
                notifyAdapter()
                viewModel.mPageCount = 1
                getAlbumReloadItemCount()
            } else {
                REQUEST_URL = ApiRequestType.GET_ALBUM_ITEM_COUNT
                viewModel.swipeRefreshActive.value = false
                setVisibiltyForNoInternetView()
            }
        } catch (ex: Exception) {

        }
    }

    //------------------------------------------------------------------------------------Broadcasts
    private fun registerLocalBroadCast() {
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            mMessageReceiver,
            IntentFilter(Constants.LOCAL_BROADCAST_ALBUM)
        )
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            mUploadReceiver,
            IntentFilter(Constants.UPLOAD_BROADCAST_ALBUM)
        )
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            mAlbumNameDescriptionUpdated,
            IntentFilter(Constants.ALBUM_NAME_UPDATED)
        )
    }

    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            val data = intent.extras
            mAlbum = data!!.getParcelable(Constants.PARCEABLE_ALBUM)
            viewModel.mPageCount = intent.getIntExtra(Constants.REQUEST_PAGE, 0)
            viewModel.mPageCount++
            viewModel.mAlbumItemCount = intent.getIntExtra(Constants.ITEM_COUNT, 0)
            notifyAdapter()
        }
    }
    private val mAlbumNameDescriptionUpdated = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val albumID = intent.getLongExtra(Constants.UPDATE_ALBUM_ID, 0)
            if (albumID == mAlbum!!.mAlbumIdEnc) {
                if (intent.getStringExtra(Constants.UPDATE_ALBUM_NAME) != "") {
                    mAlbum!!.mName = intent.getStringExtra(Constants.UPDATE_ALBUM_NAME)!!
                }
                if (intent.getStringExtra(Constants.UPDATE_ALBUM_DESCRIPTION) != "") {
                    mAlbum!!.mdescription =
                        intent.getStringExtra(Constants.UPDATE_ALBUM_DESCRIPTION)!!
                }
                assignValuesToView()
            }
        }
    }
    private val mUploadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //requestUpdatedAlbumCount()
            fetchReloadAlbum()
        }
    }

    //-----------------------------------------------------------------------------Lifecycle-helpers
    private fun saveFragmentType() {
        try {
            if(isAdded){
            requireContext().getSharedPreferences(Constants.DEFTYPE_FILE, Context.MODE_PRIVATE).edit()
                .putString(
                    Constants.DEFTYPE, Constants.DEFTYPE_KEY_ALBUM
                ).apply()
                requireContext().getSharedPreferences(Constants.DEFTYPE_FILE, Context.MODE_PRIVATE).edit()
                .putLong(
                    Constants.PREF_ALBUM_ID, mAlbumId!!
                ).apply()
            if (mAlbum != null) {
                requireContext().getSharedPreferences(Constants.DEFTYPE_FILE, Context.MODE_PRIVATE).edit()
                    .putString(
                        Constants.PREF_ALBUM_NAME, mAlbum!!.mName
                    ).apply()
            }
            val fileUploader = FileUploader.instance
            if (!fileUploader.isUploading) {
                saveAlbumPosition()
            }}
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

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

    private fun initBindingViewModel(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) {
        val binding = DataBindingUtil
            .inflate<FragmentAlbumBinding>(inflater, R.layout.fragment_album, container, false)
        binding?.let {
            rootView = it.root
            it.viewModel = viewModel
            viewModel.mPageCount = 1
        }
    }

    private fun registerRecyclerView() {
        rvMain = rootView!!.findViewById(R.id.rvMain)
        if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            rvMain.layoutManager = GridLayoutManager(activity, 3)
        } else {
            rvMain.layoutManager = GridLayoutManager(activity, 5)
        }

        adapter = AlbumRecyclerAdapter(
            requireActivity(),
            mAlbum,
            viewModel.mAlbumItemCount,
            viewModel.mPageCount
        )
        Log.d("designError","Album fragment")
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.spacing)
//        rvMain.addItemDecoration(SpacesItemDecoration(spacingInPixels))

        rvMain.adapter = adapter

    }

    private fun registerListeners() {
        rootView!!.findViewById<Button>(R.id.btn_upload).setOnClickListener(this)
        rootView!!.findViewById<Button>(R.id.btn_retry).setOnClickListener(this)
        rootView!!.findViewById<Button>(R.id.btn_upload_and_delete).setOnClickListener(this)
        rootView!!.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
            .setOnRefreshListener(this)

    }

    private fun registerGridViewOnScrollListner() {
        rvMain.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1)) {
                    //Toast.makeText(YourActivity.this, "Last", Toast.LENGTH_LONG).show();
                    L.print(this, "new state - $newState")
                    if (mAlbum != null && mAlbum!!.mitem.size != 0) {
                        //if (newState + 3 >= mAlbum!!.mitem.size && mAlbum!!.mitem.size <= mAlbumItemCount) {
                        if (mAlbum!!.mitem.size <= viewModel.mAlbumItemCount) {
                            if (!viewModel.mContentAPIFlag) {
                                getAlbumContent()
                                viewModel.mContentAPIFlag = true
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onRefresh() {
        viewModel.isSwipeRefresh = true
        fetchReloadAlbum()
    }

    private fun setVisibilityForView() {
        viewModel.topInfoVisibility.value = VISIBLE

        if (mAlbum != null && mAlbum!!.mitem.size == 0) {
            viewModel.emptyAlbumVisibility.value = VISIBLE
            viewModel.swipeRefreshVisibility.value = GONE
        } else {
            viewModel.emptyAlbumVisibility.value = GONE
            viewModel.swipeRefreshVisibility.value = VISIBLE
        }
        viewModel.swipeRefreshVisibility.value = VISIBLE
        viewModel.internetNotAvailableVisibility.value = GONE
    }

    private fun notifyAdapter() {
        if (mAlbum != null) {
            adapter.reloadAlbums(mAlbum, viewModel.mAlbumItemCount, viewModel.mPageCount)
        }
    }

    //---------------------------------------------------------------------------------Web-interface
    //--------------------------------------------------------------------------------------Requests
    private fun getAlbumItemCount() {
        L.print(this, "Start loading item count...")
        WebManager.instance.getAlbumItemCount(
            requireActivity().baseContext, this,
            mAlbumId
        )
    }

    private fun getAlbumReloadItemCount() {
        if (viewModel.isSwipeRefresh) {
            viewModel.swipeRefreshActive.value = true
            //swipeRefreshLayout.isRefreshing = true
        } else {
            //mUtility.showProgressDialog(activity!!, R.string.text_progress_bar_wait)
        }
        WebManager.instance.getAlbumItemCount(
            requireActivity().baseContext, this,
            mAlbumId
        )
    }

    private fun getAlbumContent() {
        activity?.let { activity ->
            //mUtility.showProgressDialog(activity, R.string.text_progress_bar_wait)

            L.print(this, "Start loading album content...")
            if (activity != null) {
                WebManager.instance.getAlbumContentWithPage(
                    activity.baseContext, this,
                    mAlbumId, viewModel.mPageCount
                )
            }

        }
    }

    private fun requestUpdatedAlbumCount() {
        viewModel.mContentUploaded = true
        viewModel.emptyAlbumVisibility.value = GONE
        viewModel.swipeRefreshVisibility.value = VISIBLE
        getAlbumItemCount()
    }

    //---------------------------------------------------------------------Populate-result-functions
    private fun assignValuesToView() {
        if (mAlbum != null) {
            viewModel.albumName.value = mAlbum!!.mName
            if (mAlbum!!.mdescription.isEmpty()) {
                viewModel.albumDescriptionVisibility.value = GONE
            } else {
                viewModel.albumDescriptionVisibility.value = VISIBLE
                viewModel.albumDescription.value = mAlbum!!.mdescription
            }
            viewModel.imFolderVisibility.value = VISIBLE
            viewModel.albumCount.value = AlbumHelper.appendStringMethod(
                viewModel.mAlbumSize, viewModel.mAlbumItemCount,
                viewModel.mAlbumVideosCount, viewModel.mAlbumPhotosCount
            )
        }
    }

    //get album content count from web response
    private fun getAlbumItemCountFromResponse(response: JSONObject?) {
        var response = response
        try {
            if (response != null) {
                if (response.getInt(Constants.OK) == 1) {
                    response = response.getJSONObject(Constants.DATA)
                    if (response!!.has(Constants.ITEM_COUNT)) {
                        viewModel.mAlbumItemCount = response.optInt(Constants.ITEM_COUNT)
                        viewModel.mAlbumPhotosCount = response.optInt(
                            Constants.ALBUM_PHOTOS_COUNT
                        )
                        viewModel.mAlbumVideosCount = response.optInt(
                            Constants.ALBUM_VIDEOS_COUNT
                        )
                        val albumSize = response.optString(Constants.ALBUM_SIZE)
                        viewModel.mAlbumSize = java.lang.Long.parseLong(albumSize)
                        if (viewModel.mAlbumItemCount > 0) {
                            getAlbumContent()
                            viewModel.mPageCount++

                            viewModel.swipeRefreshVisibility.value = VISIBLE
                            viewModel.emptyAlbumVisibility.value = GONE

                        } else {
                            getAlbumContent()
                            viewModel.mPageCount++

                            viewModel.swipeRefreshVisibility.value = GONE
                            viewModel.emptyAlbumVisibility.value = VISIBLE
                            viewModel.albumCount.value = AlbumHelper.appendStringMethod(
                                0, 0,
                                0, 0
                            )
                        }
                    } else {
                        //mUtility.dismissProgressDialog()

                        viewModel.swipeRefreshVisibility.value = GONE
                        viewModel.emptyAlbumVisibility.value = VISIBLE
                        viewModel.mAlbumItemCount = 0
                        viewModel.mAlbumPhotosCount = 0
                        viewModel.mAlbumVideosCount = 0
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    //get updated album content count from web response
    private fun getUpdatedAlbumCount(response: JSONObject?) {
        var response = response
        try {
            if (response != null) {
                if (response.getInt(Constants.OK) == 1) {
                    response = response.getJSONObject(Constants.DATA)
                    if (response!!.has(Constants.ITEM_COUNT)) {
                        viewModel.mAlbumItemCount = response.optInt(Constants.ITEM_COUNT)
                        viewModel.mAlbumPhotosCount = response.optInt(
                            Constants.ALBUM_PHOTOS_COUNT
                        )
                        viewModel.mAlbumVideosCount = response.optInt(
                            Constants.ALBUM_VIDEOS_COUNT
                        )
                        val albumSize = response.optString(Constants.ALBUM_SIZE)
                        viewModel.mAlbumSize = java.lang.Long.parseLong(albumSize)
                        if (viewModel.mAlbumItemCount > 0) {
                            viewModel.mPageCount--
                            getAlbumContent()
                        }
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    //populate items from web response
    private fun populateItems(mPhoto_json_array: JSONArray) {
        if (mItems == null) {
            mItems = ArrayList()
        } else {
            mItems!!.clear()
        }
        try {
            for (i in 0 until mPhoto_json_array.length()) {
                val item = ParcelableItem(
                    0,
                    "",
                    "",
                    "",
                    "",
                    "",
                    false,
                    "",
                    0
                )
                item.mAlbumIdEnc = mAlbum!!.mAlbumIdEnc
                item.mId = mPhoto_json_array.getJSONObject(i).getLong(Constants.ID)
                item.mViewUrl = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.VIEW_URL
                )
                item.mCreated = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.CREATED
                )
                item.mThumbnailUrl = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.THUMBNAIL_URL
                )
                item.mOriginalUrl = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.ORIGINAL_URL
                )
                item.mTitle = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.TITTLE
                )
                val mIsVideo = mPhoto_json_array.getJSONObject(i).getInt(
                    Constants.VIDEO
                )
                item.mVideoUrl = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.VIDEO_URL
                )
                item.mShortUrl = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.SHORT_URL
                )
                if (mPhoto_json_array.getJSONObject(i).has(Constants.VIDEO_CONVERT_STATUS)) {
                    item.mInaccessable = mPhoto_json_array.getJSONObject(i).getInt(
                        Constants.VIDEO_CONVERT_STATUS
                    )
                }

                if (mPhoto_json_array.getJSONObject(i).has(Constants.ORIGINAL_FILENAME)) {
                    item.mOriginalFilename = mPhoto_json_array.getJSONObject(i).getString(
                        Constants.ORIGINAL_FILENAME
                    )
                }

                item.mIsVideo = mIsVideo == 2
                mItems!!.add(item)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    //populate updated items from web response
    private fun populateUpdateItems(mPhoto_json_array: JSONArray) {
        if (mItems == null) {
            mItems = ArrayList()
        } else {
            mItems!!.clear()
        }
        try {
            val lastPrevElemnt = mAlbum!!.mitem.size
            for (i in lastPrevElemnt until mPhoto_json_array.length()) {
                val item = ParcelableItem(
                    0,
                    "",
                    "",
                    "",
                    "",
                    "",
                    false,
                    "",
                    0
                )
                item.mAlbumIdEnc = mAlbum!!.mAlbumIdEnc
                item.mId = mPhoto_json_array.getJSONObject(i).getLong(Constants.ID)
                item.mViewUrl = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.VIEW_URL
                )
                item.mCreated = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.CREATED
                )
                item.mThumbnailUrl = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.THUMBNAIL_URL
                )
                item.mOriginalUrl = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.ORIGINAL_URL
                )
                item.mTitle = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.TITTLE
                )
                item.mVideoUrl = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.VIDEO_URL
                )
                item.mShortUrl = mPhoto_json_array.getJSONObject(i).getString(
                    Constants.SHORT_URL
                )
                if (mPhoto_json_array.getJSONObject(i).has(Constants.VIDEO_CONVERT_STATUS)) {
                    item.mInaccessable = mPhoto_json_array.getJSONObject(i).getInt(
                        Constants.VIDEO_CONVERT_STATUS
                    )
                }

                if (mPhoto_json_array.getJSONObject(i).has(Constants.ORIGINAL_FILENAME)) {
                    item.mOriginalFilename = mPhoto_json_array.getJSONObject(i).getString(
                        Constants.ORIGINAL_FILENAME
                    )
                }
                val mIsVideo = mPhoto_json_array.getJSONObject(i).getInt(Constants.VIDEO)
                item.mIsVideo = mIsVideo == 2
                mItems!!.add(item)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    private fun rePopulateAlbumFromGetAlbumContent(response: JSONObject?) {
        if (mItems == null) {
            mItems = ArrayList()
        } else {
            mItems!!.clear()
        }

        var response = response
        try {
            if (response != null) {
                if (response.getInt(Constants.OK) == 1) {
                    response = response.getJSONObject(Constants.DATA)
                    val mAlbum_json_object = response!!.getJSONObject(
                        Constants.ALBUM
                    )
                    val mPhoto_json_array = response.getJSONArray(
                        Constants.PHOTOS
                    )
                    mAlbum = null
                    val newItemList = ArrayList<ParcelableItem>()
                    mAlbum = ParcelableAlbum(
                        0,
                        "",
                        "",
                        "",
                        newItemList,
                        ""
                    )
                    mAlbum!!.mName = mAlbum_json_object.getString(
                        Constants.ALBUM_NAME_IN_RESPONSE
                    )
                    mAlbum!!.mdescription = mAlbum_json_object.getString(
                        Constants.ALBUM_DESCRIPTION_IN_RESPONSE
                    )
                    mAlbum!!.mAlbumIdEnc = mAlbum_json_object.getLong(Constants.ID)
                    mAlbum!!.mShareUrl = mAlbum_json_object.optString(Constants.SHARE_URL)
                    populateItems(mPhoto_json_array)
                    mAlbum!!.mitem.addAll(mItems!!)
                    //swipeRefreshLayout.isRefreshing = false
                    viewModel.swipeRefreshActive.value = false
                    viewModel.isReloadAlbum = false
                    viewModel.mContentAPIFlag = false
                    viewModel.mContentUploaded = false
                    viewModel.isSwipeRefresh = false
                    REQUEST_URL = null
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    // populate album with getAlbumContent response.
    private fun populateAlbumFromGetAlbumContent(response: JSONObject?) {
        if (mItems == null) {
            mItems = ArrayList()
        } else {
            mItems!!.clear()
        }

        var response = response
        try {
            if (response != null) {
                if (response.getInt(Constants.OK) == 1) {
                    response = response.getJSONObject(Constants.DATA)
                    val mAlbum_json_object = response!!.getJSONObject(
                        Constants.ALBUM
                    )
                    val mPhoto_json_array = response.getJSONArray(
                        Constants.PHOTOS
                    )
                    if (mAlbum == null) {
                        val newItemList = ArrayList<ParcelableItem>()
                        mAlbum = ParcelableAlbum(
                            0,
                            "",
                            "",
                            "",
                            newItemList,
                            ""
                        )
                        mAlbum!!.mName = mAlbum_json_object.getString(
                            Constants.ALBUM_NAME_IN_RESPONSE
                        )
                        mAlbum!!.mdescription = mAlbum_json_object.getString(
                            Constants.ALBUM_DESCRIPTION_IN_RESPONSE
                        )
                        mAlbum!!.mAlbumIdEnc = mAlbum_json_object.getLong(Constants.ID)
                        mAlbum!!.mShareUrl = mAlbum_json_object.optString(Constants.SHARE_URL)
                        populateItems(mPhoto_json_array)
                        mAlbum!!.mitem.addAll(mItems!!)
                    } else {
                        populateItems(mPhoto_json_array)
                        mAlbum!!.mitem.addAll(mItems!!)
                        viewModel.mPageCount++
                        viewModel.mContentAPIFlag = false
                        viewModel.mContentUploaded = false
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    // populate album with getAlbumContent response.
    private fun populateUpdateAlbumContent(response: JSONObject?) {
        var response = response
        try {
            if (response != null) {
                if (response.getInt(Constants.OK) == 1) {
                    response = response.getJSONObject(Constants.DATA)
                    val mPhoto_json_array = response!!.getJSONArray(
                        Constants.PHOTOS
                    )
                    populateUpdateItems(mPhoto_json_array)
                    mAlbum!!.mitem.addAll(mItems!!)
                    viewModel.mPageCount++
                    viewModel.mContentAPIFlag = false
                    viewModel.mContentUploaded = false
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    //---------------------------------------------------------------------_Implementation-functions
    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
        //mUtility.dismissProgressDialog()
        if (apiRequestType === GET_ALBUM_ITEM_COUNT) {
            if (viewModel.mContentUploaded) {
                getUpdatedAlbumCount(response)
            } else {
                getAlbumItemCountFromResponse(response)
            }
        } else if (apiRequestType === GET_ALBUM_CONTENT) {
            if (mItems == null) {
                mItems = ArrayList()
            } else {
                mItems!!.clear()
            }

            mUtility.fotkiAppDebugLogs(TAG, response.toString())
            if (viewModel.mContentUploaded) {
                populateUpdateAlbumContent(response)
            } else if (viewModel.isReloadAlbum) {
                rePopulateAlbumFromGetAlbumContent(response)
            } else {
                populateAlbumFromGetAlbumContent(response)
            }
            assignValuesToView()
            setVisibilityForView()
            saveFragmentType()
            notifyAdapter()
        } else {
        }
    }

    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {
        //mUtility.dismissProgressDialog()
        viewModel.swipeRefreshActive.value = false
        //swipeRefreshLayout.isRefreshing = false
    }

    override fun sendNetworkFailure(
        isInterNetAvailableFlag: Boolean,
        apiRequestType: ApiRequestType
    ) {
        if (!isInterNetAvailableFlag) {
           // mUtility.dismissProgressDialog()
            setVisibiltyForNoInternetView()
            REQUEST_URL = apiRequestType
            viewModel.swipeRefreshActive.value = false
            //swipeRefreshLayout.isRefreshing = false
        }
    }

    //----------------------------------------------------------------------------------Upload-files
    private fun startUploadingFiles(fileTypes: ArrayList<FileType>, albumId: Long?) {
        val fileUploader = FileUploader.instance
        if (!fileUploader.isUploading) {
            fileUploader.isUploading = true
            L.print(this, "Compressed allowed - ${viewModel.isAllowCompress}")

            fileUploader.isCompressionAllow = viewModel.isAllowCompress
            fileUploader.mFileTypes.clear()
            fileUploader.mFileTypes.addAll(fileTypes)
            fileUploader.mAlbumId = 0L
            fileUploader.mAlbumId = albumId
            fileUploader.mContext = requireActivity()
            //mAlbum?.let { fileUploader.mAlbumName = it.mName }
            fileUploader.mAlbumName = mAlbum!!.mName
            fileUploader.startFileUploadingTask()
            viewModel.isAllowCompress = false
            switchTabWithNewUpload()
        } else {
            mUtility.showAlreadyUploadInProgress(requireActivity(), this@AlbumFragment)
        }
    }

    // switch tab new Uploads
    private fun switchTabWithNewUpload() {
        val intent = Intent(Constants.SWITCH_TAB)
        intent.putExtra(Constants.NEW_UPLOAD, true)
        LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent)
    }

    //save album position i.e save is it private or public
    private fun saveAlbumPosition() {
        val intent = Intent(Constants.PREF_ALBUM_POSITION)
        LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent)
    }

    // uploading is inProgress open that screen..
    fun openUploaderScreen() {
        val intent = Intent(Constants.UPLOADING_IN_PROGRESS)
        intent.putExtra(Constants.SWITCHSCREEN_WHILE_UPLOADING, true)
        LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent)
    }

    @SuppressLint("InflateParams")
    private fun showChoiceDialog() {
        viewModel.makeList()
        activity?.let {
            mDialog = ChoiceSizeDialog()
            val bundle = Bundle()
            bundle.putBoolean("isDelete", FileUploader.instance.isDelete)
            mDialog.arguments = bundle
            mDialog.onItemClick = setChoiceListViewListener()
            mDialog.show(it.supportFragmentManager, "choice")
        }
        /* mDialog = Dialog(activity!!)

        val inflater = activity!!.layoutInflater
        val convertView = inflater.inflate(R.layout.size_reduction_dialog_view, null) as View
        val textView = convertView.findViewById<TextView>(R.id.sharetext)
        Log.d("LOG","Is delete - ${FileUploader.instance.isDelete}")
        val str = if (FileUploader.instance.isDelete)
            SpannableStringBuilder(getString(R.string.resize_photos_text_delete))
        else
            SpannableStringBuilder(getString(R.string.resize_photos_text))

        str.setSpan(ForegroundColorSpan(
            ContextCompat.getColor(activity!!, R.color.colorDarkGrey)), 142, 156, 0)
        str.setSpan(RelativeSizeSpan(0.9f), 142, 156, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = str
        choiceAlert = convertView.findViewById(R.id.choiceDialog)
        val sizeReductionDialogAdapter = SizeReductionDialogAdapter(
            activity!!,
            R.layout.size_reduction_dialog_view, viewModel.listChoice)

        choiceAlert.adapter = sizeReductionDialogAdapter
        mDialog.setContentView(convertView)
        mDialog.show()
        setChoiceListViewListener()*/
    }

    private fun setChoiceListViewListener() = AdapterView.OnItemClickListener { _, _, position, _ ->
        L.print(this, "pos - $position")
        when (position) {
            0 -> {
                viewModel.isAllowCompress = true
                mDialog.dismiss()
                startUploadingFiles(fileTypeArrayList, mAlbumId)
            }
            1 -> {
                viewModel.isAllowCompress = false
                mDialog.dismiss()
                startUploadingFiles(fileTypeArrayList, mAlbumId)
            }
            2 -> {
                viewModel.isAllowCompress = false
                mDialog.dismiss()
                fileTypeArrayList.clear()
            }
            else -> {
                mDialog.dismiss()
                fileTypeArrayList.clear()
            }
        }
    }

    fun cancelUpload() {
        FileUploader.instance.cancelFileUploading()
    }

    companion object {

        val REQUEST_CODE_CHOOSE = 23
        val REQUEST_CODE_DELETE = 24

        fun newInstance(): AlbumFragment =
            AlbumFragment()

        private var TAG = FolderFragment::class.java.name
    }
}

