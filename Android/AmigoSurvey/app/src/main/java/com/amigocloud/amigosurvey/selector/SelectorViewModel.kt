package com.amigocloud.amigosurvey.selector

import android.arch.lifecycle.ViewModel
import android.arch.paging.PagedList
import android.databinding.ObservableField
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import com.amigocloud.amigosurvey.viewmodel.inflationException
import javax.inject.Inject

class SelectorViewModel(private val rest: AmigoRest) : ViewModel() {

    companion object {
        val PAGING_CONFIG: PagedList.Config = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(20)
                .setPrefetchDistance(40)
                .build()
    }

    val selectedProject = ObservableField<SelectorItem?>(null)

    fun getProjects() = ProjectListProvider(rest).create(0, PAGING_CONFIG)

    fun getDatasets() = DatasetsListProvider(selectedProject.get()?.id ?: -1, rest).create(0, PAGING_CONFIG)

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
