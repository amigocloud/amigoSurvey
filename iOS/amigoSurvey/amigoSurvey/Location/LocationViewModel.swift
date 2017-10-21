//
//  LocationModel.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 9/8/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import CoreLocation

class LocationModel {
    var lat: Double = 0
    var lng: Double = 0
    var alt: Double = 0
    var accuracy: Double = 0
    var bearing: Double = 0
    var speed: Double = 0
}

class LocationViewModel {
    
    static var lastLocation: LocationModel = LocationModel()
    
    static func updateLocation(location: CLLocation) {
        lastLocation.lat = location.coordinate.latitude
        lastLocation.lng = location.coordinate.longitude
        lastLocation.alt = location.altitude
        lastLocation.accuracy = location.horizontalAccuracy
        lastLocation.bearing = location.course
        lastLocation.speed = location.speed
    }
    
    static func getGPSInfoJSON() -> String {
        let pos = LocationModel()
        
        var json : String = ""
        json = "{"
        json += "\"gpsActive\":1,"
        json += "\"longitude\":" + String(pos.lng) + ","
        json += "\"latitude\":" + String(pos.lat) + ","
        json += "\"altitude\":" + String(pos.alt) + ","
        json += "\"horizontalAccuracy\":" + String(pos.accuracy) + ","
        json += "\"bearing\":" + String(pos.bearing) + ","
        json += "\"speed\":" + String(pos.speed)
        json += "}"
        return json
    }
    
    static func getLastLocationWKT() -> String {
        let lat = lastLocation.lat
        let lng = lastLocation.lng
        return "SRID=4326;POINT(\(lng) \(lat))"
    }
}
