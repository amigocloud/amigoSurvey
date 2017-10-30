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



package com.amigocloud.amigomobile.api.js;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;

import com.amigocloud.amigosurvey.form.FormActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AmigoBridge {


	public static final int REQUEST_IMAGE_CAPTURE = 1;
	public static final int REQUEST_VIDEO_CAPTURE = 2;
	public static final int REQUEST_PHOTO_GALLERY = 100;

	private Activity activity;
	private FormActivity formActivity;
	private String formType;
	private String formData;
	private long datasetId;
	private boolean deleteRelationship;
    private String amigoId;

	public AmigoBridge(Activity activity, FormActivity formActivity, long datasetId, String data) {
		this.activity = activity;
		this.formActivity = formActivity;
		this.formData = data;
		this.datasetId = datasetId;
		if (datasetId < 0) {
			IllegalStateException e =  new IllegalStateException("Dataset ID cannot be < 0");
//			Crashlytics.logException(e);
			throw e;
		}
	}

	public void setFormType(String type) {
		formType = type;
	}

    public String getFormType() {
        return formType;
    }

    public long getDatasetId() {
        return datasetId;
    }

    @JavascriptInterface
    public String getDatasetInfo(String datasetId) {
        return ""; // TODO: AmigoCloudAPI.getDatasetInfo(Long.parseLong(datasetId));
    }

    public void setCustomFieldValue(final String fieldName, final String fieldValue) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (formActivity.isReady())
                    formActivity.getWebView().loadUrl("javascript:Amigo.setCustomFieldValue(" +
                            String.valueOf(datasetId) + ",'" + fieldName + "','" + fieldValue + "')");
            }
        });
    }

    @JavascriptInterface
    public String getClientType() {
        return "mobile";
    }

    @JavascriptInterface
    public void ready() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (formActivity.isReady())
                    formActivity.getWebView().loadUrl("javascript:Amigo.loadBlock(" +
                            "AmigoPlatform.getBlockHTML('" + formType + "'), '" + formType + "', " +
                            "'" + datasetId + "', AmigoPlatform.getData());");
            }
        });
    }

    @JavascriptInterface
    public String getData() {
        return formData;
    }

    @JavascriptInterface
    public int getPageSize() {
        return 20;
    }

    public void historyBack() {
        if (formActivity.isReady()) {
            formActivity.getWebView().loadUrl("javascript:Amigo.historyBack()");
        }
    }

    public void mediaAdded() {
        if (formActivity.isReady()) {
            formActivity.getWebView().loadUrl("javascript:Amigo.mediaAdded()");
        }
    }

    @JavascriptInterface
    public void onException(String msg) {
        // Do nothing, will be deprecated
    }

    @JavascriptInterface
    public void storeException(String url, String data) {
//   TODO:     AmigoCloudAPI.storeException(url, data);
    }

    @JavascriptInterface
    public void openUrl(final String url) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(browserIntent);
            }
        });
    }

    public String getAmigoId() {
        return amigoId;
    }

    private void extractAmigoId(String json) {
        try {
            JSONObject jObject = new JSONObject(json);
            String data = jObject.getString("data");
            JSONObject jObject2 = new JSONObject(data);
            JSONArray dataArray = jObject2.getJSONArray("data");
            if (dataArray.length() == 1) {
                amigoId = ((JSONObject) dataArray.get(0)).getString("amigo_id");
            } else
                amigoId = "";
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void setState(String json) {
//        AmigoCloudAPI.clearEditorContext();
        extractAmigoId(json);
        try {
            JSONObject jObject = new JSONObject(json);
            formType = jObject.getString("formType");
            formData = jObject.getString("data");
            datasetId = Long.parseLong(jObject.getString("currentDatasetId"));
            final boolean saveButton = jObject.getBoolean("saveBtn");
            final boolean deleteButton = jObject.getBoolean("deleteBtn");
            final boolean checkButton = jObject.getBoolean("checkBtn");
            deleteRelationship = jObject.getBoolean("relationshipDeleteBtn");
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
        } catch (NumberFormatException | JSONException e) {
//            Crashlytics.logException(e);
            e.printStackTrace();
        }

    }

    @JavascriptInterface
    public String getFormDescription(String datasetId, String formType) {
        String type = formType + "_description";
        return ""; // TODO: AmigoCloudAPI.getDatasetForm(Long.valueOf(datasetId), type);
    }

    @JavascriptInterface
    public String getBlockHTML(String formType) {
        return ""; // TODO: AmigoCloudAPI.getDatasetForm(datasetId, formType);
    }

    @JavascriptInterface
    public String getBlockHTMLWithId(String formType, String datasetId) {
        try {
            return ""; // TODO: AmigoCloudAPI.getDatasetForm(Long.parseLong(datasetId), formType);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    @JavascriptInterface
    public String getDatasetRecords(String datasetId, String offset) {
        return getDatasetRecordsWithFilter(datasetId, offset, "");
    }

    @JavascriptInterface
    public String getDatasetRecordsWithFilter(String datasetId, String offset, String filter) {
        try {
            return ""; // TODO: AmigoCloudAPI.getRecordsJSON(Long.parseLong(datasetId), Integer.parseInt(offset), filter, "");
        } catch (NumberFormatException e) {
            return "";
        }
    }

    @JavascriptInterface
    public String getDatasetRecordsDistinctColumnsWithFilter(String datasetId, String columns, String filter) {
        long dsid = 0;
        try {
            dsid = Long.parseLong(datasetId);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return "{}";
        }
        return ""; // TODO: AmigoCloudAPI.getDistinctRecordsJSON(dsid, 0, filter, "", columns);
    }

    @JavascriptInterface
    public String getDatasetRecordsWithFilterOrderBy(String datasetId, String offset, String filter, String order_by) {
        try {
            return ""; // TODO: AmigoCloudAPI.getRecordsJSON(Long.parseLong(datasetId), Integer.parseInt(offset), filter, order_by);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    @JavascriptInterface
    public String getDatasetRecordsCountWithId(String datasetId) {
        return getDatasetRecordsCountWithIdAndFilter(datasetId, "");
    }

    @JavascriptInterface
    public String getDatasetRecordsCountWithIdAndFilter(String datasetId, String filter) {
        try {
            return ""; // TODO: AmigoCloudAPI.getRecordsCountJSON(Long.parseLong(datasetId), filter);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    @JavascriptInterface
    public String getRelatedRecordsCount(String originalDatasetId, String relatedDatasetId, String sourceAmigoId) {
        try {
            return ""; // TODO: AmigoCloudAPI.getRelatedRecordsCountJSON(Long.parseLong(originalDatasetId), Long.parseLong(relatedDatasetId), sourceAmigoId);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    @JavascriptInterface
    public String getNewRowWithSourceId(String datasetId, String relatedColumn, String relatedId) {
        try {
            return ""; // TODO: AmigoCloudAPI.getNewRecordJSONWithRelationship(Long.parseLong(datasetId), relatedColumn, relatedId);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    @JavascriptInterface
    public String getNewRow(String datasetId) {
        try {
            return ""; // TODO: AmigoCloudAPI.getNewRecordJSONForDataset(Long.parseLong(datasetId));
        } catch (NumberFormatException e) {
            return "";
        }
    }

    @JavascriptInterface
    public String getSchemaWithId(String datasetId) {
        try {
            return ""; // TODO: AmigoCloudAPI.getSchemaJSONForDataset(Long.parseLong(datasetId));
        } catch (NumberFormatException e) {
            return "";
        }
    }

    /**
     * Old Form Methods
     **/

    @JavascriptInterface
    public String getSchema() {
        return ""; // TODO: AmigoCloudAPI.getSchemaJSONForDataset(datasetId);
    }

    @JavascriptInterface
    public String getUser() {
        return ""; // TODO: AmigoCloudAPI.getUserInfoJSON();
    }

    @JavascriptInterface
    public String getProject() {
        return ""; // TODO: AmigoCloudAPI.getSelectedProjectInfoJSON();
    }

    @JavascriptInterface
    public String getRelatedTables(String datasetId) {
        try {
            return ""; // TODO: AmigoCloudAPI.getRelatedTablesJSON(Long.parseLong(datasetId));
        } catch (NumberFormatException e) {
            return "";
        }
    }

    @JavascriptInterface
    public String getGPSinfo() {
        return ""; // TODO: Globe.getGPSInfoJSON();
    }

    @JavascriptInterface
    public String getPermissionLevel() {
        String permission = "EDITOR"; // TODO: AmigoCloudAPI.getProjectPermissionLevel(AmigoCloudAPI.getSelectedProjectId());
        if (permission == null || permission.isEmpty()) {
            return "EDITOR";
        }
        return permission;
    }

    @JavascriptInterface
    public void editRowGeometry(final String json) {
//        TODO:
//        AmigoApplication.Companion.sendAnalyticsEvent(R.string.category_user, R.string.action_edit_geometry);
//        GeometryEditorParams.Companion.setParams(new GeometryEditorParams(datasetId, json));
//        if (activity instanceof GlobeActivity) {
//            GlobeActivityEditExtensionsKt.enableEditMode((GlobeActivity) activity, formType.equals("create_block"), null);
//        }
    }

    @JavascriptInterface
    public void updateRow(final String originalJson, final String updateJson) {
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
    public void saveRow(final String json) {
        updateRow(formData, json);
    }

    @JavascriptInterface
    public void close() {
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
	public void deleteRows(final String json) {
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
	public void newRecord() {
//        TODO:
//		String newRecordData = AmigoCloudAPI.getNewRecordJSONForDataset(datasetId);
//		if(activity instanceof GlobeActivity) {
//            GlobeActivityFragmentExtensionsKt.showEditForm((GlobeActivity) activity, datasetId, newRecordData, true);
//		}
	}

	@JavascriptInterface
	public void zoomToRows(final String json) {
//        TODO:
//		AmigoApplication.Companion.sendAnalyticsEvent(R.string.category_user, R.string.action_zoom_to_record);
//		AmigoCloudAPI.showDataset(datasetId);
//		AmigoCloudAPI.zoomToRecords(datasetId, json);
	}

	@JavascriptInterface
	public void takePhoto(final String relatedTableId, final String amigoId) {
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
    public void scanBarcode(String amigoId) {
//        TODO:
//        IntentIntegrator integrator = new IntentIntegrator(activity);
//        final GlobeActivity globeActivity = (GlobeActivity) activity;
//        globeActivity.setPhotoInfo(new PhotoInfo(datasetId, 0, amigoId, null));
//        integrator.initiateScan();
    }

	@JavascriptInterface
    public void viewPhotos(String relatedTableId, String amigoId) {
//        TODO:
//        if (activity instanceof GlobeActivity) {
//            GlobeActivityFragmentExtensionsKt.showPhotoGridFragment((GlobeActivity) activity,
//                    AmigoCloudAPI.getFileArray(datasetId, Long.parseLong(relatedTableId), amigoId),
//                    datasetId, Long.parseLong(relatedTableId), amigoId);
//        }
	}

	@JavascriptInterface
	public void dataHasChanged(final boolean hasChanged) {
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
    public void writeRfidInfo(String lat, String lon, String owner, String asset_name) {
        if(lat==null) lat = "0.0";
        if(lon==null) lon = "0.0";
        if(owner==null) owner = "N/A";
        if(asset_name==null) asset_name = "N/A";
        final String latf = lat;
        final String lonf = lon;
        final String ownerf = owner;
        final String asset_namef = asset_name;

//        TODO:
//        if (activity instanceof GlobeActivity) {
//            final GlobeActivity globeActivity = (GlobeActivity) activity;
//            GlobeActivityTSLExtensionsKt.writeToTSLMarker(globeActivity, amigoId, datasetId, latf, lonf, ownerf, asset_namef);
//        }
    }

    @JavascriptInterface
    public String getGeometryInfo(String wkb) {
        return ""; // TODO: AmigoCloudAPI.getGeometryInfo(wkb);
    }

    public void setRfidWriteStatus(final String text, final String status) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (formActivity.isReady())
                    formActivity.getWebView().loadUrl("javascript:Amigo.setRfidWriteStatus('" +
                            text + "','" + status + "')");
            }
        });
    }
}