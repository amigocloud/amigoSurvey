package com.amigocloud.amigosurvey.selector

import android.arch.paging.DataSource
import android.arch.paging.LivePagedListProvider
import android.arch.paging.TiledDataSource
import android.util.Log
import com.amigocloud.amigosurvey.repository.AmigoRest
import io.reactivex.Observable

class ProjectListProvider(private val rest: AmigoRest)
    : LivePagedListProvider<Int, SelectorItem>() {

    private val dataSource = object : TiledDataSource<SelectorItem>() {
        override fun countItems() = DataSource.COUNT_UNDEFINED

        override fun loadRange(startPosition: Int, count: Int): MutableList<SelectorItem> =
                try {
                    rest.fetchProjects(count, startPosition)
                            .flatMapObservable { Observable.fromIterable(it.results) }
                            .map { SelectorItem(SelectorItem.Type.PROJECT, it.id, it.name, it.preview_image) }
                            .toList()
                            .blockingGet()
                } catch (e: Exception) {
                    Log.d("ProjectListProvider", "Error loading projects", e)
                    invalidate()
                    mutableListOf()
                }
    }

    override fun createDataSource() = dataSource
}


