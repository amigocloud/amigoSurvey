package com.amigocloud.amigosurvey.form

import android.arch.lifecycle.ViewModel
import android.util.Log
import com.amigocloud.amigosurvey.models.RelatedRecord
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.repository.Repository
import com.amigocloud.amigosurvey.repository.SurveyConfig
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class ChunkedUploadResponse (var upload_id:String = "",
                                  var offset: Int = 0,
                                  var expires: String = "")

data class FileUploadProgress (
    var bytesSent: Int = 0,
    var bytesTotal: Long = 0,
    var message: String = "",
    var statusCode: Int = -1,
    var fileIndex: Int = 0,
    var filesTotal: Int = 0,
    var record: RelatedRecord? = null
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

@Singleton
class FileUploader @Inject constructor(private val rest: AmigoRest,
                                       private val repository: Repository,
                                       private val config: SurveyConfig): ViewModel() {

    fun uploadAllPhotos(projectId: Long, datasetId: Long): Observable<FileUploadProgress> {
        var index = 0
        val totalPhotos = repository.relatedRecordDao().all.size
        return getAllPhotos()
                .flatMap {
                    uploadPhoto(index++, it, projectId, datasetId, totalPhotos)
                }

    }

    fun deletePhoto(record: RelatedRecord) {
        repository.relatedRecordDao().delete(record)
    }

    fun getAllPhotos(): Observable<RelatedRecord> {
        return Observable.create {
            for (record in repository.relatedRecordDao().all) {
                it.onNext(record)
            }
        }
    }

    fun uploadPhoto(index: Int, record: RelatedRecord, projectId: Long, datasetId: Long, totalPhotos: Int): Observable<FileUploadProgress> {
        return rest.fetchRelatedTables(projectId, datasetId)
                .flatMapObservable {
                    Observable.fromIterable(it.results)
                }
                .filter {
                    it.id == record.relatedTableId.toLong()
                }
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

    fun chunkedFileUpload(url: String,
                          url_complete:String,
                          path: String,
                          record: RelatedRecord,
                          chunkSize: Int,
                          fileIndex: Int,
                          filesTotal: Int) : Observable<FileUploadProgress> {
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
                    FileUploadProgress(fileChunk.firstByte + fileChunk.chunkSize,
                            fileChunk.fileSize,
                            record.filename,
                            200,
                            fileIndex,
                            filesTotal,
                            fileChunk.record)
                }
    }

    fun getFileChunks(url: String, path: String, fname: String, chunkSize: Int, record: RelatedRecord): Single<List<FileChunk>> {
        return Single.create { emitter ->
            val file = File("$path/$fname")
            val totalBytes = file.length()
            var bytesRead = 0
            val chunkList = mutableListOf<FileChunk>()
            file.forEachBlock(blockSize = chunkSize, action = { buffer, count ->
                        bytesRead += count
                        val isLast = (bytesRead >= totalBytes)
                        chunkList.add(FileChunk(fname, url, buffer, bytesRead-count, count, totalBytes, record, isLast))
                    })
            emitter.onSuccess(chunkList)
        }
    }

    fun postFileChunkFirst(chunk: FileChunk): Single<ChunkedUploadResponse> {
        val body = RequestBody.create(MediaType.parse("image/*"), chunk.data, 0, chunk.chunkSize)

        val bodyMap = mutableMapOf(
                Pair("amigo_id", RequestBody.create(MediaType.parse("multipart/form-data"), chunk.record?.amigo_id!!)),
                Pair("source_amigo_id", RequestBody.create(MediaType.parse("multipart/form-data"), chunk.record?.source_amigo_id!!)),
                Pair("filename", RequestBody.create(MediaType.parse("multipart/form-data"), chunk.fileName)),
                Pair("datafile\"; filename=\"${chunk.fileName}", body))

        val headers = mapOf(
                Pair("Content-Range", "bytes ${chunk.firstByte}-${chunk.firstByte+chunk.chunkSize-1}/${chunk.fileSize}"))
        return rest.chunkedUploadFirst(
                chunk.urlStr,
                bodyMap,
                headers)
    }

    fun postFileChunk(upload_id: String, chunk: FileChunk): Single<ChunkedUploadResponse> {
        val body = RequestBody.create(MediaType.parse("image/*"), chunk.data, 0, chunk.chunkSize)

        val bodyMap = mutableMapOf(
                Pair("amigo_id", RequestBody.create(MediaType.parse("multipart/form-data"), chunk.record?.amigo_id!!)),
                Pair("source_amigo_id", RequestBody.create(MediaType.parse("multipart/form-data"), chunk.record?.source_amigo_id!!)),
                Pair("filename", RequestBody.create(MediaType.parse("multipart/form-data"), chunk.fileName)),
                Pair("upload_id", RequestBody.create(MediaType.parse("multipart/form-data"), upload_id)),
                Pair("datafile\"; filename=\"${chunk.fileName}", body))

        val headers = mapOf(
                Pair("Content-Range", "bytes ${chunk.firstByte}-${chunk.firstByte+chunk.chunkSize-1}/${chunk.fileSize}"))
        return rest.chunkedUpload(
                chunk.urlStr,
                bodyMap,
                headers)
    }

    fun postFileComplete(upload_id: String, md5:String, chunk: FileChunk): Single<ChunkedUploadResponse> {
        chunk.data?.let { data ->
            chunk.record?.let { record ->
                var headers = mapOf(Pair("Content-Type", chunk.ctype))
                return rest.chunkedUploadComplete(
                        chunk.urlStr,
                        upload_id,
                        md5,
                        record.source_amigo_id,
                        chunk.fileName,
                        headers)
            }
        }
        return Single.just(ChunkedUploadResponse())
    }

    fun createBodyForFileUpload(upload_id: String, md5: String, chunk: FileChunk): String {
        val body = StringBuffer()
        chunk.record?.let { record ->
            val boundaryPrefix = "--${chunk.boundary}\r\n"
            body.append(getContentDisposition(boundaryPrefix, "amigo_id", record.amigo_id))
            body.append(getContentDisposition(boundaryPrefix, "source_amigo_id", record.source_amigo_id))
            body.append(getContentDisposition(boundaryPrefix, "upload_id", upload_id))
            if(md5.isNotEmpty()) {
                body.append(getContentDisposition(boundaryPrefix, "md5", md5))
                body.append(getContentDisposition(boundaryPrefix, "filename", record.filename))
            }
            val mimeType = "image/*"
            body.append(boundaryPrefix)
            body.append("Content-Disposition: form-data; name=\"datafile\"; filename=\"${chunk.fileName}\"\r\n")
            body.append("Content-Type: $mimeType\r\n\r\n")

//            body.append(chunk.data.)

            chunk.data?.let {
                it.asSequence().forEachIndexed{ index, byte ->
                    if (index < chunk.chunkSize) {
                        Log.e("--- chunk ---", "'$byte', '${byte.toInt()}', '${byte.toChar()}'")
                        body.append(byte.toChar())
                    }
                }
            }
            body.append("\r\n")
            body.append("--${chunk.boundary}--")
        }
        return body.toString()
    }

    fun getContentDisposition(boundaryPrefix: String, key:String, value:String): String {
        var body = StringBuffer()
        body.append(boundaryPrefix)
        body.append("Content-Disposition: form-data; name=\"$key\"\r\n\r\n")
        body.append("$value\r\n")
        return body.toString()
    }

}
