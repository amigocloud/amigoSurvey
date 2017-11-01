package com.amigocloud.amigosurvey.selector

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import android.arch.paging.PagedList
import android.databinding.ObservableField
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import javax.inject.Inject

class SelectorViewModel(private val rest: AmigoRest) : ViewModel() {

    companion object {
        val PAGING_CONFIG: PagedList.Config = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(5)
                .setPrefetchDistance(15)
                .build()
    }

    private val projectListSource get() = ProjectListProvider(rest).create(0, PAGING_CONFIG)
    private val datasetListSource
        get() = DatasetsListProvider(selectedProject.get()?.id ?: -1, rest)
                .create(0, PAGING_CONFIG)
    private var currentSource: LiveData<PagedList<SelectorItem>> = projectListSource

    val dataSource = MediatorLiveData<PagedList<SelectorItem>>().apply {
        addSource(currentSource, { value = it })
    }

    val selectedProject = ObservableField<SelectorItem?>(null)

    fun showProjects() = updateSource(projectListSource)

    fun showDatasets() = updateSource(datasetListSource)

    private fun updateSource(newSource: LiveData<PagedList<SelectorItem>>) {
        dataSource.removeSource(currentSource)
        newSource.apply {
            dataSource.addSource(this, { dataSource.value = it })
            currentSource = this
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val rest: AmigoRest) : ViewModelFactory<SelectorViewModel>() {

        override val modelClass = SelectorViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(this.modelClass)) {
                return SelectorViewModel(rest) as T
            }
            throw IllegalArgumentException(INFLATION_EXCEPTION)
        }
    }
}
