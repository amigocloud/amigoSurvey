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



package com.amigocloud.amigomobile.api.js

import android.app.Activity
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.webkit.JavascriptInterface
import com.amigocloud.amigosurvey.form.FormActivity
import com.amigocloud.amigosurvey.form.FormViewState
import org.json.JSONException
import org.json.JSONObject

class AmigoBridge(private val activity: Activity, private val formActivity: FormActivity, datasetId: Long, data: String) {
    var formType: String? = null
    @get:JavascriptInterface
    var data: String? = null
        private set
    var datasetId: Long = 0
        private set
    private var deleteRelationship: Boolean = false
    var amigoId: String? = null
        private set

    val clientType: String
        @JavascriptInterface
        get() = "mobile"

    val pageSize: Int
        @JavascriptInterface
        get() = 20

    /**
     * Old Form Methods
     */

    // TODO: AmigoCloudAPI.getSchemaJSONForDataset(datasetId);
    val schema: String
        @JavascriptInterface
        get() = ""

    // TODO: AmigoCloudAPI.getUserInfoJSON();
    val user: String
        @JavascriptInterface
        get() = ""

    // TODO: AmigoCloudAPI.getSelectedProjectInfoJSON();
    val project: String
        @JavascriptInterface
        get() = ""

    // TODO: Globe.getGPSInfoJSON();
    val gpSinfo: String
        @JavascriptInterface
        get() = ""

    // TODO: AmigoCloudAPI.getProjectPermissionLevel(AmigoCloudAPI.getSelectedProjectId());
    val permissionLevel: String
        @JavascriptInterface
        get() {
            val permission = "EDITOR"
            return if (permission == null || permission.isEmpty()) {
                "EDITOR"
            } else permission
        }

    init {
        this.data = data
        this.datasetId = datasetId
        if (datasetId < 0) {
            val e = IllegalStateException("Dataset ID cannot be < 0")
            //			Crashlytics.logException(e);
            throw e
        }
    }

    @JavascriptInterface
    fun getDatasetInfo(datasetId: String): String {
        return "" // TODO: AmigoCloudAPI.getDatasetInfo(Long.parseLong(datasetId));
    }

    fun setCustomFieldValue(fieldName: String, fieldValue: String) {
        activity.runOnUiThread {
            if (formActivity.isReady())
                formActivity.getWebView().loadUrl("javascript:Amigo.setCustomFieldValue(" +
                        datasetId.toString() + ",'" + fieldName + "','" + fieldValue + "')")
        }
    }

    @JavascriptInterface
    fun ready() {
        activity.runOnUiThread {
            if (formActivity.isReady())
                formActivity.getWebView().loadUrl("javascript:Amigo.loadBlock(" +
                        "AmigoPlatform.getBlockHTML('$formType'), '" + formType + "', " +
                        "'" + datasetId + "', AmigoPlatform.getData());")
        }
    }

    fun historyBack() {
        if (formActivity.isReady()) {
            formActivity.getWebView().loadUrl("javascript:Amigo.historyBack()")
        }
    }

    fun mediaAdded() {
        if (formActivity.isReady()) {
            formActivity.getWebView().loadUrl("javascript:Amigo.mediaAdded()")
        }
    }

    @JavascriptInterface
    fun onException(msg: String) {
        // Do nothing, will be deprecated
    }

    @JavascriptInterface
    fun storeException(url: String, data: String) {
        //   TODO:     AmigoCloudAPI.storeException(url, data);
    }

    @JavascriptInterface
    fun openUrl(url: String) {
        activity.runOnUiThread {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(browserIntent)
        }
    }

