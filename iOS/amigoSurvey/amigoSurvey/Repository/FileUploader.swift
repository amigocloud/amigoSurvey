//
//  FileUploader.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 10/15/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import RxSwift
import RealmSwift

class FileUploadProgress: NSObject {
    var bytesSent: Int = 0
    var bytesTotal: Int = 0
    var message: String = ""
    var statusCode: Int = -1
    var fileIndex: Int = 0
    var filesTotal: Int = 0
    override init() {}
    
    init(bytesSent:Int, bytesTotal: Int, message: String, statusCode: Int, fileIndex: Int, filesTotal: Int) {
        self.bytesSent = bytesSent
        self.bytesTotal = bytesTotal
        self.message = message
        self.statusCode = statusCode
        self.fileIndex = fileIndex
        self.filesTotal = filesTotal
    }
}

class FileChunk: NSObject {
    var urlStr: String = ""
    var fileName: String = ""
    var data: Data?
    var ctype: String = ""
    var boundary: String = ""
    var firstByte: Int = 0
    var chunkSize: Int = 0
    var fileSize: Int = 0
    var extra: [String:String] = [:]
    var last: Bool = false
    
    init(urlStr: String,
         fileName: String,
         data: Data?,
         firstByte: Int,
         chunkSize: Int,
         fileSize: Int,
         extra: [String:String]) {
        self.urlStr = urlStr
        self.fileName = fileName
        self.data = data
        self.firstByte = firstByte
        self.chunkSize = chunkSize
        self.fileSize = fileSize
        self.extra = extra
        self.boundary = "Boundary-\(UUID().uuidString)"
        self.ctype = "multipart/form-data; boundary=\(boundary)"
    }
}

class FileUploader {
    static let sharedInstance = FileUploader()

    func getAllPhotos() -> Observable<RelatedRecord> {
        return Observable.create { observer in
            let realm = try! Realm()
            let r = realm.objects(RelatedRecord.self)
            let totalFiles = r.count
            for i in 0..<r.count {
                let record: RelatedRecord = RelatedRecord()
                record.amigo_id = r[i].value(forKey: "amigo_id")! as! String
                record.filename = r[i].value(forKey: "filename")! as! String
                record.source_amigo_id = r[i].value(forKey: "source_amigo_id")! as! String
                record.datetime = r[i].value(forKey: "datetime")! as! String
                record.location = r[i].value(forKey: "location")! as! String
                record.relatedTableId = r[i].value(forKey: "relatedTableId")! as! String
                record.recordsTotal = r[i].value(forKey: "recordsTotal")! as! Int
                record.recordsTotal = totalFiles
                observer.on(.next(record))
            }
            observer.on(.completed)
            return Disposables.create()
        }
    }
    
    func uploadAllPhotos() -> Observable<FileUploadProgress> {
        var index: Int = 0
        return FileUploader.sharedInstance.getAllPhotos()
            .flatMap{ record -> Observable<FileUploadProgress> in
                index += 1
                return self.uploadPhoto(index: index, record: record)
            }
    }
    
    func deleteAllPhotos() {
        let realm = try! Realm()
        let r = realm.objects(RelatedRecord.self)
        for i in 0..<r.count {
            let filename = r[i].value(forKey: "filename")! as! String
            deleteFile(fname: filename)
        }
        try! realm.write {
            let deletedNotifications = realm.objects(RelatedRecord.self)
            realm.delete(deletedNotifications)
        }
    }
    
    func deletePhoto(fname: String) {
        let realm = try! Realm()
        let r = realm.objects(RelatedRecord.self).filter("filename=\"\(fname)\"")
        if r != nil {
            try! realm.write {
                realm.delete(r)
            }
            deleteFile(fname: fname)
        }
    }
    
    func deleteFile(fname: String) -> Bool {
        do {
            let fileManager = FileManager.default
            let filePath = "\(SurveyConfig.getPhotoDir())/\(fname)"
            
            // Check if file exists
            if fileManager.fileExists(atPath: filePath) {
                // Delete file
                try fileManager.removeItem(atPath: filePath)
            } else {
                print("::deleteFile() File \(fname) does not exist")
            }
        }
        catch let error as NSError {
            print("An error took place: \(error)")
            return false
        }
        return true
    }
    
