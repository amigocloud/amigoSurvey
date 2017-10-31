package com.amigocloud.amigosurvey.repository

import android.util.Log
import com.amigocloud.amigosurvey.models.*
import com.amigocloud.amigosurvey.util.unzipFile
import com.amigocloud.amigosurvey.util.writeResponseBodyToDisk
import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.*
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.http.Url
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET


object AmigoClient {
     const val client_id = "5996bb4af375491b3d95"
     const val client_secret = "d4235bc6fd279ad93e3afc4f53d650c820d1b97f"
     const val base_url = "https://www.amigocloud.com/api/v1/"
     const val oauth = "oauth2/access_token"
}

interface AmigoApi {

    @GET("me")
    fun getUser() : Single<UserModel>

    @GET("me/projects?summary")
    fun getProjects(
            @Query("limit") limit: Int,
            @Query("offset") offset: Int
    ): Single<Projects>

    @GET("users/{user_id}/projects/{project_id}")
    fun getProject(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long) : Single<ProjectModel>

    @GET("users/{user_id}/projects/{project_id}/datasets/{dataset_id}")
    fun getDataset(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Path("dataset_id") dataset_id: Long) : Single<DatasetModel>

    @GET("users/{user_id}/projects/{project_id}/datasets?summary")
    fun getDatasets(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Query("limit") limit: Int,
            @Query("offset") offset: Int): Single<Datasets>

    @GET("related_tables/{related_table_id}")
    fun getRelatedTable(
            @Path("related_table_id") user_id: Long) : Single<RelatedTableModel>


    @GET("users/{user_id}/projects/{project_id}/datasets/{dataset_id}/related_tables")
    fun getRelatedTables(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Path("dataset_id") dataset_id: Long) : Single<RelatedTables>

    @GET("users/{user_id}/projects/{project_id}/support_files")
    fun getSupportFiles(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long) : Single<SupportFilesModel>

    @GET
    fun downloadFileWithUrlSync(@Url fileUrl: String): Call<ResponseBody>

    @GET("users/{user_id}/projects/{project_id}/datasets/{dataset_id}/forms_summary")
    fun getForms(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Path("dataset_id") dataset_id: Long) : Single<FormModel>

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

@Singleton
class AmigoRest @Inject constructor(
        private val config: SurveyConfig,
        private val moshi: Moshi,
        retrofit: Retrofit) {

    val TAG = "AmigoRest"

    val apiToken: String? = null

    private val amigoApi = retrofit.create(AmigoApi::class.java)

    private var user: UserModel? = null
    private var token: AmigoToken? = null
    internal val authHeader get() = token?.header

    private var project: ProjectModel? = null
    private var dataset: DatasetModel? = null

    init {
        val json = config.amigoTokenJson.value
        if (json.isNotEmpty()) {
            token = moshi.adapter(AmigoToken::class.java).fromJson(json)
                    ?: throw Exception("Failed to parse token")
        }
    }

    fun login(email: String, password: String): Single<UserModel> = amigoApi.login(
            AmigoClient.client_id,
            AmigoClient.client_secret,
            "password",
            email,
            password
    ).flatMapCompletable { Completable.fromAction { it.save() } }
            .andThen(amigoApi.getUser())
            .doOnSuccess { user = it }

    fun logout(): Completable = Completable.fromAction {
        token = null
        user = null
        config.amigoTokenJson.value = ""
    }

    fun fetchUser(): Single<UserModel> = user.toMaybe().switchIfEmpty(amigoApi.getUser())

    fun fetchProjects(limit: Int = 20, offset: Int = 0) = amigoApi.getProjects(limit, offset)

    fun fetchDatasets(projectId: Long, limit: Int = 20, offset: Int = 0): Single<Datasets> =
            fetchUser().flatMap { amigoApi.getDatasets(it.id, projectId, limit, offset) }

    fun fetchProject(user_id: Long, project_id: Long) = amigoApi.getProject(user_id, project_id)

    fun fetchDataset(user_id: Long, project_id: Long, dataset_id: Long) = amigoApi.getDataset(user_id, project_id, dataset_id)

    fun fetchForms(user_id: Long, project_id: Long, dataset_id: Long) = amigoApi.getForms(user_id, project_id, dataset_id)

    fun fetchRelatedTables(user_id: Long, project_id: Long, dataset_id: Long) = amigoApi.getRelatedTables(user_id, project_id, dataset_id)

    fun downloadFileWithUrlSync(url: String) = amigoApi.downloadFileWithUrlSync(url)

    fun fetchSupportFiles(user_id: Long, project_id: Long) = amigoApi.getSupportFiles(user_id, project_id)

    internal fun refreshToken(): Single<AmigoToken> = token?.let {
        amigoApi.refreshToken(
                AmigoClient.client_id,
                AmigoClient.client_secret,
                "refresh_token",
                it.refresh_token
        ).map { it.apply { save() } }
    } ?: Single.error(Exception("No token found"))

    private fun AmigoToken.save() {
        val adapter = moshi.adapter(AmigoToken::class.java)
        val json = adapter.toJson(this)
        print(json)
        config.amigoTokenJson.value = json
        token = this
    }
}



