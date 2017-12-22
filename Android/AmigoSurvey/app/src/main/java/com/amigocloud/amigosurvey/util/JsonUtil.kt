package com.amigocloud.amigosurvey.util

import android.location.Location
import com.amigocloud.amigosurvey.form.EmptyRecordData
import com.amigocloud.amigosurvey.form.GPSInfoJson
import com.amigocloud.amigosurvey.form.NewRecord
import com.amigocloud.amigosurvey.form.RelatedTableItem
import com.amigocloud.amigosurvey.models.DatasetModel
import com.amigocloud.amigosurvey.models.ProjectModel
import com.amigocloud.amigosurvey.models.RelatedTableModel
import com.amigocloud.amigosurvey.models.UserModel
import com.amigocloud.amigosurvey.repository.ProjectJSON
import com.amigocloud.amigosurvey.repository.UserJSON
import com.squareup.moshi.Moshi
import org.json.JSONObject
import java.util.*


fun UserModel.getJson(moshi: Moshi): String {
    val userObj = UserJSON(
            id = id.toString(),
            email = email,
            custom_id = custom_id,
            first_name = first_name,
            last_name = last_name,
            organization = organization,
            visible_projects = visible_projects,
            projects = projectsUrl)
    return moshi.adapter(UserJSON::class.java).toJson(userObj)
}

fun ProjectModel.getJson(moshi: Moshi): String {
    val obj = ProjectJSON(
            id = id.toString(),
            name = name,
            description = description,
            organization = organization,
            history_dataset_id = history_dataset_id.toString())
    return moshi.adapter(ProjectJSON::class.java).toJson(obj)
}

fun List<Any>.getJson(moshi: Moshi): String = moshi.adapter(Any::class.java).toJson(this)

fun Any.getJson(moshi: Moshi): String = moshi.adapter(Any::class.java).toJson(this)

fun List<Any>.getNewRecordJson(moshi: Moshi): String {
    val emptyRecords: MutableList<EmptyRecordData> = mutableListOf()
    val amigoId = UUID.randomUUID().toString().replace("-", "")
    emptyRecords.add(EmptyRecordData(amigoId))
    return NewRecord(count = 1, columns = this, data = emptyRecords, is_new = true).getJson(moshi)
}

fun List<RelatedTableModel>.getRelatedTableJson(moshi: Moshi): String =
        map {
            RelatedTableItem(
                    id = it.id.toString(),
                    name = it.name)
        }.getJson(moshi)

fun Location?.getGpsInfoJson(moshi: Moshi): String =
        (this?.let { GPSInfoJson(
                gpsActive = "1",
                longitude = it.longitude.toString(),
                latitude = it.latitude.toString(),
                altitude = it.altitude.toString(),
                horizontalAccuracy = it.accuracy.toString(),
                bearing = it.bearing.toString(),
                speed = it.speed.toString()) }  ?: GPSInfoJson()).getJson(moshi)

fun String.getChangesetJson(project: ProjectModel, dataset: DatasetModel) : String {
    var json = "{\"type\": \"DML\","
    json += "\"entity\": \"dataset_${dataset.id}\","
    json += "\"action\": \"INSERT\","
    json += "\"parent\": \"${project.hash}\","
    json += "\"data\": [{\"new\":{"

    val jsonObj = JSONObject(this)
    val data = jsonObj.getJSONArray("data")
    if (data.length() > 0) {
        val f = data[0] as JSONObject
        var count = 0
        var amigo_id = ""
        val keys = f.keys()
        while (keys.hasNext()) {
            val key = keys.next() as String
            val value = f.get(key)

            if( key == "amigo_id") {
                amigo_id = value as String
                continue
            }

            if (count > 0)
                json += ","

            if (value is String)
                json += "\"${key}\":\"${value}\""
            else
                json += "\"${key}\":${value}"

            count += 1
        }
        json += "},\"amigo_id\":\"${amigo_id}\"}"
    }
    json += "]}"
    return json.escapeJson()
}

fun String.escapeJson() = this.replace("\"", "\\\"")


