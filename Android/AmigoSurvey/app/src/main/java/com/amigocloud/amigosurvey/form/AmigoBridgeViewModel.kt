package com.amigocloud.amigosurvey.form

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import com.amigocloud.amigosurvey.models.DatasetModel
import com.amigocloud.amigosurvey.models.ProjectModel
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.repository.SurveyConfig
import com.squareup.moshi.Json

class ProjectData(val id: Long,
                  @Json(name = "history_dataset_id") val historyId: Long,
                  name: String?)

class AmigoBridgeViewModel (private val rest: AmigoRest, private val config: SurveyConfig) : ViewModel() {

    val dataset = ObservableField<DatasetModel?>()
    val project = ObservableField<ProjectModel?>()

    fun getLastLocationWKT(): String {
        val lat = 37.0
        val lng = -121.0
        return "SRID=4326;POINT($lng $lat)"
    }

//    val schema = dataset.get()?.schema ?: ""
//
//    fun getHTML(name: String): String {
//        return "" // self.formModel?.create_block_form
//    }
//
//    fun getNewRecordJSON(datasetId: Long, amigo_id: String) : String {
//        return "{}"
//    }
//
//    fun getUserInfoJSON(): String {
//        return "{}"
//    }
//
//    fun getProjectJSON(project_id: Long, history_dataset_id: Long): String {
//        var json = ""
//        json = "{"
//        json += "\"id\":" + project_id.toString() + ","
//        json += "\"name\":\"" + project.get()?.name + "\","
//        json += "\"description\":\"" + project.get()?.description + "\","
//        json += "\"organization\":\"" + project.get()?.organization + "\","
//        json += "\"history_dataset_id\":" + history_dataset_id.toString()
//        json += "}"
//        return json
//    }
//
//    fun getGPSInfoJSON(): String {
//        return "{}"
//    }
//
//    fun getPermissionLevel(project_id: Long): String {
//        return ""
//    }
}
