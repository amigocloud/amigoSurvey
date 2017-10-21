//
//  ProjectModel.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 9/1/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import ObjectMapper
import RxSwift

class ProjectModel: StaticMappable {
    
    var id: CLong = 0
    var name: String = ""
    var hash: String = ""
    var description: String = ""
    var organization: String = ""
    var permission_level: String = ""
    var datasets: [DatasetModel] = []
    var url: String = ""
    var datasetsURL: String = ""
    var submit_changesetURL: String = ""

    var preview_image: String = ""
    var preview_image_hash: String = ""

    var supportFilesHash: String = ""
    
    static func objectForMapping(map: Map) -> BaseMappable? {
        return ProjectModel()
    }
    
    func mapping(map: Map) {
        id <- map["id"]
        name <- map["name"]
        hash <- map["hash"]
        description <- map["description"]
        organization <- map["organization"]
        permission_level <- map["permission_level"]
        datasets <- map["datasets"]
        url <- map["url"]
        datasetsURL <- map["datasetsURL"]
        submit_changesetURL <- map["submit_changesetURL"]
        preview_image <- map["preview_image"]
        preview_image_hash <- map["preview_image_hash"]
        supportFilesHash <- map["supportFilesHash"]
    }
}

class ProjectViewModel {
 
   
    static func getDatasetsUrl() -> String {
        let baseURL = SurveyConfig.getBaseURL()
        let userId = SurveyConfig.getUserId()
        let projectId = SurveyConfig.getProjectId()
        let url = "\(baseURL)/api/v1/users/\(userId)/projects/\(projectId)/datasets?summary"
        return url
    }
    
    static func projectAsJSON(project: ProjectModel, datasetId: CLong) -> String {
        var json: String = ""
        json = "{"
        json += "\"id\":" + String(project.id) + ","
        json += "\"name\":\"" + project.name + "\","
        json += "\"description\":\"" + project.description + "\","
        json += "\"organization\":\"" + project.organization + "\","
        json += "\"history_dataset_id\":" + String(datasetId)
        json += "}"
        return json
    }
    
    static func getPermissionLevel(project: ProjectModel) -> String {
        return project.permission_level
    }
    
    static func getChangesetJSON(record: String, pm:ProjectModel) -> String {
        var json = "{\"type\": \"DML\",";
        json += "\"entity\": \"dataset_\(SurveyConfig.getDatasetId()!)\",";
        json += "\"action\": \"INSERT\",";
        json += "\"parent\": \"" + pm.hash + "\",";
        json += "\"data\": [";
        do {
            let buffer = [UInt8](record.utf8)
            let json_data = Data(bytes: buffer)
            let recj = try JSONSerialization.jsonObject(with: json_data, options: []) as? [String: Any]
            json += "{\"new\":{"
            let data = recj?["data"] as! NSArray
            var amigo_id = ""
            for r in data  {
                var f_count = 0
                for (field_name, value) in (r as? [String : Any])! {
                    if field_name == "amigo_id" {
                        amigo_id = value as! String
                        continue
                    }
                    if f_count > 0 {
                        json += ","
                    }
                    let typeString = String(describing: type(of: value))
                    
                    if typeString.lowercased().range(of:"string") != nil {
                        json += "\"\(field_name)\":\"\(value as! String)\""
                    } else if typeString.lowercased().range(of:"number") != nil {
                        json += "\"\(field_name)\":\(value as! NSNumber)"
                    } else if typeString.lowercased().range(of:"null") != nil {
                        json += "\"\(field_name)\":null"
                    }
                    f_count += 1
                }
            }
            json += "},\"amigo_id\":\"\(amigo_id)\"}"
        } catch {
            print("getChangesetJSON() Error: JSON parser failed.")
        }
        json += "]}";
        return json
    }
    
    static func escapeJSON(json: String) -> String {
        var output = ""
        for i in json.characters.indices {
            switch (json[i]) {
            case "\"":
                output += "\\\""
                break
            case "/":
                output += "\\/"
                break
            case "\n":
                output += "\\n"
                break
            case "\r":
                output += "\\r"
                break
            case "\t":
                output += "\\t"
                break
            case "\\":
                output += "\\\\"
                break
            default:
                output.append(json[i])
                break
            }
        }
        return output
    }
    
    static func addNewRecord(rec: String) -> Single<Data>? {
        let project = UserViewModel.findProject(projectId: SurveyConfig.getProjectId()!)
        if project != nil {
            let json = getChangesetJSON(record: rec, pm: project!)
            let changeset_json = "{\"changeset\":\"[" + escapeJSON(json: json) + "]\"}";
            return AmigoRest.sharedInstance.postOAuth(urlStr:(project?.submit_changesetURL)!, body: changeset_json, ctype: "application/json")
        }
        return nil
    }
    
}
