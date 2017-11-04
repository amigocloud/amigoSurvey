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

import android.webkit.JavascriptInterface
import android.content.Intent
import android.location.Location
import android.net.Uri



class AmigoBridge(private val formActivity: FormActivity) {

    var formType: String = ""

    var formViewState: FormViewState = FormViewState()

    var lastLocation: Location = Location("")


    @JavascriptInterface
    fun getClientType(): String {
        return "mobile"
    }

    @JavascriptInterface
    fun ready() {
        formActivity.runOnUiThread(Runnable {
            if (formActivity.isReady())
                formActivity.getWebView().loadUrl("javascript:Amigo.loadBlock(" +
                        "AmigoPlatform.getBlockHTML('" + formType + "'), '" + formType + "', " +
                        "'" + formViewState.dataset?.id + "', AmigoPlatform.getData());")
        })
    }

    @JavascriptInterface
    fun getData(): String {
        return formViewState.recordJson //formData
    }

    @JavascriptInterface
    fun getPageSize(): Int {
        return 20
    }


    @JavascriptInterface
    fun onException(msg: String) {
        // Do nothing, will be deprecated
        print("onException() -----: " + msg)
    }

    @JavascriptInterface
    fun storeException(url: String, data: String) {
//        AmigoCloudAPI.storeException(url, data)
        print("onException() -----: " + data)
    }

    @JavascriptInterface
    fun openUrl(url: String) {
        formActivity.runOnUiThread(Runnable {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            formActivity.startActivity(browserIntent)
        })
    }

    @JavascriptInterface
    fun setState(json: String) {
        print("setState() -----: " + json)
//        AmigoCloudAPI.clearEditorContext()
//        extractAmigoId(json)
//        try {
//            val jObject = JSONObject(json)
//            formType = jObject.getString("formType")
//            formData = jObject.getString("data")
//            datasetId = java.lang.Long.parseLong(jObject.getString("currentDatasetId"))
//            val saveButton = jObject.getBoolean("saveBtn")
//            val deleteButton = jObject.getBoolean("deleteBtn")
//            val checkButton = jObject.getBoolean("checkBtn")
//            deleteRelationship = jObject.getBoolean("relationshipDeleteBtn")
//            activity.runOnUiThread(Runnable {
//                if (webFragment is BaseFormFragment) {
//                    val formFragment = webFragment as BaseFormFragment
//                    formFragment.setSaveButtonEnabled(saveButton)
//                    formFragment.setDeleteButtonEnabled(deleteButton)
//                    formFragment.setCheckButtonEnabled(checkButton)
//                    formFragment.setHeaderTitle(AmigoCloudAPI.getDatasetName(datasetId))
//                }
//            })
//        } catch (e: NumberFormatException) {
//            Crashlytics.logException(e)
//            e.printStackTrace()
//        } catch (e: JSONException) {
//            Crashlytics.logException(e)
//            e.printStackTrace()
//        }
//
    }

    @JavascriptInterface
    fun getFormDescription(datasetId: String, formType: String): String {
        val type = formType + "_description"
        return "" //AmigoCloudAPI.getDatasetForm(java.lang.Long.valueOf(datasetId), type)
    }

    @JavascriptInterface
    fun getBlockHTML(formType: String): String {
        formViewState.form?.let {
            if (formType == "create_block") {
                return it.create_block_form
            }
        }
        return ""
    }

