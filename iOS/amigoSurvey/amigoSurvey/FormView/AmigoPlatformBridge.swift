//
//  AmigoPlatformBridge.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 9/3/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import UIKit
import JavaScriptCore
import RxSwift

@objc protocol AigoPlatformProtocol: JSExport {
    func ready()
    func onException(_ msg: String)
    func storeException(_ url: String, _ data: String)
    func getSchema() -> String?
    func setState(_ json: String)
    func getHTML(_ name: String) -> String?
    func getData() -> String?
    func refreshGeometry(_ wkt: String)
    func setCustomFieldValue(_ fieldName: String, _ fieldValue: String)
    func getRelatedTables(_ datasetID: String) -> String?
    func getFormDescription(_ datasetID: String, _ type: String) -> String?
    func getBlockHTML(_ formType: String) -> String?
    func getBlockHTMLWithId(_ formType: String, _ datasetID: String) -> String?
    func openUrl(_ url: String)
    func getDatasetInfo(_ datasetID: String) -> String?
    func getClientType() -> String?
    func getPageSize() -> String?
    func getDatasetRecords(_ datasetID: String, _ offset: String) -> String?
    func getDatasetRecordsWithFilter(_ datasetID: String, _ offset: String, _ filter: String) -> String?
    func getDatasetRecordsDistinctColumnsWithFilter(_ datasetID: String, _ offset: String, _ filter: String) -> String?
    func getDatasetRecordsWithFilterOrderBy(_ datasetID: String, _ offset: String, _ filter: String, _ orderBy: String) -> String?
    func getDatasetRecordsCountWithId(_ datasetID: String) -> String?
    func getDatasetRecordsCountWithIdAndFilter(_ datasetID: String, _ filter: String) -> String?
    func getRelatedRecordsCount(_ originalDatasetId: String, _ relatedDatasetId: String, _ sourceAmigoId: String) -> String?
    func getNewRowWithSourceId(_ datasetID: String, _ relatedColumn: String, _ relatedId: String) -> String?
    func getNewRow(_ datasetID: String) -> String?
    func getSchemaWithId(_ datasetID: String) -> String?
    func getUser() -> String?
    func getProject() -> String?
    func getGPSinfo() -> String?
    func editRowGeometry(_ json: String)
    func updateRow(_ originalJson: String, _ updateJson: String)
    func saveRow(_ json: String)
    func close()
    func deleteRows(_ json: String)
    func newRecord()
    func zoomToRows(_ json: String)
    func takePhoto(_ relatedTableId: String, _ amigoId: String)
    func scanBarcode(_ amigoId: String)
    func viewPhotos(_ relatedTableId: String, _ amigoId: String)
    func dataHasChanged(_ hasChanged: Bool)
    func writeRfidInfo(_ lat: String, _ lon: String, _ owner: String, _ asset_name: String)
    func getGeometryInfo(_ wkb: String) -> String?
    func setRfidWriteStatus(_ text: String, _ status: String) -> String?
    func getPermissionLevel() -> String?
}

@objc class AmigoPlatform: NSObject, AigoPlatformProtocol {
    var schema: String = ""
    
    var formVC: FormViewController?
    var webView: UIWebView?
    var formModel: FormModel?
    var name: String = ""
    var data: String = ""
    var disposeBag = DisposeBag()
    var isGeometrySet = false
    var amigo_id: String = ""
    
    // AmigoPlatform JS Bridge implementation
    
    func runJS(_ js: String) {
        DispatchQueue.main.async {
            self.webView?.stringByEvaluatingJavaScript(from: js)
        }
    }
    
    func submit() {
        print("submit()")
        if !isGeometrySet {
            setGeometryPosition()
        }
        runJS("javascript:Amigo.nativeSaveButton();")
    }
    
    func setGeometryPosition() {
        refreshGeometry(LocationViewModel.getLastLocationWKT())
        isGeometrySet = true
    }
    
    func ready() {
        print("ready()")
        let datasetId = SurveyConfig.getDatasetId()
        runJS("javascript:Amigo.loadBlock(AmigoPlatform.getHTML(), '\(self.name)', '\(datasetId!)', AmigoPlatform.getData());")
    }

    func onException(_ msg: String) {
        print("JS Exception: \(msg)")
    }
    
