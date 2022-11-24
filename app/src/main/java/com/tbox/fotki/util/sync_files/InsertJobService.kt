package com.tbox.fotki.util.sync_files

import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.JobIntentService
import com.tbox.fotki.model.database.AppDatabase
import com.tbox.fotki.model.database.FilesEntity
import com.tbox.fotki.model.database.PhotoCursorProvider
import com.tbox.fotki.model.database.SingletonHolder
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import com.tbox.fotki.util.LocalBroadcastHelper
import com.tbox.fotki.util.upload_files.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class InsertJobService : JobIntentService() {

    private lateinit var arrayFiles: List<FileType>

    private val db = object : SingletonHolder<AppDatabase, Context>({
        androidx.room.Room.databaseBuilder(it, AppDatabase::class.java, "history.db").build()
    }) {}

    override fun onHandleWork(intentIn: Intent) {
        PreferenceHelper(baseContext).applyPrefs(hashMapOf(Constants.SYNC_STARTED to true))
        L.print(this, "MEDIA onHandleWork")
        LogProvider.writeToFile("Start syncing", baseContext)
        try {
            arrayFiles = intentIn.getParcelableArrayListExtra(EXTRA_FILES)!!
            arrayFiles.let {
                if (intentIn.action == ACTION_INSERT) {
                    L.print(this, "MEDIA readAndSync")
                    LogProvider.writeToFile("readAndSync", baseContext)
                    try {
                        GlobalScope.launch(Dispatchers.IO) {
                            val dbFilesSet = getDBFilesSet()
                            var inserted = 0
                            for (file in arrayFiles) {
                                if (!dbFilesSet.contains(file.mFilePath)) {
                                    db.getInstance(baseContext).historyDao().insert(
                                        FilesEntity(
                                            file.mFilePath,
                                            file.mFileMimeType,
                                            "",
                                            0L,
                                            FilesEntity.STATUS_ONLY_ADDED,
                                            HashGenerator.hashSHA1(File(file.mFilePath))
                                        )
                                    )
                                    if (inserted > 200) break
                                    else inserted++
                                    L.print(this, "MEDIA Inserted $inserted")
                                }
                            }
                            val preferenceHelper = PreferenceHelper(baseContext)
                            L.print(
                                this,
                                "MEDIA Is started JOB - ${preferenceHelper.getBoolean(Constants.SYNC_STARTED)}"
                            )

                            LogProvider.writeToFile(
                                "Is started JOB - ${preferenceHelper.getBoolean(Constants.SYNC_STARTED)}",
                                baseContext
                            )

                            LocalBroadcastHelper.sendProgressFileSync(baseContext)
                            SyncJobService.enqueueWork(baseContext)
                        }
                    } catch (e: Exception) {

                    }
                }
            }

        } catch (e: Exception) {
        }
    }

    private fun getDBFilesSet(): HashSet<String> {
        val res = HashSet<String>()
        val list = db.getInstance(baseContext).historyDao().all
        for (item in list) {
            L.print(this, "item - $item")
            res.add(item.fileName)
        }
        return res
    }

    companion object {
        const val EXTRA_FILES = "property"
        private const val JOB_ID = 1100
        const val ACTION_INSERT = "insert"
        const val PREF_LAST_UPLOADED_TIME = "last_uploaded_time"

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun enqueueWork(context: Context) {
            try {
                L.print(this, "TAG_MEDIA enqueueWork")
                LogProvider.writeToFile("Before start syncing", context)
                val intent = Intent(ACTION_INSERT)
                val arrayFiles = PhotoCursorProvider.preparePhotoQuery(context)

                if (arrayFiles.size == 0) return

                intent.putParcelableArrayListExtra(EXTRA_FILES, arrayFiles)

                if (!PreferenceHelper(context).getBoolean(Constants.SYNC_STARTED)) {
                    val scheduler =
                        context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                    var hasBeenScheduled = false
                    for (jobInfo in scheduler.allPendingJobs) {
                        if (jobInfo.id == JOB_ID) {
                            hasBeenScheduled = true
                            break
                        }
                    }
                    if (!hasBeenScheduled)
                        enqueueWork(context, InsertJobService::class.java, JOB_ID, intent)
                    else {
                        LocalBroadcastHelper.sendProgressFileSync(context, false)
                        PreferenceHelper(context).applyPrefs(hashMapOf(Constants.SYNC_STARTED to false))
                    }

                }

                LocalBroadcastHelper.sendProgressFileSync(context, false)
                PreferenceHelper(context).applyPrefs(hashMapOf(Constants.SYNC_STARTED to false))
            } catch (e: Exception) {
                PreferenceHelper(context).applyPrefs(hashMapOf(Constants.SYNC_STARTED to false))
                LocalBroadcastHelper.sendProgressFileSync(context, false)
            }
        }
    }
}