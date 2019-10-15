import Foundation
import Capacitor
import PolarBleSdk
import RxSwift
import CoreBluetooth

@objc(CapacitorPluginPolar)
public class CapacitorPluginPolar: CAPPlugin {

    private static var TAG : String = NSStringFromClass(CapacitorPluginPolar.self) + " HNL Polar -> "
    
    var api: PolarBleApi
    var ppiDisposable: Disposable?

    /**
    * Polar device's id
    */
    private var DEVICE_ID: String
    
    override init() {
        self.api = PolarBleApiDefaultImpl.polarImplementation(DispatchQueue.main, features: Features.allFeatures.rawValue)
        self.api.polarFilter(false)
        self.DEVICE_ID = ""
        super.init()
    }

    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.success([
            "value": value
        ])
    }
  
    @objc func connect(_ call: CAPPluginCall) {
        self.DEVICE_ID = call.getString("deviceId")!
        NSLog("\(CapacitorPluginPolar.TAG) DEVICE_ID: ", self.DEVICE_ID)
        
        do {
            try self.api.connectToDevice(self.DEVICE_ID)
        } catch let err {
            NSLog("\(CapacitorPluginPolar.TAG) ", err.localizedDescription)
        }
    }
    
    @objc func startOhrPPIStreaming() {
        NSLog("\(CapacitorPluginPolar.TAG) startOhrPPIStreaming on DEVICE_ID ", self.DEVICE_ID);
        
        if ppiDisposable == nil {
            NSLog("\(CapacitorPluginPolar.TAG) startOhrPPIStreaming ppiDisposable");
            ppiDisposable = api.startOhrPPIStreaming(self.DEVICE_ID).observeOn(MainScheduler.instance).subscribe{ e in
            
                switch e {
                    case .completed:
                        NSLog("\(CapacitorPluginPolar.TAG) startOhrPPIStreaming - complete")
                
                    case .error(let err):
                        NSLog("\(CapacitorPluginPolar.TAG) startOhrPPIStreaming - ", err.localizedDescription)
                        self.ppiDisposable = nil
                        
                    case .next(let data):
                        var dataDict: [String: NSObject] = [:]
                        var samplesDict: [[String: NSObject]] = [[:]]
                        
                        dataDict["timeStamp"] = data.timeStamp as NSObject
                        
                        for sample in data.samples {
                            let sampleDict: [String: NSObject] =
                                ["ppi": sample.ppInMs as NSObject,
                                 "errorEstimate": sample.ppErrorEstimate as NSObject,
                                 "hr": sample.hr as NSObject,
                                 "blockerBit": sample.blockerBit as NSObject,
                                 "skinContactStatus": sample.skinContactStatus as NSObject,
                                 "skinContactSupported": sample.skinContactSupported as NSObject]
                            
                            samplesDict.append(sampleDict)
                        }
                        
                        dataDict["samples"] = samplesDict as NSObject
                        
                        NSLog("\(CapacitorPluginPolar.TAG) startOhrPPIStreaming - Data ", dataDict.debugDescription)
                        
                        self.notifyListeners("OhrPPIStreamEvent", data: dataDict)
                }
            }
        } else {
            NSLog("\(CapacitorPluginPolar.TAG) startOhrPPIStreaming - dispose not null")
            ppiDisposable?.dispose()
            ppiDisposable = nil
        }
    }
    
    func deviceConnecting(_ polarDeviceInfo: PolarDeviceInfo) {
        NSLog("\(CapacitorPluginPolar.TAG) DEVICE CONNECTING: \(polarDeviceInfo)")
        self.notifyListeners("deviceConnectionStatusEvent", data: ["value": "CONNECTING"])
    }
    
    func deviceConnected(_ polarDeviceInfo: PolarDeviceInfo) {
        NSLog("\(CapacitorPluginPolar.TAG) DEVICE CONNECTED: \(polarDeviceInfo)")
        self.DEVICE_ID = polarDeviceInfo.deviceId
        self.notifyListeners("deviceConnectionStateEvent", data: ["value": "CONNECTED"])
    }
    
    func deviceDisconnected(_ polarDeviceInfo: PolarDeviceInfo) {
        NSLog("\(CapacitorPluginPolar.TAG) DISCONNECTED: \(polarDeviceInfo)")
        self.ppiDisposable = nil
        notifyListeners("deviceConnectionStatusEvent", data: ["value": "DISCONNECTED"])
    }
    
    // PolarBleApiDeviceInfoObserver
    func batteryLevelReceived(_ identifier: String, batteryLevel: UInt) {
        NSLog("\(CapacitorPluginPolar.TAG) Battery level updated: \(batteryLevel)")
    }
    
    func disInformationReceived(_ identifier: String, uuid: CBUUID, value: String) {
        NSLog("\(CapacitorPluginPolar.TAG) Dis info: \(uuid.uuidString) value: \(value)")
    }
    
    // PolarBleApiDeviceHrObserver
    func hrValueReceived(_ identifier: String, data: PolarBleApiDeviceHrObserver.PolarHrData) {
        NSLog("\(CapacitorPluginPolar.TAG)(\(identifier)) HR value: \(data.hr) rrs: \(data.rrs) rrsMs: \(data.rrsMs) contactStatus: \(data.contact),  \(data.contactSupported)")
        
        let hrDict: [String: String] =
            ["hr": String(data.hr),
             "contactStatus": String(data.contact),
             "contactStatusSupported": String(data.contactSupported)]
        
        self.notifyListeners("hrNotificationReceived", data: hrDict)
    }
    
    func hrFeatureReady(_ identifier: String) {
        NSLog("\(CapacitorPluginPolar.TAG) HR READY \(identifier)")
    }

    // PolarBleApiDeviceEcgObserver
    func ecgFeatureReady(_ identifier: String) {
        NSLog("\(CapacitorPluginPolar.TAG) ECG READY \(identifier)")
    }
    
    // PolarBleApiDeviceAccelerometerObserver
    func accFeatureReady(_ identifier: String) {
        NSLog("\(CapacitorPluginPolar.TAG) ACC READY \(identifier)")
    }
    
    func ohrPPGFeatureReady(_ identifier: String) {
        NSLog("\(CapacitorPluginPolar.TAG) OHR PPG ready \(identifier)")
    }
    
    // PolarBleApiPowerStateObserver
    func blePowerOn() {
        NSLog("\(CapacitorPluginPolar.TAG) BLE ON")
        notifyListeners("deviceConnectionStateEvent", data: ["value": "true"])
        
    }
    
    func blePowerOff() {
        NSLog("\(CapacitorPluginPolar.TAG) BLE OFF")
        notifyListeners("deviceConnectionStateEvent", data: ["value": "false"])
    }
    
    // PPI
    func ohrPPIFeatureReady(_ identifier: String) {
        NSLog("\(CapacitorPluginPolar.TAG) PPI Feature ready \(identifier)")
        self.startOhrPPIStreaming()
    }

    func ftpFeatureReady(_ identifier: String) {
        NSLog("\(CapacitorPluginPolar.TAG) FTP ready \(identifier)")
    }
    
    func message(_ str: String) {
        NSLog("\(CapacitorPluginPolar.TAG) \(str)")
    }
    
    /// ccc write observer
    func cccWrite(_ address: UUID, characteristic: CBUUID) {
        NSLog("\(CapacitorPluginPolar.TAG) ccc write: \(address) chr: \(characteristic)")
    }
}

