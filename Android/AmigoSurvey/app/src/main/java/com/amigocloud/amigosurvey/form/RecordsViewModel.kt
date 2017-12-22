package com.amigocloud.amigosurvey.form

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.ViewModel
import com.amigocloud.amigosurvey.models.DatasetModel
import com.amigocloud.amigosurvey.models.ProjectModel
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.repository.Repository
import com.amigocloud.amigosurvey.repository.SurveyConfig
import com.amigocloud.amigosurvey.util.escapeJson
import com.amigocloud.amigosurvey.util.getChangesetJson
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import com.squareup.moshi.Moshi
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.processors.BehaviorProcessor
import okhttp3.MediaType
import org.json.JSONObject
import javax.inject.Inject
import okhttp3.RequestBody

data class RecordsUploadProgress (
        var recordIndex: Int = 0,
        var recordsTotal: Int = 0,
        var record: FormRecord
)

data class RecordsUploadRequest(
       val records: List<FormRecord>,
       val project: ProjectModel,
       val dataset: DatasetModel
)

class RecordsViewModel(private val rest: AmigoRest,
                       private val repository: Repository) : ViewModel() {

    private val processor = BehaviorProcessor.create<RecordsUploadRequest>()

    val events: LiveData<RecordsUploadProgress> = LiveDataReactiveStreams.fromPublisher(processor
            .flatMap { request -> submitRecords(request).toFlowable(BackpressureStrategy.LATEST) })


    fun submitAllRecords(project: ProjectModel, dataset: DatasetModel) {
        val records = repository.formRecordDao().all
        val request = RecordsUploadRequest(records, project, dataset)
        processor.onNext(request)
    }

    private fun submitRecords(request: RecordsUploadRequest): Observable<RecordsUploadProgress> {
        return Observable.fromIterable(request.records)
                .zipWith(Observable.range(0, Int.MAX_VALUE), BiFunction { record:FormRecord, index:Int -> record.to(index)})
                .flatMapSingle { recWithIndex ->
                    val json = recWithIndex.first.json.getChangesetJson(request.project, request.dataset)
                    val body = RequestBody.create(MediaType.parse("application/json"),
                            "{\"changeset\":\"[${json}]\"}")
                    rest.submitChangeset(request.project.submit_changeset, body).map { recWithIndex }
                }
                .map { (rec, index) ->
                    RecordsUploadProgress(index, request.records.size, rec)
                }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val rest: AmigoRest,
                                      private val repository: Repository) : ViewModelFactory<RecordsViewModel>() {

        override val modelClass = RecordsViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(this.modelClass)) {
                return RecordsViewModel(rest, repository) as T
            }
            throw IllegalArgumentException(INFLATION_EXCEPTION)
        }
    }
}
