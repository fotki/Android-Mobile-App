package com.tbox.fotki.refactoring.service

import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.tbox.fotki.BuildConfig
import com.tbox.fotki.model.entities.Session
import com.tbox.fotki.refactoring.api.OnByteWrittenListener
import com.tbox.fotki.refactoring.api.PhotoApi
import com.tbox.fotki.refactoring.api.counting
import com.tbox.fotki.refactoring.api.toRequestBody
import com.tbox.fotki.refactoring.general.ForegroundService
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import com.tbox.fotki.util.NotificationHelperNew
import com.tbox.fotki.util.upload_files.FileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UploadFilesService : ForegroundService() {

    private val executorService = Executors.newFixedThreadPool(4)
    private var uploadJob: Job? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val myBinder = MyLocalBinder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startWithForeground(3333, "Upload files started")
        }
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        notificationHelper = NotificationHelperNew(this)
        return myBinder
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): UploadFilesService {
            return this@UploadFilesService
        }
    }

    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder().baseUrl(BuildConfig.API_PROD).client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create()).build()
    }

    fun provideLogInterceptor(): HttpLoggingInterceptor {
        val logInterceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger {
            Log.d("Fotki", "Retrofit log - $it")
        })
        logInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return logInterceptor
    }

    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient().newBuilder()
            .addInterceptor(loggingInterceptor).build()
    }

    fun uploadRecord(fileToUpload: FileType?) {
        if (fileToUpload == null) {
            return
        }

        val file = File(fileToUpload.mFilePath)


        /*for (i in 0..10) {
            L.print(this, "file for upload - ${file.absolutePath} i - $i")
            Thread.sleep(3000)
        }*/

        val filePart = MultipartBody.Part.createFormData(
            "photo",
            file.name,
            file.toRequestBody(fileToUpload.mFileMimeType.toMediaType()).counting(object :
                OnByteWrittenListener {
                override fun onByteWritten(byteWritten: Long, contentLength: Long) {
                    //byteWritten.toFloat() / contentLength.toFloat()
                    //100 * (byteWritten.toFloat() / contentLength.toFloat())
                    Log.d(
                        "upload progress ${file.absolutePath} ---->",
                        (100 * (byteWritten.toFloat() / contentLength.toFloat())).toString()
                    )
                }
            })
        )

        filePart.toString()

        val url = Constants.BASE_URL
        val client = provideOkHttpClient(provideLogInterceptor())
            .newBuilder()
            .readTimeout(150, TimeUnit.SECONDS)
            .writeTimeout(150, TimeUnit.SECONDS)
            .connectTimeout(150, TimeUnit.SECONDS)
            .build()

        val retrofit =
            Retrofit.Builder().client(client).baseUrl(url).addConverterFactory(
                GsonConverterFactory.create()
            ).build()
        val audioApi = retrofit.create(PhotoApi::class.java)

        try {
            audioApi.uploadAudioAsync(
                Constants.FILE_UPLOAD,
                fileToUpload.albumId.toString(),
                Session.getInstance(this@UploadFilesService).mSessionId ?: "",
                filePart
            )
                .enqueue(object : Callback<String> {
                    override fun onFailure(call: Call<String>, t: Throwable) {
                        t.toString()
                        L.print(this, "API_UPLOAD Failure")
                        //stateLiveData.postValue(ErrorUpload("Something was wrong. Please try again"))
                    }

                    override fun onResponse(
                        call: Call<String>,
                        response: Response<String>
                    ) {
                        if (response.isSuccessful) {
                            L.print(this, "API_UPLOAD Success!")
                            /*stateLiveData.postValue(FinishUpload)
                                if (response.body() != null) {
                                    val code = response.body()!!.getSafeString("error_code")
                                    val message =
                                        response.body()!!.getSafeString("error_string")
                                    if (code == "100") {
                                        stateLiveData.postValue(ErrorUpload(message))
                                    } else {
                                        stateLiveData.postValue(FinishUpload)
                                    }
                                } else {
                                    stateLiveData.postValue(ErrorUpload("Something was wrong. Please try again"))
                                }*/
                            } else {
                                L.print(this, "API_UPLOAD Error parse")
                                /*val result = parseErrorBody(response.errorBody()!!)
                                stateLiveData.postValue(ErrorUpload(result.error_string))*/
                            }
                        }
                    })
            } catch (e: Exception) {
                L.print(this, "API_UPLOAD Exception error")
                //stateLiveData.postValue(ErrorUpload("Something was wrong. Please try again"))
            }


    }

    fun stopUpload() {
        TODO("Not yet implemented")
    }

    fun pauseUploadService() {
        TODO("Not yet implemented")
    }

    fun resumeUploadService() {
        TODO("Not yet implemented")
    }

    fun upload(file: FileType) {
        executorService.submit {
            uploadRecord(file)
        }
    }

    companion object {
        const val COUNT = "count"
        const val FILE = "file"
    }
}