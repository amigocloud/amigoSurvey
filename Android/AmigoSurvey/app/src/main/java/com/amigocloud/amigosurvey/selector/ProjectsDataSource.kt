package com.amigocloud.amigosurvey.selector

import android.arch.paging.LivePagedListProvider
import android.arch.paging.TiledDataSource
import android.util.Log
import com.amigocloud.amigosurvey.repository.AmigoRest
import io.reactivex.Observable

class ProjectListProvider(private val rest: AmigoRest)
    : LivePagedListProvider<Int, SelectorItem>() {

    private val dataSource = object : TiledDataSource<SelectorItem>() {
        override fun countItems() =
                try {
                    rest.fetchProjects(1, 0).blockingGet().count
                } catch (e: Exception) {
                    0 /* return 0 items on error */
                }

        override fun loadRange(startPosition: Int, count: Int): MutableList<SelectorItem> =
                try {
                    rest.fetchProjects(count, startPosition)
                            .flatMapObservable { Observable.fromIterable(it.results) }
                            .map { SelectorItem(SelectorItem.Type.PROJECT, it.id, it.name, it.preview_image) }
                            .toList()
                            .blockingGet()
                } catch (e: Exception) {
                    Log.d("ProjectListProvider", "Error loading projects", e)
                    mutableListOf()
                }
    }

    override fun createDataSource() = dataSource
}


