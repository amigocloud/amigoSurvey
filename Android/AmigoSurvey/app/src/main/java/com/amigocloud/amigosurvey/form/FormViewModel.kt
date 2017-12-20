/*
 *
 *  AmigoMobile
 *
 *  Copyright (c) 2011-2015 AmigoCloud Inc., All rights reserved.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation; either
 *  version 3.0 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library.
 *
 */

package com.amigocloud.amigosurvey.form

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import android.location.Location
import com.amigocloud.amigosurvey.models.*
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.repository.SurveyConfig
import com.amigocloud.amigosurvey.util.unzipFile
import com.amigocloud.amigosurvey.util.writeToDisk
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.internal.operators.single.SingleFromCallable
import io.reactivex.processors.BehaviorProcessor
import ru.solodovnikov.rx2locationmanager.RxLocationManager
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class FormViewState(val userJson: String = "{}",
                         val recordJson: String = "",
                         val project: ProjectModel? = null,
                         val dataset: DatasetModel? = null,
                         val form: FormModel? = null,
                         val webFormDir: String? = null,
                         var schemaJson: String = "[]",
                         var relatedTablesJson: String = "[]",
                         var projectJson: String = "[]",
                         var formDescription: String = "{}",
                         var gpsInfo: String = "{}",
                         val error: Throwable? = null)

data class EmptyRecordData(var amigo_id: String)

data class NewRecord(
        var count: Int,
        var columns: List<Any>,
        var data: List<EmptyRecordData>,
        var is_new: Boolean)

data class RelatedTableItem(
        val id : String? = null,
        val name: String? = null)

data class GPSInfoJson(
        val gpsActive: String = "0",
        val longitude: String = "0",
        val latitude: String = "0",
        val altitude: String = "0",
        val horizontalAccuracy: String = "0",
        val bearing: String = "0",
        val speed: String = "0")

