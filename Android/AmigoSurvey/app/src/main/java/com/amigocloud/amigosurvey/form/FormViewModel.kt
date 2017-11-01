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
import com.amigocloud.amigosurvey.models.DatasetModel
import com.amigocloud.amigosurvey.models.FormModel
import com.amigocloud.amigosurvey.models.ProjectModel
import com.amigocloud.amigosurvey.models.RelatedTableModel
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
import io.reactivex.processors.PublishProcessor
import ru.solodovnikov.rx2locationmanager.LocationTime
import ru.solodovnikov.rx2locationmanager.RxLocationManager
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class FormViewState(val project: ProjectModel? = null,
                         val dataset: DatasetModel? = null,
                         val form: FormModel? = null,
                         val error: Throwable? = null)

class FormViewModel(private val rest: AmigoRest,
                    private val config: SurveyConfig,
                    private val locationManager: RxLocationManager) : ViewModel() {
    private val TAG = "FormViewModel"
    private val supportFname = "support_files.zip"

    private val processor = PublishProcessor.create<Pair<Long, Long>>()

    val dataset = ObservableField<DatasetModel>()
    val project = ObservableField<ProjectModel>()

    val events: LiveData<FormViewState> = LiveDataReactiveStreams.fromPublisher(processor
            .flatMapSingle { (pId, dId) ->
                loadProjectAndDataset(pId, dId)
                        .doOnSuccess { (p, d) ->
                            project.set(p)
                            dataset.set(d)
                        }
                        .flatMap { fetchSupportFiles() }
                        .flatMap { fetchRelatedTables() }
                        .flatMap { fetchForm() }
                        .map {
                            FormViewState(
                                    project = project.get(),
                                    dataset = dataset.get(),
                                    form = it)
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

    private fun fetchForm(): Single<FormModel> = rest.fetchForms(project.get().id, dataset.get().id)

    private fun fetchSupportFiles(): Single<File> =
            rest.fetchSupportFiles(project.get().id)
                    .flatMap { downloadSupportFiles(it.zip) }
                    .flatMap { unzipSupportFiles() }

    private fun fetchRelatedTables(): Single<List<RelatedTableModel>> =
            rest.fetchRelatedTables(project.get().id, dataset.get().id)
                    .flatMapObservable { Observable.fromIterable(it.results) }
                    .toList()

    private fun loadProjectAndDataset(projectId: Long, datasetId: Long): Single<Pair<ProjectModel, DatasetModel>> =
            rest.fetchProject(projectId).zipWith(rest.fetchDataset(projectId, datasetId), BiFunction { project, dataset ->
                project.to(dataset)
            })

    private fun unzipSupportFiles() =
            unzipFile(config.webFormDir + supportFname, config.webFormDir)

    private fun downloadSupportFiles(url: String) = rest.downloadFile(url)
            .flatMap { it.writeToDisk(config.webFormDir + supportFname) }
            .flatMap { unzipFile(config.webFormDir + supportFname, config.webFormDir) }


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