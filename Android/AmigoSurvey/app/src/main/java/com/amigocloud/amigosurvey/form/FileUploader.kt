package com.amigocloud.amigosurvey.form

import android.arch.lifecycle.ViewModel
import com.amigocloud.amigosurvey.models.ProjectModel
import com.amigocloud.amigosurvey.models.RelatedRecord
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.repository.Repository
import com.amigocloud.amigosurvey.repository.SurveyConfig
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import io.reactivex.Observable
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


data class FileUploadProgress (
    var bytesSent: Int = 0,
    var bytesTotal: Long = 0,
    var message: String = "",
    var statusCode: Int = -1,
    var fileIndex: Int = 0,
    var filesTotal: Int = 0 )

data class FileChunk (
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
        var index = -1
        val totalPhotos = repository.relatedRecordDao().all.size
        return getAllPhotos()
                .flatMap {
                    index++
                    uploadPhoto(index, it, projectId, datasetId, totalPhotos)
                }

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

    fun chunkedFileUpload(url: String,
                          url_complete:String,
                          path: String,
                          record: RelatedRecord,
                          chunkSize: Int,
                          fileIndex: Int,
                          filesTotal: Int) : Observable<FileUploadProgress> {

        return getFileChunks(url, path, record.filename, chunkSize, record)
                .flatMap { chunk ->

                    Observable.just(FileUploadProgress(chunk.firstByte,
                            chunk.fileSize,
                            record.filename,
                            200,
                            fileIndex,
                            filesTotal))
                }
    }

    fun getFileChunks(url: String, path: String, fname: String, chunkSize: Int, record: RelatedRecord): Observable<FileChunk> {
        return Observable.create { observable ->
            val file = File("${path}/${fname}")
            val totalBytes = file.length()
            var bytesRead = 0;
            file.forEachBlock(blockSize = chunkSize, action = { buffer, count ->
                        bytesRead += count
                        val last = (bytesRead >= totalBytes)
                        observable.onNext(FileChunk(url,buffer, bytesRead-count, chunkSize, totalBytes, record, last = last))
                    })
        }
    }

}