    @JavascriptInterface
    fun getBlockHTMLWithId(formType: String, datasetId: String): String {
        try {
            return "" //AmigoCloudAPI.getDatasetForm(java.lang.Long.parseLong(datasetId), formType)
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getDatasetInfo(datasetId: String): String {
        return "" //AmigoCloudAPI.getDatasetInfo(java.lang.Long.parseLong(datasetId))
    }

    @JavascriptInterface
    fun getDatasetRecords(datasetId: String, offset: String): String {
        return getDatasetRecordsWithFilter(datasetId, offset, "")
    }

    @JavascriptInterface
    fun getDatasetRecordsWithFilter(datasetId: String, offset: String, filter: String): String {
        try {
            return "" //AmigoCloudAPI.getRecordsJSON(java.lang.Long.parseLong(datasetId), Integer.parseInt(offset), filter, "")
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getDatasetRecordsDistinctColumnsWithFilter(datasetId: String, columns: String, filter: String): String {
        var dsid: Long = 0
        try {
            dsid = java.lang.Long.parseLong(datasetId)
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            return "{}"
        }

        return "" //AmigoCloudAPI.getDistinctRecordsJSON(dsid, 0, filter, "", columns)
    }

    @JavascriptInterface
    fun getDatasetRecordsWithFilterOrderBy(datasetId: String, offset: String, filter: String, order_by: String): String {
        try {
            return "" //AmigoCloudAPI.getRecordsJSON(java.lang.Long.parseLong(datasetId), Integer.parseInt(offset), filter, order_by)
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getDatasetRecordsCountWithId(datasetId: String): String {
        return getDatasetRecordsCountWithIdAndFilter(datasetId, "")
    }

    @JavascriptInterface
    fun getDatasetRecordsCountWithIdAndFilter(datasetId: String, filter: String): String {
        try {
            return "" //AmigoCloudAPI.getRecordsCountJSON(java.lang.Long.parseLong(datasetId), filter)
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getRelatedRecordsCount(originalDatasetId: String, relatedDatasetId: String, sourceAmigoId: String): String {
        try {
            return "" //AmigoCloudAPI.getRelatedRecordsCountJSON(java.lang.Long.parseLong(originalDatasetId), java.lang.Long.parseLong(relatedDatasetId), sourceAmigoId)
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getNewRowWithSourceId(datasetId: String, relatedColumn: String, relatedId: String): String {
        try {
            return "" //AmigoCloudAPI.getNewRecordJSONWithRelationship(java.lang.Long.parseLong(datasetId), relatedColumn, relatedId)
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getNewRow(datasetId: String): String {
        try {
            return "" //AmigoCloudAPI.getNewRecordJSONForDataset(java.lang.Long.parseLong(datasetId))
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getSchemaWithId(datasetId: String): String {
        try {
            return "" //AmigoCloudAPI.getSchemaJSONForDataset(java.lang.Long.parseLong(datasetId))
        } catch (e: NumberFormatException) {
            return ""
        }
    }

    @JavascriptInterface
    fun getSchema(): String
    {
        return ""
    }

    @JavascriptInterface
    fun getUser(): String
    {
        return formViewState.userJson
    }

    @JavascriptInterface
    fun getProject(): String
    {
        return ""
    }

    @JavascriptInterface
    fun getRelatedTables(datasetId: String): String {
        try {
            return "" //AmigoCloudAPI.getRelatedTablesJSON(java.lang.Long.parseLong(datasetId))
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getGPSinfo(): String {
        return "" //Globe.getGPSInfoJSON()
    }

    @JavascriptInterface
    fun getPermissionLevel(): String {
        return "EDITOR"
    }

    @JavascriptInterface
    fun editRowGeometry(json: String) {
    }

    @JavascriptInterface
    fun updateRow(originalJson: String, updateJson: String) {
    }

    @JavascriptInterface
    fun saveRow(json: String) {
//        updateRow(formData, json)
    }

    @JavascriptInterface
    fun close() {
    }

    @JavascriptInterface
    fun deleteRows(json: String) {}

    @JavascriptInterface
    fun newRecord() {
        print("newRecord")
//        val newRecordData = AmigoCloudAPI.getNewRecordJSONForDataset(datasetId)
//        if (activity is GlobeActivity) {
//            GlobeActivityFragmentExtensionsKt.showEditForm(activity as GlobeActivity, datasetId, newRecordData, true)
//        }
    }

    @JavascriptInterface
    fun zoomToRows(json: String) {}

    @JavascriptInterface
    fun takePhoto(relatedTableId: String , amigoId: String )
    {
    }

    @JavascriptInterface
    fun scanBarcode(amigoId: String) {
    }

    @JavascriptInterface
    fun viewPhotos(relatedTableId: String, amigoId: String) {
    }

    @JavascriptInterface
    fun dataHasChanged(hasChanged: Boolean) {
    }

    @JavascriptInterface
    fun writeRfidInfo(lat: String?, lon: String?, owner: String?, asset_name: String?) {
    }

    @JavascriptInterface
    fun getGeometryInfo(wkb: String): String {
        return "" //AmigoCloudAPI.getGeometryInfo(wkb)
    }


}