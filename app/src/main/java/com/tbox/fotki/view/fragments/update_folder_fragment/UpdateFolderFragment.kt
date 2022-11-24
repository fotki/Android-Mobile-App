package com.tbox.fotki.view.fragments.update_folder_fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.android.volley.*
import com.tbox.fotki.model.entities.ApiRequestType.*
import com.tbox.fotki.model.entities.FragmentType.CREATE_FOLDER_FRAGMENT
import com.tbox.fotki.R
import com.tbox.fotki.databinding.FragmentUpdateAlbumBinding
import com.tbox.fotki.databinding.FragmentUpdateFolderBinding
import com.tbox.fotki.model.entities.*
import com.tbox.fotki.model.web_providers.web_manager.WebManager
import com.tbox.fotki.model.web_providers.web_manager.WebManagerInterface
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.FotkiUpdateFolderInterface
import com.tbox.fotki.util.Utility
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import com.tbox.fotki.view.fragments.BaseFragment
import com.tbox.fotki.view.fragments.folder_fragment.FolderFragment
import com.tbox.fotki.view.fragments.update_album_fragment.UpdateAlbumFragmentViewModel
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


@Suppress("NAME_SHADOWING", "VARIABLE_WITH_REDUNDANT_INITIALIZER")
class UpdateFolderFragment : BaseFragment(), View.OnClickListener,
    WebManagerInterface {
    private var rootView: View? = null
    private var mFolderID: Long? = 0L

    private var mFotkiUpdateFolderInterface: FotkiUpdateFolderInterface? = null
    private var mUtility = Utility.instance
    private val mFolder = Folder()
    private lateinit  var mFragmentType: Enum<*>
    private  var mFolderName = " "
    private var mFolderDescription = " "
    private lateinit var viewModel:UpdateFolderFragmentViewModel

    override fun onStart() {
        super.onStart()
        checkFolderRootType()
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (rootView == null) {
            //rootView = inflater!!.inflate(R.layout.fragment_update_folder, container, false)
            viewModel = ViewModelProviders.of(requireActivity()).
                get(UpdateFolderFragmentViewModel::class.java)
            initBindingViewModel(inflater,container)
            viewModel.albumETName.value = ""
            viewModel.albumETDescription.value = ""
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
            .inflate<FragmentUpdateFolderBinding>(inflater,R.layout.fragment_update_folder,container,false)
        rootView = binding.root
        binding.viewModel = viewModel
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_create -> {
                hideKeyboard(view)
                callCreateFolderApi()
            }
            R.id.btn_cancel -> {
                hideKeyboard(view)
                mFragmentNavigation.popFragment()
            }
            R.id.btn_save -> {
                hideKeyboard(view)
                callUpdateFolderApi()
            }
            R.id.btn_retry -> if (mFragmentType === CREATE_FOLDER_FRAGMENT) {
                hideKeyboard(view)
                callCreateFolderApi()
            } else {
                hideKeyboard(view)
                callUpdateFolderApi()
            }
        }
    }

    fun setFragmentType(fragmentType: FragmentType) {
        mFragmentType = fragmentType
    }

    fun setFolderID(folderID: Long?) {
        this.mFolderID = folderID
    }

    // hide keyBoard is visble
    private fun hideKeyboard(view: View?) {
        var view = view
        view = activity!!.currentFocus
        if (view != null) {
            val imm = activity!!.getSystemService(
                    Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun setFolderNameWithDescription(folderName: String, folderDescription: String) {
        this.mFolderName = folderName
        this.mFolderDescription = folderDescription
    }

    // intialize listner
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

    // register views
    private fun registerView() {
        checkFragmentType()
    }

    // check fragment fragment inflate ui according to that.
    private fun checkFragmentType() {
        if (mFragmentType === CREATE_FOLDER_FRAGMENT) {
            viewModel.albumETDescriptionVisibility.value = View.GONE
            viewModel.albumTVDescriptionVisibility.value = View.GONE
            viewModel.btnCreateVisibility.value = View.VISIBLE
            viewModel.btnSaveVisibility.value = View.GONE
        } else {
            viewModel.albumETDescription.value = mFolderDescription
            viewModel.albumETName.value = mFolderName
            viewModel.albumETDescriptionVisibility.value = View.VISIBLE
            viewModel.btnSaveVisibility.value = View.VISIBLE
            viewModel.btnCreateVisibility.value = View.GONE
        }
    }

    // register buttons listner
    private fun registerListViewListner() {
        rootView?.let{
            it.findViewById<Button>(R.id.btn_create).setOnClickListener(this)
            it.findViewById<Button>(R.id.btn_save).setOnClickListener(this)
            it.findViewById<Button>(R.id.btn_retry).setOnClickListener(this)
            it.findViewById<Button>(R.id.btn_cancel).setOnClickListener(this)
        }
    }


    //  call create Folder api
    private fun callCreateFolderApi() {
        val etName = rootView!!.findViewById<EditText>(R.id.edit_text_create_item)
        if (etName.length() != 0) {
            mUtility.showProgressDialog(activity!!, R.string.text_progress_bar_wait)
            WebManager.instance.createFolder(activity!!.baseContext, this,
                    mFolderID, etName.text.toString())
        } else {
            mUtility.showAlertDialog(activity!!, "Please enter Folder name.")
        }
    }

    // call get updated folder content
    private fun callGetFolderContentAPI() {
        activity?.baseContext?.let {
            WebManager.instance.getFolderContent(
                it, this,
                mFolderID)
        }
    }

    // call update folder api
    private fun callUpdateFolderApi() {
        val etName = rootView!!.findViewById<EditText>(R.id.edit_text_create_item)
        val etDescription = rootView!!.findViewById<EditText>(R.id.edit_text_description_item)

        if (etName.length() != 0) {
            mUtility.showProgressDialog(activity!!, R.string.text_progress_bar_wait)
            WebManager.instance.updateFolder(activity!!.baseContext, this,
                    mFolderID, etName.text.toString(),
                    etDescription.text.toString())
        } else {
            mUtility.showAlertDialog(activity!!, "Please enter Folder name.")
        }
    }

    // get create folder response
    private fun getCreateFolderResponse(response: JSONObject?) {
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

    // get update folder resposne
    private fun getUpdateFolderResponse(response: JSONObject?) {
        try {
            if (response != null) {
                if (response.getInt(Constants.OK) == 1) {
                    mFolderName = rootView!!.findViewById<EditText>(R.id.edit_text_create_item)!!.text.toString()
                    mFolderDescription = rootView!!.findViewById<EditText>(R.id.edit_text_description_item)!!.text.toString()
                    mFotkiUpdateFolderInterface!!.sendSuccess(mFolderName, mFolderDescription)
                    mUtility.dismissProgressDialog()
                    mFragmentNavigation.popFragment()
                    // callGetFolderContentAPI(); this code was  remove from here.
                } else {
                    mUtility.dismissProgressDialog()
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

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
                album.mShareUrl = jsonArray.getJSONObject(i).getString(Constants.SHARE_URL)
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
                var albums = ArrayList<ParcelableAlbum>()
                var folders = ArrayList<Folder>()
                if (jsonArray.getJSONObject(i).has(Constants.ALBUMS)){
                    albums = parseAlbum(jsonArray.getJSONObject(i).getJSONArray(Constants.ALBUMS))
                }
                if (jsonArray.getJSONObject(i).has(Constants.FOLDERS)){
                    folders = parseFolder(jsonArray.getJSONObject(i).getJSONArray(
                        Constants.FOLDERS))
                }

                mFolder.setData(jsonArray.getJSONObject(i).getLong(
                        Constants.FOLDER_ID_ENC),
                        jsonArray.getJSONObject(i).getLong(
                                Constants.NUMBER_OF_FOLDER),
                        jsonArray.getJSONObject(i).getLong(
                                Constants.NUMBER_OF_ALBUMS),
                        jsonArray.getJSONObject(i).getString(Constants.DESCRICTION),
                        jsonArray.getJSONObject(i).getString(Constants.FOLDER_NAME),
                        albums,
                        folders,
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

    // get update folder content from response and notify listner
    private fun getFolderContentFomResponse(response: JSONObject) {
        loadFolder(mFolder, response)
        mFotkiUpdateFolderInterface!!.sendSuccess(mFolder)
        mUtility.dismissProgressDialog()
        mFragmentNavigation.popFragment()
    }

    // interface implementation
    override fun sendSuccess(response: JSONObject, apiRequestType: ApiRequestType) {
        when {
            apiRequestType === CREATE_FOLDER_API -> getCreateFolderResponse(response)
            apiRequestType === GET_FOLDER_CONTENT -> {
                mUtility.fotkiAppDebugLogs(TAG, response.toString())
                getFolderContentFomResponse(response)
            }
            apiRequestType === UPDATE_FOLDER -> getUpdateFolderResponse(response)
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
        mUtility.showAlertDialog(activity!!, message)
    }

    override fun sendNetworkFailure(isInterNetAvailableFlag: Boolean, apiRequestType: ApiRequestType) {
        if (!isInterNetAvailableFlag) {
            mUtility.dismissProgressDialog()
            viewModel.mainLayoutVisibility.value = View.GONE
            viewModel.internetNotAvailableVisibility.value = View.VISIBLE
        }
    }

    companion object {
        private var TAG = FolderFragment::class.java.name

        fun newInstance(tabType: String): UpdateFolderFragment {
            val fragment =
                UpdateFolderFragment()
            val args = Bundle()
            args.putString(ARGS_INSTANCE, tabType)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(): UpdateFolderFragment =
            UpdateFolderFragment()
    }
}
