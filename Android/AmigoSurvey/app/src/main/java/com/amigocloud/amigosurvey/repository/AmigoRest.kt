package com.amigocloud.amigosurvey.repository

import android.content.Context
import com.amigocloud.amigosurvey.models.*
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import io.reactivex.Single
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.http.*


/**
 * Created by victor on 10/20/17.
 */

object AmigoClient {
     const val client_id = "5996bb4af375491b3d95"
     const val client_secret = "d4235bc6fd279ad93e3afc4f53d650c820d1b97f"
     const val base_url = "https://www.amigocloud.com/api/v1/"
     const val oauth = "oauth2/access_token"
}

interface AmigoApiInterface {

    @GET("me")
    fun getUser(@Header("Authorization") auth: String) : Observable<UserModel>

    @GET("me/projects")
    fun getProjects(@Header("Authorization") auth: String) : Observable<Projects>

    @GET("users/{user_id}/projects/{project_id}")
    fun getProject(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Header("Authorization") auth: String) : Observable<ProjectModel>

    @GET("users/{user_id}/projects/{project_id}/datasets/{dataset_id}")
    fun getDataset(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Path("dataset_id") dataset_id: Long,
            @Header("Authorization") auth: String) : Observable<DatasetModel>

    @GET("users/{user_id}/projects/{project_id}/datasets")
    fun getDatasets(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Header("Authorization") auth: String) : Observable<Datasets>

    @GET("related_tables/{related_table_id}")
    fun getRelatedTable(
            @Path("related_table_id") user_id: Long,
            @Header("Authorization") auth: String) : Observable<RelatedTableModel>


    @GET("users/{user_id}/projects/{project_id}/datasets/{dataset_id}/related_tables")
    fun getRelatedTables(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Path("dataset_id") dataset_id: Long,
            @Header("Authorization") auth: String) : Observable<RelatedTables>

    @GET("users/{user_id}/projects/{project_id}/support_files")
    fun getSupportFiles(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Header("Authorization") auth: String) : Observable<SupportFilesModel>

    @GET("users/{user_id}/projects/{project_id}/datasets/{dataset_id}/forms_summary")
    fun getForms(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Path("dataset_id") dataset_id: Long,
            @Header("Authorization") auth: String) : Observable<FormModel>

    @POST(AmigoClient.oauth)
    @FormUrlEncoded
    fun login(@Field("client_id") client_id: String,
              @Field("client_secret") client_secret: String,
              @Field("grant_type") grant_type: String,
              @Field("username") username: String,
              @Field("password") password: String): Observable<AmigoToken>

    @POST(AmigoClient.oauth)
    @FormUrlEncoded
    fun refreshToken(@Field("client_id") client_id: String,
              @Field("client_secret") client_secret: String,
              @Field("grant_type") grant_type: String,
              @Field("refresh_token") refresh_token: String): Observable<AmigoToken>

    companion object {
        fun create(): AmigoApiInterface {

            val retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(AmigoClient.base_url)
                    .build()

            return retrofit.create(AmigoApiInterface::class.java)
        }
    }
}

val amigoApiInterface by lazy {
    AmigoApiInterface.create()
}

class AmigoRest {

    fun getAuthToken(context: Context) : String {
        val sc =  SurveyConfig(context)
        val json = sc.getAmigoTokenJSON()
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        val adapter = moshi.adapter(AmigoToken::class.java)
        val amigoToken = adapter.fromJson(json) as AmigoToken
        val token = "Bearer " + amigoToken.access_token
        return token
    }

    fun login(email: String, password: String): Observable<AmigoToken> {
        return amigoApiInterface.login(
                client_id = AmigoClient.client_id,
                client_secret = AmigoClient.client_secret,
                grant_type = "password",
                username = email,
                password = password)

    }

    fun refreshToken(context: Context): Observable<AmigoToken> {
        val token = ""
        return amigoApiInterface.refreshToken(
                client_id = AmigoClient.client_id,
                client_secret = AmigoClient.client_secret,
                grant_type = "refresh_token",
                refresh_token = token)
    }

    fun fetchUser(context: Context) : Observable<UserModel> {
        return amigoApiInterface.getUser(getAuthToken(context))
    }

    fun fetchProjects(context: Context) : Observable<Projects> {
        return amigoApiInterface.getProjects(auth = getAuthToken(context))
    }

    fun fetchProject(user_id: Long, project_id: Long, context: Context) : Observable<ProjectModel> {
        return amigoApiInterface.getProject(user_id = user_id, project_id = project_id, auth = getAuthToken(context))
    }
}


