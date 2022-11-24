package com.tbox.fotki.refactoring.screens

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tbox.fotki.BuildConfig
import com.tbox.fotki.model.entities.Session
import com.tbox.fotki.refactoring.api.DashboardEvents
import com.tbox.fotki.refactoring.api.MainApi
import com.tbox.fotki.util.Constants
import com.tbox.fotki.util.L
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

class MainViewModel(val mainApi: MainApi, val session:Session):ViewModel() {

    private val gson = Gson()

    val eventsLiveData: MutableLiveData<DashboardEvents> by lazy {
        MutableLiveData<DashboardEvents>()
    }

    fun loadFolder(folderId:Long, level:Int) {

        L.print(this,"${BuildConfig.API_PROD}${Constants.GET_FOLDER_CONTENT_R}")
        L.print(this, "sessionId - ${session.mSessionId} " +
                "folderId - $folderId level - $level")

        viewModelScope.launch {
            session.mSessionId?.let{ sessionId->
                try {
                    val call = mainApi.getFolder(
                        sessionId,
                        folderId,
                        level)
                        .awaitResponse()
                    if (call.isSuccessful) {
                        val data = call.body()
                        if (data != null) {
                            L.print(this,"Data - $data")
/*                            questionList.clear()
                            questionList.addAll(data)
                            eventsLiveData.postValue(QuestionList(data))*/
                        } else {
                            L.print(this,"Error")
                            //eventsLiveData.postValue(Error)
                        }
                    } else {
                        L.print(this,"Request failed")
                    }
                } catch (e: Exception) {
                    L.print(this,"Error happend!")
                }
            }
        }
    }
}