    func uploadPhoto(index: Int, record: RelatedRecord) -> Observable<FileUploadProgress> {
        let rt_id = CLong(record.relatedTableId)
        if let rt_id = rt_id {
            var extra: [String:String] = [:]
            extra["amigo_id"] = record.amigo_id
            extra["source_amigo_id"] = record.source_amigo_id
            //            extra["datetime"] = record.datetime
            //            extra["location"] = "\"\(record.location)\""
            
            let project = Repository.sharedInstance.findProject(projectId: SurveyConfig.getProjectId()!)
            if let project = project {
                let dataset = Repository.sharedInstance.findDataset(datasetId: SurveyConfig.getDatasetId()!, project: project)
                if let dataset = dataset {
                    let rt = Repository.sharedInstance.findRelatedTable(related_table_id: rt_id, dataset: dataset)
                    if let rt = rt {
                        return FileUploader.sharedInstance.chunkedFileUpload(url: rt.chunked_upload,
                                                                              url_complete: rt.chunked_upload_complete,
                                                                              path: SurveyConfig.getPhotoDir(),
                                                                              fname: record.filename,
                                                                              relatedTableId: record.relatedTableId,
                                                                              extra: extra,
                                                                              chunkSize: 500000,
                                                                              fileIndex: index,
                                                                              filesTotal: record.recordsTotal)
                    }
                }
            }
        }
        return Observable<FileUploadProgress>.just(FileUploadProgress())
    }

    func createBodyForFileUpload(parameters: [String: String],
                                 boundary: String,
                                 data: Data?,
                                 mimeType: String,
                                 filename: String) -> Data {
        let body = NSMutableData()
        
        let boundaryPrefix = "--\(boundary)\r\n"
        
        for (key, value) in parameters {
            body.appendString(boundaryPrefix)
            body.appendString("Content-Disposition: form-data; name=\"\(key)\"\r\n\r\n")
            body.appendString("\(value)\r\n")
        }
        
        body.appendString(boundaryPrefix)
        body.appendString("Content-Disposition: form-data; name=\"datafile\"; filename=\"\(filename)\"\r\n")
        body.appendString("Content-Type: \(mimeType)\r\n\r\n")
        if data != nil {
            body.append(data!)
        }
        body.appendString("\r\n")
        body.appendString("--".appending(boundary.appending("--")))
        
        return body as Data
    }
    
    func paramsToUrlEncoded(params: [String:String]) -> String {
        var str = ""
        var count = 0
        for (key, value) in params {
            if count > 0 {
                str += "&"
            }
            str += "\(key)=\(value)"
            count += 1
        }
        return str
    }
    
    func chunkedFileUpload(url: String,
                           url_complete:String,
                           path: String,
                           fname: String,
                           relatedTableId: String,
                           extra: [String:String],
                           chunkSize: Int,
                           fileIndex: Int,
                           filesTotal: Int) -> Observable<FileUploadProgress> {
        var upload_id = ""
        var statusCode = -1
        return self.getFileChunks(url: url, path: path, fname: fname, chunkSize: chunkSize, extra: extra)
            .flatMap { chunk -> Observable<FileUploadProgress> in
                    if !chunk.last {
                        chunk.extra["upload_id"] = upload_id
                        upload_id = self.postFileChunk(chunk: chunk)
                        if upload_id == "" {
                            statusCode = -1
                        }
                    } else {
                        var params = extra
                        params["md5"] = "90affbd9a1954ec9ff029b7ad7183a16" // Bogus value
                        params["filename"] = fname
                        params["upload_id"] = upload_id
                        let data = self.paramsToUrlEncoded(params: params)
                        let escdata = data.addingPercentEncoding(withAllowedCharacters: .urlHostAllowed)!
                        chunk.data = escdata.data(using: String.Encoding.utf8)
                        chunk.ctype = "application/x-www-form-urlencoded"
                        chunk.urlStr = url_complete
                        statusCode = self.postFileComplete(chunk: chunk)
                    }
                    return Observable.from(FileUploadProgress(bytesSent: chunk.firstByte,
                                                              bytesTotal: chunk.fileSize,
                                                              message: chunk.fileName,
                                                              statusCode: statusCode,
                                                              fileIndex: fileIndex,
                                                              filesTotal: filesTotal))
                }
    }
    
