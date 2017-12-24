package com.amigocloud.amigosurvey.form

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.amigocloud.amigosurvey.models.DatasetModel
import com.amigocloud.amigosurvey.models.ProjectModel
import com.amigocloud.amigosurvey.models.RelatedRecord
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.repository.Repository
import com.amigocloud.amigosurvey.repository.SurveyConfig
import com.amigocloud.amigosurvey.util.getChangesetJson
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.processors.BehaviorProcessor
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File
import java.util.*
import javax.inject.Inject

data class ChunkedUploadResponse (var upload_id:String = "",
                                  var offset: Int = 0,
                                  var expires: String = "")

sealed class ProgressEvent(var message: String,
                           var index: Int,
                           var total: Int)

class FileUploadProgressEvent(
        message: String = "",
        index: Int = 0,
        total: Int = 0,
        val bytesSent: Int = 0,
        val bytesTotal: Long = 0
) : ProgressEvent(message, index, total)

class FileUploadCompleteEvent(message: String = "",
                              index: Int = 0,
                              total: Int = 0) : ProgressEvent(message, index, total)

class RecordsUploadProgressEvent(
        message: String = "",
        index: Int = 0,
        total: Int = 0): ProgressEvent(message, index, total)

class RecordsUploadCompleteEvent(message: String = ""): ProgressEvent(message, -1, 0)

data class RecordsUploadRequest(
        val records: List<FormRecord>,
        val project: ProjectModel,
        val dataset: DatasetModel
)

data class FileChunk (
        var fileName: String = "",
        var urlStr: String = "",
        var data: ByteArray? = null,
        var firstByte: Int = 0,
        var chunkSize: Int = 0,
        var fileSize: Long = 0,
        var record: RelatedRecord? = null,
        var last: Boolean = false,
        var boundary: String = "Boundary-${UUID.randomUUID()}",
        var ctype: String = "multipart/form-data; boundary=${boundary}"
)

