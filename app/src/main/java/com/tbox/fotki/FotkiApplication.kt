package com.tbox.fotki

import android.app.Application
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.facebook.common.logging.FLog
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.imagepipeline.listener.RequestLoggingListener
import com.tbox.fotki.refactoring.api.dataModule
import com.tbox.fotki.refactoring.api.networkModule
import com.tbox.fotki.refactoring.api.viewModule
//import io.fabric.sdk.android.Fabric
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.*


/**
* Created by Junaid on 4/17/17.
*/

class FotkiApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        if (Config.IS_DARK_MODE){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        FirebaseCrashlytics.getInstance()
        if (BuildConfig.DEBUG) FLog.setMinimumLoggingLevel(FLog.VERBOSE)
        val listeners = HashSet<RequestListener>()
        listeners.add(RequestLoggingListener())
        val config = ImagePipelineConfig.newBuilder(this)
            .setRequestListeners(listeners)
            .setDownsampleEnabled(true)
            .build()
        Fresco.initialize(this, config)
        startKoin {
            androidContext(this@FotkiApplication)
            modules(networkModule, viewModule, dataModule)
        }
    }
}