    func getFileChunks(url: String, path: String, fname: String, chunkSize: Int, extra: [String:String]) -> Observable<FileChunk> {
        var lastChunk: FileChunk? = nil
        return Observable.create { observer in
            let fullPath = "\(path)/\(fname)"
            if let data = NSData(contentsOfFile: fullPath) {
                let fileSize = data.length
                var bytesRead: Int = 0
                var buffer: [UInt8] = Array(repeating: 0, count: fileSize)
                data.getBytes(&buffer, range: NSRange(location: 0, length: fileSize))
                repeat {
                    let bytesLeft = fileSize - bytesRead
                    var thisChunkSize = chunkSize
                    if bytesLeft < chunkSize {
                        thisChunkSize = bytesLeft
                    }
                    lastChunk = FileChunk(urlStr:url,
                                          fileName: fname,
                                          data: Data(buffer[bytesRead..<bytesRead+thisChunkSize]),
                                          firstByte: bytesRead,
                                          chunkSize: thisChunkSize,
                                          fileSize: fileSize,
                                          extra: extra)
                    if let lastChunk = lastChunk {
                        observer.on(.next(lastChunk))
                    }
                    bytesRead += thisChunkSize
                } while bytesRead < fileSize
                if let lastChunk = lastChunk {
                    lastChunk.last = true
                    observer.on(.next(lastChunk))
                }
                observer.on(.completed)
            }
            return Disposables.create()
        }
    }

    func postFileChunk(chunk: FileChunk) -> String  {
        var upload_id = ""
        let url = URL(string: chunk.urlStr)
        let config = URLSessionConfiguration.default
        let request = NSMutableURLRequest(url: url!)
        request.httpMethod = "POST"
        request.setValue("Bearer " + AmigoRest.sharedInstance.amigoToken.access_token, forHTTPHeaderField: "Authorization")
        request.setValue(chunk.ctype, forHTTPHeaderField: "Content-Type")
        let content_range = "bytes \(chunk.firstByte)-\(chunk.firstByte+chunk.chunkSize-1)/\(chunk.fileSize)"
        request.setValue(content_range, forHTTPHeaderField: "Content-Range")
        request.httpBody = self.createBodyForFileUpload(parameters: chunk.extra,
                                                        boundary: chunk.boundary,
                                                        data: chunk.data,
                                                        mimeType: "image/*",
                                                        filename: chunk.fileName)
        let semaphore = DispatchSemaphore(value: 0)
        let session = URLSession(configuration: config, delegate: AmigoRest.sharedInstance, delegateQueue: OperationQueue.current)
        let task = session.dataTask(with: request as URLRequest, completionHandler: {(data, response, error) in
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            if statusCode == 200 && data != nil {
                let json = try! JSONSerialization.jsonObject(with: data as! Data, options: []) as! [String: Any]
                upload_id = json["upload_id"] as! String
            } else {
//                print("Response: \(response.debugDescription)")
            }
            semaphore.signal()
            } as (Data?, URLResponse?, Error?) -> Void)
        task.resume()
        semaphore.wait(timeout: DispatchTime.distantFuture)
        return upload_id
    }
    
    func postFileComplete(chunk: FileChunk) -> Int {
        let url = URL(string: chunk.urlStr)
        let config = URLSessionConfiguration.default
        let request = NSMutableURLRequest(url: url!)
        request.httpMethod = "POST"
        request.setValue("Bearer " + AmigoRest.sharedInstance.amigoToken.access_token, forHTTPHeaderField: "Authorization")
        request.setValue(chunk.ctype, forHTTPHeaderField: "Content-Type")
        request.httpBody = chunk.data
        var statusCode = -1
        let semaphore = DispatchSemaphore(value: 0)
        let session = URLSession(configuration: config, delegate: AmigoRest.sharedInstance, delegateQueue: OperationQueue.current)
        let task = session.dataTask(with: request as URLRequest, completionHandler: {(data, response, error) in
            statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            semaphore.signal()
            } as (Data?, URLResponse?, Error?) -> Void)
        task.resume()
        semaphore.wait(timeout: DispatchTime.distantFuture)
        return statusCode
    }
 
}