    func storeException(_ url: String, _ data: String) {
        print("JS storeException: \(data)")
    }
    
    func getSchema() -> String? {
        print("getSchema()")
        let datasetId = SurveyConfig.getDatasetId()
        if datasetId != nil {
            let schema = FormViewModel.getDatasetSchema(datasetId: datasetId!)
            return schema
        }
        return nil
    }

    func setState(_ json: String) {
        do {
            let buffer = [UInt8](json.utf8)
            let json_data = Data(bytes: buffer)
            let json = try JSONSerialization.jsonObject(with: json_data, options: []) as! [String: Any]
        } catch {
            NSLog("JSON parser failed")
        }
    }
    
    func getHTML(_ name: String) -> String? {
        if (self.name == "create_block") {
            return self.formModel?.create_block_form
        } else if (self.name == "edit_block") {
            return self.formModel?.edit_block_form
        }
        return nil
    }

    func getData() -> String? {
        return self.data
    }

    func refreshGeometry(_ wkt: String) {
        runJS("javascript:Amigo.refreshGeometry('\(wkt)');")
    }

    func setCustomFieldValue(_ fieldName: String, _ fieldValue: String) {
         runJS("javascript:Amigo.setCustomFieldValue(\(SurveyConfig.getDatasetId()!), '\(fieldName)', '\(fieldValue)')")
    }

    func getDataset(datasetId: String) -> DatasetModel? {
        var dataset : DatasetModel?
        let dataset_id = CLong(datasetId)
        if dataset_id != nil {
            dataset = DatasetViewModel.findDataset(datasetId: dataset_id!)
        }
        return dataset
    }
    
    func getRelatedTables(_ datasetID: String) -> String? {
        let dataset_id = CLong(datasetID)
        if dataset_id != nil {
            return DatasetViewModel.getRelatedTablesJSON(datasetId: dataset_id!)
        }
        return nil
    }
    
    func getFormDescription(_ datasetID: String, _ type: String) -> String? {
        if (type == "create") {
            return self.formModel?.create_block_json
        } else if (type == "edit") {
            return self.formModel?.edit_block_json
        }
        return nil
    }
    
    func getBlockHTML(_ formType: String) -> String? {
        return nil
    }
    
    func getBlockHTMLWithId(_ formType: String, _ datasetID: String) -> String? {
        return nil
    }
    
    func openUrl(_ url: String) {
    }
    
    func getDatasetInfo(_ datasetID: String) -> String? {
        let dataset = self.getDataset(datasetId: datasetID)
        let json = dataset?.toJSONString()
        return json
    }
    
    func getClientType() -> String? {
        return "mobile"
    }
    
    func getPageSize() -> String? {
        return "20"
    }
    
    func getDatasetRecords(_ datasetID: String, _ offset: String) -> String? {
        return nil
    }
    
    func getDatasetRecordsWithFilter(_ datasetID: String, _ offset: String, _ filter: String) -> String? {
        return nil
    }
    
    func getDatasetRecordsDistinctColumnsWithFilter(_ datasetID: String, _ offset: String, _ filter: String) -> String? {
        return nil
    }
    
    func getDatasetRecordsWithFilterOrderBy(_ datasetID: String, _ offset: String, _ filter: String, _ orderBy: String) -> String? {
        return nil
    }
    
    func getDatasetRecordsCountWithId(_ datasetID: String) -> String? {
        return nil
    }
    
    func getDatasetRecordsCountWithIdAndFilter(_ datasetID: String, _ filter: String) -> String? {
        return nil
    }
    
    func getRelatedRecordsCount(_ originalDatasetId: String, _ relatedDatasetId: String, _ sourceAmigoId: String) -> String? {
        return nil
    }
    
    func getNewRowWithSourceId(_ datasetID: String, _ relatedColumn: String, _ relatedId: String) -> String? {
        return nil
    }
    
    func getNewRow(_ datasetID: String) -> String? {
        self.amigo_id = FormViewModel.generateAmigoId()
        let json = DatasetViewModel.getNewRecordJSON(datasetId: CLong(datasetID)!, amigo_id: self.amigo_id)
        self.data = json!
        return json
    }
    
