package com.amigocloud.amigosurvey.form

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.ViewModel
import com.amigocloud.amigosurvey.models.RelatedRecord
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.repository.Repository
import com.amigocloud.amigosurvey.repository.SurveyConfig
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import com.squareup.moshi.Moshi
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class ChunkedUploadResponse (var upload_id:String = "",
                                  var offset: Int = 0,
                                  var expires: String = "")

sealed class FileProgressEvent(val message: String,
                               val fileIndex: Int,
                               val filesTotal: Int,
                               val record: RelatedRecord?)

class FileUploadProgressEvent(
        message: String = "",
        fileIndex: Int = 0,
        filesTotal: Int = 0,
        record: RelatedRecord? = null,
        val bytesSent: Int = 0,
        val bytesTotal: Long = 0
) : FileProgressEvent(message, fileIndex, filesTotal, record)

class FileUploadCompleteEvent(message: String = "",
                              fileIndex: Int = 0,
                              filesTotal: Int = 0,
                              record: RelatedRecord? = null) : FileProgressEvent(message, fileIndex, filesTotal, record)

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

class FileUploader(private val rest: AmigoRest,
                   private val repository: Repository,
                   private val config: SurveyConfig): ViewModel() {

    private val processor = BehaviorProcessor.create<Pair<Long, Long>>()

    val events: LiveData<FileProgressEvent> = LiveDataReactiveStreams.fromPublisher(processor
            .flatMap { (pId, dId) -> uploadPhotos(pId, dId).toFlowable(BackpressureStrategy.LATEST) })

    fun uploadAllPhotos(projectId: Long, datasetId: Long) {
        processor.onNext(projectId.to(datasetId))
    }

    private fun uploadPhotos(projectId: Long, datasetId: Long): Observable<FileProgressEvent> {
        var index = 0
        val totalPhotos = repository.relatedRecordDao().all.size
        return if (totalPhotos == 0) {
            Observable.just(FileUploadCompleteEvent("No files to upload", -1, 0, null))
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

    private fun uploadPhoto(index: Int, record: RelatedRecord, projectId: Long, datasetId: Long, totalPhotos: Int): Observable<FileProgressEvent> {
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
                          filesTotal: Int) : Observable<FileProgressEvent> {
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
                        FileUploadCompleteEvent("fileIndex:${fileIndex}, filesTotal:${filesTotal}, uploaded bytes:${fileChunk.firstByte + fileChunk.chunkSize}, fileSize:${fileChunk.fileSize}",
                                fileIndex,
                                filesTotal,
                                fileChunk.record)
                    } else {
                        FileUploadProgressEvent(
                                record.filename,
                                fileIndex,
                                filesTotal,
                                fileChunk.record,
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

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val rest: AmigoRest,
                                      private val repository: Repository,
                                      private val config: SurveyConfig) : ViewModelFactory<FileUploader>() {

        override val modelClass = FileUploader::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(this.modelClass)) {
                return FileUploader(rest, repository, config) as T
            }
            throw IllegalArgumentException(INFLATION_EXCEPTION)
        }
    }
}
