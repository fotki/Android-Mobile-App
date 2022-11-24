package com.tbox.fotki.view.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.tbox.fotki.view.activities.backup_settings_activity.BackupSettingsActivity
import com.tbox.fotki.util.sync_files.BackupProperties
import com.tbox.fotki.R
import com.tbox.fotki.util.L
import com.zhihu.matisse.internal.entity.Album
import kotlinx.android.synthetic.main.album_folder_dialog.view.*

class AlbumFoldersDialog:DialogFragment() {

    private val selectedAlbums = HashSet<String>()
    private lateinit var backupProperties:BackupProperties

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val listFolders = arguments?.getParcelableArrayList<Album>(LIST_FOLDERS_EXTRA) ?: ArrayList()
        val root = LayoutInflater.from(context).inflate(R.layout.album_folder_dialog,null,false)
        val layout = root.llCheckBoxes
        backupProperties = BackupProperties(activity as AppCompatActivity)

        for (album:Album in listFolders){
            var checkbox = CheckBox(context)
            checkbox.text = "${album.getDisplayName(context)} (${album.count})"
            if (backupProperties.folderList.contains(album.id)){
                checkbox.isChecked = true
                selectedAlbums.add(album.id)
            }
            checkbox.tag = album.id
            checkbox.setOnCheckedChangeListener(checkedChange)
            checkbox.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            layout.addView(checkbox)
        }

        return AlertDialog.Builder(requireContext())
            .setView(root)
            .setPositiveButton(R.string.save) { _, _->saveSelected()}
            .create()
    }

    private fun saveSelected() {
        Log.d("TAG","List - $selectedAlbums")
        backupProperties.commitAlbumsToPrefs(selectedAlbums)
        arguments?.let {
            if (it.getBoolean(IS_STARTED_EXTRA)){
                (activity as BackupSettingsActivity).startWithAlbums()
            } else {
                (activity as BackupSettingsActivity).reloadAlbums(true)
            }
        }
    }

    private val checkedChange = CompoundButton.OnCheckedChangeListener { p0, p1 ->
        val id = p0.tag as String
        L.print(this,"TAG checked $id to $p1")
        if (p1){
            selectedAlbums.add(id)
        } else {
            selectedAlbums.remove(id)
        }
    }

    companion object{
        const val IS_STARTED_EXTRA = "is_started"
        const val LIST_FOLDERS_EXTRA = "list_folders"
    }
}