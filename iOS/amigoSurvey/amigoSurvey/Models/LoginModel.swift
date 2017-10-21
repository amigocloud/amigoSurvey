//
//  LoginModel.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 10/9/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import RxSwift

class LoginModelView {
    
    static func login(email: String, password: String) -> Single<Bool> {
        return AmigoRest.sharedInstance.login(username: email, password: password)
    }

}
