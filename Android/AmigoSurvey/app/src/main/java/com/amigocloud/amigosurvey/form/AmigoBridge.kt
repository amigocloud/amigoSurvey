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
    var geometrySet = false

    fun getLastLocationWKT() : String {
        return "SRID=4326;POINT(${lastLocation.longitude} ${lastLocation.latitude})"
    }

    fun setGeometryPosition() {
        refreshGeometry(getLastLocationWKT())
        geometrySet = true
    }

    @JavascriptInterface
    fun getClientType(): String {
        return "mobile"
    }

    @JavascriptInterface
    fun ready() {
        runJS("javascript:Amigo.loadBlock(" +
                        "AmigoPlatform.getBlockHTML('" + formType + "'), '" + formType + "', " +
                        "'" + formViewState.dataset?.id + "', AmigoPlatform.getData());")
    }

    fun runJS(url: String) {
        formActivity.runOnUiThread(Runnable {
            if (formActivity.isReady())
                formActivity.getWebView().loadUrl(url)
        })
    }

    fun back()
    {
        runJS("javascript:Amigo.historyBack();")
    }

    fun refreshGeometry(wkt: String) {
        runJS("javascript:Amigo.refreshGeometry('$wkt');")
    }

    fun submit() {
        if (!geometrySet) {
            setGeometryPosition()
        }
        runJS("javascript:Amigo.nativeSaveButton();")
    }

    @JavascriptInterface
    fun getData(): String {
        return formViewState.recordJson
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
    }

    @JavascriptInterface
    fun getFormDescription(datasetId: String, formType: String): String {
        if (formType == "create") {
            val json = formViewState.formDescription
            return json
        }
        return "{}"
    }

    @JavascriptInterface
    fun getBlockHTML(formType: String): String {
        var data = ""
        formViewState.form?.let {
            if (formType == "create_block") {
                data = it.create_block_form
            }
        }
        return data
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
        return formViewState.schemaJson
    }

    @JavascriptInterface
    fun getSchema(): String
    {
        return formViewState.schemaJson
    }

    @JavascriptInterface
    fun getUser(): String
    {
        return formViewState.userJson
    }

    @JavascriptInterface
    fun getProject(): String
    {
        return formViewState.projectJson
    }

    @JavascriptInterface
    fun getRelatedTables(datasetId: String): String {
        return formViewState.relatedTablesJson
    }

    @JavascriptInterface
    fun getGPSinfo(): String {
        return formActivity.getViewModel().getGPSInfoJSON(lastLocation)
    }

    @JavascriptInterface
    fun getPermissionLevel(): String {
        var perm = "READER"
        formViewState.project?.let {
            perm = it.permission_level
        }
        return perm
    }

    @JavascriptInterface
    fun editRowGeometry(json: String) {
        setGeometryPosition()
    }

    @JavascriptInterface
    fun updateRow(originalJson: String, updateJson: String) {
        formActivity.addNewRecord(updateJson)
    }

    @JavascriptInterface
    fun saveRow(json: String) {
        updateRow(formViewState.recordJson, json)
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
        print("$relatedTableId")
        formActivity.takePhoto(relatedTableId, amigoId)
    }

    @JavascriptInterface
    fun scanBarcode(amigoId: String) {
        formActivity.scanBarcode(amigoId)
    }

    fun setCustomFieldValue(fieldName: String, fieldValue: String) {
        runJS("javascript:Amigo.setCustomFieldValue(" +
                formViewState.dataset?.id.toString() + ",'" + fieldName + "','" + fieldValue + "')")
    }

    @JavascriptInterface
    fun viewPhotos(relatedTableId: String, amigoId: String) {
        print("$relatedTableId")
    }

    @JavascriptInterface
    fun dataHasChanged(hasChanged: Boolean) {
        back()
    }

    @JavascriptInterface
    fun writeRfidInfo(lat: String?, lon: String?, owner: String?, asset_name: String?) {
    }

    @JavascriptInterface
    fun getGeometryInfo(wkb: String): String {
        var centroid = "{\"centroid_latitude\":${lastLocation.latitude},"
        centroid += "\"centroid_longitude\":${lastLocation.longitude}}"
        return centroid
    }

    fun mediaAdded() {
        runJS("javascript:Amigo.mediaAdded()")
    }
}