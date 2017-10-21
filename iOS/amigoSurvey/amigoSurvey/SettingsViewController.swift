//
//  SecondViewController.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 8/31/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import UIKit
import RxSwift

class SettingsViewController: UIViewController {

    @IBOutlet weak var logoutButton: UIButton!    
    @IBOutlet weak var selectDatasetButton: UIButton!
    
    @IBAction func selectDataset(_ sender: Any) {
        presentDatasetSelection()
    }
    
    @IBAction func logout(_ sender: Any) {
        SurveyConfig.setLoggedin(loggedin: false)
        presentDatasetSelection()
    }
    
    func setupView() {
        UserViewModel.loadUser(useCache: true)
            .subscribeOn(ConcurrentDispatchQueueScheduler(queue: DispatchQueue.global()))
            .observeOn(MainScheduler())
            .subscribe(onSuccess: { data in
                self.setupButtons()
            })
            .addDisposableTo(FormViewController.disposeBag)
    }
    
    func setupButtons() {
        let name = UserViewModel.getUserName()
        logoutButton.setTitle("Logout: \(name)", for: UIControlState.normal)
        if SurveyConfig.isLoggedin() {
            logoutButton.isHidden = false
            selectDatasetButton.setTitle("Select Project and Dataset", for: UIControlState.normal)
        } else {
            logoutButton.isHidden = true
            selectDatasetButton.setTitle("Login", for: UIControlState.normal)
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        setupView()
    }

    override func viewDidAppear(_ animated: Bool) {
        self.setupButtons()
        if SurveyConfig.isDatasetSelected() {
            presentFormView()
        } else {
//            presentDatasetSelection()
        }
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    func presentDatasetSelection() {
        let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
        let newViewController = storyBoard.instantiateViewController(withIdentifier: "DatasetPickerViewController")
        self.present(newViewController, animated: true, completion: nil)
    }
    
    func presentFormView() {
        let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
        let newViewController = storyBoard.instantiateViewController(withIdentifier: "FormViewController")
        self.present(newViewController, animated: true, completion: nil)
    }
}

