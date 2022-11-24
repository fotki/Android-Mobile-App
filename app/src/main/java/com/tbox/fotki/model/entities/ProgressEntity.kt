package com.tbox.fotki.model.entities

import android.content.Intent
import com.tbox.fotki.util.Constants

data class ProgressEntity(val currentFileCount: Int, val totalFiles: Int, val isBackup: Boolean) {

    constructor(intent: Intent) : this(
        intent.getIntExtra(Constants.CURRENT_FILE_COUNT, 0),
        intent.getIntExtra(Constants.TOTAL_FILES_TO_UPLOAD, 0),
        intent.getStringExtra(Constants.UPLOADING_ALBUM_NAME)!!.isEmpty()
    )
}