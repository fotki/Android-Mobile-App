package com.tbox.fotki.view.fragments.upload_file_fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.*
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import com.tbox.fotki.view.adapters.SizeReductionDialogAdapter
import com.tbox.fotki.model.entities.UploadProperty
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.upload_files.*
import com.tbox.fotki.util.Utility
import com.tbox.fotki.BuildConfig
import com.tbox.fotki.R
import com.tbox.fotki.databinding.FragmentFileUploaderBinding
import com.tbox.fotki.util.L
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivityViewModel
import com.tbox.fotki.view.fragments.BaseFragment
import com.tbox.fotki.view.fragments.folder_fragment.FolderFragment
import com.tbox.fotki.view.fragments.update_folder_fragment.UpdateFolderFragmentViewModel
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_file_uploader.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

@Suppress("NAME_SHADOWING")
/**
 * A simple [Fragment] subclass.
 */
class UploadFileFragment : BaseFragment(), ReturnToAlbumInterface, View.OnClickListener {
    private var rootView: View? = null

    private var isAlertIsDisplayed = false
    private var isPreviewMediaCalled = false
    private var mTotalCount = 0
    private var mAlbumName = " "
    private var mUtility = Utility.instance
    private var fileTypeArrayList = ArrayList<FileType>()
    private lateinit var filePath: String
    private lateinit var mDialog: Dialog
    private lateinit var choiceAlert: ListView
    private var listChoice = ArrayList<String>()
    private var isAllowCompress = false
    private lateinit var albumUploadName: String
    private var albumId: Long = 0
    private var isPaused = false
    private lateinit var viewModel:UploadFileFragmentViewModel
    private lateinit var activityViewModel: FotkiTabActivityViewModel

    private var mImageView: ImageView? = null
    private var mCurrentFileProgress: ProgressBar? = null
    private var mAllUploadProgress: ProgressBar? = null

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
            //rootView = inflater!!.inflate(R.layout.fragment_file_uploader, container, false)
            viewModel = ViewModelProviders.of(requireActivity()).
                get(UploadFileFragmentViewModel::class.java)
            activityViewModel = ViewModelProviders.of(requireActivity()).get(FotkiTabActivityViewModel::class.java)

            initBindingViewModel(inflater,container)

            registerView()
            registerClickListner()
            registerLocalBroadCasts()
            restoreFromPreferences()
            Log.d("cancelError","on crete ")

