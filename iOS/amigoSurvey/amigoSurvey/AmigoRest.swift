//
//  AmigoRest.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 9/15/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import RxSwift
import ObjectMapper

struct AmigoClient {
    static let client_id = "5996bb4af375491b3d95"
    static let client_secret = "d4235bc6fd279ad93e3afc4f53d650c820d1b97f"
    static let base_url = "https://www.amigocloud.com"
    static let api_prefix = "/api/v1/"
    static let oauth = "oauth2/access_token"
}

extension NSMutableData {
    func appendString(_ string: String) {
        let data = string.data(using: String.Encoding.utf8, allowLossyConversion: false)
        append(data!)
    }
}

class AmigoToken: StaticMappable {
    var access_token : String = ""
    var token_type: String = ""
    var expires_in: CLong = 0
    var refresh_token: String = ""
    var scope: String = ""
    var created_at: Double
    
    init() {
        created_at = Date().timeIntervalSince1970
    }
    
    func parse(data: Data) {
        do {
            let json = try JSONSerialization.jsonObject(with: data, options: []) as! [String: Any]
            access_token = json["access_token"] as! String
            token_type = json["token_type"] as! String
            expires_in = json["expires_in"] as! CLong
            refresh_token = json["refresh_token"] as! String
            scope = json["scope"] as! String
            created_at = Date().timeIntervalSince1970
            AmigoToken.save(amigoToken: self)
        } catch {
            NSLog("JSON parser failed")
        }
    }
    
    func isValid() -> Bool {
        let current_time = Date().timeIntervalSince1970
        let expires_at = created_at + Double(expires_in)
        if  expires_at > current_time {
            return true
        }
        return false
    }
    
    static func objectForMapping(map: Map) -> BaseMappable? {
        return AmigoToken()
    }
    
    func mapping(map: Map) {
        access_token <- map["access_token"]
        token_type <- map["token_type"]
        expires_in <- map["expires_in"]
        refresh_token <- map["refresh_token"]
        scope <- map["scope"]
        created_at <- map["created_at"]
    }
    
    static func save(amigoToken: AmigoToken) {
        let json = amigoToken.toJSONString()
        if (json != nil) {
            SurveyConfig.setAmigoTokenJSON(json: json!)
        }
    }
    
    static func restore() -> AmigoToken {
        let json = SurveyConfig.getAmigoTokenJSON()
        if (json != nil) {
            return Mapper<AmigoToken>().map(JSONString: json!)!
        }
        return AmigoToken()
    }
    
}

class AmigoRest: NSObject, URLSessionDelegate {
    static let sharedInstance = AmigoRest()
    var amigoToken = AmigoToken.restore()
    var disposeBag = DisposeBag()
    
    func login(username: String, password: String) -> Single<Bool> {
        let ctype = "application/x-www-form-urlencoded"
        let data =
            "client_id=" + AmigoClient.client_id  +
                "&client_secret=" + AmigoClient.client_secret +
                "&grant_type=password" +
                "&username=" + username +
                "&password=" + password
        let url = AmigoClient.base_url + AmigoClient.api_prefix + AmigoClient.oauth
        return self.post(urlStr: url, body: data, ctype: ctype)
            .map {data in
                self.amigoToken.parse(data: data)
                return true
        }
    }
    
    func refresh_token(token: AmigoToken) -> Single<AmigoToken>  {
        let ctype = "application/x-www-form-urlencoded"
        let data =
            "client_id=" + AmigoClient.client_id  +
                "&client_secret=" + AmigoClient.client_secret +
                "&grant_type=refresh_token" +
                "&refresh_token=" + token.refresh_token
        let url = AmigoClient.base_url + AmigoClient.api_prefix + AmigoClient.oauth
        return self.post(urlStr: url, body: data, ctype: ctype)
            .map { data in
                self.amigoToken.parse(data: data)
                return self.amigoToken
        }
    }
    
    func getJSON(urlStr: String) -> Single<[String: Any]?> {
        return self.get(urlStr: urlStr)
            .map { data in
                do {
                    return try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
                } catch {
                    throw AmigoError.JSONError("Form json parsing failed.")
                }
        }
    }
    
