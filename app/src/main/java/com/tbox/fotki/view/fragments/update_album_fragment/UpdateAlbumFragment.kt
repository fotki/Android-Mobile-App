package com.tbox.fotki.view.fragments.update_album_fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.*
import com.tbox.fotki.model.entities.ApiRequestType.*
import com.tbox.fotki.model.entities.FragmentType.CREATE_ALBUM_FRAGMENT
import com.tbox.fotki.R
import com.tbox.fotki.databinding.FragmentAlbumBinding
import com.tbox.fotki.databinding.FragmentUpdateAlbumBinding
import com.tbox.fotki.model.entities.*
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.model.web_providers.web_manager.WebManagerInterface
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.FotkiUpdateFolderInterface
import com.tbox.fotki.util.L
import com.tbox.fotki.util.Utility
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import com.tbox.fotki.view.fragments.BaseFragment
import com.tbox.fotki.view.fragments.album_fragment.AlbumFragment
import com.tbox.fotki.view.fragments.album_fragment.AlbumFragmentViewModel
import com.tbox.fotki.view.fragments.folder_fragment.FolderFragment
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER", "NAME_SHADOWING")
class UpdateAlbumFragment : BaseFragment(), View.OnClickListener,
    WebManagerInterface {
    private var rootView: View? = null
    private var mFolderID: Long? = 0L
    private var mUtility = Utility.instance
    private val mFolder = Folder()
    private lateinit var mFragmentType: Enum<*>
    private var mAlbumName = " "
    private var mAlbumDescription = " "
    private var mAlbumID: Long? = 0L
    private var mFotkiUpdateFolderInterface: FotkiUpdateFolderInterface? = null
    private lateinit var viewModel:UpdateAlbumFragmentViewModel

    override fun onStart() {
        super.onStart()
        checkFolderRootType()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (rootView == null) {
            //rootView = inflater!!.inflate(R.layout.fragment_update_album, container, false)
            viewModel = ViewModelProviders.of(requireActivity()).
                get(UpdateAlbumFragmentViewModel::class.java)
            initBindingViewModel(inflater,container)

            viewModel.albumETDescription.value = ""
            viewModel.albumETName.value = ""

            registerView()
            registerListViewListner()

            rootView
        } else {
            rootView
        }
    }

    private fun initBindingViewModel(inflater: LayoutInflater,
                                     container: ViewGroup?) {
        val binding = DataBindingUtil
            .inflate<FragmentUpdateAlbumBinding>(inflater,R.layout.fragment_update_album,container,false)
        rootView = binding.root
        binding.viewModel = viewModel
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_create -> {
                hideKeyboard(view)
                callCreateAlbumApi()
            }
            R.id.btn_save -> {
                hideKeyboard(view)
                callUpdateAlbumApi()
            }
            R.id.btn_cancel -> {
                hideKeyboard(view)
                mFragmentNavigation.popFragment()
            }
            R.id.btn_retry -> if (mFragmentType === CREATE_ALBUM_FRAGMENT) {
                hideKeyboard(view)
                callCreateAlbumApi()
            } else {
                hideKeyboard(view)
                callUpdateAlbumApi()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as FotkiTabActivity).hideToolbar()
    }

    override fun onPause() {
        super.onPause()
        super.onResume()
        (requireActivity() as FotkiTabActivity).showToolbar()

    }

    private fun hideKeyboard(view: View?) {
        var view = view
        view = requireActivity().currentFocus
        if (view != null) {
            val imm = requireActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun setFragmentType(fragmentType: FragmentType) {
        mFragmentType = fragmentType
    }

    fun setFolderID(folderID: Long?) {
        this.mFolderID = folderID
    }

    fun setAlbumID(albumID: Long?) {
        this.mAlbumID = albumID
    }

    fun setFolderNameWithDescription(albumName: String, albumDescription: String) {
        this.mAlbumName = albumName
        this.mAlbumDescription = albumDescription
    }

    //intialize listner
    fun setFotkiUpdateFolderListner(fotkiUpdateFolderListner: FotkiUpdateFolderInterface) {
        mFotkiUpdateFolderInterface = fotkiUpdateFolderListner
    }

    //helper function of on start
    private fun checkFolderRootType() {
        val args = this.arguments
        if (args != null) {
            val tabType = args.getString(ARGS_INSTANCE)
            if (tabType == "public") {
                (activity as AppCompatActivity).supportActionBar!!.setTitle(
                        R.string.tab_type_public)
            } else {
                (activity as AppCompatActivity).supportActionBar!!.setTitle(
                        R.string.tab_type_private)
            }
        } else {
            (activity as AppCompatActivity).supportActionBar!!.setTitle(
                    R.string.tab_type_test)
        }
    }

    // register views...
    private fun registerView() {
        checkFragmentType()
    }

    // check fragment type.
    private fun checkFragmentType() {
        if (mFragmentType === CREATE_ALBUM_FRAGMENT) {
            viewModel.albumETDescriptionVisibility.value = View.GONE
            viewModel.albumTVDescriptionVisibility.value = View.GONE
            viewModel.btnCreateVisibility.value = View.VISIBLE
            viewModel.btnSaveVisibility.value = View.GONE
        } else {
            viewModel.albumETDescription.value = mAlbumDescription
            viewModel.albumETName.value = mAlbumName
            viewModel.btnSaveVisibility.value = View.VISIBLE
            viewModel.btnCreateVisibility.value = View.GONE
        }
    }

    //  registner listners of button
    private fun registerListViewListner() {
        rootView?.let {
            it.findViewById<Button>(R.id.btn_retry).setOnClickListener(this)
            it.findViewById<Button>(R.id.btn_create).setOnClickListener(this)
            it.findViewById<Button>(R.id.btn_cancel).setOnClickListener(this)
            it.findViewById<Button>(R.id.btn_save).setOnClickListener(this)
        }
    }

    // call create album api
    private fun callCreateAlbumApi() {
        L.print(this,"MEDIA folder id - $mFolderID")
        val etAlbumName = rootView!!.findViewById<EditText>(R.id.edit_text_create_item)

        if(etAlbumName!!.length()!=0){
            mUtility.showProgressDialog(requireActivity(), R.string.text_progress_bar_wait)
            WebManager.instance.createAlbum(requireActivity().baseContext, this, mFolderID,
                etAlbumName.text.toString()
            )
        } else {
            mUtility.showAlertDialog(requireActivity(), "Please enter Album name.")
        }
    }

    // call get folder content api
    private fun callGetFolderContentAPI() {
        WebManager.instance.getFolderContent(requireActivity().baseContext, this,
                mFolderID)
    }

    // call update album api
    private fun callUpdateAlbumApi() {
        val etAlbumName = rootView!!.findViewById<EditText>(R.id.edit_text_create_item)
        val etAlbumDescription = rootView!!.findViewById<EditText>(R.id.edit_text_description_item)

        L.print(this,"try to update albumname - ${etAlbumName.text} description - ${etAlbumDescription.text} ")

        if (etAlbumName!!.length()!=0){
            mUtility.showProgressDialog(requireActivity(), R.string.text_progress_bar_wait)
            WebManager.instance.updateAlbum(requireActivity().baseContext, this,
                    mAlbumID, etAlbumName.text.toString(),
                    etAlbumDescription.text.toString())
        } else {
            mUtility.showAlertDialog(requireActivity(), "Please enter Album name.")
        }
    }

    // get create album response and call get folder content api
    private fun getCreateAlbumResponse(response: JSONObject?) {
        try {
            if (response != null) {
                if (response.getInt(Constants.OK) == 1) {
                    callGetFolderContentAPI()
                } else {
                    mUtility.dismissProgressDialog()
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    // get update album response
    private fun getUpdateAlbumResponse(response: JSONObject?) {
        try {
            if (response != null) {
                if (response.getInt(Constants.OK) == 1) {
                    val name = viewModel.albumETName.value
                    val desc = viewModel.albumETDescription.value
                    /*val name = mEditText_AlbumName!!.text.toString()
                    val desc = mEditText_AlbumDescription!!.text.toString()*/
                    val intent = Intent(Constants.ALBUM_NAME_UPDATED)
                    intent.putExtra(Constants.UPDATE_ALBUM_NAME, name)
                    intent.putExtra(Constants.UPDATE_ALBUM_DESCRIPTION, desc)
                    intent.putExtra(Constants.UPDATE_ALBUM_ID, mAlbumID)
                    LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent)
                    callGetFolderContentAPI()
                } else {
                    mUtility.dismissProgressDialog()
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    // parse updated album from response.
    private fun parseAlbum(jsonArray: JSONArray): ArrayList<ParcelableAlbum> {
        val mAlbumArray = ArrayList<ParcelableAlbum>()
        for (i in 0 until jsonArray.length()) {
            // val album = Album()
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
                album.mShareUrl =jsonArray.getJSONObject(i).optString(Constants.SHARE_URL)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            mAlbumArray.add(album)
        }
        return mAlbumArray
    }

    // parse updated folder from response
    private fun parseFolder(jsonArray: JSONArray): ArrayList<Folder> {
        val mFoldersArray = ArrayList<Folder>()
        for (i in 0 until jsonArray.length()) {
            val mFolder = Folder()
            try {
                mFolder.setData(jsonArray.getJSONObject(i).getLong(
                        Constants.FOLDER_ID_ENC),
                        jsonArray.getJSONObject(i).getLong(
                                Constants.NUMBER_OF_FOLDER),
                        jsonArray.getJSONObject(i).getLong(
                                Constants.NUMBER_OF_ALBUMS),
                        jsonArray.getJSONObject(i).getString(Constants.DESCRICTION),
                        jsonArray.getJSONObject(i).getString(Constants.FOLDER_NAME),
                        parseAlbum(jsonArray.getJSONObject(i).getJSONArray(
                                Constants.ALBUMS)),
                        parseFolder(jsonArray.getJSONObject(i).getJSONArray(
                                Constants.FOLDERS)),
                        jsonArray.getJSONObject(i).optString(Constants.SHARE_URL))
                mFoldersArray.add(mFolder)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        return mFoldersArray
    }

    // load new updated folder
    private fun loadFolder(folder: Folder, `object`: JSONObject) {
        var `object` = `object`
        try {
            if (`object`.getInt(Constants.OK) == 1) {
                `object` = `object`.getJSONObject(Constants.DATA)
                folder.setData(`object`.getLong(Constants.FOLDER_ID_ENC),
                        `object`.getLong(Constants.NUMBER_OF_FOLDER),
                        `object`.getLong(Constants.NUMBER_OF_ALBUMS),
                        `object`.getString(Constants.DESCRICTION),
                        `object`.getString(Constants.FOLDER_NAME),
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

    // get updated folder and notify listner
    private fun getFolderContentFomResponse(response: JSONObject) {
        loadFolder(mFolder, response)
        mFotkiUpdateFolderInterface!!.sendSuccess(mFolder)
        mUtility.dismissProgressDialog()
        mFragmentNavigation.popFragment()
    }

    // interface method implementaion
    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
        when {
            apiRequestType === CREATE_ALBUM_API -> getCreateAlbumResponse(response)
            apiRequestType === GET_FOLDER_CONTENT -> {
                mUtility.fotkiAppDebugLogs(TAG, response.toString())
                getFolderContentFomResponse(response)
            }
            apiRequestType === UPDATE_ALBUM -> getUpdateAlbumResponse(response)
            else -> mUtility.dismissProgressDialog()
        }
    }

    override fun sendFailure(error: VolleyError, apiRequestType: ApiRequestType) {
        var message = " "
        mUtility.dismissProgressDialog()
        message = when (error) {
            is NetworkError -> getString(R.string.network_not_found)
            is ServerError -> getString(R.string.server_error)
            is AuthFailureError -> getString(R.string.network_not_found)
            is ParseError -> getString(R.string.parse_error)
            is TimeoutError -> getString(R.string.time_out_error)
            else -> getString(R.string.network_not_found)
        }
        mUtility.showAlertDialog(requireActivity(), message)
    }

    override fun sendNetworkFailure(isInterNetAvailableFlag: Boolean, apiRequestType: ApiRequestType) {
        if (!isInterNetAvailableFlag) {
            mUtility.dismissProgressDialog()
            viewModel.mainLayoutVisibility.value = View.GONE
            viewModel.internetNotAvailableVisibility.value = View.GONE
            /*mMainLayout.visibility = View.GONE
            mInternetNotAvaible.visibility = View.VISIBLE*/
        }
    }

    companion object {

        private var TAG = FolderFragment::class.java.name

        fun newInstance(tabType: String): UpdateAlbumFragment {
            val fragment =
                UpdateAlbumFragment()
            val args = Bundle()
            args.putString(ARGS_INSTANCE, tabType)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(): UpdateAlbumFragment =
            UpdateAlbumFragment()
    }
}
