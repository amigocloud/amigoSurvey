package com.amigocloud.amigosurvey.form

import android.app.DialogFragment
import android.os.Bundle
import com.amigocloud.amigosurvey.R
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import kotlinx.android.synthetic.main.progress_fragment.*


class ProgressFragment : DialogFragment() {

    interface ProgressDialogListener {
        fun onChangeProgressValue(value: Int)
    }
    private var listener: ProgressDialogListener? = null
    private var progress = 0L
    private var message = ""

    companion object {
        fun newInstance(num: Int): ProgressFragment {
            return ProgressFragment()
        }
    }

    fun updateProgress(progress: Long, message: String) {
        this.progress = progress
        this.message = message
        upload_progress?.let {it.progress = progress.toInt() }
        text?.let { it.text = message }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.progress_fragment, container, false).apply {
                updateProgress(progress, message)
            }

}