class UploadViewModel(private val rest: AmigoRest,
                      private val repository: Repository,
                      private val config: SurveyConfig): ViewModel() {

    private val processor = BehaviorProcessor.create<RecordsUploadRequest>()

    val events: LiveData<ProgressEvent> = LiveDataReactiveStreams.fromPublisher(processor
            .flatMap { request -> submitRecords(request).toFlowable(BackpressureStrategy.LATEST) })

    fun submitAll(project: ProjectModel, dataset: DatasetModel) {
        val records = repository.formRecordDao().all
        val request = RecordsUploadRequest(records, project, dataset)
        processor.onNext(request)
    }

    private fun uploadPhotos(projectId: Long, datasetId: Long): Observable<ProgressEvent> {
        var index = 0
        val totalPhotos = repository.relatedRecordDao().all.size
        return if (totalPhotos == 0) {
            Observable.just(FileUploadCompleteEvent("No files to upload", -1, 0))
        } else {
            getAllPhotos().concatMap { uploadPhoto(index++, it, projectId, datasetId, totalPhotos) }
        }
    }

    fun deletePhoto(record: RelatedRecord) {
        repository.relatedRecordDao().delete(record)
    }

    private fun getAllPhotos(): Observable<RelatedRecord> {
        return Observable.create {
            for (record in repository.relatedRecordDao().all) {
                it.onNext(record)
            }
        }
    }

    fun getSavedPhotosNum(): Int {
        val records = repository.relatedRecordDao().all
        return records.count()
    }

    private fun uploadPhoto(index: Int, record: RelatedRecord, projectId: Long, datasetId: Long, totalPhotos: Int): Observable<ProgressEvent> {
        return rest.fetchRelatedTables(projectId, datasetId)
                .flatMapObservable { Observable.fromIterable(it.results) }
                .filter { it.id == record.relatedTableId.toLong() }
                .flatMap {
                    chunkedFileUpload(it.chunked_upload,
                            it.chunked_upload_complete,
                            config.photosDir,
                            record,
                            500000,
                            index,
                            totalPhotos)
                }
    }

    private fun chunkedFileUpload(url: String,
                          url_complete:String,
                          path: String,
                          record: RelatedRecord,
                          chunkSize: Int,
                          fileIndex: Int,
                          filesTotal: Int) : Observable<ProgressEvent> {
        val md5 = "90affbd9a1954ec9ff029b7ad7183a16" // Bogus value
        return getFileChunks(url, path, record.filename, chunkSize, record)
                .filter { it.isNotEmpty() }
                .flatMapSingle { chunks ->
                    postFileChunkFirst(chunks[0]).map { chunks.to(chunks[0].to(it)) }
                }
                .flatMapObservable { (chunks, firstChunk) ->
                    val uploadId = firstChunk.second.upload_id
                    val observables = mutableListOf<Observable<FileChunk>>(Observable.just(firstChunk.first))
                    var lastChunk : FileChunk? = null
                    chunks.forEachIndexed { index, chunk ->
                        if (chunk.last) lastChunk = chunk
                        if(index > 0) {
                            observables.add(postFileChunk(uploadId, chunk)
                                    .toObservable().map { chunk })
                        }
                    }
                    lastChunk?.let { lc ->
                        lc.urlStr = url_complete
                        lc.ctype = "application/x-www-form-urlencoded"
                        observables.add(postFileComplete(uploadId, md5, lc)
                                .toObservable().map { lc })

                        Observable.concat(observables)
                    }
                }
                .map { fileChunk ->
                    if ( (fileChunk.firstByte + fileChunk.chunkSize).toLong() == fileChunk.fileSize) {
                        fileChunk.record?.let { deletePhoto(it) }
                        FileUploadCompleteEvent("index:${fileIndex}, total:${filesTotal}, uploaded bytes:${fileChunk.firstByte + fileChunk.chunkSize}, fileSize:${fileChunk.fileSize}",
                                fileIndex,
                                filesTotal)
                    } else {
                        FileUploadProgressEvent(
                                record.filename,
                                fileIndex,
                                filesTotal,
                                fileChunk.firstByte + fileChunk.chunkSize,
                                fileChunk.fileSize)
                    }
                }
    }

    private fun getFileChunks(url: String, path: String, fname: String, chunkSize: Int, record: RelatedRecord): Single<List<FileChunk>> {
        return Single.create { emitter ->
            val file = File("$path/$fname")
            val totalBytes = file.length()
            var bytesRead = 0
            val chunkList = mutableListOf<FileChunk>()
            file.forEachBlock(blockSize = chunkSize, action = { buffer, count ->
                        bytesRead += count
                        val isLast = (bytesRead >= totalBytes)
                        chunkList.add(FileChunk(fname, url, buffer.clone(), bytesRead-count, count, totalBytes, record, isLast))
                    })
            emitter.onSuccess(chunkList)
        }
    }

    private fun postFileChunkFirst(chunk: FileChunk): Single<ChunkedUploadResponse> {
        val body = RequestBody.create(MediaType.parse("application/octet-stream"), chunk.data, 0, chunk.chunkSize)

        val bodyMap = mutableMapOf(
                Pair("amigo_id", RequestBody.create(MediaType.parse("multipart/form-data"), chunk.record?.amigo_id!!)),
                Pair("source_amigo_id", RequestBody.create(MediaType.parse("multipart/form-data"), chunk.record?.source_amigo_id!!)),
                Pair("filename", RequestBody.create(MediaType.parse("multipart/form-data"), chunk.fileName))
                ,Pair("datafile\"; filename=\"${chunk.fileName}", body)
        )

        val headers = mapOf(
                Pair("Content-Range", "bytes ${chunk.firstByte}-${chunk.firstByte+chunk.chunkSize-1}/${chunk.fileSize}"))
        return rest.chunkedUploadFirst(
                chunk.urlStr,
                bodyMap,
                headers)
    }

    private fun postFileChunk(upload_id: String, chunk: FileChunk): Single<ChunkedUploadResponse> {
        val body = RequestBody.create(MediaType.parse("application/octet-stream"), chunk.data, 0, chunk.chunkSize)

        val bodyMap = mutableMapOf(
                Pair("amigo_id", RequestBody.create(MediaType.parse("multipart/form-data"), chunk.record?.amigo_id!!)),
                Pair("source_amigo_id", RequestBody.create(MediaType.parse("multipart/form-data"), chunk.record?.source_amigo_id!!)),
                Pair("filename", RequestBody.create(MediaType.parse("multipart/form-data"), chunk.fileName)),
                Pair("upload_id", RequestBody.create(MediaType.parse("multipart/form-data"), upload_id))
                ,Pair("datafile\"; filename=\"${chunk.fileName}", body)
        )

        val headers = mapOf(
                Pair("Content-Range", "bytes ${chunk.firstByte}-${chunk.firstByte+chunk.chunkSize-1}/${chunk.fileSize}"))
        return rest.chunkedUpload(
                chunk.urlStr,
                bodyMap,
                headers)
    }

    private fun postFileComplete(upload_id: String, md5: String, chunk: FileChunk): Single<ChunkedUploadResponse> {
        chunk.record?.let { record ->
            return rest.chunkedUploadComplete(
                    chunk.urlStr,
                    upload_id,
                    md5,
                    record.source_amigo_id,
                    chunk.fileName,
                    mapOf(Pair("Content-Type", chunk.ctype)))
        }
        return Single.just(ChunkedUploadResponse())
    }

    private fun submitRecords(request: RecordsUploadRequest): Observable<ProgressEvent> {
        if(request.records.isEmpty()) {
            return Observable.just(RecordsUploadCompleteEvent("No Records to submit"))
        } else
            return Observable.fromIterable(request.records)
                    .zipWith(Observable.range(0, Int.MAX_VALUE), BiFunction { record:FormRecord, index:Int -> record.to(index)})
                    .concatMap { recWithIndex ->
                        val json = recWithIndex.first.json.getChangesetJson(request.project, request.dataset)
                        val body = RequestBody.create(MediaType.parse("application/json"),
                                "{\"changeset\":\"[${json}]\"}")
                        rest.submitChangeset(request.project.submit_changeset, body).map { recWithIndex }.toObservable()
                    }
                    .map { (rec:FormRecord, index:Int) ->
                        rec?.let { deleteSavedRecord(rec) }
                        if (index+1 < request.records.size) {
                            RecordsUploadProgressEvent("Submit record.", index, request.records.size)
                        } else {
                            RecordsUploadCompleteEvent("Submit all records done.")
                        }
                    }
                    .flatMap {
                        uploadPhotos(request.project.id, request.dataset.id)
                    }
    }

    fun saveRecord(rec: String) {
        try {
            repository.formRecordDao().insert(FormRecord(rec))
        } catch(e :Exception) {
            Log.e("RecordViewModel", e.toString())
        }
    }

    fun getSavedRecordsNum(): Int {
        val records = repository.formRecordDao().all
        return records.count()
    }

    fun deleteSavedRecord(record: FormRecord) {
        repository.formRecordDao().delete(record)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val rest: AmigoRest,
                                      private val repository: Repository,
                                      private val config: SurveyConfig) : ViewModelFactory<UploadViewModel>() {

        override val modelClass = UploadViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(this.modelClass)) {
                return UploadViewModel(rest, repository, config) as T
            }
            throw IllegalArgumentException(INFLATION_EXCEPTION)
        }
    }
}
