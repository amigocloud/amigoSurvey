package com.amigocloud.amigosurvey.form

import android.arch.lifecycle.ViewModel
import com.amigocloud.amigosurvey.models.DatasetModel
import com.amigocloud.amigosurvey.models.ProjectModel
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.repository.SurveyConfig
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import com.squareup.moshi.Moshi
import io.reactivex.Single
import io.reactivex.internal.operators.single.SingleFromCallable
import okhttp3.MediaType
import org.json.JSONObject
import javax.inject.Inject
import okhttp3.RequestBody




class ChangesetViewModel(private val rest: AmigoRest,
                         private val moshi: Moshi,
                         private val config: SurveyConfig) : ViewModel() {

    fun addNewRecord(rec:String, project: ProjectModel, dataset: DatasetModel): Single<Any> {
            val json = escapeJSON(getChangesetJSON(rec, project, dataset))
            val body = RequestBody.create(MediaType.parse("application/json"),
                    "{\"changeset\":\"[${json}]\"}")
            return rest.submitChangeset(project.submit_changeset, body)
    }

    fun getChangesetJSON(record: String, project: ProjectModel, dataset: DatasetModel) : String {
        var json = "{\"type\": \"DML\","
        json += "\"entity\": \"dataset_${dataset.id}\","
        json += "\"action\": \"INSERT\","
        json += "\"parent\": \"${project.hash}\","
        json += "\"data\": [{\"new\":{"

        val jsonObj = JSONObject(record)
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
        return json
    }

    fun escapeJSON(json: String) : String {
        return json.replace("\"", "\\\"")
    }

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val rest: AmigoRest,
                                      private val moshi: Moshi,
                                      private val config: SurveyConfig) : ViewModelFactory<ChangesetViewModel>() {

        override val modelClass = ChangesetViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(this.modelClass)) {
                return ChangesetViewModel(rest, moshi, config) as T
            }
            throw IllegalArgumentException(INFLATION_EXCEPTION)
        }
    }
}
