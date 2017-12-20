package com.amigocloud.amigosurvey.form

import android.app.Activity
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.text.Editable
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

    companion object {
        fun newInstance(num: Int): ProgressFragment {
            return ProgressFragment()
        }
    }

    fun updateProgress(progress: Long, message: String) {
        upload_progress.progress = progress.toInt()
        text.text = message
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.progress_fragment, container, false)
        return v
    }
}