    func getSchemaWithId(_ datasetID: String) -> String? {
        var datasetId = CLong(datasetID)
        let schema = DatasetViewModel.getSchema(datasetId: datasetId!)
        return schema
    }
    
    func getUser() -> String? {
        return UserViewModel.getUserInfoJSON()
    }
    
    func getProject() -> String? {
        let history_dataset_id = DatasetViewModel.findHistoryDatasetId()
        let projectId = SurveyConfig.getProjectId()
        if projectId != nil {
        let project = Repository.sharedInstance.findProject(projectId: projectId!)
            if project != nil {
                return ProjectViewModel.projectAsJSON(project: project!, datasetId: history_dataset_id)
            }
        }
        return nil
    }
    
    func getGPSinfo() -> String? {
        let json = LocationViewModel.getGPSInfoJSON()
        return json
    }
    
    func editRowGeometry(_ json: String) {
        print("editRowGeometry")
        setGeometryPosition()
    }
    
    
    func refreshFormState() {
        print("refreshFormState")
        runJS("javascript:Amigo.refreshFormState();")
    }
    
    func checkForRowChange()
    {
        print("checkForRowChange")
        runJS("javascript:Amigo.utils.checkForRowChange();")
    }
    
    func back()
    {
        print("back")
        runJS("javascript:Amigo.historyBack();")
    }
    
    func saveCurrentRow()
    {
        print("saveCurrentRow")
        runJS("javascript:Amigo.saveCurrentRow();")
    }
    
    func deleteCurrentRow()
    {
        print("deleteCurrentRow")
        runJS("javascript:Amigo.deleteCurrentRow();")
    }
    
    func checkButtonPressed()
    {
        print("checkButtonPressed")
        runJS("javascript:Amigo.selectPrimaryAndBack();")
    }
    
    func mediaAdded()
    {
        print("mediaAdded")
        runJS("javascript:Amigo.mediaAdded();")
    }
    
    func updateRow(_ originalJson: String, _ updateJson: String) {
        print("updateRow")
        print(updateJson)
        ProjectViewModel.addNewRecord(rec: updateJson)?
            .subscribeOn(ConcurrentDispatchQueueScheduler(queue: DispatchQueue.global()))
            .observeOn(MainScheduler())
            .subscribe(onSuccess: { data in
                print("Done.")
                self.formVC?.dismiss(animated: true, completion: nil)
            }, onError: {error in
                print(error)
            })
            .addDisposableTo(FormViewController.disposeBag)
        
    }
    
    func saveRow(_ json: String) {
        print("saveRow")
        self.updateRow(self.data, json)
    }
    
    func close() {
        print("close()")
    }
    
    func deleteRows(_ json: String) {
    }
    
    func newRecord() {
        print("newRecord()")
    }
    
    func zoomToRows(_ json: String) {
    }
    
    func takePhoto(_ relatedTableId: String, _ amigoId: String) {
        formVC?.takePhoto(relatedTableId: relatedTableId, amigoId:amigoId)
    }
    
    func scanBarcode(_ amigoId: String) {
        print("scanBarcode()")
        formVC?.scanBarcode()
    }
    
    func viewPhotos(_ relatedTableId: String, _ amigoId: String) {
        print("viewPhotos()")
    }
    
    func dataHasChanged(_ hasChanged: Bool) {
        print("dataHasChanged \(hasChanged)")
        back()
    }
    
    func writeRfidInfo(_ lat: String, _ lon: String, _ owner: String, _ asset_name: String) {
    }
    
    func getGeometryInfo(_ wkb: String) -> String? {
        print("getGeometryInfo()")
        let latitude = LocationViewModel.lastLocation.lat
        let longitude = LocationViewModel.lastLocation.lng
        var centroid = "{\"centroid_latitude\":\(latitude),"
        centroid += "\"centroid_longitude\":\(longitude)}"
        return centroid
    }
    
    func setRfidWriteStatus(_ text: String, _ status: String) -> String? {
        return nil
    }
    
    func getPermissionLevel() -> String? {
        let projectId = SurveyConfig.getProjectId()
        if projectId != nil {
            let project = Repository.sharedInstance.findProject(projectId: projectId!)
            if project != nil {
                return ProjectViewModel.getPermissionLevel(project: project!)
            }
        }
        return nil
    }
}
