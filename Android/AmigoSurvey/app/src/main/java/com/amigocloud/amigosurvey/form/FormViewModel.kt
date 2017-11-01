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

import android.arch.lifecycle.ViewModel
import com.amigocloud.amigosurvey.models.FormModel
import com.amigocloud.amigosurvey.models.RelatedTableModel
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.repository.SurveyConfig
import com.amigocloud.amigosurvey.util.unzipFile
import com.amigocloud.amigosurvey.util.writeToDisk
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File
import javax.inject.Inject


class FormViewModel(private val rest: AmigoRest, private val config: SurveyConfig) : ViewModel() {
    private val TAG = "FormViewModel"
    private val supportFname = "support_files.zip"

    fun fetchForm(user_id: Long, project_id: Long, dataset_id: Long): Single<FormModel> =
            rest.fetchForms(user_id, project_id, dataset_id)

    fun fetchSupportFiles(user_id: Long, project_id: Long): Single<File> =
            rest.fetchSupportFiles(user_id, project_id)
                    .flatMap { downloadSupportFiles(it.zip) }
                    .flatMap { unzipSupportFiles() }

    fun fetchRelatedTables(user_id: Long, project_id: Long, dataset_id: Long): Single<List<RelatedTableModel>> =
            rest.fetchRelatedTables(user_id, project_id, dataset_id)
                    .flatMapObservable { Observable.fromIterable(it.results) }
                    .toList()

    private fun unzipSupportFiles() =
            unzipFile(config.webFormDir + supportFname, config.webFormDir)

    private fun downloadSupportFiles(url: String) = rest.downloadFile(url)
            .flatMap { it.writeToDisk(config.webFormDir + supportFname) }
            .flatMap { unzipFile(config.webFormDir + supportFname, config.webFormDir) }


    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val rest: AmigoRest,
                                      private val config: SurveyConfig) : ViewModelFactory<FormViewModel>() {

        override val modelClass = FormViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(this.modelClass)) {
                return FormViewModel(rest, config) as T
            }
            throw IllegalArgumentException(INFLATION_EXCEPTION)
        }
    }

}