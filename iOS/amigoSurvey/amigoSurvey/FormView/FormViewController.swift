//
//  FormViewController.swift
//  amigoSurvey
//
//  Created by Victor Chernetsky on 8/31/17.
//  Copyright © 2017 AmigoCloud. All rights reserved.
//

import UIKit
import JavaScriptCore
import CoreLocation
import RxSwift
import Photos

class FormViewController:
    UIViewController,
    UIImagePickerControllerDelegate,
    UINavigationControllerDelegate,
    CLLocationManagerDelegate,
    BarcodeScannerCodeDelegate,
    BarcodeScannerErrorDelegate,
    BarcodeScannerDismissalDelegate {

    @IBOutlet weak var webView: UIWebView!
    @IBOutlet weak var formTitle: UILabel!
    @IBOutlet weak var gpsIcon: UIImageView!
    
    static var disposeBag = DisposeBag()
    var context: JSContext?
    var bridge: AmigoPlatform = AmigoPlatform()
    var imagePicker: UIImagePickerController?
    let locationManager = CLLocationManager()

    enum Modes {
        case Normal
        case ReturnFromPhotoPick
        case ReturnFromBarcodeScan
    }
    
    var mode: Modes = Modes.Normal
    var relatedTableId: String = ""

    func initLocation() {
        locationManager.requestAlwaysAuthorization()
        
        if CLLocationManager.locationServicesEnabled() {
            locationManager.delegate = self
            locationManager.desiredAccuracy = kCLLocationAccuracyBest // You can change the locaiton accuary here.
            locationManager.startUpdatingLocation()
        }
    }
    
    // Print out the location to the console
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let location = locations.first {
            LocationViewModel.updateLocation(location: location)
            if location.horizontalAccuracy <= 10 {
                self.gpsIcon.image = UIImage(named: "gpsLocator_green")
            } else if location.horizontalAccuracy <= 65 {
                self.gpsIcon.image = UIImage(named: "gpsLocator_yellow")
            }else if location.horizontalAccuracy <= 100 {
                self.gpsIcon.image = UIImage(named: "gpsLocator_red")
            } else  {
                self.gpsIcon.image = UIImage(named: "gpsLocator_off")
            }
        }
    }
    
    // If we have been deined access give the user the option to change it
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if(status == CLAuthorizationStatus.denied) {
        }
    }
    
    @IBAction func settingsPressed(_ sender: Any) {
        SurveyConfig.setDatasetSelected(selected: false)
        self.dismiss(animated: true, completion: nil)
    }
    
    @IBAction func sendPressed(_ sender: Any) {
        print("Submit pressed!")
        bridge.submit()
        uploadPhotos()
    }
    
    func uploadPhotos() {
        RelatedTableViewModel.uploadAllPhotos()
            .subscribeOn(ConcurrentDispatchQueueScheduler(queue: DispatchQueue.global()))
            .observeOn(MainScheduler())
            .subscribe(
                onNext: { progress in
                    print("onNext: \(progress.fileIndex) of \(progress.filesTotal): \(progress.message) -> \(progress.bytesSent)")
            },
                onCompleted: {
                    print("onCompleted:")
                    RelatedTableViewModel.deleteAllPhotos()
            })
            .addDisposableTo(FormViewController.disposeBag)
    }
    
    func tappedGPSIcon()
    {
        print("Tapped on GPS icon")
        var msg = "Latitude: \(LocationViewModel.lastLocation.lat)\nLongitude: \(LocationViewModel.lastLocation.lng)\n"
        msg += "Accuracy: \(LocationViewModel.lastLocation.accuracy)"
        let alert = UIAlertController(title: self.localized("location_info"), message: msg, preferredStyle: .actionSheet)
        alert.addAction(UIAlertAction(title: self.localized("Cancel"), style: .default, handler: nil))
        alert.popoverPresentationController?.sourceView = self.gpsIcon
        self.present(alert, animated: true, completion: nil)
    }
    
    func loadMobileForm() {
        let datasetId = SurveyConfig.getDatasetId()
        if datasetId != nil {
            let dataset = DatasetViewModel.findDataset(datasetId: datasetId!)
            if dataset != nil {
               
                formTitle?.text = dataset?.name
                
                bridge.formVC = self
                bridge.webView = webView
                bridge.formModel = dataset?.formModel
                bridge.name = "create_block"
                bridge.amigo_id = FormViewModel.generateAmigoId()
                bridge.data = DatasetViewModel.getNewRecordJSON(datasetId: datasetId!, amigo_id: bridge.amigo_id)!
                
                var base_form = dataset?.formModel.base_form
                let path = SurveyConfig.getWebFormDir() + "/" + bridge.name
                
                // Remove <base...> tag from html
                let pat = "<base[^>]*>"
                let regex = try! NSRegularExpression(pattern: pat, options: [])
                let range = NSMakeRange(0, (base_form?.characters.count)!)
                let html = regex.stringByReplacingMatches(in: base_form!, options: [], range: range, withTemplate: "")
                
                self.context = self.webView.value(forKeyPath: "documentView.webView.mainFrame.javaScriptContext") as? JSContext
                context?.setObject(bridge, forKeyedSubscript: "AmigoPlatform" as NSCopying & NSObjectProtocol)
                webView.stringByEvaluatingJavaScript(from: "window.onerror = function (message, url, lineNumber) {AmigoPlatform.onException(message + ' URL: ' + url + ' Line:' + lineNumber);}")
                
                context?.exceptionHandler = { context, exception in
                    if let exc = exception {
                        print("JS Exception:", exc.toString())
                    }
                }                
                webView.loadHTMLString(html, baseURL: URL(string: path))
            }
        }
    }
    
    func login() {
        let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
        let newViewController = storyBoard.instantiateViewController(withIdentifier: "LoginViewController")
        self.present(newViewController, animated: false, completion: nil)
    }
    
    func selectDataset() {
        let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
        let newViewController = storyBoard.instantiateViewController(withIdentifier: "DatasetPickerViewController")
        self.present(newViewController, animated: false, completion: nil)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        initLocation()
        
        let tap = UITapGestureRecognizer(target: self, action: #selector(self.tappedGPSIcon))
        gpsIcon.addGestureRecognizer(tap)
        gpsIcon.isUserInteractionEnabled = true
        
        SurveyConfig.setBaseURL(url: "https://www.amigocloud.com")
        
        imagePicker = UIImagePickerController()
        imagePicker?.allowsEditing = false
        imagePicker?.delegate = self
      
    }
    
    func loadUser() {
        UserViewModel.loadUser(useCache: true)
            .subscribeOn(ConcurrentDispatchQueueScheduler(queue: DispatchQueue.global()))
            .observeOn(MainScheduler())
            .subscribe(onSuccess: { data in
                self.loadMobileForm()
            })
            .addDisposableTo(FormViewController.disposeBag)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        if mode == Modes.ReturnFromPhotoPick || mode == Modes.ReturnFromBarcodeScan {
            mode = Modes.Normal
            return
        }
        
        if !SurveyConfig.isDatasetSelected() {
            self.selectDataset()
        } else {
            self.loadUser()
        }
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    func localized(_ key: String) -> String {
        return NSLocalizedString(key, comment: "")
    }
    
    func takePhoto(relatedTableId: String, amigoId: String) {
        DispatchQueue.main.async {
            let alert = UIAlertController(title: self.localized("Select_media_option"), message: "", preferredStyle: .actionSheet)
            alert.addAction(UIAlertAction(title: self.localized("Take_new_photo_or_video"), style: .default, handler: { action in
                self.actuallyTakePhoto(takeNew: true, relatedTableId: relatedTableId, amigoId: amigoId)
            }))
            
            alert.addAction(UIAlertAction(title: self.localized("Select_existing_media"), style: .default, handler: { action in
                self.actuallyTakePhoto(takeNew: false, relatedTableId: relatedTableId, amigoId: amigoId)
            }))
            
            alert.addAction(UIAlertAction(title: self.localized("Cancel"), style: .default, handler: nil))
            
            alert.popoverPresentationController?.sourceView = self.webView
            self.present(alert, animated: true, completion: nil)
        }
    }

    func actuallyTakePhoto(takeNew: Bool, relatedTableId: String, amigoId: String) {
        if takeNew {
            self.imagePicker?.sourceType = .camera
        } else {
            self.imagePicker?.sourceType = .photoLibrary
        }
        mode = Modes.ReturnFromPhotoPick
        self.relatedTableId = relatedTableId
        self.present(self.imagePicker!, animated: true, completion: nil)
    }
    
    public func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [String : Any]) {
        var fname: String? = nil
        
        if let url = info[UIImagePickerControllerReferenceURL] as? URL {
            let result = PHAsset.fetchAssets(withALAssetURLs: [url], options: nil)
            let asset = result.firstObject
            fname = asset?.value(forKey: "filename") as? String
        } else {
            fname = "IMG_\(NSDate().timeIntervalSince1970 * 1000).jpg"
        }
        
        if let pickedImage = info[UIImagePickerControllerOriginalImage] as? UIImage {
            if let name = fname {
                self.savePhoto(image: pickedImage, fname: name)
            }
        }        
        picker.dismiss(animated: true, completion: nil)
    }
    
    func savePhoto(image : UIImage, fname: String) {
        if let data = UIImageJPEGRepresentation(image, 0.7) {
            let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let fileURL = documentsURL.appendingPathComponent("\(SurveyConfig.getPhotoDirName())/\(fname)")
            try? data.write(to: fileURL, options: .atomic)
            RelatedTableViewModel.savePhotoRecord(filename: fname, source_amigo_id: bridge.amigo_id, relatedTableId: relatedTableId)
        }
    }
    
    public func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true, completion: nil)
    }

    // Barcode scanner ---------------------------------------------
    func barcodeScanner(_ controller: BarcodeScannerController, didCaptureCode code: String, type: String) {
        print(" barcode: \(code)")
        if let datasetId = SurveyConfig.getDatasetId() {
            if let ds = DatasetViewModel.findDataset(datasetId: datasetId) {
                if let field_name = DatasetViewModel.getCustomFieldName(custom_type: "barcode", datasetId: datasetId) {
                    self.bridge.setCustomFieldValue(field_name, code)
                }
            }
        }
        
        controller.dismiss(animated: true, completion: nil)
//        std::string fieldName = AmigoCloud::APIv1::getCustomFieldName(bridge.datasetId, "barcode");
//        if(!fieldName.empty()) {
//            [[[[GlobeContainerVC instance] formVC] bridge] setCustomFieldValue:[NSString stringWithUTF8String:fieldName.c_str()]
//                :[NSString stringWithUTF8String:scannedBarcode.c_str()]];
    }

    func barcodeScanner(_ controller: BarcodeScannerController, didReceiveError error: Error) {
        print(error)
    }
    
    func barcodeScannerDidDismiss(_ controller: BarcodeScannerController) {
        controller.dismiss(animated: true, completion: nil)
    }
    
    func scanBarcode() {
        DispatchQueue.main.async {
            let controller = BarcodeScannerController()
            controller.codeDelegate = self
            controller.errorDelegate = self
            controller.dismissalDelegate = self
            self.mode = Modes.ReturnFromBarcodeScan
            self.present(controller, animated: true, completion: nil)
        }
    }
}

