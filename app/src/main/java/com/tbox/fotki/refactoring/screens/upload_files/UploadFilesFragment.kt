package com.tbox.fotki.refactoring.screens.upload_files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.UploadProperty
import com.tbox.fotki.util.*
import com.tbox.fotki.util.upload_files.FileUploader
import com.tbox.fotki.util.upload_files.ReturnToAlbumInterface
import com.tbox.fotki.view.activities.fotki_tab_activity.FotkiTabActivity
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_upload.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

@Parcelize
data class UploadStatus(
    var totalValue: Float, var speed: Float,
    var loaded: Int, var allCount: Int,
    var fileName1: String, var progress1: Float, var isVideo1: Boolean,
    var fileName2: String, var progress2: Float, var isVideo2: Boolean,
    var fileName3: String, var progress3: Float, var isVideo3: Boolean,
    var fileName4: String, var progress4: Float, var isVideo4: Boolean
) : Parcelable

class UploadFilesFragment: Fragment(){

    private val uploadFilesViewModel: UploadFilesViewModel by viewModel()
    private var isAlertIsDisplayed = false
    private var isPaused = false

    private var albumId = 0L
    private var albumUploadName = ""

    private lateinit var notificationHelper: NotificationHelperNew

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_upload, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val property = UploadProperty()
        property.fromPreferences(requireContext())
        notificationHelper = NotificationHelperNew(requireContext())
        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(
            R.string.tab_type_upload
        )

        if(property.isFinished){
            allItemsLoading.visibility = View.GONE
            grNotLoading.visibility = View.VISIBLE
            checkFragmentType()
        }
        uploadFilesViewModel.initBroadCasts({
            try {
                allItemsLoading.visibility = View.VISIBLE
                grNotLoading.visibility = View.GONE
            } catch (ex: Exception) {
            }
        }, {
            try {
                showUploadSuccessfull()
                allItemsLoading.visibility = View.GONE
                grNotLoading.visibility = View.VISIBLE
                FileUploader.instance.isUploading = false
            } catch (ex: Exception) {
            }
        }, {

            val resultIntent = Intent(requireContext(), FotkiTabActivity::class.java)
            notificationHelper.sendNotification(
                "Upload error", resultIntent,
                "Network connection failed."
            )
        }
        )
        requireActivity().registerBroadcasts(uploadFilesViewModel.broadcasts)

        uploadFilesViewModel.currentStatus.observe(viewLifecycleOwner, Observer { status ->
            L.print(this, "Status updated - $status")
            populateStatus(status)
        })

        btnCancel.setOnClickListener {
            Log.d("UploadFilesViewModel","UploadFilesFragment onClick btn_cancel")
            FileUploader.instance.mContext = requireActivity()
            FileUploader.instance.cancelFileUploading()
            showUploadCancelAlert()
            Handler().postDelayed({
                val resultIntent = Intent(requireContext(), FotkiTabActivity::class.java)
                notificationHelper.sendNotification(
                    "Upload canceled", resultIntent,
                    "You stopped files uploading."
                )
            }, 2000)

        }

