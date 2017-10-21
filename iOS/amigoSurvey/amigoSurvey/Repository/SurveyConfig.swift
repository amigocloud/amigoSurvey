//
//  SurveyConfig.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 9/1/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation

struct ConfigKey {
    static let email = "email_key"
    static let password = "password_key"
    static let loggedin = "loggedin_key"
    static let datasetSelected = "datasetSelected_key"
    static let token = "token_key"
    static let userId = "user_id_key"
    static let projectId = "project_id_key"
    static let datasetId = "dataset_id_key"
    static let baseURL = "base_url_key"
    static let userModel = "UserModel_key"
    static let amigoToken = "amigoToken_key"
}

class SurveyConfig {
    static let userDefaults = UserDefaults.standard
    
    static func isKeyPresent(key: String) -> Bool {
        return UserDefaults.standard.object(forKey: key) != nil
    }
    
    static func getString(key:String) -> String {
        if isKeyPresent(key: key) {
            return userDefaults.string(forKey: key)!
        }
        return ""
    }

    static func getInt(key:String) -> CLong? {
        if isKeyPresent(key: key) {
            return userDefaults.integer(forKey: key)
        }
        return nil
    }
    
    static func isKeyPresentInUserDefaults(key: String) -> Bool {
        return UserDefaults.standard.object(forKey: key) != nil
    }

    static func setEmail(email: String) {
        userDefaults.set(email, forKey: ConfigKey.email)
    }
    
    static func getEmail() -> String {
        return getString(key: ConfigKey.email)
    }

    static func setPassword(password: String) {
        userDefaults.set(password, forKey: ConfigKey.password)
    }
    
    static func getPassword() -> String {
        return getString(key: ConfigKey.password)
    }

    static func setLoggedin(loggedin: Bool) {
        userDefaults.set(loggedin, forKey: ConfigKey.loggedin)
    }
    
    static func isLoggedin() -> Bool {
        return userDefaults.bool(forKey: ConfigKey.loggedin)
    }

    static func setDatasetSelected(selected: Bool) {
        userDefaults.set(selected, forKey: ConfigKey.datasetSelected)
    }
    
    static func isDatasetSelected() -> Bool {
        return userDefaults.bool(forKey: ConfigKey.datasetSelected)
    }
    
    static func setToken(token: String) {
        userDefaults.set(token, forKey: ConfigKey.token)
    }

    static func setUserId(id: CLong) {
        userDefaults.set(id, forKey: ConfigKey.userId)
    }
    
    static func getUserId() -> CLong? {
        return getInt(key: ConfigKey.userId)
    }

    static func setProjectId(id: CLong) {
        userDefaults.set(id, forKey: ConfigKey.projectId)
    }
    
    static func getProjectId() -> CLong? {
        return getInt(key: ConfigKey.projectId)
    }
    
    static func setDatasetId(id: CLong) {
        userDefaults.set(id, forKey: ConfigKey.datasetId)
        setDatasetSelected(selected: true)
    }
    
    static func getDatasetId() -> CLong? {
        return getInt(key: ConfigKey.datasetId)
    }
    
    static func mkdir(dirname: String) -> Bool {
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let dataPath = documentsDirectory.appendingPathComponent(dirname)
        
        do {
            try FileManager.default.createDirectory(atPath: dataPath.path, withIntermediateDirectories: true, attributes: nil)
        } catch let error as NSError {
            print("Error creating directory: \(error.localizedDescription)")
            return false
        }
        return true
    }
    
    static func getWebFormDir() -> String {
        let documentsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0]
        mkdir(dirname: "webform")
        return documentsPath  + "/webform"
    }

    static func getPhotoDirName() -> String {
        mkdir(dirname: "photos")
        return "photos"
    }
    
    static func getPhotoDir() -> String {
        let documentsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0]
        mkdir(dirname: getPhotoDirName())
        return documentsPath  + "/\(getPhotoDirName())"
    }
    
    static func setBaseURL(url: String) {
        userDefaults.set(url, forKey: ConfigKey.baseURL)
    }
    
    static func getBaseURL() -> String {
        return getString(key: ConfigKey.baseURL)
    }

    static func setUserJSON(json: String) {
        userDefaults.set(json, forKey: ConfigKey.userModel)
    }
    
    static func getUserJSON() -> String? {
        if SurveyConfig.isKeyPresentInUserDefaults(key: ConfigKey.userModel) {
            return userDefaults.string(forKey: ConfigKey.userModel)!
        } else {
            return nil
        }
    }

    static func setAmigoTokenJSON(json: String) {
        userDefaults.set(json, forKey: ConfigKey.amigoToken)
    }
    
    static func getAmigoTokenJSON() -> String? {
        if SurveyConfig.isKeyPresentInUserDefaults(key: ConfigKey.amigoToken) {
            return userDefaults.string(forKey: ConfigKey.amigoToken)!
        } else {
            return nil
        }
    }

}
