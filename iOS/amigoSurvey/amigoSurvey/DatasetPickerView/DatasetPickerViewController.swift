//
//  DatasetPickerViewController.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 10/9/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import UIKit
import RxSwift

extension UIImageView {
    func downloadImageFrom(link link:String, contentMode: UIViewContentMode) {
        AmigoRest.sharedInstance.get(urlStr: link)
            .subscribeOn(ConcurrentDispatchQueueScheduler(queue: DispatchQueue.global()))
            .observeOn(MainScheduler())
            .map { data in
                self.contentMode =  contentMode
                self.image = UIImage(data: data)
            }
            .subscribe()
            .addDisposableTo(FormViewController.disposeBag)
    }
}

class DatasetPickerViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {

    
    @IBOutlet weak var tableTitle: UILabel!
    @IBOutlet weak var tableView: UITableView!
    
    var adapter : TableDataAdapter? = nil
    
    @IBAction func cancelPressed(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }
    
    func login() {
        let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
        let newViewController = storyBoard.instantiateViewController(withIdentifier: "LoginViewController")
        self.present(newViewController, animated: true, completion: nil)
    }
    
    func loadUser() {
        UserViewModel.loadUser(useCache: true)
            .subscribeOn(ConcurrentDispatchQueueScheduler(queue: DispatchQueue.global()))
            .observeOn(MainScheduler())
            .subscribe(onSuccess: { data in
                self.tableView.reloadData()
            })
            .addDisposableTo(FormViewController.disposeBag)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        adapter = ProjectDataAdapter()
    }
 
    override func viewWillAppear(_ animated: Bool) {
        if !SurveyConfig.isLoggedin() {
            self.login()
        } else {
            self.loadUser()
        }
        tableTitle.text = adapter?.getTitle()
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        let count = (adapter?.getCount())!
        return count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "AmigoTableViewCell", for: indexPath) as? AmigoTableViewCell else {
            fatalError("The dequeued cell is not an instance of AmigoTableViewCell.")
        }
        
        // Lazy thumbnail download
        cell.thumbnail.image = UIImage()
        let imageURL = adapter?.getImageURL(index: indexPath.row)
        if imageURL != nil {
            cell.thumbnail.downloadImageFrom(link: imageURL!, contentMode: UIViewContentMode.scaleAspectFit)
        }
        cell.label?.text = adapter?.getName(index: indexPath.row)
        return cell
    }
    
    @objc func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if let adapter = self.adapter as? ProjectDataAdapter {
            
            let url = adapter.getURL(index: indexPath.row)
            
            // Fetch project data
            Repository.sharedInstance.fetchProject(urlStr: url)
                .subscribeOn(ConcurrentDispatchQueueScheduler(queue: DispatchQueue.global()))
                .observeOn(MainScheduler())
                .subscribe(onSuccess: { project in
                    // Select project
                    SurveyConfig.setProjectId(id: adapter.getId(index: indexPath.row))
                    
                    // Create dataset adapter
                    let dataset_adapter = DatasetDataAdapter()
                    dataset_adapter.setProjectIndex(index: indexPath.row)
                    
                    // Swap adapters
                    self.adapter = dataset_adapter
                    self.tableTitle.text = dataset_adapter.getTitle()
                    self.tableView.reloadData()
                })
                .addDisposableTo(FormViewController.disposeBag)
        } else if let adapter = self.adapter as? DatasetDataAdapter {
            // Select dataset
            SurveyConfig.setDatasetId(id: adapter.getId(index: indexPath.row))
            self.dismiss(animated: true, completion: nil)
            presentFormView()
        }
    }
    
    func presentFormView() {
        let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
        let newViewController = storyBoard.instantiateViewController(withIdentifier: "FormViewController")
        self.present(newViewController, animated: true, completion: nil)
    }
    
}
