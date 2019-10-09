import {Component, OnInit} from '@angular/core';
import {Plugins} from '@capacitor/core';

const {CapacitorPluginPolar} = Plugins;

@Component({
    selector: 'app-tab2',
    templateUrl: 'tab2.page.html',
    styleUrls: ['tab2.page.scss']
})
export class Tab2Page implements OnInit {

    deviceId: any;

    powerStateEventListener: any;
    deviceConnectionStateEventListener: any;
    hrNotificationReceivedListener: any;

    deviceConnectionState: String;
    powerState: String;

    hr: any;
    contactStatus: any;
    contactStatusSupported: any;
    ppi: any;

    ngOnInit() {
        this.ppi = 0;
    }

    constructor() {

        // Polar OH1 -> device id: 539B3229 -> address: A0:9E:1A:53:9B:32
        this.deviceId = '539B3229';
        this.hr = 0;
    }

    async show() {
        const ret = await CapacitorPluginPolar.echo({
            value: 'Malick'
        });
        console.log('Got CapacitorPluginPolar.echo: ', JSON.stringify(ret));
        console.log('Got CapacitorPluginPolar.echo.value: ', ret.value);
    }

    handlePowerStateEvent(value) {
        this.powerState = "test1";
        this.powerState = value;
        console.log("handlePowerStateEvent value=" + value);
        this.powerState = value;
        console.log("handlePowerStateEvent power state : " + this.powerState);
        this.powerState = "test2";
    }

    async connect() {
        console.log("connect start");
        this.ppi = Date.now();

        const ret = await CapacitorPluginPolar.connect({
            deviceId: this.deviceId
        });

        this.powerStateEventListener = CapacitorPluginPolar.addListener("blePowerStateChangedEvent", (data) => {
            console.log("blePowerStateChangedEvent was fired");
            console.log(JSON.stringify(data));
            this.powerState = data.value;
            console.log("this.powerState : " + this.powerState);
            this.handlePowerStateEvent(this.powerState);
        });

        this.deviceConnectionStateEventListener = CapacitorPluginPolar.addListener("deviceConnectionStateEvent", (data: any) => {
            console.log("deviceConnectionStateEvent was fired");
            console.log(JSON.stringify(data));
            this.deviceConnectionState = data.value;
            console.log("deviceConnectionState : " + this.deviceConnectionState);
        });

        this.hrNotificationReceivedListener = CapacitorPluginPolar.addListener("hrNotificationReceived", (data: any) => {
            console.log("hrNotificationReceived : " + JSON.stringify(data));
            this.hr = data.hr;
            this.contactStatus= data.contactStatus;
            //this.ppi = info.hr;
            this.contactStatusSupported = data.contactStatusSupported;
            console.log("contactStatusSupported : " + this.contactStatusSupported);

        });
        console.log("connect end");
    }

    ngOnDestroy() {
        console.log("ngOnDestroy myPluginEventListener remove");
        this.powerStateEventListener.remove();
        this.deviceConnectionStateEventListener.remove();
        this.hrNotificationReceivedListener.remove();
    }

}
