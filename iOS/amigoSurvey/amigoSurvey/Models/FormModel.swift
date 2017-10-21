//
//  FormModel.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 8/31/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import ObjectMapper

class FormModel: StaticMappable {
    var base_form: String = ""
    var create_block_form: String = ""
    var create_block_json:  String = ""
    var edit_block_form: String = ""
    var edit_block_json:  String = ""
    
    static func objectForMapping(map: Map) -> BaseMappable? {
        return FormModel()
    }
    
    func mapping(map: Map) {
        base_form <- map["base_form"]
        create_block_form <- map["create_block_form"]
        create_block_json <- map["create_block_json"]
        edit_block_form <- map["edit_block_form"]
        edit_block_json <- map["edit_block_json"]
    }
}
