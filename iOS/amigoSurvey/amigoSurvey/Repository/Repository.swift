//
//  Repository.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 9/4/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import ObjectMapper
import RxSwift
import Zip

enum AmigoError : Error {
    case JSONError(String)
    case UNZIPError(String)
}

class Repository {
    
    static let sharedInstance = Repository()
    var user: UserModel = UserModel()
    
    func findProject(projectId: CLong) -> ProjectModel? {
        for project in self.user.visible_projects {
            if project.id == projectId {
                return project
            }
        }
        return nil
    }

    func findDataset(datasetId: CLong, project: ProjectModel) -> DatasetModel? {
        for dataset in project.datasets {
            if dataset.id == datasetId {
                return dataset
            }
        }
        return nil
    }

    func findRelatedTable(related_table_id: CLong, dataset: DatasetModel) -> RelatedTableModel? {
        for rt in dataset.related_tables {
            if rt.id == related_table_id {
                return rt
            }
        }
        return nil
    }
    
    func fetchUser(urlStr: String, useCache: Bool = false) -> Single<UserModel> {
        return Single.create { single in
            
        if useCache {
            let savedUser: UserModel? = self.restore()
            if savedUser != nil {
                self.user = savedUser!
                single( .success(savedUser!) )
                return Disposables.create()
            }
        }
        
        AmigoRest.sharedInstance.getJSON(urlStr: urlStr)
            .subscribe(onSuccess: { json in
                self.user = self.parseUserJSON(json: json)
//                self.user.selectedDatasetId = SurveyConfig.getDatasetId()
                
                Repository.sharedInstance.fetchProjects(urlStr: self.user.projectsURL)
                    .subscribe(
                        onNext: { project in
//                            print(project.name)
                        },
                        onCompleted: {
                            self.save(user: self.user)
                            single(.success(self.user))
                        })
            })
            .addDisposableTo(AmigoRest.sharedInstance.disposeBag)
        
            return Disposables.create()
        }
    }
    
    func fetchProjects(urlStr: String) -> Observable<ProjectModel> {
        let url = urlStr + "?summary"
        self.user.visible_projects = []
        return self.fetchProjectsPage(urlStr: url)
            .flatMap { project -> Observable<ProjectModel> in
                self.user.visible_projects.append(project)
                if project.id == SurveyConfig.getProjectId() {
                    return self.fetchProject(urlStr: project.url).asObservable()
                }
                return Observable.from(project)
        }
    }

    func fetchProjectsPage(urlStr: String, projects: [ProjectModel] = []) -> Observable<ProjectModel> {
        return AmigoRest.sharedInstance.getJSON(urlStr: urlStr)
            .asObservable()
            .flatMap { json -> Observable<ProjectModel> in
                let nextURL = json?["next"] as? String
                let results = json?["results"] as? [[String:Any]]
                var projects_list: [ProjectModel] = projects
                for p in results! {
                    let pm: ProjectModel = self.parseProjectJSON(json: p)
                    projects_list.append(pm)
                }
                if nextURL != nil {
                    return self.fetchProjectsPage(urlStr: nextURL!, projects: projects_list)
                }
                return Observable.from(projects_list)
            }
    }
    
    func fetchProject(urlStr: String) -> Single<ProjectModel> {
        var project: ProjectModel? = nil
        return AmigoRest.sharedInstance.getJSON(urlStr: urlStr)
            .flatMap { json  -> Single<Bool> in
                let project_id = (json?["id"] as? CLong!)!
                project = self.findProject(projectId: project_id!)
                project?.name = (json?["name"] as? String!)!
                project?.description = (json?["description"] as? String!)!
                project?.organization = (json?["organization"] as? String!)!
                project?.permission_level = (json?["permission_level"] as? String!)!
                project?.url = (json?["url"] as? String!)!
                project?.datasetsURL = (json?["datasets"] as? String!)!
                project?.submit_changesetURL = (json?["submit_changeset"] as? String!)!
                
                let supportFilesURL = (json?["support_files"] as? String!)!
                project?.supportFilesHash = (json?["support_files_hash"] as? String!)!
                
                return self.fetchSupportFiles(urlStr: supportFilesURL!)
            }
            .asObservable()
            .flatMap { success -> Observable<DatasetModel> in
                // After support files dowloaded, fetch project's datasets
                project?.datasets = []
                return self.fetchDatasets(urlStr: (project?.datasetsURL)!)
            }
            .flatMap { dataset -> Observable<DatasetModel> in
                return self.fetchRelatedTables(urlStr: dataset.related_tablesURL, datasetModel: dataset)
            }
            .flatMap { dataset -> Observable<(FormModel, DatasetModel)> in
                project?.datasets.append(dataset)
                return Observable.zip(self.fetchForm(url: dataset.forms_summaryURL).asObservable(), Observable.just(dataset))
                { (formModel, dataset) in
                    return (formModel, dataset)
                }
            }
            .flatMap { (formModel, dataset) -> Observable<String> in
                dataset.formModel = formModel
                return self.fetchSchema(urlStr: dataset.schemaURL, datasetModel: dataset).asObservable()
            }
            .toArray()
            .asSingle()
            .map {_ in
                self.save(user: self.user)
                return project!
            }
    }

