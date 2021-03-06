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

import android.Manifest
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
import toothpick.Toothpick
import javax.inject.Inject
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.location.Location
import android.net.ConnectivityManager
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.util.Log
import android.view.View
import com.amigocloud.amigosurvey.databinding.ActivityFormBinding
import com.amigocloud.amigosurvey.util.isConnected
import com.android.IntentIntegrator
import com.squareup.moshi.Moshi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_form.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class FormActivity : AppCompatActivity() {
    companion object {
        const val TAG = "FormActivity"
        const val INTENT_USER_ID = "user_id"
        const val INTENT_PROJECT_ID = "project_id"
        const val INTENT_DATASET_ID = "dataset_id"

        private val INITIAL_PERMS = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)

        private const val INITIAL_REQUEST = 1337
        private const val CAMERA_REQUEST = INITIAL_REQUEST + 1
        private const val FINE_LOCATION_REQUEST = INITIAL_REQUEST + 2
        private const val COARSE_LOCATION_REQUEST = INITIAL_REQUEST + 3

        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_VIDEO_CAPTURE = 2
        const val REQUEST_PHOTO_GALLERY = 100
    }

    var user_id: Long = 0
    var project_id: Long = 0
    var dataset_id: Long = 0
    var haveLocation: Boolean = false

    var ready: Boolean = false
    lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var viewModel: FormViewModel
    private lateinit var photoViewModel: PhotoViewModel
    private lateinit var uploadViewModel: UploadViewModel
//    private lateinit var recordsViewModel: RecordsViewModel
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var bridge: AmigoBridge

    private var disposables = CompositeDisposable()

    private var photoInfo: PhotoInfo? = null

    private lateinit var binding: ActivityFormBinding

    @Inject lateinit var viewModelFactory: FormViewModel.Factory
    @Inject lateinit var photoModelFactory: PhotoViewModel.Factory
//    @Inject lateinit var recordsModelFactory: RecordsViewModel.Factory
    @Inject lateinit var locationViewModelFactory: LocationViewModel.Factory
    @Inject lateinit var uploadViewModelFactory: UploadViewModel.Factory
    @Inject lateinit var connectivityManager: ConnectivityManager
    @Inject lateinit var moshi: Moshi

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toothpick.openScopes(ApplicationScope::class.java, this).let {
            Toothpick.inject(this, it)
        }
        uploadViewModel = uploadViewModelFactory.get(this)
        viewModel = viewModelFactory.get(this)
        photoViewModel = photoModelFactory.get(this)
