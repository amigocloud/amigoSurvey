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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView
import android.widget.ProgressBar
import com.amigocloud.amigosurvey.ApplicationScope
import com.amigocloud.amigosurvey.R
import com.amigocloud.amigosurvey.models.FormModel
import com.amigocloud.amigosurvey.models.RelatedTableModel
import toothpick.Toothpick
import javax.inject.Inject
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.amigocloud.amigosurvey.databinding.ActivityFormBinding
import com.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_form.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class FormActivity : AppCompatActivity() {
    companion object {
        val INTENT_USER_ID = "user_id"
        val INTENT_PROJECT_ID = "project_id"
        val INTENT_DATASET_ID = "dataset_id"
        val BASE_URL = "https://www.amigocloud.com"
        internal val FORM_FRAGMENT_TAG = "FORM_FRAGMENT_TAG"
    }

    val REQUEST_IMAGE_CAPTURE = 1
    val REQUEST_VIDEO_CAPTURE = 2
    val REQUEST_PHOTO_GALLERY = 100

    var user_id: Long = 0
    var project_id: Long = 0
    var dataset_id: Long = 0

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var viewModel: FormViewModel
    private lateinit var photoViewModel: PhotoViewModel
    private lateinit var changesetViewModel: ChangesetViewModel
    private lateinit var relatedTables: List<RelatedTableModel>
    private lateinit var forms: FormModel
    private lateinit var bridge: AmigoBridge
    private var ready: Boolean = false

    private var photoInfo: PhotoInfo? = null

    private lateinit var binding: ActivityFormBinding

    @Inject lateinit var viewModelFactory: FormViewModel.Factory
    @Inject lateinit var photoModelFactory: PhotoViewModel.Factory
    @Inject lateinit var changesetModelFactory: ChangesetViewModel.Factory

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toothpick.openScopes(ApplicationScope::class.java, this).let {
            Toothpick.inject(this, it)
        }
        viewModel = viewModelFactory.get(this)
        photoViewModel = photoModelFactory.get(this)
        changesetViewModel = changesetModelFactory.get(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_form)
        binding.viewModel = viewModel
        binding.activity = this

        user_id = intent.getLongExtra(INTENT_USER_ID, 2L)
        project_id = intent.getLongExtra(INTENT_PROJECT_ID, 0L)
        dataset_id = intent.getLongExtra(INTENT_DATASET_ID, 0L)
        webView = findViewById(R.id.webview)
        progressBar = findViewById(R.id.load_progress)

        webView.settings?.javaScriptEnabled = true
        webView.settings?.allowFileAccess = true

        bridge = AmigoBridge(this)
        viewModel.events.observe(this, Observer {
            it?.let { state ->
                bridge.formViewState = state
                ready = true
                _loadForm(state)
                progressBar.visibility = ProgressBar.INVISIBLE
            }
        })

        viewModel.location.observe(this, Observer {
            it?.let { location ->
                Log.e("-location-", location.toString())
                if (location.hasAccuracy()) {
                    bridge.lastLocation = location
                    when {
                        location.accuracy <= 10 -> gps_info_button.setBackgroundResource(R.drawable.gps_green)
                        location.accuracy <= 65 -> gps_info_button.setBackgroundResource(R.drawable.gps_yellow)
                        location.accuracy >  65 -> gps_info_button.setBackgroundResource(R.drawable.gps_red)
                    }
                } else {
                    gps_info_button.setBackgroundResource(R.drawable.gps_off)
                }
            }})
            viewModel.onFetchForm(project_id, dataset_id)
            WebView.setWebContentsDebuggingEnabled(true)
        }

        fun onSave() {
            Log.e("---", "save")
            bridge.submit()
        }

        fun onGPSInfo() {
            val ad = AlertDialog.Builder(this)
            ad.setTitle(getString(R.string.gps_info))
            val msg = StringBuffer()
            msg.append(getString(R.string.latitude) + ": \t${bridge.lastLocation.latitude}\n")
            msg.append(getString(R.string.longitude)+ ": \t${bridge.lastLocation.longitude}\n")
            msg.append(getString(R.string.accuracy)+ ": \t${bridge.lastLocation.accuracy}\n")
            ad.setMessage(msg.toString())
            ad.setNegativeButton(getString(R.string.ui_cancel), null)
            ad.show()
        }

        fun addNewRecord(rec:String) {
            changesetViewModel.addNewRecord(rec, viewModel.project.get(), viewModel.dataset.get())
                    .doOnError { error ->
                        Log.e("---", error.toString())
                    }
                    .subscribe({ it ->
                        Log.e("---", it.toString())
                    })

        }

        fun isReady(): Boolean {
            return ready
        }

        fun getWebView() = this.webView

        fun getViewModel() = this.viewModel

        fun _loadForm(state: FormViewState) {
            bridge.formType = "create_block"
            webView.let { webView ->
                webView.addJavascriptInterface(bridge, "AmigoPlatform")
//            webView.loadUrl("window.onerror = function (message, url, lineNumber) {AmigoPlatform.onException(message + ' URL: ' + url + ' Line:' + lineNumber);}")
                val html = state.form?.base_form?.replaceFirst("<base href=\".*\".*>".toRegex(), "")
                val url = "file://" + state.webFormDir + bridge.formType
                webView.loadDataWithBaseURL(url, html, "text/html", "UTF-8", "")
            }
        }

        fun takePhoto(relatedTableId: String , amigoId: String ) {
            val builder = AlertDialog.Builder(this)
            val entries = arrayOf<String>(this.getString(R.string.take_photo), this.getString(R.string.capture_video), this.getString(R.string.use_existing_file))
            builder.setItems(entries) { dialogInterface, which ->
                if (which == 0 || which == 1) {
                    val takePictureIntent: Intent
                    val fileExtension: String
                    var mode = 0
                    if (which == 0) {
                        fileExtension = ".jpg"
                        takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        mode = REQUEST_IMAGE_CAPTURE
                    } else {
                        fileExtension = ".mp4"
                        takePictureIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                        mode = REQUEST_VIDEO_CAPTURE
                    }
                    val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + "amigocloud"
                    val storageDirFile = File(storageDir)
                    if (!storageDirFile.exists())
                        storageDirFile.mkdirs()
                    val timeStamp = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US).format(Date())

                    val image = File(storageDir, relatedTableId + timeStamp + fileExtension)

                    this.setPhotoInfo(PhotoInfo(dataset_id, java.lang.Long.parseLong(relatedTableId), amigoId, image))
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image))
                    try {
                        this.startActivityForResult(takePictureIntent, mode)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else {
                    this.setPhotoInfo(PhotoInfo(dataset_id, java.lang.Long.parseLong(relatedTableId), amigoId, null))
                    val photoPickerIntent = Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    photoPickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    photoPickerIntent.type = "image/* video/* audio/* text/* application/*"
                    try {
                        this.startActivityForResult(photoPickerIntent, REQUEST_PHOTO_GALLERY)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                dialogInterface.dismiss()
            }
            builder.create().show()
        }

        fun setPhotoInfo(photoInfo: PhotoInfo) {
            this.photoInfo = photoInfo
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if(resultCode == Activity.RESULT_OK && requestCode != IntentIntegrator.REQUEST_CODE)
                bridge.mediaAdded()

            if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
                photoInfo?.let { photoViewModel.savePhoto(this, it).subscribe() }
            } else if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == Activity.RESULT_OK) {
                photoInfo?.let { photoViewModel.saveVideo(this, it).subscribe() }
            } else if (requestCode == REQUEST_PHOTO_GALLERY && resultCode == Activity.RESULT_OK)
            {
                // Save multiple selected files
                photoInfo?.let { photoInfo ->
                    data?.let { data ->
                        if (data.clipData != null) {
                            var count = data.clipData.getItemCount()
                            var i = 0
                            while (i < count) {
                                val item = data.clipData.getItemAt(i)
                                photoViewModel.saveFile(this, photoInfo, item.uri).subscribe()
                                i++
                            }
                        } else if(data.data != null) {
                            photoViewModel.saveFile(this, photoInfo, data.data).subscribe()
                        }
                    }
                }
            } else if (requestCode == IntentIntegrator.REQUEST_CODE) {
                data?.let {
                    val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
                    scanResult?.let {
                        var br = scanResult.contents
                        val barcode = br.replace("\n", "")
                        barcode.let {
                            onBarcodeScanInForm(barcode, photoInfo)
                        }
                    }
                }
            }
        }


        private fun onBarcodeScanInForm(barcode: String, photoInfo: PhotoInfo?) {
            // Received data from barcode scanner
            photoInfo?.let {
                val datasetId = it.datasetId
                val amigoId = it.sourceAmigoId
                val ad = AlertDialog.Builder(this)
                ad.setTitle(getString(R.string.barcode_scanned))
                ad.setMessage(getString(R.string.use_this_value) + barcode)
                ad.setNegativeButton(getString(R.string.ui_no), null)
                ad.setPositiveButton(getString(R.string.ui_yes)) { dialogInterface, i ->
                    // Retrieve barcode filed name
                    viewModel.getCustomFieldName("barcode")
                            .subscribe({ field_name ->
                                bridge.setCustomFieldValue(field_name, barcode)
//                          recordHistoryRead(amigoId, datasetId, barcode)
                            })
                }
                ad.show()
            }
        }

        fun scanBarcode(amigoId: String) {
            val integrator = IntentIntegrator(this)
            setPhotoInfo(PhotoInfo(dataset_id, 0, amigoId, null))
            integrator.initiateScan()
        }

    }



