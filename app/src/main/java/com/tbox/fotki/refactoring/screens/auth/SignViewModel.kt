package com.tbox.fotki.refactoring.screens.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson

class SignViewModel(
    //private val authApi: AuthApi, private val userAccount: UserAccount
) :
    ViewModel() {

    private val gson = Gson()

    private val eventsLiveData: MutableLiveData<AuthEvents> by lazy {
        MutableLiveData<AuthEvents>()
    }

    fun subscribeToEvents() = eventsLiveData


   /* fun startSocialAuth(
        network: ApiModels.SocialNetworkNetwork,
        id: String,
        email: String,
        firstName: String,
        lastName: String
    ) {
        viewModelScope.launch {
            try {
                val response =
                    authApi.socialLoginAsync(
                        network.name.toLowerCase(),
                        id,
                        email,
                        firstName,
                        lastName,
                        "android"
                    ).awaitResponse()
                if (response.isSuccessful) {
                    handleQuery(response)
                } else {
                    val error = parseErrorToString(response.errorBody())
                    error.toString()
                    val res = parseErrorBody(response.errorBody()!!)
                    eventsLiveData.postValue(OnError(res.error_string))
                }
            } catch (e: Exception) {
                eventsLiveData.postValue(OnFailure(R.string.dialog_no_internet))
            }
        }
    }

    private fun handleQuery(response: Response<JsonObject>) {
        var newUser: User? = null
        response.body()?.let { body ->
            val res = body.getSafeJsonObject("result")
            res?.let { userJson ->
                newUser = gson.fromJson(userJson, User::class.java)
                newUser?.let {
                    userAccount.saveUser(it)
                    eventsLiveData.postValue(OnSuccess)
                }
            }
            if (newUser == null) {
                eventsLiveData.postValue(OnError(response.message()))
            }
        }
    }

    fun createAccount(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        deviceType: String = "android"
    ) {
        viewModelScope.launch {
            try {
                val response = authApi.signUpAsync(firstName, lastName, email, password, deviceType)
                    .awaitResponse()
                if (response.isSuccessful) {
                    handleQuery(response)
                } else {
                    val res = parseErrorBody(response.errorBody()!!)
                    eventsLiveData.postValue(OnError(res.error_string))
                }
            } catch (e: Exception) {
                eventsLiveData.postValue(OnFailure(R.string.dialog_no_internet))
            }
        }
    }

    fun login(email: String, password: String) {

        viewModelScope.launch {
            try {
                val response = authApi.loginAsync(email, password).awaitResponse()
                if (response.isSuccessful) {
                    handleQuery(response)
                } else {
                    val res = parseErrorBody(response.errorBody()!!)
                    eventsLiveData.postValue(OnError(res.error_string))
                }
            } catch (e: Exception) {
                eventsLiveData.postValue(OnFailure(R.string.dialog_no_internet))
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            try {
                val response = authApi.forgotAsync(email).awaitResponse()
                if (response.isSuccessful) {
                    val msg = response.body()?.getSafeString("error_string") ?: ""
                    eventsLiveData.postValue(OnSuccessMsg(msg.replace("\"", "")))
                } else {
                    val res = parseErrorBody(response.errorBody()!!)
                    eventsLiveData.postValue(OnError(res.error_string))
                }
            } catch (e: Exception) {
                eventsLiveData.postValue(OnFailure(R.string.dialog_no_internet))
            }
        }
    }*/
}