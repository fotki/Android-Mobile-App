package com.tbox.fotki.model.web_providers.web_manager

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

/**
 * Created by Junaid on 4/6/17.
 */

internal class SingeltonRequestQueue {
    private var mRequestQueue: RequestQueue? = null

    private fun getRequestQueue(context: Context): RequestQueue? {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context.applicationContext)
        }
        return mRequestQueue
    }

    fun <T> addToRequestQueue(context: Context, req: Request<T>) {
        req.retryPolicy = DefaultRetryPolicy(
            5000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        getRequestQueue(context)!!.add(req)
    }

    companion object {
        private var mInstance: SingeltonRequestQueue? = null

        val instance: SingeltonRequestQueue
            @Synchronized get() {
                if (mInstance == null) {
                    mInstance =
                        SingeltonRequestQueue()
                }
                return mInstance as SingeltonRequestQueue
            }
    }
}