    private fun extractAmigoId(json: String) {
        try {
            val jObject = JSONObject(json)
            val data = jObject.getString("data")
            val jObject2 = JSONObject(data)
            val dataArray = jObject2.getJSONArray("data")
            if (dataArray.length() == 1) {
                amigoId = (dataArray.get(0) as JSONObject).getString("amigo_id")
            } else
                amigoId = ""
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @JavascriptInterface
    fun setState(json: String) {
        //        AmigoCloudAPI.clearEditorContext();
        extractAmigoId(json)
        try {
            val jObject = JSONObject(json)
            formType = jObject.getString("formType")
            data = jObject.getString("data")
            datasetId = java.lang.Long.parseLong(jObject.getString("currentDatasetId"))
            val saveButton = jObject.getBoolean("saveBtn")
            val deleteButton = jObject.getBoolean("deleteBtn")
            val checkButton = jObject.getBoolean("checkBtn")
            deleteRelationship = jObject.getBoolean("relationshipDeleteBtn")
            // TODO:
            //            activity.runOnUiThread(new Runnable() {
            //                @Override
            //                public void run() {
            //                    if (formActivity instanceof BaseFormFragment) {
            //                        BaseFormFragment formFragment = (BaseFormFragment) formActivity;
            //                        formFragment.setSaveButtonEnabled(saveButton);
            //                        formFragment.setDeleteButtonEnabled(deleteButton);
            //                        formFragment.setCheckButtonEnabled(checkButton);
            //                        formFragment.setHeaderTitle(AmigoCloudAPI.getDatasetName(datasetId));
            //                    }
            //                }
            //            });
        } catch (e: NumberFormatException) {
            //            Crashlytics.logException(e);
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    @JavascriptInterface
    fun getFormDescription(datasetId: String, formType: String): String {
        val type = formType + "_description"
        return "" // TODO: AmigoCloudAPI.getDatasetForm(Long.valueOf(datasetId), type);
    }

    @JavascriptInterface
    fun getBlockHTML(formType: String): String {
        return "" // TODO: AmigoCloudAPI.getDatasetForm(datasetId, formType);
    }

    @JavascriptInterface
    fun getBlockHTMLWithId(formType: String, datasetId: String): String {
        try {
            return "" // TODO: AmigoCloudAPI.getDatasetForm(Long.parseLong(datasetId), formType);
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getDatasetRecords(datasetId: String, offset: String): String {
        return getDatasetRecordsWithFilter(datasetId, offset, "")
    }

    @JavascriptInterface
    fun getDatasetRecordsWithFilter(datasetId: String, offset: String, filter: String): String {
        try {
            return "" // TODO: AmigoCloudAPI.getRecordsJSON(Long.parseLong(datasetId), Integer.parseInt(offset), filter, "");
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

        return "" // TODO: AmigoCloudAPI.getDistinctRecordsJSON(dsid, 0, filter, "", columns);
    }

    @JavascriptInterface
    fun getDatasetRecordsWithFilterOrderBy(datasetId: String, offset: String, filter: String, order_by: String): String {
        try {
            return "" // TODO: AmigoCloudAPI.getRecordsJSON(Long.parseLong(datasetId), Integer.parseInt(offset), filter, order_by);
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
            return "" // TODO: AmigoCloudAPI.getRecordsCountJSON(Long.parseLong(datasetId), filter);
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getRelatedRecordsCount(originalDatasetId: String, relatedDatasetId: String, sourceAmigoId: String): String {
        try {
            return "" // TODO: AmigoCloudAPI.getRelatedRecordsCountJSON(Long.parseLong(originalDatasetId), Long.parseLong(relatedDatasetId), sourceAmigoId);
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getNewRowWithSourceId(datasetId: String, relatedColumn: String, relatedId: String): String {
        try {
            return "" // TODO: AmigoCloudAPI.getNewRecordJSONWithRelationship(Long.parseLong(datasetId), relatedColumn, relatedId);
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getNewRow(datasetId: String): String {
        try {
            return "" // TODO: AmigoCloudAPI.getNewRecordJSONForDataset(Long.parseLong(datasetId));
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getSchemaWithId(datasetId: String): String {
        try {
            return "" // TODO: AmigoCloudAPI.getSchemaJSONForDataset(Long.parseLong(datasetId));
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun getRelatedTables(datasetId: String): String {
        try {
            return "" // TODO: AmigoCloudAPI.getRelatedTablesJSON(Long.parseLong(datasetId));
        } catch (e: NumberFormatException) {
            return ""
        }

    }

    @JavascriptInterface
    fun editRowGeometry(json: String) {
        //        TODO:
        //        AmigoApplication.Companion.sendAnalyticsEvent(R.string.category_user, R.string.action_edit_geometry);
        //        GeometryEditorParams.Companion.setParams(new GeometryEditorParams(datasetId, json));
        //        if (activity instanceof GlobeActivity) {
        //            GlobeActivityEditExtensionsKt.enableEditMode((GlobeActivity) activity, formType.equals("create_block"), null);
        //        }
    }

    @JavascriptInterface
    fun updateRow(originalJson: String?, updateJson: String) {
        //        TODO:
        //        if (formType.equals("create_block"))
        //            AmigoApplication.Companion.sendAnalyticsEvent(R.string.category_user, R.string.action_create_record);
        //        else
        //            AmigoApplication.Companion.sendAnalyticsEvent(R.string.category_user, R.string.action_change_record);
        //
        //        AmigoCloudAPI.addToStagingData(datasetId, originalJson, updateJson);
        //
        //        if (activity instanceof GlobeActivity) {
        //            GlobeActivityEditExtensionsKt.disableEditMode((GlobeActivity) activity);
        //        }
    }

    @JavascriptInterface
    fun saveRow(json: String) {
        updateRow(data, json)
    }

    @JavascriptInterface
    fun close() {
        //        TODO:
        //        if (AmigoCloudAPI.flushStagingData()) {
        //            if(AmigoCloudAPI.isAutoSyncEnabled()) {
        //                AmigoCloudAPI.syncProject(AmigoCloudAPI.getSelectedProjectId());
        //            }
        //            AmigoCloudAPI.clearDrawingCache();
        //        } else {
        //            activity.runOnUiThread(new Runnable() {
        //                @Override
        //                public void run() {
        //                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        //                    alertDialogBuilder.setTitle(activity.getString(R.string.error));
        //                    alertDialogBuilder.setMessage(activity.getString(R.string.malformed_data));
        //                    alertDialogBuilder.setNegativeButton(activity.getString(R.string.ui_ok), null);
        //                    alertDialogBuilder.create().show();
        //                }
        //            });
        //        }
        //        activity.runOnUiThread(new Runnable() {
        //            @Override
        //            public void run() {
        //                if (activity instanceof GlobeActivity) {
        //                    GlobeActivityFragmentExtensionsKt.clearPopUps(((GlobeActivity) activity), GlobeActivity.Companion.getCLEAR_ALL());
        //                } else
        //                    activity.onBackPressed();
        //            }
        //        });
    }

    @JavascriptInterface
    fun deleteRows(json: String) {
        //        TODO:
        //		activity.runOnUiThread(new Runnable() {
        //			@Override
        //			public void run() {
        //				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        //				if(deleteRelationship) {
        //					alertDialogBuilder.setTitle(activity.getString(R.string.delete_rows));
        //					alertDialogBuilder.setMessage(activity.getString(R.string.delete_relationships));
        //					alertDialogBuilder.setPositiveButton(activity.getString(R.string.relationships_only), new DialogInterface.OnClickListener() {
        //						public void onClick(DialogInterface dialog, int id) {
        //							if (formActivity.isReady()) {
        //								formActivity.getWebView().loadUrl("javascript:Amigo.deleteSelectedRelationships()");
        //							}
        //							dialog.dismiss();
        //						}
        //					});
        //					alertDialogBuilder.setNeutralButton(activity.getString(R.string.ui_both), new DialogInterface.OnClickListener() {
        //						public void onClick(DialogInterface dialog, int id) {
        //							AmigoApplication.Companion.sendAnalyticsEvent(R.string.category_user, R.string.action_delete_record);
        //							AmigoCloudAPI.deleteRows(datasetId, json);
        //							historyBack();
        //							dialog.dismiss();
        //						}
        //					});
        //					alertDialogBuilder.setNegativeButton(activity.getString(R.string.ui_cancel), null);
        //				} else {
        //					alertDialogBuilder.setTitle(activity.getString(R.string.delete_rows));
        //					alertDialogBuilder.setMessage(activity.getString(R.string.are_you_sure));
        //					alertDialogBuilder.setPositiveButton(activity.getString(R.string.ui_yes), new DialogInterface.OnClickListener() {
        //						public void onClick(DialogInterface dialog, int id) {
        //							AmigoApplication.Companion.sendAnalyticsEvent(R.string.category_user, R.string.action_delete_record);
        //							AmigoCloudAPI.deleteRows(datasetId, json);
        //							historyBack();
        //							dialog.dismiss();
        //						}
        //					});
        //					alertDialogBuilder.setNegativeButton(activity.getString(R.string.ui_no), null);
        //				}
        //				alertDialogBuilder.create().show();
        //			}
        //		});
    }

    @JavascriptInterface
    fun newRecord() {
        //        TODO:
        //		String newRecordData = AmigoCloudAPI.getNewRecordJSONForDataset(datasetId);
        //		if(activity instanceof GlobeActivity) {
        //            GlobeActivityFragmentExtensionsKt.showEditForm((GlobeActivity) activity, datasetId, newRecordData, true);
        //		}
    }

    @JavascriptInterface
    fun zoomToRows(json: String) {
        //        TODO:
        //		AmigoApplication.Companion.sendAnalyticsEvent(R.string.category_user, R.string.action_zoom_to_record);
        //		AmigoCloudAPI.showDataset(datasetId);
        //		AmigoCloudAPI.zoomToRecords(datasetId, json);
    }

    @JavascriptInterface
    fun takePhoto(relatedTableId: String, amigoId: String) {
        //        TODO:
        //		if(activity instanceof GlobeActivity) {
        //            final GlobeActivity globeActivity = (GlobeActivity) activity;
        //            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        //            String[] entries = {activity.getString(R.string.take_photo),
        //                    activity.getString(R.string.capture_video),
        //                    activity.getString(R.string.use_existing_file)};
        //            builder.setItems(entries, new DialogInterface.OnClickListener() {
        //                @Override
        //                public void onClick(DialogInterface dialogInterface, int which) {
        //                    if (which == 0 || which == 1) {
        //                        Intent takePictureIntent;
        //                        String fileExtension;
        //                        int mode = 0;
        //                        if (which == 0) {
        //                            fileExtension = ".jpg";
        //                            takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //                            mode = REQUEST_IMAGE_CAPTURE;
        //                        } else {
        //                            fileExtension = ".mp4";
        //                            takePictureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        //                            mode = REQUEST_VIDEO_CAPTURE;
        //                        }
        //                        String storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "amigocloud";
        //                        File storageDirFile = new File(storageDir);
        //                        if (!storageDirFile.exists())
        //                            storageDirFile.mkdirs();
        //                        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US).format(new Date());
        //
        //                        File image = new File(storageDir, relatedTableId + timeStamp + fileExtension);
        //
        //                        globeActivity.setPhotoInfo(new PhotoInfo(datasetId, Long.parseLong(relatedTableId), amigoId, image));
        //                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
        //                        try {
        //                            activity.startActivityForResult(takePictureIntent, mode);
        //                        } catch (Exception e) {
        //                            Crashlytics.logException(e);
        //                            e.printStackTrace();
        //                        }
        //                    } else {
        //                        globeActivity.setPhotoInfo(new PhotoInfo(datasetId, Long.parseLong(relatedTableId), amigoId, null));
        //                        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        //                        photoPickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        //                        photoPickerIntent.setType("image/* video/* audio/* text/* application/*");
        //                        try {
        //                            activity.startActivityForResult(photoPickerIntent, REQUEST_PHOTO_GALLERY);
        //                        } catch (Exception e) {
        //                            Crashlytics.logException(e);
        //                            e.printStackTrace();
        //                        }
        //                    }
        //
        //                    dialogInterface.dismiss();
        //                }
        //            });
        //            builder.create().show();
        //        }
    }

    @JavascriptInterface
    fun scanBarcode(amigoId: String) {
        //        TODO:
        //        IntentIntegrator integrator = new IntentIntegrator(activity);
        //        final GlobeActivity globeActivity = (GlobeActivity) activity;
        //        globeActivity.setPhotoInfo(new PhotoInfo(datasetId, 0, amigoId, null));
        //        integrator.initiateScan();
    }

    @JavascriptInterface
    fun viewPhotos(relatedTableId: String, amigoId: String) {
        //        TODO:
        //        if (activity instanceof GlobeActivity) {
        //            GlobeActivityFragmentExtensionsKt.showPhotoGridFragment((GlobeActivity) activity,
        //                    AmigoCloudAPI.getFileArray(datasetId, Long.parseLong(relatedTableId), amigoId),
        //                    datasetId, Long.parseLong(relatedTableId), amigoId);
        //        }
    }

    @JavascriptInterface
    fun dataHasChanged(hasChanged: Boolean) {
        //        TODO:
        //		activity.runOnUiThread(new Runnable() {
        //			@Override
        //			public void run() {
        //				if(hasChanged) {
        //					AlertDialog.Builder ad = new AlertDialog.Builder(activity, R.style.AmigoThemeDialog);
        //					ad.setTitle(activity.getString(R.string.warning));
        //					ad.setMessage(activity.getString(R.string.discard_changes));
        //
        //					ad.setNegativeButton(activity.getString(R.string.ui_cancel), null);
        //					ad.setPositiveButton(activity.getString(R.string.ui_discard), new DialogInterface.OnClickListener() {
        //						@Override
        //						public void onClick(DialogInterface dialogInterface, int i) {
        //                            AmigoCloudAPI.cleanStagingData();
        //                            historyBack();
        //                            dialogInterface.dismiss();
        //                        }
        //                    });
        //                    ad.show();
        //                } else {
        //                    historyBack();
        //                }
        //            }
        //        });
    }

    @JavascriptInterface
    fun writeRfidInfo(lat: String?, lon: String?, owner: String?, asset_name: String?) {
        var lat = lat
        var lon = lon
        var owner = owner
        var asset_name = asset_name
        if (lat == null) lat = "0.0"
        if (lon == null) lon = "0.0"
        if (owner == null) owner = "N/A"
        if (asset_name == null) asset_name = "N/A"
        val latf = lat
        val lonf = lon
        val ownerf = owner
        val asset_namef = asset_name

        //        TODO:
        //        if (activity instanceof GlobeActivity) {
        //            final GlobeActivity globeActivity = (GlobeActivity) activity;
        //            GlobeActivityTSLExtensionsKt.writeToTSLMarker(globeActivity, amigoId, datasetId, latf, lonf, ownerf, asset_namef);
        //        }
    }

    @JavascriptInterface
    fun getGeometryInfo(wkb: String): String {
        return "" // TODO: AmigoCloudAPI.getGeometryInfo(wkb);
    }

    fun setRfidWriteStatus(text: String, status: String) {
        activity.runOnUiThread {
            if (formActivity.isReady())
                formActivity.getWebView().loadUrl("javascript:Amigo.setRfidWriteStatus('" +
                        text + "','" + status + "')")
        }
    }

    companion object {


        val REQUEST_IMAGE_CAPTURE = 1
        val REQUEST_VIDEO_CAPTURE = 2
        val REQUEST_PHOTO_GALLERY = 100
    }

    fun setViewState(state: FormViewState) {
//        this.state = state.dataset
    }

    fun setLocation(location: Location) {

    }
}