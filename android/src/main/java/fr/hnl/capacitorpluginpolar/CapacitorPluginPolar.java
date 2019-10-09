package fr.hnl.capacitorpluginpolar;

import android.Manifest;
import android.content.Context;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.util.UUID;

import io.reactivex.disposables.Disposable;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.errors.PolarInvalidArgument;
import polar.com.sdk.api.model.PolarDeviceInfo;
import polar.com.sdk.api.model.PolarHrData;

@NativePlugin(
        permissions={
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }
)
public class CapacitorPluginPolar extends Plugin {

    private final static String TAG = CapacitorPluginPolar.class.getSimpleName() + "HNL Polar ->";

    public PolarBleApi api;
    Disposable ecgDisposable;
    Disposable accDisposable;
    Disposable ppgDisposable;
    Disposable ppiDisposable;

    /**
     * Polar device's id
     */
    private String DEVICE_ID;

    @PluginMethod()
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", value);
        call.success(ret);
    }

    @PluginMethod()
    public void connect(PluginCall call) {
        DEVICE_ID = call.getString("deviceId");

        Log.d(TAG, "DEVICE_ID: " + DEVICE_ID);

        Context ctx = this.getActivity().getApplicationContext();

        // Load the default api implementation and add callback.
        try {

            // Notice PolarBleApi.ALL_FEATURES are enabled
            api = PolarBleApiDefaultImpl.defaultImplementation(ctx, PolarBleApi.ALL_FEATURES);
            api.setPolarFilter(false);

            api.setApiLogger(new PolarBleApi.PolarBleApiLogger() {
                @Override
                public void message(String s) {
                    Log.d(TAG, s);
                }
            });

            Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo());

            setPolarApiCallBack();

        } catch (Exception e) {
            e.printStackTrace();
            call.reject(e.getLocalizedMessage(), e);
        }

        /* CONNECT */
        try {
            api.connectToDevice(DEVICE_ID);
        } catch (PolarInvalidArgument a){
            a.printStackTrace();
            call.reject(a.getLocalizedMessage(), a);
        }

        JSObject ret = new JSObject();
        ret.put("value", DEVICE_ID);
        call.success(ret);
    }


    private void setPolarApiCallBack() {
        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void blePowerStateChanged(boolean powered) {
                Log.d(TAG, "BLE power: " + powered);

                JSObject ret = new JSObject();
                ret.put("value", powered);
                notifyListeners("blePowerStateChangedEvent", ret);
            }

            @Override
            public void deviceConnected(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "CONNECTED: " + polarDeviceInfo.deviceId);
                DEVICE_ID = polarDeviceInfo.deviceId;

                JSObject ret = new JSObject();
                ret.put("value", "CONNECTED");
                notifyListeners("deviceConnectionStateEvent", ret);
            }

            @Override
            public void deviceConnecting(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "CONNECTING: " + polarDeviceInfo.deviceId);
                DEVICE_ID = polarDeviceInfo.deviceId;

                JSObject ret = new JSObject();
                ret.put("value", "CONNECTING");
                notifyListeners("deviceConnectionStatusEvent", ret);

            }

            @Override
            public void deviceDisconnected(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: " + polarDeviceInfo.deviceId);
                ecgDisposable = null;
                accDisposable = null;
                ppgDisposable = null;
                ppiDisposable = null;

                JSObject ret = new JSObject();
                ret.put("value", "DISCONNECTED");
                notifyListeners("deviceConnectionStatusEvent", ret);

            }

            @Override
            public void ecgFeatureReady(String identifier) {
                Log.d(TAG, "ECG READY: " + identifier);
                // ecg streaming can be started now if needed
            }

            @Override
            public void accelerometerFeatureReady(String identifier) {
                Log.d(TAG, "ACC READY: " + identifier);
                // acc streaming can be started now if needed
            }

            @Override
            public void ppgFeatureReady(String identifier) {
                Log.d(TAG, "PPG READY: " + identifier);
                // ohr ppg can be started
            }

            @Override
            public void ppiFeatureReady(String identifier) {
                Log.d(TAG, "PPI READY: " + identifier);
                // ohr ppi can be started
            }

            @Override
            public void biozFeatureReady(String identifier) {
                Log.d(TAG, "BIOZ READY: " + identifier);
                // ohr ppi can be started
            }

            @Override
            public void hrFeatureReady(String identifier) {
                Log.d(TAG, "HR READY: " + identifier);
                // hr notifications are about to start
            }

            @Override
            public void disInformationReceived(String identifier, UUID uuid, String value) {
                Log.d(TAG, "uuid: " + uuid + " value: " + value);

            }

            @Override
            public void batteryLevelReceived(String identifier, int level) {
                Log.d(TAG, "BATTERY LEVEL: " + level);

            }

            @Override
            public void hrNotificationReceived(String identifier, PolarHrData data) {
                Log.d(TAG, "HR value: " + data.hr + " rrsMs: " + data.rrsMs + " rr: " + data.rrs + " contact: " + data.contactStatus + "," + data.contactStatusSupported);

                JSObject ret = new JSObject();
                ret.put("hr", data.hr);
                ret.put("contactStatus", data.contactStatus);
                ret.put("contactStatusSupported", data.contactStatusSupported);
                notifyListeners("hrNotificationReceived", ret);
            }

            @Override
            public void polarFtpFeatureReady(String s) {
                Log.d(TAG, "FTP ready");
            }
        });
    }

}
