//
//  RelatedTableModel.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 10/12/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import ObjectMapper
import RealmSwift
import RxSwift

class RelatedTableModel: StaticMappable {
    var id: CLong = 0
    var name: String = ""
    var chunked_upload: String = ""
    var chunked_upload_complete: String = ""
    var schema: String = ""
    var table_name: String = ""
    var type: String = ""
    
    static func objectForMapping(map: Map) -> BaseMappable? {
        return RelatedTableModel()
    }
    
    func mapping(map: Map) {
        id <- map["id"]
        name <- map["name"]
        chunked_upload <- map["chunked_upload"]
        chunked_upload_complete <- map["chunked_upload_complete"]
        schema <- map["schema"]
        table_name <- map["table_name"]
        type <- map["type"]
    }
}

class RelatedRecord: Object {
    var filename: String = ""
    var source_amigo_id: String = ""
    var datetime: String = ""
    var location: String = ""
    var amigo_id: String = ""
    var relatedTableId: String = ""
    var recordsTotal: Int = 0
}

class RelatedTableViewModel {
    
    static func savePhotoRecord(filename:String, source_amigo_id:String, relatedTableId: String) {
        let rec: RelatedRecord = RelatedRecord()
        rec.filename = filename
        rec.source_amigo_id = source_amigo_id
        rec.datetime = NSDate().description
        rec.location = LocationViewModel.getLastLocationWKT()
        rec.amigo_id = FormViewModel.generateAmigoId()
        rec.relatedTableId = relatedTableId
        
        let realm = try! Realm()
        try! realm.write {
            realm.add(rec)
        }
    }
    
    static func uploadAllPhotos() -> Observable<FileUploadProgress> {
        return FileUploader.sharedInstance.uploadAllPhotos()
    }
    
    static func deleteAllPhotos() {
        FileUploader.sharedInstance.deleteAllPhotos()
    }

}
