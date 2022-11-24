package com.tbox.fotki.refactoring.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.tbox.fotki.util.Constants
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

object ApiModels {

    enum class SocialNetworkNetwork {
        FACEBOOK,
        GOOGLE
    }
}

/*interface AuthApi {
    @FormUrlEncoded
    @POST("/mob-app/qpc/signup/")
    fun signUpAsync(
        @Field("FirstName") firstName: String,
        @Field("LastName") lastName: String,
        @Field("Email") email: String,
        @Field("Password") password: String,
        @Field("DeviceType") deviceType: String
    ): Call<JsonObject>

    @FormUrlEncoded
    @POST("/mob-app/qpc/signup/")
    fun socialLoginAsync(
        @Field("SocialType") network: String,
        @Field("SocialId") id: String,
        @Field("Email") email: String?,
        @Field("FirstName") firstName: String?,
        @Field("LastName") lastName: String?,
        @Field("DeviceType") DeviceType: String?
    ): Call<JsonObject>

    @FormUrlEncoded
    @POST("/mob-app/qpc/login/")
    fun loginAsync(
        @Field("Email") email: String,
        @Field("Password") password: String
    ): Call<JsonObject>

    @FormUrlEncoded
    @POST("/mob-app/qpc/forgot-password/")
    fun forgotAsync(@Field("Email") email: String): Call<JsonObject>
}*/

interface MainApi {

/*    @GET("/mob-app/qpc/questions/")
    fun getQuestions(): Call<List<Question>>

    @POST("/mob-app/qpc/reply/")
    fun replyQuestions(
        @Body data: JsonObject
    ): Call<JsonObject>*/

    @GET("get_folder_content")
    fun getFolder(
        @Field(Constants.SESSION_ID) sessionId: String,
        @Field(Constants.FOLDER_ID_ENC) folderId: Long,
        @Field("level") level: Int
    ): Call<String>
/*
    @GET("/mob-app/qpc/subject/")
    fun getSubjects(): Call<List<Subject>>

    @GET("/mob-app/qpc/subject/{id}")
    fun getSubjectById(@Path("id") id: Int): Call<JsonObject>

    @FormUrlEncoded
    @POST("/mob-app/qpc/subject/")
    fun addNewSubject(
        @Field("email") email: String,
        @Field("first_name") firstName: String,
        @Field("last_name") lastName: String
    ): Call<Subject>

    @FormUrlEncoded
    @POST("/mob-app/qpc/profile/")
    fun updateLocation(
        @Field("location_latitude") lat: Double,
        @Field("location_longitude") lon: Double,
        @Field("location_city") city: String
    ): Call<JsonObject>

    @FormUrlEncoded
    @POST("/mob-app/qpc/profile/")
    fun updateProfile(@FieldMap hashFields: HashMap<String, String>): Call<JsonObject>

    @FormUrlEncoded
    @PUT("/mob-app/qpc/subject/{id}/profile/")
    fun updateSubject(@Path("id") id: Int,@FieldMap hashFields: HashMap<String, String>): Call<JsonObject>*/

}

interface PhotoApi {
    @POST
    @Multipart
    fun uploadAudioAsync(
        @Url url: String,
        @Part("album_id_enc") album_id_enc: String,
        @Part("session_id") session_id: String,
        @Part part: MultipartBody.Part
    ): Call<String>

}