            rootView
        } else {
            registerLocalBroadCasts()
            mImageView?.setImageResource(android.R.color.transparent)
            rootView
        }
    }

    private fun initBindingViewModel(inflater: LayoutInflater,
                                     container: ViewGroup?) {
        val binding = DataBindingUtil
            .inflate<FragmentFileUploaderBinding>(inflater,R.layout.fragment_file_uploader,container,false)
        rootView = binding.llRoot
        binding.viewModel = viewModel
    }

    private fun restoreFromPreferences() {
        val uploadProperty = UploadProperty()
        uploadProperty.fromPreferences(requireActivity())
        if(uploadProperty.isUploading){
            viewModel.showUploadInProgress()
/*
            mRelativeLayoutUploadNotInProgress!!.visibility = View.GONE
            mRelativeLayoutUploadInProgress!!.visibility = View.VISIBLE
            mRelativeLayoutNoInternetAccess!!.visibility = View.GONE
*/

            val activity = activity
            if (activity != null && isAdded) {
                // Get extra data included in the Intent
                val currentFileStatus = 0
                val currentFileCount = uploadProperty.itemCounter
                val totalFiles = uploadProperty.fileTypes.size
                mTotalCount = totalFiles
                mAlbumName = uploadProperty.albumName
                FileUploader.instance.isDelete = uploadProperty.isDelete
                val mFileMimeType = uploadProperty.fileTypes[currentFileCount].mFileMimeType
                val filePath = uploadProperty.fileTypes[currentFileCount].mFilePath
                if (!isPreviewMediaCalled) {
                    previewMedia(filePath, mFileMimeType)
                    isPreviewMediaCalled = true
                }

                val loadedSize = uploadProperty.loadedSize
                updateProgressBar(currentFileStatus, currentFileCount, totalFiles, loadedSize)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isAlertIsDisplayed = false
        isPreviewMediaCalled = false
        if ((activity as AppCompatActivity).supportActionBar!!.isShowing) {
            (activity as AppCompatActivity).supportActionBar!!.hide()
        }
        if (activity != null) {
            if (mUtility.isConnectingToInternet(requireActivity())) {
                val fileUploader = FileUploader.instance
                L.print(this, "TAG is uploading - ${fileUploader.isUploading}")
                if (fileUploader.isUploading) {
                    viewModel.showUploadInProgress()
/*
                    mRelativeLayoutUploadInProgress!!.visibility = View.VISIBLE
                    mRelativeLayoutUploadNotInProgress!!.visibility = View.GONE
                    mRelativeLayoutNoInternetAccess!!.visibility = View.GONE
*/
                } else {
                    checkFragmentType()
                    viewModel.showNotUploadInProgress()
                    /*mRelativeLayoutUploadInProgress!!.visibility = View.GONE
                    mRelativeLayoutUploadNotInProgress!!.visibility = View.VISIBLE
                    mRelativeLayoutNoInternetAccess!!.visibility = View.GONE*/
                }
            } else {
                viewModel.showNoInternet()
                /*mRelativeLayoutNoInternetAccess!!.visibility = View.VISIBLE
                mRelativeLayoutUploadNotInProgress!!.visibility = View.GONE
                mRelativeLayoutUploadInProgress!!.visibility = View.GONE*/
            }

        }
    }

    private fun checkFragmentType() {
        val fragmentType = (requireActivity().getSharedPreferences(Constants.DEFTYPE_FILE, Context.MODE_PRIVATE)
                .getString(Constants.DEFTYPE, Constants.DEFTYPE_KEY_ALBUM))
        if ((fragmentType).equals(Constants.DEFTYPE_KEY_ALBUM)) {
            viewModel.textDescription.value = getString(R.string.text_Upload_from_Album)
            //mTextDescription.setText(getString(R.string.text_Upload_from_Album))
            viewModel.btnUploadVisibility.value = View.VISIBLE
            viewModel.btnUploadAndDelVisibility.value = View.VISIBLE
/*
            btn_Upload.visibility = View.VISIBLE
            btn_Upload_and_Delete.visibility = View.VISIBLE
*/
            albumId = requireActivity().getSharedPreferences(Constants.DEFTYPE_FILE, Context.MODE_PRIVATE).getLong(
                Constants.PREF_ALBUM_ID, 0L)
            albumUploadName = requireActivity().getSharedPreferences(Constants.DEFTYPE_FILE, Context.MODE_PRIVATE).getString(
                Constants.PREF_ALBUM_NAME, " ")!!
        } else {
            viewModel.textDescription.value = getString(R.string.text_cannot_uploload_trough_folder)
            //mTextDescription.setText(getString(R.string.text_cannot_uploload_trough_folder))
            viewModel.btnUploadVisibility.value = View.GONE
            viewModel.btnUploadAndDelVisibility.value = View.GONE
/*
            btn_Upload.visibility = View.GONE
            btn_Upload_and_Delete.visibility = View.GONE
*/

        }
    }

    //register localbroadcast for ui updation
    private fun registerLocalBroadCasts() {
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(mUploadingStarted,
                IntentFilter(Constants.UPLOAD_STARTED))
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(mMessageReceiver,
                IntentFilter(Constants.UPDATE_CURRENT_PROGRESS))
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(mAllUploadDone,
                IntentFilter(Constants.ALL_UPLOADS_DONE))
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(mUploadingStopped,
                IntentFilter(Constants.UPLOADINGSTOPPED))
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(mUploadingError,
            IntentFilter(Constants.SERVICE_ERROR))
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(mAllUploadCancel,
            IntentFilter(Constants.SERVICE_CANCEL))
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(mUploadingMessage,
            IntentFilter(Constants.SERVICE_MESSAGE))
    }

    private val mUploadingMessage = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra(Constants.MESSAGE)
            //viewModel.textStatus.value = message
            viewModel.textStatusNoInternet.value = message

/*
            mStatus!!.text = message
            mStatusNoInternet!!.text = message
*/
        }
    }

    private val mUploadingStopped = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            isPaused = false
            viewModel.showNoInternet()
            /*mRelativeLayoutUploadNotInProgress!!.visibility = View.GONE
            mRelativeLayoutUploadInProgress!!.visibility = View.GONE
            mRelativeLayoutNoInternetAccess!!.visibility = View.VISIBLE*/
        }
    }
    private val mUploadingError = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val error = intent.getStringExtra(Constants.SHARING_FILE_ERROR)
            L.print(this,"TAG Uploading error receive")
            //Utility.instance.showAlertDialog(requireActivity(), error) { UploadJobService.restartWork(requireActivity()) }
            viewModel.showNoInternet()
