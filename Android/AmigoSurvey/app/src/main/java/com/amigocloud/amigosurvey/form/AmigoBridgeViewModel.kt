package com.amigocloud.amigosurvey.form

import android.arch.lifecycle.ViewModel
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.repository.SurveyConfig

class AmigoBridgeViewModel (private val rest: AmigoRest, private val config: SurveyConfig) : ViewModel() {

    fun getLastLocationWKT(): String {
        val lat = 37.0
        val lng = -121.0
        return "SRID=4326;POINT($lng $lat)"
    }

    fun getDatasetSchema(datasetId: Long): String {
        return ""
    }

    fun getHTML(name: String): String {
        return "" // self.formModel?.create_block_form
    }

    fun getNewRecordJSON(datasetId: Long, amigo_id: String) : String {
        return "{}"
    }

    fun getUserInfoJSON(): String {
        return "{}"
    }

    fun getProjectJSON(project_id: Long, history_dataset_id: Long): String {
        var json: String = ""
        json = "{"
        json += "\"id\":" + project_id.toString() + ","
//        json += "\"name\":\"" + project.name + "\","
//        json += "\"description\":\"" + project.description + "\","
//        json += "\"organization\":\"" + project.organization + "\","
        json += "\"history_dataset_id\":" + history_dataset_id.toString()
        json += "}"
        return json
    }

    fun getGPSInfoJSON(): String {
        return "{}"
    }

    fun getPermissionLevel(project_id: Long): String {
        return ""
    }
}
