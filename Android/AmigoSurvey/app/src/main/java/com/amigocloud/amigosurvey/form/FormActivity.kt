package com.amigocloud.amigosurvey.form

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import com.amigocloud.amigosurvey.R

class FormActivity : AppCompatActivity() {
    companion object {
        val INTENT_USER_ID = "user_id"
        val INTENT_PROJECT_ID = "project_id"
        val INTENT_DATASET_ID = "dataset_id"
    }

    var user_id: Long = 0
    var project_id: Long = 0
    var dataset_id: Long = 0

    var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)
        user_id = intent.getLongExtra(INTENT_USER_ID, 0)
        project_id = intent.getLongExtra(INTENT_PROJECT_ID, 0)
        dataset_id = intent.getLongExtra(INTENT_DATASET_ID, 0)
        webView = findViewById(R.id.webview)

        webView?.settings?.javaScriptEnabled = true
        webView?.settings?.allowFileAccess = true
    }

    fun isReady(): Boolean {
        return webView != null
    }


}
