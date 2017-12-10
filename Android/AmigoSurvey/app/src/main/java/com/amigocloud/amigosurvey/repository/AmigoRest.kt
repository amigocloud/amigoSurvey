package com.amigocloud.amigosurvey.repository

import android.support.v4.media.VolumeProviderCompat
import com.amigocloud.amigosurvey.form.ChunkedUploadResponse
import com.amigocloud.amigosurvey.models.*
import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.http.*
import javax.inject.Inject
import javax.inject.Singleton


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

    @GET("users/{user_id}/projects/{project_id}/datasets/{dataset_id}/schema")
    fun getSchema(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Path("dataset_id") dataset_id: Long) : Single<SchemaModel>

    @GET("users/{user_id}/projects/{project_id}/support_files")
    fun getSupportFiles(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long) : Single<SupportFilesModel>

    @GET
    fun downloadFile(@Url fileUrl: String): Single<ResponseBody>

    @GET("users/{user_id}/projects/{project_id}/datasets/{dataset_id}/forms_summary")
    fun getForms(
            @Path("user_id") user_id: Long,
            @Path("project_id") project_id: Long,
            @Path("dataset_id") dataset_id: Long) : Single<FormModel>

    @Headers("Content-Type: application/json")
    @POST
    fun submitChangeset(@Url submit_changeset: String, @Body body: RequestBody): Single<Any>

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

    @POST
    fun chunkedUpload(@Url url: String,
                      @Body body: RequestBody,
                      @HeaderMap headers: Map<String, String>): Single<ChunkedUploadResponse>

    @POST
    @FormUrlEncoded
    fun chunkedUploadComplete(@Url url: String,
                              @Field("upload_id") upload_id: String,
                              @Field("md5") md5: String,
                              @Field("source_amigo_id") source_amigo_id: String,
                              @Field("filename") filename: String,
                              @HeaderMap headers: Map<String, String>): Single<ChunkedUploadResponse>
}

data class UserJSON(
        var email: String = "",
        var id: String = "",
        var custom_id: String = "",
        var first_name: String = "",
        var last_name: String = "",
        var organization: String = "",
        var visible_projects: String = "",
        var projects: String = ""
)

data class ProjectJSON(
        var id: String = "",
        var name: String = "",
        var description: String = "",
        var organization: String = "",
        var history_dataset_id: String = ""
)

@Singleton
class AmigoRest @Inject constructor(
        private val config: SurveyConfig,
        private val moshi: Moshi,
        private val amigoApi: AmigoApi) {

    private val TAG = "AmigoRest"

    val apiToken: String? = null

    private var user: UserModel? = null
    private var token: AmigoToken? = null
    internal val authHeader get() = token?.header

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
    ).flatMapCompletable { Completable.fromAction { it.save() } }.andThen(fetchUser())


    fun logout(): Completable = Completable.fromAction {
        token = null
        user = null
        config.amigoTokenJson.value = ""
    }

    fun getUserJSON(user: UserModel): String {
        val userObj = UserJSON(
                id = user.id.toString(),
                email = user.email,
                custom_id = user.custom_id,
                first_name = user.first_name,
                last_name = user.last_name,
                organization = user.organization,
                visible_projects = user.visible_projects,
                projects = user.projectsUrl)
        return moshi.adapter(UserJSON::class.java).toJson(userObj)
    }

    fun getProjectJSON(p: ProjectModel): String {
        val obj = ProjectJSON(
                id = p.id.toString(),
                name = p.name,
                description = p.description,
                organization = p.organization,
                history_dataset_id = p.history_dataset_id.toString())
        return moshi.adapter(ProjectJSON::class.java).toJson(obj)
    }

    fun getListJSON(list: List<Any>): String = moshi.adapter(Any::class.java).toJson(list)

    fun getJSON(obj: Any): String = moshi.adapter(Any::class.java).toJson(obj)

    fun fetchUser(): Single<UserModel> = user.toMaybe()
            .switchIfEmpty(amigoApi.getUser().doOnSuccess { user = it })

    fun fetchProjects(limit: Int = 20, offset: Int = 0) = amigoApi.getProjects(limit, offset)

    fun fetchDatasets(projectId: Long, limit: Int = 20, offset: Int = 0): Single<Datasets> =
            fetchUser().flatMap { amigoApi.getDatasets(it.id, projectId, limit, offset) }

    fun fetchProject(project_id: Long): Single<ProjectModel> =
            fetchUser().flatMap { amigoApi.getProject(it.id, project_id) }

    fun fetchDataset(project_id: Long, dataset_id: Long): Single<DatasetModel> =
            fetchUser().flatMap { amigoApi.getDataset(it.id, project_id, dataset_id) }

    fun fetchForms(project_id: Long, dataset_id: Long): Single<FormModel> =
            fetchUser().flatMap { amigoApi.getForms(it.id, project_id, dataset_id) }

    fun fetchRelatedTables(project_id: Long, dataset_id: Long): Single<RelatedTables> =
            fetchUser().flatMap { amigoApi.getRelatedTables(it.id, project_id, dataset_id) }

    fun fetchSchema(project_id: Long, dataset_id: Long): Single<SchemaModel> =
            fetchUser().flatMap { amigoApi.getSchema(it.id, project_id, dataset_id) }

    fun downloadFile(url: String) = amigoApi.downloadFile(url)

    fun fetchSupportFiles(project: ProjectModel): Single<SupportFilesModel> =
            fetchUser()
                    .flatMap {amigoApi.getSupportFiles(it.id, project.id)}

    fun submitChangeset(url: String, body: RequestBody): Single<Any> =
            amigoApi.submitChangeset(url, body)

    fun chunkedUpload(url: String,
                      body: RequestBody,
                      headers: Map<String, String>): Single<ChunkedUploadResponse> =
            amigoApi.chunkedUpload(url, body, headers)

    fun chunkedUploadComplete(url: String,
                              upload_id: String,
                              md5: String,
                              source_amigo_id: String,
                              filename: String,
                              headers: Map<String, String>): Single<ChunkedUploadResponse> =
            amigoApi.chunkedUploadComplete(url, upload_id, md5, source_amigo_id, filename, headers)

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
        config.amigoTokenJson.value = json
        token = this
    }
}



