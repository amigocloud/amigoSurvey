//
//  SupportFilesModel.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 9/1/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import ObjectMapper

class SupportFilesModel: StaticMappable {
    
    var zipURL: String = ""
    var supportFilesHash: String = ""
    
    static func objectForMapping(map: Map) -> BaseMappable? {
        return SupportFilesModel()
    }
    
    func mapping(map: Map) {
        zipURL <- map["zipURL"]
        supportFilesHash <- map["supportFilesHash"]
    }
}

class SupportFilesViewModel {
        
}
