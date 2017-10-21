//
//  LoginViewController.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 9/18/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import Foundation
import UIKit
import RxSwift

class LoginViewController: UIViewController {
    
    @IBOutlet weak var emailText: UITextField!
    @IBOutlet weak var passwordText: UITextField!

    @IBAction func loginPressed(_ sender: Any) {
        print("loginPressed")
        SurveyConfig.setEmail(email: emailText.text!)
        SurveyConfig.setPassword(password: passwordText.text!)
        LoginModelView.login(email: emailText.text!, password: passwordText.text!)
            .subscribeOn(ConcurrentDispatchQueueScheduler(queue: DispatchQueue.global()))
            .observeOn(MainScheduler())
            .subscribe(
                onSuccess: { data in
                    self.loadUser()
            })
            .addDisposableTo(FormViewController.disposeBag)

    }

    @IBAction func cancelPressed(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }
    
    func loadUser() {
        UserViewModel.loadUser(useCache: true)
            .subscribeOn(ConcurrentDispatchQueueScheduler(queue: DispatchQueue.global()))
            .observeOn(MainScheduler())
            .subscribe(onSuccess: { data in
                SurveyConfig.setLoggedin(loggedin: true)
                self.dismiss(animated: true, completion: nil)
            })
            .addDisposableTo(FormViewController.disposeBag)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        let email = SurveyConfig.getEmail()
        if email.characters.count > 0 {
            emailText.text = email
        }
        let pwd = SurveyConfig.getPassword()
        if pwd.characters.count > 0 {
            passwordText.text = pwd
        }
    }
}
