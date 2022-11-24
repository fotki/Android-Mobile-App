package com.tbox.fotki.refactoring.api

import android.content.Context
import android.util.Log
import com.tbox.fotki.BuildConfig
import com.tbox.fotki.model.entities.Session
import com.tbox.fotki.refactoring.screens.MainViewModel
import com.tbox.fotki.refactoring.screens.slider.SliderViewModel
import com.tbox.fotki.refactoring.screens.upload_files.UploadFilesViewModel
import com.tbox.fotki.util.sync_files.PreferenceHelper
import com.tbox.fotki.view.fragments.album_fragment.AlbumFragmentViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

val networkModule = module {
    factory { provideLogInterceptor() }
    factory { provideOkHttpClient(get()) }
    //factory { provideAuthApi(get()) }
    factory { provideMainApi(get()) }
    //factory { provideAudioApi(get()) }
    single { provideRetrofit(get()) }
}

val viewModule = module {
    viewModel { AlbumFragmentViewModel() }
    viewModel { MainViewModel(get(),get()) }
    viewModel { SliderViewModel()}
    viewModel { UploadFilesViewModel()}
}

val dataModule = module {
    single { providePreferences(get()) }
    single { provideSession(get()) }
}

fun providePreferences(context: Context): PreferenceHelper {
    return PreferenceHelper(context)
}

fun provideSession(context: Context) = Session.getInstance(context)

fun provideMainApi(retrofit: Retrofit): MainApi = retrofit.create(
    MainApi::class.java)

/*fun provideAudioApi(retrofit: Retrofit): AudioApi = retrofit.create(
    AudioApi::class.java)*/

fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
    return Retrofit.Builder().baseUrl(BuildConfig.API_PROD).client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create()).build()
}

fun provideLogInterceptor(): HttpLoggingInterceptor {
    val logInterceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger {
        Log.d("Fotki","Retrofit log - ${it.toString()}")
    })
    logInterceptor.level = HttpLoggingInterceptor.Level.BODY
    return logInterceptor
}

fun provideOkHttpClient(loggingInterceptor:HttpLoggingInterceptor): OkHttpClient {
    return OkHttpClient().newBuilder()
        .addInterceptor(loggingInterceptor).build()
}