        btnPause.setOnClickListener { v ->
            (v as Button).text = if(isPaused)
                requireContext().resources.getString(R.string.bt_pause)
            else
                requireContext().resources.getString(R.string.bt_resume)

            if (isPaused){
//                    Log.d("TAG","Restart uploading photo!")

                restartUploading()
            } else {
                pauseUploading()
//                    Log.d("TAG","Pausing uploading photo!")
            }
            isPaused = !isPaused
        }
    }

    private fun checkFragmentType() {
        val fragmentType = (requireContext().getSharedPreferences(
            Constants.DEFTYPE_FILE,
            Context.MODE_PRIVATE
        )
            .getString(Constants.DEFTYPE, Constants.DEFTYPE_KEY_ALBUM))
        if ((fragmentType).equals(Constants.DEFTYPE_KEY_ALBUM)) {
            tvAlbumEmpty.text = getString(R.string.text_Upload_from_Album)
            btnUpload.visibility = View.VISIBLE
            btnUploadAndDelete.visibility = View.VISIBLE
            albumId = requireContext().getSharedPreferences(
                Constants.DEFTYPE_FILE,
                Context.MODE_PRIVATE
            ).getLong(
                Constants.PREF_ALBUM_ID, 0L
            )
            albumUploadName = requireContext().getSharedPreferences(
                Constants.DEFTYPE_FILE,
                Context.MODE_PRIVATE
            ).getString(
                Constants.PREF_ALBUM_NAME, " "
            )!!
        } else {
            tvAlbumEmpty.text = getString(R.string.text_cannot_uploload_trough_folder)
            btnUpload.visibility = View.GONE
            btnUploadAndDelete.visibility = View.GONE
        }
    }

    private fun pauseUploading() {
        (activity as FotkiTabActivity).uploadThreadManager.pauseUploadService()
    }

    private fun restartUploading() {
        (activity as FotkiTabActivity).uploadThreadManager.resumeUploadService()
    }

    private fun showUploadSuccessfull() {
        if (!isAlertIsDisplayed) {
            isAlertIsDisplayed = true
            val mUtility = Utility.instance
            val uploadProperty = UploadProperty()
            uploadProperty.fromPreferences(requireContext())
            var message = ""
            message = if (FileUploader.instance.isDelete)
                String.format(
                    "Uploading and deleting of %d files " +
                            "to album \"%s\" has been finished, " +
                            "and %.1fMB has been cleared",
                    uploadProperty.fileTypes.size,
                    uploadProperty.albumName,
                    uploadProperty.loadedSize / (1024F * 1024F)
                )
            else
                "Uploading of ${uploadProperty.fileTypes.size} files to album \"${uploadProperty.albumName}\" has been finished."
//                message=String.format("\n %.1f Mb memory space cleared",loadedSize/(1024F*1024F))

            mUtility.showUploadSuccessAlert(requireContext(), returnToAlbumInterface, message)
        }
    }

    private fun showUploadCancelAlert() {
        if (!isAlertIsDisplayed) {
            isAlertIsDisplayed = true
            val mUtility = Utility.instance
            val message = "Uploading cancelled!"
            mUtility.showUploadSuccessAlert(requireContext(), returnToAlbumInterface, message)
            //deleteOldDirectory()
        }
    }

    private val returnToAlbumInterface = object: ReturnToAlbumInterface{
        override fun returnToAlbumScreen(mFlag: Boolean) {
            isAlertIsDisplayed = false
            val intent = Intent(Constants.UPLOAD_BROADCAST_ALBUM)
            LocalBroadcastManager.getInstance(context!!).sendBroadcast(intent)
            val mSwitchIntent = Intent(Constants.SWITCH_TAB)
            intent.putExtra(Constants.NEW_UPLOAD, false)
            LocalBroadcastManager.getInstance(context!!).sendBroadcast(mSwitchIntent)
        }
    }

    private fun populateStatus(status: UploadStatus?) {
        if(!isPaused){
            status?.let { status ->
                //tvSpeed.text = "Speed: ${status.speed} MB/sec"
                //val filesInQueue = status.allCount - status.loaded
                tvUploadedNumber.text = "${status.loaded} of ${status.allCount}"

                populateItem(
                    status.fileName1,
                    status.progress1,
                    tvName1,
                    tvPercent1,
                    ivImage1,
                    grImages1,
                    status.isVideo1
                )
                populateItem(
                    status.fileName2,
                    status.progress2,
                    tvName2,
                    tvPercent2,
                    ivImage2,
                    grImages2,
                    status.isVideo2
                )
                populateItem(
                    status.fileName3,
                    status.progress3,
                    tvName3,
                    tvPercent3,
                    ivImage3,
                    grImages3,
                    status.isVideo3
                )
                populateItem(
                    status.fileName4,
                    status.progress4,
                    tvName4,
                    tvPercent4,
                    ivImage4,
                    grImages4,
                    status.isVideo4
                )
            }
        }
    }

    private fun populateItem(
        fileName: String, progress: Float, tvName: TextView,
        tvPercent: TextView, ivImage: ImageView, grImages: Group, isVideo: Boolean
    ){
        if (progress>1){
            Log.d("uploadError","Progress $progress")
            grImages.visibility = View.VISIBLE
            tvName.text = File(fileName).name
            tvPercent.text = "${progress.toInt()}%"

            //ivImage.setImageDrawable(requireContext().getDrawable(R.drawable.ic_album))

            if(isVideo){
                Glide.with(requireContext())
                    .asBitmap()
                    .load(Uri.fromFile(File(fileName)))
                    .into(ivImage);
            } else {
                Picasso.get()
                    .load("file://$fileName")
                    //.resize(200, 200)
                    .fit().centerCrop()
                    .into(ivImage, object : Callback {
                        override fun onSuccess() {}
                        override fun onError(e: java.lang.Exception?) {
                            ivImage.setImageDrawable(requireContext().getDrawable(R.drawable.ic_album))
                        }
                    })
                ivImage.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        } else {
            grImages.visibility = View.GONE
        }
    }
}