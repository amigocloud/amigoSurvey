package com.amigocloud.amigosurvey.repository

import android.content.Context
import com.amigocloud.amigosurvey.ApplicationScope
import com.amigocloud.amigosurvey.models.*
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import io.reactivex.Single
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Created by victor on 10/20/17.
 */

object AmigoClient {
     const val client_id = "5996bb4af375491b3d95"
     const val client_secret = "d4235bc6fd279ad93e3afc4f53d650c820d1b97f"
     const val base_url = "https://www.amigocloud.com/api/v1/"
     const val oauth = "oauth2/access_token"
}

interface AmigoApi {

    @GET("me")
    fun getUser(@Header("Authorization") auth: String) : Single<UserModel>

    @GET("me/projects")
    fun getProjects(@Header("Authorization") auth: String) : Single<Projects>

    @GET("users/{user_id}/projects/{project_id}")
    fun getProject(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Header("Authorization") auth: String) : Single<ProjectModel>

    @GET("users/{user_id}/projects/{project_id}/datasets/{dataset_id}")
    fun getDataset(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Path("dataset_id") dataset_id: Long,
            @Header("Authorization") auth: String) : Single<DatasetModel>

    @GET("users/{user_id}/projects/{project_id}/datasets")
    fun getDatasets(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Header("Authorization") auth: String) : Single<Datasets>

    @GET("related_tables/{related_table_id}")
    fun getRelatedTable(
            @Path("related_table_id") user_id: Long,
            @Header("Authorization") auth: String) : Single<RelatedTableModel>


    @GET("users/{user_id}/projects/{project_id}/datasets/{dataset_id}/related_tables")
    fun getRelatedTables(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Path("dataset_id") dataset_id: Long,
            @Header("Authorization") auth: String) : Single<RelatedTables>

    @GET("users/{user_id}/projects/{project_id}/support_files")
    fun getSupportFiles(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Header("Authorization") auth: String) : Single<SupportFilesModel>

    @GET("users/{user_id}/projects/{project_id}/datasets/{dataset_id}/forms_summary")
    fun getForms(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Path("dataset_id") dataset_id: Long,
            @Header("Authorization") auth: String) : Single<FormModel>

    @POST(AmigoClient.oauth)
    @FormUrlEncoded
    fun login(@Field("client_id") client_id: String,
              @Field("client_secret") client_secret: String,
              @Field("grant_type") grant_type: String,
              @Field("username") username: String,
              @Field("password") password: String): Single<AmigoToken>

    @POST(AmigoClient.oauth)
    @FormUrlEncoded
    fun refreshToken(@Field("client_id") client_id: String,
              @Field("client_secret") client_secret: String,
              @Field("grant_type") grant_type: String,
              @Field("refresh_token") refresh_token: String): Single<AmigoToken>
}

class TokenRefreshedException : Exception("Token Refreshed")

@Singleton
class AmigoRest @Inject constructor(
        private val config: SurveyConfig,
        private val moshi: Moshi,
        private val amigoApi: AmigoApi) {

    private var currentToken: AmigoToken? = null

    private val token
        get() : String {
            currentToken?.let { return it.access_token }
            val json = config.getAmigoTokenJSON()
            val token = moshi.adapter(AmigoToken::class.java).fromJson(json)
                    ?: throw Exception("Failed to parse token")
            return "Bearer " + token.access_token
    }

    fun login(email: String, password: String): Single<AmigoToken> {
        return amigoApi.login(
                client_id = AmigoClient.client_id,
                client_secret = AmigoClient.client_secret,
                grant_type = "password",
                username = email,
                password = password)
                .map { it.apply { save() } }

    }

    fun refreshToken(): Single<AmigoToken> {
        return currentToken?.let {
            amigoApi.refreshToken(
                    client_id = AmigoClient.client_id,
                    client_secret = AmigoClient.client_secret,
                    grant_type = "refresh_token",
                    refresh_token = it.refresh_token)
                    .map { it.apply { save() } }
        } ?: Single.error(Exception("No token found"))
    }

    fun AmigoToken.save() {
        val adapter = moshi.adapter(AmigoToken::class.java)
        val json = adapter.toJson(this)
        print(json)
        config.setAmigoTokenJSON(json)
        currentToken = this
    }

    fun fetchUser() : Single<UserModel> {
        return amigoApi.getUser(token).checkToken()
    }

    fun fetchProjects() : Single<Projects> {
        return amigoApi.getProjects(token).checkToken()
    }

    fun fetchProject(user_id: Long, project_id: Long) : Single<ProjectModel> {
        return amigoApi.getProject(user_id, project_id, token).checkToken()
    }

    private fun <T> Single<T>.checkToken(): Single<T> {
        return this.onErrorResumeNext{ error ->
            if(error is HttpException && error.code() == 403) {
                refreshToken().flatMap { Single.error<T>(TokenRefreshedException()) }
            } else Single.error(error)
        }.retry { error -> error is TokenRefreshedException }
    }
}



