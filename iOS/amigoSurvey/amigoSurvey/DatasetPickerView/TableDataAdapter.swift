//
//  TableDataAdapter.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 10/9/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation


public protocol TableDataAdapter {
    func getTitle() -> String
    func getCount() -> Int
    func getName(index: Int) -> String
    func getImageURL(index: Int) -> String
    func getId(index: Int) -> Int
    func getURL(index: Int) -> String
}

class ProjectDataAdapter: TableDataAdapter {
    
    func getTitle() -> String {
        return "Projects"
    }

    public func getCount() -> Int {
       return Repository.sharedInstance.user.visible_projects.count
    }

    func getName(index: Int) -> String {
        return Repository.sharedInstance.user.visible_projects[index].name
    }

    func getImageURL(index: Int) -> String {
        return Repository.sharedInstance.user.visible_projects[index].preview_image
    }

    func getId(index: Int) -> Int {
        return Repository.sharedInstance.user.visible_projects[index].id
    }

    func getURL(index: Int) -> String {
        return Repository.sharedInstance.user.visible_projects[index].url
    }
}


class DatasetDataAdapter: TableDataAdapter {
    var projectIndex: Int = 0
    var project: ProjectModel = ProjectModel()
    
    func setProjectIndex(index: Int) {
        projectIndex = index
        project = Repository.sharedInstance.user.visible_projects[projectIndex]
    }
    
    func getTitle() -> String {
        return "Datasets for " + project.name
    }
    
    public func getCount() -> Int {
        return project.datasets.count
    }
    
    func getName(index: Int) -> String {
        return project.datasets[index].name
    }

    func getImageURL(index: Int) -> String {
        return project.datasets[index].preview_image
    }

    func getId(index: Int) -> Int {
        return project.datasets[index].id
    }

    func getURL(index: Int) -> String {
        return project.datasets[index].url
    }

}
