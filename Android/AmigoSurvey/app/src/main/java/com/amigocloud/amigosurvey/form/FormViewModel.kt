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
import com.amigocloud.amigosurvey.models.*
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.repository.SurveyConfig
import com.amigocloud.amigosurvey.util.unzipFile
import com.amigocloud.amigosurvey.util.writeResponseBodyToDisk
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import io.reactivex.Single
import java.io.IOException
import javax.inject.Inject


class FormViewModel(private val rest: AmigoRest, private val config: SurveyConfig) : ViewModel() {
    private val TAG = "FormViewModel"
    private val supportFname = "support_files.zip"

    fun fetchForm(user_id: Long, project_id: Long, dataset_id: Long): Single<FormModel> {
        return rest.fetchForms(user_id, project_id, dataset_id)
    }

    fun fetchSupportFiles(user_id: Long, project_id: Long): Single<Boolean> {
        return rest.fetchSupportFiles(user_id, project_id )
                .flatMap { supportFiles ->
                    downloadSupportFiles(supportFiles.zip)
                }
                .flatMap { flag ->
                    val path = config.webFormDir + supportFname
                    val ok = unzipFile(path, config.webFormDir)
                    Single.just(ok)
                }
    }

    fun fetchRelatedTables(user_id: Long, project_id: Long, dataset_id: Long): Single<RelatedTables> {
        return rest.fetchRelatedTables(user_id, project_id, dataset_id)
    }

    fun downloadSupportFiles(url: String): Single<Boolean> {
        val call = rest.downloadFileWithUrlSync(url)

        val response = call.execute()
        if(response.isSuccessful) {
            val fullPath = config.webFormDir + supportFname
            try {
                if (writeResponseBodyToDisk(response.body(), fullPath)) {
                    unzipFile(fullPath, config.webFormDir)
                    return Single.just(true)
                }
            } catch(e: IOException) {
                return Single.just(false)
            }
        }
        return Single.just(false)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val rest: AmigoRest, private val config: SurveyConfig) : ViewModelFactory<FormViewModel>() {

        override val modelClass = FormViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(this.modelClass)) {
                return FormViewModel(rest, config) as T
            }
            throw IllegalArgumentException(INFLATION_EXCEPTION)
        }
    }

}