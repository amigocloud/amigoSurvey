package com.amigocloud.amigosurvey.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity

const val inflationException = "ViewModel could not be inflated"

abstract class ViewModelFactory<VM : ViewModel> : ViewModelProvider.Factory {

    abstract protected val modelClass: Class<VM>

    fun get(activity: AppCompatActivity): VM = ViewModelProviders.of(activity, this).get(modelClass)

    fun get(fragment: Fragment): VM = ViewModelProviders.of(fragment, this).get(modelClass)
}