@Singleton
class FormViewModel @Inject constructor(private val rest: AmigoRest,
                                        private val config: SurveyConfig,
                                        private val locationManager: RxLocationManager,
                                        private val fileUploader: FileUploader,
                                        val locationViewModel: LocationViewModel) : ViewModel() {

    private val supportFname = "support_files.zip"

    private val processor = BehaviorProcessor.create<Pair<Long, Long>>()

    val user = ObservableField<UserModel>()
    val dataset = ObservableField<DatasetModel>()
    val history_dataset = ObservableField<DatasetModel>()
    val project = ObservableField<ProjectModel>()
    val related_tables = ObservableField<List<RelatedTableModel>>()
    val schema = ObservableField<List<Any>>()
    var recordJSON: String? = null


//    var lastLocationSingle: Single<Location> = Single.just(Location(LocationManager.PASSIVE_PROVIDER))

    val events: LiveData<FormViewState> = LiveDataReactiveStreams.fromPublisher(processor
            .filter { (pId, dId) ->
                // Prevent from creating the same state multiple times
                !((project.get() != null && project.get().id == pId) ||
                  (dataset.get() != null && dataset.get().id == dId))
            }
            .flatMapSingle { (pId, dId) ->
                loadProjectAndDataset(pId, dId)
                        .doOnSuccess { (p, d) ->
                            project.set(p)
                            dataset.set(d)
                        }
                        .flatMap { fetchUser() }
                        .flatMap {
                            if (isSupportFilesHashChanged(project.get())) {
                                fetchSupportFiles()
                            } else {
                                SingleFromCallable.just(File(config.webFormDir + supportFname))
                            }
                        }
                        .flatMap { fetchRelatedTables()
                                .doOnSuccess { rt ->
                                    related_tables.set(rt)
                                }}
                        .flatMap { fetchSchema().doOnSuccess { sc -> schema.set(sc) }}
                        .flatMap { fetchHistoryDataset()
                                .doOnSuccess {
                                    if (it.isNotEmpty()) {
                                        history_dataset.set(it[0])
                                        project.get().history_dataset_id = history_dataset.get().id.toString()
                                    }
                                }
                        }
                        .flatMap { fetchForm() }
                        .map {
                            var create_block_json: Any = "{}"
                            it.create_block_json?.let{ create_block_json = it }
                            if (recordJSON == null) {
                                recordJSON = getNewRecordJSON()
                            }
                            FormViewState(
                                    userJson = getUserJSON(user.get()),
                                    project = project.get(),
                                    dataset = dataset.get(),
                                    form = it,
                                    webFormDir = config.webFormDir,
                                    schemaJson = rest.getListJSON(schema.get()),
                                    recordJson = recordJSON!!,
                                    relatedTablesJson = getRelatedTablesJSON(),
                                    projectJson = rest.getProjectJSON(project.get()),
                                    formDescription = rest.getJSON(create_block_json),
                                    gpsInfo = getGPSInfoJSON(locationViewModel.lastLocationSingle.blockingGet())
                            )
                        }
                        .onErrorReturn { FormViewState(error = it) }
            })

    fun onFetchForm(projectId: Long, datasetId: Long) {
        processor.onNext(projectId.to(datasetId))
    }

    fun getUserJSON(user: UserModel): String = rest.getUserJSON(user)

    fun getNewRecordJSON(): String {
        val emptyRecords : MutableList<EmptyRecordData> = mutableListOf()
        val amigoId = UUID.randomUUID().toString().replace("-", "")
        emptyRecords.add(EmptyRecordData(amigoId))
        val rec = NewRecord(count = 1, columns = schema.get(), data = emptyRecords, is_new = true)
        return  rest.getJSON(rec)
    }

    fun getRelatedTablesJSON(): String {
        val rtlist: MutableList<RelatedTableItem> = mutableListOf()
        related_tables.get().forEach { it->
            rtlist.add(RelatedTableItem(
                    id = it.id.toString(),
                    name = it.name))
        }
        return rest.getListJSON(rtlist)
    }

    fun getGPSInfoJSON(location: Location?): String {
        var json = rest.getJSON(GPSInfoJson())
        location?.let { l ->
            val gps = GPSInfoJson(
                    gpsActive = "1",
                    longitude = l.longitude.toString(),
                    latitude = l.latitude.toString(),
                    altitude = l.altitude.toString(),
                    horizontalAccuracy = l.accuracy.toString(),
                    bearing = l.bearing.toString(),
                    speed = l.speed.toString())
            json = rest.getJSON(gps)
        }
        return json
    }

    private fun fetchUser(): Single<UserModel> = rest.fetchUser().doOnSuccess {this.user.set(it) }

    private fun fetchForm(): Single<FormModel> = rest.fetchForms(project.get().id, dataset.get().id)

    private fun fetchHistoryDataset(): Single<List<DatasetModel>> =
            rest.fetchDatasets(project.get().id)
                    .flatMapObservable { Observable.fromIterable(it.results) }
                    .filter { it.type == "r_history" && it.name == "record_history" }
                    .toList()

    fun isSupportFilesHashChanged(project: ProjectModel): Boolean =
            (project.support_files_hash != config.supportFilesHash.value)

    private fun fetchSupportFiles(): Single<File> =
            rest.fetchSupportFiles(project.get())
                    .flatMap { downloadSupportFiles(it.zip) }
                    .flatMap { unzipSupportFiles() }
                    .doOnSuccess {
                        config.supportFilesHash.value = project.get().support_files_hash
                    }

    private fun fetchRelatedTables(): Single<List<RelatedTableModel>> =
            rest.fetchRelatedTables(project.get().id, dataset.get().id)
                    .flatMapObservable { Observable.fromIterable(it.results) }
                    .toList()

    private fun fetchSchema(): Single<List<SchemaItem>> =
            rest.fetchSchema(project.get().id, dataset.get().id)
                    .flatMapObservable {
                        Observable.fromIterable(it.schema)
                    }
                    .toList()

    private fun loadProjectAndDataset(projectId: Long, datasetId: Long): Single<Pair<ProjectModel, DatasetModel>> =
            rest.fetchProject(projectId).zipWith(rest.fetchDataset(projectId, datasetId), BiFunction { project, dataset ->
                project.to(dataset)
            })

    private fun unzipSupportFiles() =
            unzipFile(config.webFormDir + supportFname, config.webFormDir)


    private fun downloadSupportFiles(url: String) = rest.downloadFile(url)
            .flatMap { it.writeToDisk(config.webFormDir + supportFname) }

    fun getCustomFieldName(custom_type: String): Observable<String> {
        return rest.fetchSchema(project.get().id, dataset.get().id)
                .flatMapObservable { Observable.fromIterable(it.schema) }
                .filter{ (it.custom_type == custom_type) }
                .flatMap { Observable.just(it.name) }
    }

    fun uploadPhotos(projectId: Long, datasetId: Long): Observable<FileProgressEvent>
            = fileUploader.uploadAllPhotos(projectId, datasetId)

    fun deletePhotoRecord(record: RelatedRecord)
            = fileUploader.deletePhoto(record)

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val rest: AmigoRest,
                                      private val config: SurveyConfig,
                                      private val locationManager: RxLocationManager,
                                      private val fileUploader: FileUploader,
                                      private val locationViewModel: LocationViewModel) : ViewModelFactory<FormViewModel>() {

        override val modelClass = FormViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(this.modelClass)) {
                return FormViewModel(rest, config, locationManager, fileUploader, locationViewModel) as T
            }
            throw IllegalArgumentException(INFLATION_EXCEPTION)
        }
    }

}