/*
            mRelativeLayoutUploadNotInProgress!!.visibility = View.GONE
            mRelativeLayoutUploadInProgress!!.visibility = View.GONE
            mRelativeLayoutNoInternetAccess!!.visibility = View.VISIBLE
*/
            viewModel.textStatus.value = error
            viewModel.textStatusNoInternet.value = error

            /*mStatus!!.text = error
            mStatusNoInternet!!.text = error
            */
            //TimeUnit.SECONDS.sleep(5)
            //FileUploader.instance.retryFileUploading()
        }
    }
    private val mUploadingStarted = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val activity = activity
            if (activity != null && isAdded) {
                isPaused = false
                // Get extra data included in the Intent
                val currentFileStatus = intent.getIntExtra(
                        Constants.BEING_UPLOADED_STATUS, 0)
                val filePath = intent.getStringExtra(
                        Constants.FILE_BEING_UPLOADING)
                val currentFileCount = intent.getIntExtra(
                        Constants.CURRENT_FILE_COUNT, 0)
                val totalFiles = intent.getIntExtra(Constants.TOTAL_FILES_TO_UPLOAD, 0)
                val loadedSize = intent.getLongExtra(Constants.FILE_UPLOADED_LENGTH, 0L)
                mTotalCount = totalFiles
                mAlbumName = intent.getStringExtra(Constants.UPLOADING_ALBUM_NAME)!!
                val mFileMimeType = intent.getStringExtra(
                        Constants.BEING_UPLOADING_FILE_MIMETYPE)
                viewModel.textFileUpload.value = activity.resources?.getString(
                    if (FileUploader.instance.isDelete)
                        R.string.text_file_uploading else R.string.text_file_uploading_and_delete
                )

                tvFile_upload.text = activity.resources?.getString(
                    if (FileUploader.instance.isDelete)
                        R.string.text_file_uploading else R.string.text_file_uploading_and_delete
                )

                previewMedia(filePath!!, mFileMimeType!!)
                L.print(this,"$currentFileStatus, $currentFileCount, $totalFiles, $loadedSize")
                updateProgressBar(currentFileStatus, currentFileCount, totalFiles, loadedSize)
            }
        }
    }
    var loadMemory = 0

    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.showUploadInProgress()
