package com.tbox.fotki.util

import android.content.Context
import android.content.Intent
import androidx.annotation.NonNull
import androidx.core.app.JobIntentService


class FcmRegistrationJobIntentService : JobIntentService() {

    override fun onHandleWork(@NonNull intent: Intent) {
        // the code from IntentService.onHandleIntent() ...
    }

    companion object {
        // Unique job ID for this service.
        internal val JOB_ID = 42

        // Convenience method for enqueuing work in to this service.
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, FcmRegistrationJobIntentService::class.java,
                JOB_ID, work)
        }
    }
}