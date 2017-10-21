//
//  FormViewModel.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 9/13/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import RxSwift

class FormViewModel {
    
    static func getDatasetSchema(datasetId: CLong) -> String? {
        let ds = DatasetViewModel.findDataset(datasetId: datasetId)
        return ds?.schema
    }
    
    static func generateAmigoId() -> String {
        let uuid = UUID().uuidString
        return uuid.replacingOccurrences(of: "-", with: "")
    }
    
}
