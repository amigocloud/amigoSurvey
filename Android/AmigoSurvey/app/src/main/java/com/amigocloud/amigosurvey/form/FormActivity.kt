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
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView
import android.widget.ProgressBar
import com.amigocloud.amigosurvey.ApplicationScope
import com.amigocloud.amigosurvey.R
import com.amigocloud.amigosurvey.models.FormModel
import com.amigocloud.amigosurvey.models.RelatedTableModel
import com.amigocloud.amigosurvey.repository.SurveyConfig
import toothpick.Toothpick
import javax.inject.Inject

class FormActivity : AppCompatActivity() {
    companion object {
        val INTENT_USER_ID = "user_id"
        val INTENT_PROJECT_ID = "project_id"
        val INTENT_DATASET_ID = "dataset_id"
        val BASE_URL = "https://www.amigocloud.com"
    }

    var user_id: Long = 0
    var project_id: Long = 0
    var dataset_id: Long = 0

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var viewModel: FormViewModel
    private lateinit var relatedTables: List<RelatedTableModel>
    private lateinit var forms: FormModel
    private lateinit var bridge: AmigoBridge
    private var ready: Boolean = false

    @Inject lateinit var viewModelFactory: FormViewModel.Factory

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toothpick.openScopes(ApplicationScope::class.java, this).let {
            Toothpick.inject(this, it)
        }
        viewModel = viewModelFactory.get(this)

        setContentView(R.layout.activity_form)
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
            it?.let { location -> bridge.lastLocation = location }
        })

        viewModel.onFetchForm(project_id, dataset_id)

        WebView.setWebContentsDebuggingEnabled(true)
    }

    fun isReady(): Boolean {
        return ready
    }

    fun getWebView() = this.webView

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

}
