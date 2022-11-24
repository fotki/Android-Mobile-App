package com.tbox.fotki.util

import android.Manifest
import android.content.*
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.ParcelableAlbum
import com.tbox.fotki.view.fragments.album_fragment.AlbumFragment
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zaphlabs.filechooser.KnotFileChooser
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.io.File
import java.io.IOException


object AlbumHelper {

    fun sendIntent(fragment: Fragment){
        val chooseFile = Intent(Intent.ACTION_OPEN_DOCUMENT)
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE)
        chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        chooseFile.type = "*/*"
        chooseFile.flags = FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION

        fragment.startActivityForResult(
            Intent.createChooser(chooseFile, "Choose a file"),
            AlbumFragment.REQUEST_CODE_CHOOSE
        )
    }

    fun browseMulipleItems(activity: FragmentActivity, fragment: Fragment, isDelete: Boolean) {
        val rxPermissions = RxPermissions(activity)
        rxPermissions.request(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
            .subscribe(object : Observer<Boolean> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: Boolean) {
                    if (t) {

                        sendIntent(fragment)

                        /*val intent = Intent(activity, FilePickerActivity::class.java)

                        val configs = Configurations.Builder()
                            .setCheckPermission(true)
                            .setShowImages(true)
                            .setShowAudios(true)
                            .setShowFiles(true)
                            .setTitle("Select files")
                            //.enableImageCapture(true)
                            //.setMaxSelection(1000)
                            .setSkipZeroSizeFiles(true)
                            .build()
                        intent.putExtra(
                            FilePickerActivity.CONFIGS, configs
                        )
                        intent.putExtra(
                            FilePickerActivity.IS_DELETE, if (isDelete) 1 else 0
                        )
                        fragment.startActivityForResult(intent, AlbumFragment.REQUEST_CODE_CHOOSE)*/

                        //val fileType = FileType.ALL //Select Which Files you want to show (By Default : ALL)

                         /*KnotFileChooser(activity,
                            allowBrowsing = true, // Allow User Browsing
                            allowCreateFolder = true, // Allow User to create Folder
                            allowMultipleFiles = false, // Allow User to Select Multiple Files
                            allowSelectFolder = false, // Allow User to Select Folder
                            minSelectedFiles = 0, // Allow User to Selec Minimum Files Selected
                            maxSelectedFiles = 0, // Allow User to Selec Minimum Files Selected
                            showFiles = true, // Show Files or Show Folder Only
                            showFoldersFirst = true, // Show Folders First or Only Files
                            showFolders = true, //Show Folders
                            showHiddenFiles = false, // Show System Hidden Files
                            initialFolder = Environment.getExternalStorageDirectory(), //Initial Folder
                            restoreFolder = false, //Restore Folder After Adding
                            cancelable = true, //Dismiss Dialog On Cancel (Optional)
                            fileType = FileType.ALL
                        )
                            .title("Select a File") // Title of Dialog
                            .sorter(Sorter.ByNewestModification) // Sort Data (Optional)
                            .onSelectedFilesListener {files ->
                                L.print(this, "files - $files")
                                // Callback Returns Selected File Object  (Optional)
                                //Toast.makeText(activity, it.toString(), Toast.LENGTH_SHORT).show()
                            }
                            .onSelectedFileUriListener { // Callback Returns Uri of File (Optional)

                            }
                            .show()*/


                        /* Matisse.from(fragment)
                            .choose(MimeType.ofAll())
                            .countable(true)
                            .maxSelectable(250)
                            .gridExpectedSize(
                                activity.resources.getDimensionPixelSize(
                                    R.dimen.grid_expected_size))
                            .restrictOrientation(
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                            .thumbnailScale(0.85f)
                            .imageEngine(GlideEngine())
                            .forResult(
                                AlbumFragment.REQUEST_CODE_CHOOSE
                                , if (isDelete) 1 else 0)*/
                    } else {
                        Toast.makeText(
                            activity,
                            R.string.permission_download_denied, Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onError(e: Throwable) {

                }

                override fun onComplete() {

                }
            })
    }
    fun copyToClipBoard(activity: FragmentActivity, album: ParcelableAlbum?) {
        album?.let {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("fotki_label", it.mShareUrl)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(activity, "Link copied to clipboard", Toast.LENGTH_LONG).show() }
    }
    fun sendLinkThroughImplicitIntent(activity: FragmentActivity, album: ParcelableAlbum?) {
        album?.let {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_TEXT, album.mShareUrl)
            shareIntent.type = "text/plain"
            activity.startActivity(shareIntent)
        }
    }
    fun appendStringMethod(
        mAlbumSize: Long, mAlbumItemCount: Int,
        mAlbumVideosCount: Int, mAlbumPhotosCount: Int
    ): String {
        val size = humanReadableByteCount(mAlbumSize, true)
        return " $mAlbumItemCount files, $mAlbumVideosCount videos | $mAlbumPhotosCount photos ($size)"
    }
    //calculate size of album
    private fun humanReadableByteCount(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024
        if (bytes < unit) return bytes.toString() + " B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    fun getPath(activity: FragmentActivity?, contentUri: Uri): String {
        activity?.let{
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            val loader = CursorLoader(activity.application, contentUri, proj, null, null, null)
            val cursor = loader.loadInBackground()
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            val result = cursor.getString(columnIndex)
            cursor.close()
            return result
        }
        return ""
    }

    // Delete

    private fun deleteOldDirectory(activity: FragmentActivity) {
        val path = Environment.getExternalStorageDirectory().toString() + java.io.File.separator + "FotkiUploader"
        val file = File(path)
        deleteRecursive(file)
        remove(path)
        deleteFromDatabase(activity, file)
        file.delete()
        val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        scanIntent.data = Uri.fromFile(file)
        activity.sendBroadcast(scanIntent)
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
        val result = resolver.delete(
            contentUri, MediaStore.Images.ImageColumns.DATA + " LIKE ?",
            arrayOf(file.path)
        )
    }

}