//        recordsViewModel = recordsModelFactory.get(this)
        locationViewModel = locationViewModelFactory.get(this)

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

        bridge = AmigoBridge(this, moshi)

        viewModel.events.observe(this, Observer {
            it?.let { state ->
                bridge.formViewState = state
                ready = true
                _loadForm(state)
                progressBar.visibility = ProgressBar.INVISIBLE
            }
        })

        uploadViewModel.events.observe(this, Observer { progress ->
            when (progress) {
                is RecordsUploadProgressEvent -> {
                    val pr = (progress.index.toFloat() / progress.total.toFloat()) * 100.0
                    val msg = "Record submit: ${progress.index + 1} of ${progress.total}"
                    updateProgress(pr.toInt(), msg)
                }
                is RecordsUploadCompleteEvent -> {
                    updateProgress(100, "Submit records finished.")
                }
                is FileUploadProgressEvent -> {
                    val pr = (progress.bytesSent.toFloat() / progress.bytesTotal.toFloat()) * 100.0
                    val msg = "File ${progress.index + 1} of ${progress.total}: ${progress.message}"
                    updateProgress(pr.toInt(), msg)
                }
                is FileUploadCompleteEvent -> {
                    if (progress.index == progress.total - 1) {
                        hideProgressBar()
                        this.finish()
                    }
                }
            }
        })

        requestPermissions(INITIAL_PERMS, INITIAL_REQUEST)

        locationViewModel.location.connect().let { disposables.add(it) }

        locationViewModel.location
                .startWith(locationViewModel.lastLocation)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { location -> updateGPSLocation(location) }

        viewModel.onFetchForm(project_id, dataset_id)
        WebView.setWebContentsDebuggingEnabled(true)

        val rn = uploadViewModel.getSavedRecordsNum()
        val pn = uploadViewModel.getSavedPhotosNum()
        if(rn > 0 ) {
            if(pn > 0 ) {
                binding.recordsInfo.text = "Records:$rn Photos:$pn"
            } else {
                binding.recordsInfo.text = "Records:$rn"
            }
        }
    }

    fun updateGPSLocation(location: Location) {
        Log.e("-updateGPSLocation-", location.toString())
        if (location.hasAccuracy()) {
            bridge.lastLocation = location
            haveLocation = true
            when {
                location.accuracy <= 10 -> binding.gpsInfoButton.setBackgroundResource(R.drawable.gps_green)
                location.accuracy <= 65 -> binding.gpsInfoButton.setBackgroundResource(R.drawable.gps_yellow)
                location.accuracy > 65 -> binding.gpsInfoButton.setBackgroundResource(R.drawable.gps_red)
            }
        } else {
            binding.gpsInfoButton.setBackgroundResource(R.drawable.gps_off)
            haveLocation = false
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    fun onSave() {
        Log.e("---", "save")
        if(true) {//haveLocation) {
            bridge.submit()
        } else {
            val ad = AlertDialog.Builder(this)
            ad.setTitle(getString(R.string.no_location_title))
            ad.setMessage(getString(R.string.no_location_message))
            ad.setNeutralButton(getString(R.string.ui_ok), null)
            ad.show()
        }
    }

    fun updateProgress(progress: Int, msg: String) {
        progress_bar.visibility = View.VISIBLE
        progress_bar.progress = progress
        binding.recordsInfo.text = msg
    }

    fun hideProgressBar() {
        progress_bar.visibility = View.INVISIBLE
        binding.recordsInfo.text = ""
    }

    fun onGPSInfo() {
        val ad = AlertDialog.Builder(this)
        ad.setTitle(getString(R.string.gps_info))
        val msg = StringBuffer()
        msg.append(getString(R.string.latitude) + ": \t${bridge.lastLocation.latitude}\n")
        msg.append(getString(R.string.longitude) + ": \t${bridge.lastLocation.longitude}\n")
        msg.append(getString(R.string.accuracy) + ": \t${bridge.lastLocation.accuracy}\n")
        ad.setMessage(msg.toString())
        ad.setNegativeButton(getString(R.string.ui_cancel), null)
        ad.show()
    }

    fun submitNewRecord(rec: String) {
        uploadViewModel.saveRecord(rec)

        if(connectivityManager.isConnected()) {
            uploadViewModel.submitAll(viewModel.project.get(), viewModel.dataset.get())
        } else {
            this.finish()
        }
    }

    fun _loadForm(state: FormViewState) {
        bridge.formType = "create_block"
        webView.let { webView ->
            webView.addJavascriptInterface(bridge, "AmigoPlatform")
            val html = state.form?.base_form?.replaceFirst("<base href=\".*\".*>".toRegex(), "")
            val url = "file://" + state.webFormDir + bridge.formType
            webView.loadDataWithBaseURL(url, html, "text/html", "UTF-8", "")
        }
    }

    fun takePhoto(relatedTableId: String, amigoId: String) {
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

                val photoURI = FileProvider.getUriForFile(this, "com.amigocloud.amigosurvey.fileprovider", image)

                this.setPhotoInfo(PhotoInfo(dataset_id, java.lang.Long.parseLong(relatedTableId), amigoId, image))
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
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

        if (resultCode == Activity.RESULT_OK && requestCode != IntentIntegrator.REQUEST_CODE)
            bridge.mediaAdded()

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            photoInfo?.let { photoViewModel.savePhoto(this, it).subscribe() }
        } else if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == Activity.RESULT_OK) {
            photoInfo?.let { photoViewModel.saveVideo(this, it).subscribe() }
        } else if (requestCode == REQUEST_PHOTO_GALLERY && resultCode == Activity.RESULT_OK) {
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
                    } else if (data.data != null) {
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

    private fun hasPermission(perm: String): Boolean {
        return PackageManager.PERMISSION_GRANTED == checkSelfPermission(perm)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        when (requestCode) {
            CAMERA_REQUEST -> if (hasPermission(Manifest.permission.CAMERA)) {}
            FINE_LOCATION_REQUEST -> if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {}
            COARSE_LOCATION_REQUEST -> if (hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {}
        }
    }

}