/*
            mRelativeLayoutUploadNotInProgress!!.visibility = View.GONE
            mRelativeLayoutUploadInProgress!!.visibility = View.VISIBLE
            mRelativeLayoutNoInternetAccess!!.visibility = View.GONE
*/

            val activity = activity
            if (activity != null && isAdded) {
                // Get extra data included in the Intent

                val currentFileStatus = intent.getIntExtra(
                        Constants.BEING_UPLOADED_STATUS, 0)
                val currentFileCount = intent.getIntExtra(
                        Constants.CURRENT_FILE_COUNT, 0)
                val totalFiles = intent.getIntExtra(Constants.TOTAL_FILES_TO_UPLOAD, 0)
                mTotalCount = totalFiles
                mAlbumName = intent.getStringExtra(Constants.UPLOADING_ALBUM_NAME)!!
                val mFileMimeType = intent.getStringExtra(
                        Constants.BEING_UPLOADING_FILE_MIMETYPE)
                val filePath = intent.getStringExtra(
                        Constants.FILE_BEING_UPLOADING)
                viewModel.textCurrentUpload.value = "$filePath"

                val currentFileSize = intent.getIntExtra(
                    Constants.FILE_LOAD_SIZE, 0)/1024f
                val currentSpeed = intent.getIntExtra(
                    Constants.FILE_LOAD_SPEED, 0)

                val speed = currentSpeed/1024f

                viewModel.textStatus.value = String.format("File size: %.2f MB\n Speed: %.3f MB/sec",
                    currentFileSize, speed)

                previewMedia(filePath!!, mFileMimeType!!)
                /*if (!isPreviewMediaCalled) {
                    previewMedia(filePath, mFileMimeType)
                    isPreviewMediaCalled = true
                }*/

                val loadedSize = intent.getLongExtra(Constants.FILE_UPLOADED_LENGTH,0L)

                L.print(this,"Broadcast - file count: $currentFileCount ")
                updateProgressBar(currentFileStatus, currentFileCount, totalFiles, loadedSize)
                L.print(this,"$currentFileStatus, $currentFileCount, $totalFiles, $loadedSize")
                /*if (currentFileStatus>loadMemory+1 || currentFileStatus < 2){
                    updateProgressBar(currentFileStatus, currentFileCount, totalFiles, loadedSize)
                    loadMemory = currentFileStatus
                }
*/
            }
        }
    }
    private val mAllUploadDone = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val activity = activity
            if (activity != null && isAdded) {
                isPaused = false
                val totalFiles = intent.getIntExtra(Constants.TOTAL_FILES_TO_UPLOAD, 0)
                mTotalCount = totalFiles
                mAlbumName = intent.getStringExtra(Constants.UPLOADING_ALBUM_NAME)!!
                var loadedSize = intent.getLongExtra(Constants.FILE_UPLOADED_LENGTH,0L)
                updateProgressBar(0, 0, 0, 0L)
                showUploadSuccesfullAlert(loadedSize)
                FileUploader.instance.isUploading = false
                viewModel.showNotUploadInProgress()

                /*mRelativeLayoutUploadNotInProgress!!.visibility = View.VISIBLE
                mRelativeLayoutUploadInProgress!!.visibility = View.GONE
                mRelativeLayoutNoInternetAccess!!.visibility = View.GONE*/
                viewModel.textFileUpload.value = ""
                //tvFile_upload.text = ""
            }
        }
    }
    private val mAllUploadCancel = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val activity = activity
            if (activity != null && isAdded) {
                isPaused = false
                // Get extra data included in the Intent
                val currentFileStatus = intent.getIntExtra(Constants.BEING_UPLOADED_STATUS, 0)
                val currentFileCount = intent.getIntExtra(Constants.CURRENT_FILE_COUNT, 0)
                val totalFiles = intent.getIntExtra(Constants.TOTAL_FILES_TO_UPLOAD, 0)
                mTotalCount = totalFiles
                mAlbumName = intent.getStringExtra(Constants.UPLOADING_ALBUM_NAME)!!
                updateProgressBar(currentFileStatus, currentFileCount, totalFiles, 0L)
                showUploadCancelAlert()
                viewModel.textFileUpload.value = ""
                //tvFile_upload.text = ""
            }
        }
    }

    override fun onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mUploadingStarted)
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mMessageReceiver)
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mAllUploadDone)
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mUploadingStopped)
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mAllUploadCancel)
        super.onDestroy()
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

    private fun registerView() {
/*        mRelativeLayoutUploadInProgress = rootView!!.findViewById(
                R.id.rel_inProgress)
        mRelativeLayoutUploadNotInProgress = rootView!!.findViewById(
                R.id.rel_noUploads)
        mRelativeLayoutNoInternetAccess = rootView!!.findViewById(
                R.id.rel_noInternet)*/
        //mTextDescription = rootView!!.findViewById(R.id.tv_albumEmpty)
        /*btn_Upload = rootView!!.findViewById(R.id.btn_upload)
        btn_Upload_and_Delete = rootView!!.findViewById(R.id.btn_upload_and_delete)
        btn_Cancel = rootView!!.findViewById(R.id.btn_cancel)
        btn_Pause = rootView!!.findViewById(R.id.btn_pause)*/
        //tvFile_upload = rootView!!.findViewById(R.id.tvFile_upload)
        mImageView = rootView!!.findViewById(R.id.drawee_item)
        mCurrentFileProgress = rootView!!.findViewById(R.id.pbCurrent_file)
        //mCurrentUplod = rootView!!.findViewById(R.id.tvUpload_percentage)
        mAllUploadProgress = rootView!!.findViewById(R.id.pbAll_file)
        //mTotaltUplod = rootView!!.findViewById(R.id.tv_totalUpload)
        mCurrentFileProgress!!.max = 100
        /*mStatus = rootView!!.findViewById(R.id.tvStatus)
        mStatusNoInternet = rootView!!.findViewById(R.id.tvStatusNoInternet)
        tvCurrent_file = rootView!!.findViewById(R.id.tvCurrent_file)
        */
        rootView!!.findViewById<TextView>(R.id.tvVersion).text = "${BuildConfig.VERSION_NAME}"
    }

    private fun registerClickListner() {
        /*btn_Upload.setOnClickListener(this)
        btn_Cancel.setOnClickListener(this)
        btn_Pause.setOnClickListener(this)
        btn_Upload_and_Delete.setOnClickListener(this)*/
        rootView?.let{
            it.findViewById<Button>(R.id.btn_upload).setOnClickListener(this)
            it.findViewById<Button>(R.id.btn_cancel).setOnClickListener(this)
            it.findViewById<Button>(R.id.btn_pause).setOnClickListener(this)
            it.findViewById<Button>(R.id.btn_upload_and_delete).setOnClickListener(this)
        }
    }

    private fun previewMedia(filePath: String, fileMimeType: String) {
        if (fileMimeType == Constants.VIDEO) {
            val bitmap = ThumbnailUtils.createVideoThumbnail(filePath,
                    MediaStore.Video.Thumbnails.MINI_KIND)
            mImageView!!.setImageBitmap(bitmap)
        } else {
            // bitmap factory
            val options = BitmapFactory.Options()
            // down sizing image as it throws OutOfMemory Exception for larger
            // images
            options.inSampleSize = 8

            GlobalScope.launch(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeFile(filePath, options)
                val getFullBitmap = getBitmap(filePath,bitmap)
                withContext(Dispatchers.Main){
                    mImageView!!.setImageBitmap(getFullBitmap)
                }
            }
        }
    }

    private fun getBitmap(photoPath:String, bitmap:Bitmap): Bitmap {
        val ei = ExifInterface(photoPath)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_UNDEFINED);
        return when(orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90F)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180F)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f);
            else -> bitmap;
        }
    }

    private fun rotateImage(source:Bitmap, angle:Float):Bitmap {
        val matrix = Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                               matrix, true);
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgressBar(currProcess: Int, currFileCount: Int,
                                  totalFileCount: Int, loadedSize: Long) {
        L.print(this,"current - $currProcess")
        //mCurrentFileProgress!!.progress = currProcess
        //viewModel.textCurrentUpload.value = "$currProcess%"
        var currFileCount = currFileCount
        mAllUploadProgress!!.max = totalFileCount
        mCurrentFileProgress!!.progress = currProcess
        viewModel.textCurrentUpload.value = "$currProcess%"
        //mCurrentUplod!!.text = "$currProcess%"
        if (currProcess < currFileCount) {
            currFileCount -= 1
        }
        mAllUploadProgress!!.progress = currFileCount
        var text = "$currFileCount of $totalFileCount  ${getText(R.string.text_been_upload)}"
        L.print(this,"TAG isDelete updateProgressBar - ${FileUploader.instance.isDelete}")
        if (FileUploader.instance.isDelete){
            text += String.format(" %.1f MB has been cleared",loadedSize/(1024F*1024F))
            viewModel.textFileUpload.value = "Uploading and Delete"
            //tvFile_upload.text = "Uploading and Delete"
        } else {
            viewModel.textFileUpload.value = "Uploading"
            //tvFile_upload.text = "Uploading"
        }
        viewModel.textTotalUpload.value = text

        //mTotaltUplod!!.text = text
    }

    private fun showUploadSuccesfullAlert(loadedSize: Long) {
        if (!isAlertIsDisplayed) {
            isAlertIsDisplayed = true
            val mUtility = Utility.instance
            var message = ""
            message = if (FileUploader.instance.isDelete)
                String.format("Uploading and deleting of %d files " +
                        "to album \"%s\" has been finished, " +
                        "and %.1fMB has been cleared",
                    mTotalCount, mAlbumName, loadedSize/(1024F*1024F))
            else
                "Uploading of $mTotalCount files to album $mAlbumName has been finished."
//                message=String.format("\n %.1f Mb memory space cleared",loadedSize/(1024F*1024F))

            mUtility.showUploadSuccessAlert(requireActivity(), this, message)
            deleteOldDirectory()
        }
    }

    private fun showUploadCancelAlert() {
        if (!isAlertIsDisplayed) {
            isAlertIsDisplayed = true
            val mUtility = Utility.instance
            val message = "Uploading cancelled!"
            mUtility.showUploadSuccessAlert(requireActivity(), this, message)
            //deleteOldDirectory()
        }
    }

    private fun browseMulipleItems(isDelete:Boolean) {
        val rxPermissions = RxPermissions(requireActivity())
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE)
            .subscribe(object : Observer<Boolean> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: Boolean) {
                    if (t) {
                        deleteOldDirectory()
                        Matisse.from(this@UploadFileFragment)
                            .choose(MimeType.ofAll())
                            .countable(true)
                            .maxSelectable(250)
                            .gridExpectedSize(
                                resources.getDimensionPixelSize(
                                    R.dimen.grid_expected_size))
                            .restrictOrientation(
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                            .thumbnailScale(0.85f)
                            .imageEngine(GlideEngine())
                            .forResult(REQUEST_CODE_CHOOSE
                                , if (isDelete) 1 else 0)
                    } else {
                        Toast.makeText(activity,
                            R.string.permission_download_denied, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onError(e: Throwable) {

                }

                override fun onComplete() {

                }
            })
    }

    @SuppressLint("InflateParams")
    private fun showChoiceDialog() {
        makeList()
        mDialog = Dialog(requireActivity())

        val inflater = requireActivity().layoutInflater
        val convertView = inflater.inflate(R.layout.size_reduction_dialog_view, null) as View
        val textView = convertView.findViewById<TextView>(R.id.sharetext)
        val str = SpannableStringBuilder(
                getString(R.string.resize_photos_text))
        str.setSpan(ForegroundColorSpan(
                ContextCompat.getColor(requireActivity(), R.color.colorDarkGrey)), 142, 156, 0)
        str.setSpan(RelativeSizeSpan(0.9f), 142, 156, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = str
        choiceAlert = convertView.findViewById(R.id.choiceDialog)
        val sizeReductionDialogAdapter = SizeReductionDialogAdapter(
                requireActivity(),
                R.layout.size_reduction_dialog_view, listChoice)

        choiceAlert.adapter = sizeReductionDialogAdapter
        mDialog.setContentView(convertView)
        mDialog.show()
        setChoiceListViewListner()
    }

    private fun makeList() {
        listChoice.clear()
        listChoice.add("Resized")
        listChoice.add(" Originals")
        listChoice.add("Cancel")
    }

    private fun setChoiceListViewListner() {
        choiceAlert.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val choiceSelected = listChoice[position]
            when (choiceSelected) {
                "Resized" -> {
                    isAllowCompress = true
                    mDialog.dismiss()
                    startUploadingFiles(fileTypeArrayList, albumId)
                }
                " Originals" -> {
                    isAllowCompress = false
                    mDialog.dismiss()
                    startUploadingFiles(fileTypeArrayList, albumId)
                }
                "Cancel" -> {
                    isAllowCompress = false
                    mDialog.dismiss()
                    fileTypeArrayList.clear()
                }
                else -> {
                    mDialog.dismiss()
                    fileTypeArrayList.clear()
                }
            }
        }
    }

    //start file uploading and Load File Uploader progress fragment.
    private fun startUploadingFiles(fileTypes: ArrayList<FileType>, albumId: Long?) {
        if (mUtility.isConnectingToInternet(requireActivity())) {
            val fileUploader = FileUploader.instance
            if (!fileUploader.isUploading) {
                //if ((activity as FotkiTabActivity).myService == null) Log.d("LOG","Service - null!!")
                fileUploader.myService = (activity as FotkiTabActivity).uploadThreadManager.myService
                fileUploader.isUploading = true
                fileUploader.isCompressionAllow = isAllowCompress
                fileUploader.mFileTypes.clear()
                fileUploader.mFileTypes.addAll(fileTypes)
                fileUploader.mAlbumId = 0L
                fileUploader.mAlbumId = albumId
                fileUploader.mContext = activity
                fileUploader.mAlbumName = albumUploadName
                fileUploader.startFileUploadingTask()
                isAllowCompress = false

                viewModel.showUploadInProgress()
/*
                mRelativeLayoutUploadInProgress!!.visibility = View.VISIBLE
                mRelativeLayoutUploadNotInProgress!!.visibility = View.GONE
*/
            }
        } else{
            viewModel.showNoInternet()
/*
            mRelativeLayoutNoInternetAccess!!.visibility = View.VISIBLE
            mRelativeLayoutUploadNotInProgress!!.visibility = View.GONE
            mRelativeLayoutUploadInProgress!!.visibility = View.GONE
*/
        }
    }


    //get Uri Path of image.
    private fun getPath(contentUri: Uri): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val loader = CursorLoader(requireActivity().application, contentUri, proj, null, null, null)
        val cursor = loader.loadInBackground()
        val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        val result = cursor.getString(column_index)
        cursor.close()
        return result
    }

    private fun deleteOldDirectory() {
        val path = Environment.getExternalStorageDirectory().toString() + java.io.File.separator + "FotkiUploader"

        val file = File(path)
        deleteRecursive(file)
        remove(path)
        deleteFromDatabase(requireActivity(), file)
        file.delete()
        val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        scanIntent.data = Uri.fromFile(file)
        requireActivity().sendBroadcast(scanIntent)
    }

    private fun remove(path: String) {
        val file = File(path)
        if (file.exists()) {
            val deleteCmd = "rm -r " + path
            val runtime = Runtime.getRuntime()
            try {
                runtime.exec(deleteCmd)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            for (child in fileOrDirectory.listFiles()) {
                deleteRecursive(child)
            }
        }
        fileOrDirectory.delete()
    }

    private fun deleteFromDatabase(context: Context, file: File) {
        val contentUri = MediaStore.Images.Media.getContentUri("external")
        val resolver = context.contentResolver
        val result = resolver.delete(contentUri, MediaStore.Images.ImageColumns.DATA + " LIKE ?",
                arrayOf(file.path))
        if (result > 0) {
            // success
            mUtility.fotkiAppDebugLogs(TAG, "Success")
        } else {
            // fail or item not exists in database
            mUtility.fotkiAppDebugLogs(TAG, "Failure")
        }
    }

    override fun returnToAlbumScreen(mFlag: Boolean) {
        isAlertIsDisplayed = false
        val intent = Intent(Constants.UPLOAD_BROADCAST_ALBUM)
        LocalBroadcastManager.getInstance(requireContext()!!).sendBroadcast(intent)
        val mSwitchIntent = Intent(Constants.SWITCH_TAB)
        intent.putExtra(Constants.NEW_UPLOAD, false)
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(mSwitchIntent)
    }



    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_upload -> {
                val fileUploader = FileUploader.instance
                fileUploader.isDelete = false
                if (!fileUploader.isUploading) {
                    if (mUtility.isConnectingToInternet(requireActivity())) {
                        //    clickAction = null
                        browseMulipleItems(false)
                    } else {
                        viewModel.showNoInternet()
/*
                        mRelativeLayoutNoInternetAccess!!.visibility = View.VISIBLE
                        mRelativeLayoutUploadNotInProgress!!.visibility = View.GONE
                        mRelativeLayoutUploadInProgress!!.visibility = View.GONE
*/
                    }
                }
            }
            R.id.btn_upload_and_delete -> {
                val fileUploader = FileUploader.instance
                fileUploader.isDelete = true
                if (!fileUploader.isUploading) {
                    if (mUtility.isConnectingToInternet(requireActivity())) {
                        //mClickAction = null
                        browseMulipleItems(true)
                    } else {
                        viewModel.showNoInternet()
/*
                        mRelativeLayoutNoInternetAccess!!.visibility = View.VISIBLE
                        mRelativeLayoutUploadNotInProgress!!.visibility = View.GONE
                        mRelativeLayoutUploadInProgress!!.visibility = View.GONE
*/
                    }
                }
            }

            R.id.btn_pause -> {

                (v as Button).text = if(isPaused)
                    requireActivity().resources.getString(R.string.bt_pause)
                else
                    requireActivity().resources.getString(R.string.bt_resume)

                if (isPaused){
//                    Log.d("TAG","Restart uploading photo!")

                    restartUploading()
                } else {
                    pauseUploading()
//                    Log.d("TAG","Pausing uploading photo!")
                }
                isPaused = !isPaused
            }
            R.id.btn_cancel -> {
                Log.d("UploadFilesViewModel","UploadFileFragment onClick btn_cancel")

                //Toast.makeText(activity,"Stopped!", Toast.LENGTH_LONG).show()
                FileUploader.instance.mContext = activity
                FileUploader.instance.cancelFileUploading()

                rootView!!.findViewById<Button>(R.id.btn_pause).text =
                    requireActivity().resources.getString(R.string.bt_pause)
                //btn_Pause.text = requireActivity().resources.getString(R.string.bt_pause)
                mCurrentFileProgress!!.progress = 0
                mAllUploadProgress!!.progress = 0

            }
        }
    }

    private fun pauseUploading() {
        (activity as FotkiTabActivity).uploadThreadManager.pauseUploadService()
    }

    private fun restartUploading() {
        (activity as FotkiTabActivity).uploadThreadManager.resumeUploadService()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == Activity.RESULT_OK) {
            val uris = ArrayList<Uri>()
            uris.addAll(Matisse.obtainResult(data))
            fileTypeArrayList.clear()
            for (i in uris.indices) {
                val picUri = uris[i]
                val mimeType = requireActivity().contentResolver.getType(picUri)
                val split = mimeType!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                filePath = getPath(picUri)
                val fileType: FileType
                fileType = if (true && split[1] == "gif") FileType(filePath, split[1]) else {
                    FileType(filePath, split[0])
                }
                fileTypeArrayList.add(fileType)
            }
            if (mUtility.isConnectingToInternet(requireActivity())) {
                if (FileUploader.instance.isDelete){
                    isAllowCompress = false
                    startUploadingFiles(fileTypeArrayList, albumId)
                } else{
                    showChoiceDialog()
                }
            } else {
                viewModel.showNoInternet()
/*
                mRelativeLayoutNoInternetAccess!!.visibility = View.VISIBLE
                mRelativeLayoutUploadNotInProgress!!.visibility = View.GONE
                mRelativeLayoutUploadInProgress!!.visibility = View.GONE
*/
            }
        }
    }

    companion object {
        private val REQUEST_CODE_CHOOSE = 23
        private var TAG = FolderFragment::class.java.name


        fun newInstance(tabType: String): UploadFileFragment {
            val fragment =
                UploadFileFragment()
            val args = Bundle()
            args.putString(ARGS_INSTANCE, tabType)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(): UploadFileFragment =
            UploadFileFragment()
    }
}