    func fetchDatasets(urlStr: String) -> Observable<DatasetModel> {
        return fetchDatasetsPage(urlStr: urlStr)
    }

    func fetchDatasetsPage(urlStr: String, datasets: [DatasetModel] = []) -> Observable<DatasetModel> {
        return AmigoRest.sharedInstance.getJSON(urlStr: urlStr)
            .asObservable()
            .flatMap { json -> Observable<DatasetModel> in
                let nextURL = json?["next"] as? String
                let results = json?["results"] as? [[String:Any]]
                var datasets_list: [DatasetModel] = datasets
                for ds in results! {
                    let dsm: DatasetModel = self.parseDatasetJSON(json: ds)
                    if dsm.visible {
                        datasets_list.append(dsm)
                    }
                }
                if nextURL != nil {
                    return self.fetchDatasetsPage(urlStr: nextURL!, datasets:  datasets_list)
                }
                return Observable.from(datasets_list)
        }
    }

    func parseUserJSON(json: [String: Any]?) -> UserModel {
        var user = UserModel()
        user.id = (json?["id"] as? CLong!)!
        user.email = (json?["email"] as? String!)!
        user.custom_id = (json?["custom_id"] as? String!)!
        user.first_name = (json?["first_name"] as? String!)!
        user.last_name = (json?["last_name"] as? String!)!
        user.organization = (json?["organization"] as? String!)!
        user.projectsURL = (json?["visible_projects"] as? String!)!
        return user
    }
    
    func parseProjectJSON(json: [String: Any]?) -> ProjectModel {
        let project = ProjectModel()
        project.id = (json?["id"] as? CLong!)!
        project.name = (json?["name"] as? String!)!
        project.url = (json?["url"] as? String!)!
        project.hash = (json?["hash"] as? String!)!
        project.preview_image = (json?["preview_image"] as? String!)!
        project.preview_image_hash = (json?["preview_image_hash"] as? String!)!
        return project
    }
    
    func parseDatasetJSON(json: [String: Any]?) -> DatasetModel {
        let dataset = DatasetModel()
        dataset.id = (json?["id"] as? CLong!)!
        dataset.name = (json?["name"] as? String!)!
        dataset.boundingbox = (json?["boundingbox"] as? String!)!
        dataset.url = (json?["url"] as? String!)!
        dataset.visible = (json?["visible"] as? Bool!)!
        dataset.type = (json?["type"] as? String!)!
        dataset.preview_image = (json?["preview_image"] as? String!)!
        dataset.preview_image_hash = (json?["preview_image_hash"] as? String!)!
        dataset.read_only = (json?["read_only"] as? Bool!)!
        dataset.online_only = (json?["online_only"] as? Bool!)!
        dataset.display_field = (json?["display_field"] as? String!)!
        dataset.auto_sync = (json?["auto_sync"] as? Bool!)!
        dataset.table_name = (json?["table_name"] as? String!)!
        dataset.master_state = (json?["master_state"] as? String!)!
        dataset.related_tablesURL = (json?["related_tables"] as? String!)!
        dataset.schemaURL = (json?["schema"] as? String!)!
        dataset.submit_changeURL = (json?["submit_change"] as? String!)!
        dataset.schemaHash = (json?["schema_hash"] as? String!)!
        dataset.forms_summaryURL = (json?["forms_summary"] as? String!)!
        return dataset
    }
    
