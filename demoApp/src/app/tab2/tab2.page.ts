import {Component} from '@angular/core';
import {Plugins} from '@capacitor/core';

const {CapacitorPluginPolar} = Plugins;

@Component({
    selector: 'app-tab2',
    templateUrl: 'tab2.page.html',
    styleUrls: ['tab2.page.scss']
})
export class Tab2Page {

    deviceId: any;

    constructor() {

        // Polar OH1 -> device id: 539B3229 -> address: A0:9E:1A:53:9B:32
        this.deviceId = '539B3229';

    }

    async show() {
        const ret = await CapacitorPluginPolar.echo({
            value: 'Malick'
        });
        console.log('Got CapacitorPluginPolar.echo: ', JSON.stringify(ret));
        console.log('Got CapacitorPluginPolar.echo.value: ', ret.value);
    }

    async connect() {
        const ret = await CapacitorPluginPolar.connect({
            value: this.deviceId
        });
    }
}
