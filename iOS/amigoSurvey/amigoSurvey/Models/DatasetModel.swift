//
//  DatasetModel.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 9/3/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import ObjectMapper

class DatasetModel: StaticMappable {
    
    var id: CLong = 0
    var name: String = ""
    var boundingbox: String = ""
    var tiles: String = ""
    var url: String = ""
    var visible: Bool = false
    var type: String = ""
    var preview_image: String = ""
    var preview_image_hash: String = ""
    var read_only: Bool = false
    var online_only: Bool = false
    var display_field: String = ""
    var auto_sync: Bool = false
    var table_name: String = ""
    var master_state: String = ""
    var schemaURL: String = ""
    var submit_changeURL: String = ""
    var forms_summaryURL: String = ""
    
    var schema: String = ""
    var schemaHash: String = ""
    var schema_fields: [String] = []
    
    var related_tablesURL: String = ""
    var related_tables: [RelatedTableModel] = []

    var formModel = FormModel()

    static func objectForMapping(map: Map) -> BaseMappable? {
        return DatasetModel()
    }
    
    func mapping(map: Map) {
        id <- map["id"]
        name <- map["name"]
        boundingbox <- map["boundingbox"]
        tiles <- map["tiles"]
        url <- map["url"]
        visible <- map["visible"]
        type <- map["type"]
        preview_image <- map["preview_image"]
        preview_image_hash <- map["preview_image_hash"]
        read_only <- map["read_only"]
        online_only <- map["online_only"]
        display_field <- map["display_field"]
        auto_sync <- map["auto_sync"]
        table_name <- map["table_name"]
        master_state <- map["master_state"]
        schemaURL <- map["schemaURL"]
        submit_changeURL <- map["submit_changeURL"]
        schema <- map["schema"]
        schemaHash <- map["schemaHash"]
        schema_fields <- map["schema_fields"]
        related_tablesURL <- map["related_tablesURL"]
        related_tables <- map["related_tables"]
        forms_summaryURL <- map["forms_summaryURL"]
        formModel <- map["formModel"]
    }
}


class DatasetViewModel {
    
    static func findDataset(datasetId: CLong) -> DatasetModel? {
        let projectId = SurveyConfig.getProjectId()
        if projectId != nil {
            let project = Repository.sharedInstance.findProject(projectId: projectId!)
            if project != nil {
                for ds in project!.datasets {
                    if ds.id == datasetId {
                        return ds
                    }
                }
            }
        }
        return nil
    }
    
    static func getSchema(datasetId: CLong) -> String? {
        let ds = findDataset(datasetId: datasetId)
        if ds != nil {
            var schema = "["
            var count = 0
            for field in (ds?.schema_fields)! {
                if count > 0 {
                    schema += ","
                }
                schema += field
                count += 1
            }
            schema += "]"
            return schema
        }
        return nil
    }
    
    static func getCustomFieldName(custom_type:String, datasetId: CLong) -> String? {
        let ds = findDataset(datasetId: datasetId)
        if ds != nil {
            var schema = "["
            var count = 0
            for field in (ds?.schema_fields)! {
                let field = try! JSONSerialization.jsonObject(with: field.data(using: .utf8)!, options: []) as? [String: Any]
                let ct = field?["custom_type"] as? String
                let name = field?["name"] as? String
                if ct == custom_type {
                    return name
                }
            }
        }
        return nil
    }
    
    static func getNewRecordJSON(datasetId: CLong, amigo_id: String) -> String? {
        let ds = DatasetViewModel.findDataset(datasetId: datasetId)
        if ds != nil {
            var json: String = "{\"count\":1,\"columns\":["
            var count = 0
            for f in (ds?.schema_fields)! {
                if count > 0 {
                    json += ","
                }
                json += f
                count += 1
            }
            json += "],\"data\":["
            json +=  "{\"amigo_id\":\"" + amigo_id + "\"}],";
            json += "\"is_new\": true}";
            return json
        }
        return nil
    }
    
    static func getRelatedTablesJSON(datasetId: CLong) -> String {
        let ds = DatasetViewModel.findDataset(datasetId: datasetId)
        var json: String = "["
        var count = 0
        for rt in (ds?.related_tables)! {
            if count > 0 {
                json += ","
            }
            json += "{\"id\":" + String(rt.id) + ", \"name\":\"" + rt.name + "\"}";
            count += 1
        }
        
        json += "]"
        return json
    }
    
    static func findHistoryDatasetId() -> CLong {
        let projectId = SurveyConfig.getProjectId()
        if projectId != nil {
            let project = Repository.sharedInstance.findProject(projectId: projectId!)
            if project != nil {
                for ds in project!.datasets {
                    if ds.type == "r_history" && ds.name == "record_history" {
                        return ds.id
                    }
                }
            }
        }
        return -1
    }
    
}