    func get(urlStr: String) -> Single<Data> {
        return Single.create { single in
            let url = URL(string: urlStr)
            let config = URLSessionConfiguration.default
            let request = NSMutableURLRequest(url: url!)
            request.httpMethod = "GET"
            request.setValue("Bearer " + self.amigoToken.access_token, forHTTPHeaderField: "Authorization")
            let session = URLSession(configuration: config, delegate: self, delegateQueue: OperationQueue.current)
            let task = session.dataTask(with: request as URLRequest, completionHandler: {(data, response, error) in
                let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
                if statusCode == 200 && data != nil {
                    single(.success(data as! Data))
                } else {
                    single(.error(NSError(domain: "GET failed", code: statusCode)))
                }
            } as (Data?, URLResponse?, Error?) -> Void)
            task.resume()
            return Disposables.create()
        }
    }
    
    func postOAuth(urlStr: String, body: String, ctype: String) -> Single<Data> {
        return Single.create { single in
            let url = URL(string: urlStr)
            let config = URLSessionConfiguration.default
            let request = NSMutableURLRequest(url: url!)
            request.httpMethod = "POST"
            request.setValue("Bearer " + self.amigoToken.access_token, forHTTPHeaderField: "Authorization")
            request.httpBody = body.data(using: String.Encoding.utf8)
            request.setValue(ctype, forHTTPHeaderField: "Content-Type")
            let session = URLSession(configuration: config, delegate: self, delegateQueue: OperationQueue.current)
            let task = session.dataTask(with: request as URLRequest, completionHandler: {(data, response, error) in
                let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
                if statusCode == 200 && data != nil {
                    single(.success(data as! Data))
                } else {
                    single(.error(NSError(domain: "POST", code: statusCode)))
                }
                } as (Data?, URLResponse?, Error?) -> Void)
            task.resume()
            return Disposables.create()
        }
    }
    
    func post(urlStr: String, body: String, ctype: String) -> Single<Data> {
        return Single.create { single in
            let url = URL(string: urlStr)
            let config = URLSessionConfiguration.default
            let request = NSMutableURLRequest(url: url!)
            request.httpMethod = "POST"
            request.httpBody = body.data(using: String.Encoding.utf8)
            request.setValue(ctype, forHTTPHeaderField: "Content-Type")
            let session = URLSession(configuration: config, delegate: self, delegateQueue: OperationQueue.current)
            let task = session.dataTask(with: request as URLRequest, completionHandler: {(data, response, error) in
                let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
                if statusCode == 200 && data != nil {
                    single(.success(data as! Data))
                } else {
                    single(.error(NSError(domain: "POST", code: statusCode)))
                }
            } as (Data?, URLResponse?, Error?) -> Void)
            task.resume()
            return Disposables.create()
        }
    }
    
    public func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Swift.Void) {
        
        if challenge.protectionSpace.authenticationMethod == (NSURLAuthenticationMethodServerTrust) {
            
            let serverTrust:SecTrust = challenge.protectionSpace.serverTrust!
            let certificate: SecCertificate = SecTrustGetCertificateAtIndex(serverTrust, 0)!
            let remoteCertificateData = CFBridgingRetain(SecCertificateCopyData(certificate))!
            let cerPath: String = Bundle.main.path(forResource: "ca-bundle", ofType: "crt")!
            let localCertificateData = NSData(contentsOfFile: cerPath)!
            
            if true || (remoteCertificateData.isEqual(localCertificateData) == true) {
                let credential:URLCredential = URLCredential(trust: serverTrust)
                
                challenge.sender?.use(credential, for: challenge)
                
                completionHandler(URLSession.AuthChallengeDisposition.useCredential, URLCredential(trust: challenge.protectionSpace.serverTrust!))
                
            } else {
                
                completionHandler(URLSession.AuthChallengeDisposition.cancelAuthenticationChallenge, nil)
            }
        } else
        {
            completionHandler(URLSession.AuthChallengeDisposition.cancelAuthenticationChallenge, nil);
        }
    }
    
}