    func fetchRelatedTables(urlStr: String, datasetModel: DatasetModel) -> Observable<DatasetModel> {
        return AmigoRest.sharedInstance.getJSON(urlStr: urlStr)
            .asObservable()
            .map{ json in
                let results = json?["results"] as? [[String:Any]]
                for rt in results! {
                    let rtm: RelatedTableModel = RelatedTableModel()
                    rtm.id = (rt["id"] as? CLong!)!
                    rtm.name = (rt["name"] as? String!)!
                    rtm.chunked_upload = (rt["chunked_upload"] as? String!)!
                    rtm.chunked_upload_complete = (rt["chunked_upload_complete"] as? String!)!
                    rtm.schema = (rt["schema"] as? String!)!
                    rtm.table_name = (rt["table_name"] as? String!)!
                    rtm.type = (rt["type"] as? String!)!
                    datasetModel.related_tables.append(rtm)
                }
                return datasetModel
            }
    }
    
    func fetchSupportFiles(urlStr: String) -> Single<Bool> {
        return AmigoRest.sharedInstance.getJSON(urlStr: urlStr)
            .flatMap { json -> Single<Data> in
                return AmigoRest.sharedInstance.get(urlStr: (json?["zip"] as? String!)!)
            }
            .map {data in
                do {
                    let zipPath = "file://" + SurveyConfig.getWebFormDir()
                    let zipPathFile = zipPath + "/support_files.zip"
                    try data.write(to: URL(string:zipPathFile)!)
                    try Zip.unzipFile( URL(string: zipPathFile)!, destination: URL(string:zipPath)!, overwrite: true, password: nil, progress:
                    {
                        (progress) -> () in
                    })
                }catch {
                    throw AmigoError.UNZIPError("Write ZIP failed")
                }
                return true
        }
    }
    
    func save(user: UserModel?) {
        let json = user?.toJSONString()
        if (json != nil) {
            SurveyConfig.setUserJSON(json: json!)
        }
    }
    
    func restore() -> UserModel? {
        let json = SurveyConfig.getUserJSON()
        if (json != nil) {
            return Mapper<UserModel>().map(JSONString: json!)!
        }
        return nil
    }
    
    func fetchForm(url: String) -> Single<FormModel> {
        return AmigoRest.sharedInstance.getJSON(urlStr: url)
            .map { json  -> FormModel in
                let formModel: FormModel = FormModel()
                formModel.base_form = (json?["base_form"] as? String!)!
                formModel.create_block_form = (json?["create_block_form"] as? String!)!
                do {
                    let data = try JSONSerialization.data(withJSONObject: json?["create_block_json"] as Any )
                    formModel.create_block_json = String(data: data, encoding: .utf8)!
                } catch {
                    throw AmigoError.JSONError("Form json parsing failed.")
                }
                formModel.edit_block_form = (json?["edit_block_form"] as? String!)!
                do {
                    let data = try JSONSerialization.data(withJSONObject: json?["edit_block_json"] as Any )
                    formModel.edit_block_json = String(data: data, encoding: .utf8)!
                } catch {
                    throw AmigoError.JSONError("Form json parsing failed.")
                }
                return formModel
        }
    }

    
    func fetchSchema(urlStr: String, datasetModel: DatasetModel) -> Single<String> {
        return AmigoRest.sharedInstance.getJSON(urlStr: urlStr)
            .map { schemaObj in
                do {
                    let data = try JSONSerialization.data(withJSONObject: schemaObj as Any )
                    datasetModel.schema = String(data: data, encoding: .utf8)!
                    // Loop through schema fields
                    let json_obj = try JSONSerialization.jsonObject(with: data) as! [String: Any]
                    let schema = json_obj["schema"] as? [[String:Any]]
                    for field in schema! {
                        let fd = try JSONSerialization.data(withJSONObject: field)
                        let fs = String(data: fd, encoding: .utf8)
                        datasetModel.schema_fields.append(fs!)
                    }
                    return datasetModel.schema
                } catch {
                    throw AmigoError.JSONError("Schema parsing failed.")
                }
        }
    }
    

}
