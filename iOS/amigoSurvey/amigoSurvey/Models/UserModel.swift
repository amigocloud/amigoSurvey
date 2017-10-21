//
//  UserModel.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 8/31/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import ObjectMapper
import RxSwift

class UserModel: StaticMappable {
    
    var email: String = ""
    var id: CLong = 0
    var custom_id: String = ""
    var first_name: String = ""
    var last_name: String = ""
    var organization: String = ""
    var projectsURL: String = ""
    var visible_projects: [ProjectModel] = []
//    var selectedProjectId: CLong = 0
//    var selectedDatasetId: CLong = 0
    
    static func objectForMapping(map: Map) -> BaseMappable? {
        return UserModel()
    }
    
    func mapping(map: Map) {
        email <- map["email"]
        id <- map["id"]
        custom_id <- map["custom_id"]
        first_name <- map["first_name"]
        last_name <- map["last_name"]
        organization <- map["organization"]
        projectsURL <- map["projectsURL"]
        visible_projects <- map["visible_projects"]
//        selectedProjectId <- map["selectedProjectId"]
//        selectedDatasetId <- map["selectedDatasetId"]
    }
}

class UserViewModel {
   
    static func loadUser(useCache: Bool = true) -> Single<UserModel> {
        return Single.create { single in
            let user = Repository.sharedInstance.user
            if(!useCache || user.id == 0) {
                Repository.sharedInstance.fetchUser(urlStr: "https://www.amigocloud.com/api/v1/me", useCache: useCache)
                    .subscribe(onSuccess: { data in
                        single(.success(data))
                    })
            } else {
                single(.success(user))
            }
            return Disposables.create()
        }
    }
    
    static func getUserName() -> String {
        self.loadUser()
        let user = Repository.sharedInstance.user
        return "\(user.first_name) \(user.last_name)"
    }
    
    static func getUserInfoJSON() -> String {
        let user = Repository.sharedInstance.user
        var json: String = "{"
        json += "\"id\":" + String(user.id) + ","
        json += "\"custom_id\":\"" + user.custom_id + "\","
        json += "\"first_name\":\"" + user.first_name + "\","
        json += "\"last_name\":\"" + user.last_name + "\","
        json += "\"email\":\"" + user.email + "\","
        json += "\"projects\":\"" + user.projectsURL + "\","
        json += "\"organization\":\"" + user.organization + "\""
        json += "}";
        return json
    }
    
    static func findProject(projectId: CLong) -> ProjectModel? {
        for project in Repository.sharedInstance.user.visible_projects {
            if project.id == projectId {
                return project
            }
        }
        return nil
    }
    
}
