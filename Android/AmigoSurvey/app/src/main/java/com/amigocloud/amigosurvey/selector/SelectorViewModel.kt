package com.amigocloud.amigosurvey.selector

import android.arch.lifecycle.ViewModel
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import com.amigocloud.amigosurvey.viewmodel.inflationException
import io.reactivex.Observable
import javax.inject.Inject

class SelectorViewModel(private val rest: AmigoRest) : ViewModel() {

    fun getProjectList() = rest.fetchProjects()
            .filter { it.results != null }
            .flatMapObservable { Observable.fromIterable(it.results) }
            .map { SelectorItem(it.id, it.name, it.preview_image) }
            .toList()


    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val rest: AmigoRest) : ViewModelFactory<SelectorViewModel>() {

        override val modelClass = SelectorViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(this.modelClass)) {
                return SelectorViewModel(rest) as T
            }
            throw IllegalArgumentException(inflationException)
        }
    }
}
