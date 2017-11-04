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
import android.location.LocationManager
import com.amigocloud.amigosurvey.models.*
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.repository.SurveyConfig
import com.amigocloud.amigosurvey.util.unzipFile
import com.amigocloud.amigosurvey.util.writeToDisk
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.processors.BehaviorProcessor
import ru.solodovnikov.rx2locationmanager.LocationTime
import ru.solodovnikov.rx2locationmanager.RxLocationManager
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class FormViewState(val userJson: String = "",
                         val recordJson: String = "",
                         val project: ProjectModel? = null,
                         val dataset: DatasetModel? = null,
                         val form: FormModel? = null,
                         val webFormDir: String? = null,
                         var schema: List<Any>? = null,
                         var related_tables: List<RelatedTableModel>? = null,
                         val error: Throwable? = null)

data class EmptyRecordData(var amigo_id: String)

data class NewRecord(
        var count: Int,
        var columns: List<Any>,
        var data: List<EmptyRecordData>,
        var is_new: Boolean)

class FormViewModel(private val rest: AmigoRest,
                    private val config: SurveyConfig,
                    private val locationManager: RxLocationManager) : ViewModel() {
    private val TAG = "FormViewModel"
    private val supportFname = "support_files.zip"

    private val processor = BehaviorProcessor.create<Pair<Long, Long>>()

    val user = ObservableField<UserModel>()
    val dataset = ObservableField<DatasetModel>()
    val project = ObservableField<ProjectModel>()
    val related_tables = ObservableField<List<RelatedTableModel>>()
    val schema = ObservableField<List<Any>>()

    val events: LiveData<FormViewState> = LiveDataReactiveStreams.fromPublisher(processor
            .flatMapSingle { (pId, dId) ->
                loadProjectAndDataset(pId, dId)
                        .doOnSuccess { (p, d) ->
                            project.set(p)
                            dataset.set(d)
                        }
                        .flatMap { fetchUser() }
                        .flatMap { fetchSupportFiles() }
                        .flatMap { fetchRelatedTables()
                                .doOnSuccess { rt ->
                                    related_tables.set(rt)
                                }}
                        .flatMap { fetchSchema()
                                .doOnSuccess { sc ->
                                    schema.set(sc)
                                }}
                        .flatMap { fetchForm() }
                        .map {
                            FormViewState(
                                    userJson = getUserJSON(user.get()),
                                    project = project.get(),
                                    dataset = dataset.get(),
                                    form = it,
                                    webFormDir = config.webFormDir,
                                    schema = schema.get(),
                                    recordJson = getNewRecordJSON()
                            )
                        }
                        .onErrorReturn { FormViewState(error = it) }
            })

    val location: LiveData<Location> = LiveDataReactiveStreams.fromPublisher(
            Flowable.timer(10, TimeUnit.SECONDS)
                    .flatMapSingle {
                        locationManager.requestLocation(LocationManager.GPS_PROVIDER,
                                LocationTime(10, TimeUnit.SECONDS))
                                .onErrorReturn { Location("") }
                    })

    fun onFetchForm(projectId: Long, datasetId: Long) {
        processor.onNext(projectId.to(datasetId))
    }

    fun getUserJSON(user: UserModel): String = rest.getUserJSON(user)

    fun getNewRecordJSON(): String {
        var emptyRecords : MutableList<EmptyRecordData> = mutableListOf()
        val amigoId = UUID.randomUUID().toString().replace("-", "")
        emptyRecords.add(EmptyRecordData(amigoId))
        var rec = NewRecord(count = 1, columns = schema.get(), data = emptyRecords, is_new = true)
        return  rest.getJSON(rec)
    }

    private fun fetchUser(): Single<UserModel> = rest.fetchUser().doOnSuccess {this.user.set(it) }

    private fun fetchForm(): Single<FormModel> = rest.fetchForms(project.get().id, dataset.get().id)

    private fun fetchSupportFiles(): Single<File> =
            rest.fetchSupportFiles(project.get().id)
                    .flatMap { downloadSupportFiles(it.zip) }
                    .flatMap { unzipSupportFiles() }

    private fun fetchRelatedTables(): Single<List<RelatedTableModel>> =
            rest.fetchRelatedTables(project.get().id, dataset.get().id)
                    .flatMapObservable { Observable.fromIterable(it.results) }
                    .toList()

    private fun fetchSchema(): Single<List<Any>> =
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


    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val rest: AmigoRest,
                                      private val config: SurveyConfig,
                                      private val locationManager: RxLocationManager) : ViewModelFactory<FormViewModel>() {

        override val modelClass = FormViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(this.modelClass)) {
                return FormViewModel(rest, config, locationManager) as T
            }
            throw IllegalArgumentException(INFLATION_EXCEPTION)
        }
    }

}