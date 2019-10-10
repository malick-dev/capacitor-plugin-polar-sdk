package fr.hnl.capacitorpluginpolar;

import android.Manifest;
import android.content.Context;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.errors.PolarInvalidArgument;
import polar.com.sdk.api.model.PolarDeviceInfo;
import polar.com.sdk.api.model.PolarHrData;
import polar.com.sdk.api.model.PolarOhrPPIData;

@NativePlugin(
        permissions = {
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
        } catch (PolarInvalidArgument a) {
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
                startOhrPPIStreaming();
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

                // Java Objects to JSON
                Gson gson = new Gson();

                String jsonStr = gson.toJson(data);
                Log.e(TAG, "retJSONString = " + jsonStr);

                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(jsonStr);

                    Log.e(TAG, "retTmp = " + jsonObject.toString());

                    JSObject ret = JSObject.fromJSONObject(jsonObject);
                    Log.e(TAG, "ret = " + ret.toString());

//                JSObject ret = new JSObject();
//                ret.put("hr", data.hr);
//                ret.put("contactStatus", data.contactStatus);
//                ret.put("contactStatusSupported", data.contactStatusSupported);
                    notifyListeners("hrNotificationReceived", ret);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "JSONException =" + e.getLocalizedMessage());
                }
            }

            @Override
            public void polarFtpFeatureReady(String s) {
                Log.d(TAG, "FTP ready");
            }
        });
    }

    private void startOhrPPIStreaming() {
        Log.d(TAG, "startOhrPPIStreaming on DEVICE_ID " + DEVICE_ID);
        if (ppiDisposable == null) {
            Log.d(TAG, "startOhrPPIStreaming ppiDisposable");
            ppiDisposable = api.startOhrPPIStreaming(DEVICE_ID).observeOn(AndroidSchedulers.mainThread()).subscribe(
                    new Consumer<PolarOhrPPIData>() {
                        @Override
                        public void accept(PolarOhrPPIData ppiData) throws Exception {
                            for (PolarOhrPPIData.PolarOhrPPISample sample : ppiData.samples) {
                                Log.d(TAG, "ppi: " + sample.ppi
                                        + " hr: " + sample.hr
                                        + " skinContactStatus: " + sample.skinContactStatus
                                        + " skinContactSupported : " + sample.skinContactSupported
                                        + " blocker: " + sample.blockerBit + " errorEstimate: " + sample.errorEstimate);
                            }

                            try {
                                // Java Objects to JSON
                                Gson gson = new Gson();

                                String retJSONString = gson.toJson(ppiData);
                                Log.e(TAG, "retJSONString = " + retJSONString);

                                JSONObject retTmp = new JSONObject(retJSONString);
                                Log.e(TAG, "retTmp = " + retTmp.toString());

                                JSObject ret = JSObject.fromJSONObject(retTmp);
                                Log.e(TAG, "ret = " + ret.toString());

                                notifyListeners("OhrPPIStreamEvent", ret);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.d(TAG, "JSONException =" + e.getLocalizedMessage());
                            }
                        }
                    },
                    new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Log.e(TAG, "startOhrPPIStreaming - " + throwable);
                        }
                    },
                    new Action() {
                        @Override
                        public void run() throws Exception {
                            Log.d(TAG, "startOhrPPIStreaming - complete");
                        }
                    }
            );
        } else {
            Log.d(TAG, "startOhrPPIStreaming - dispose not null");
            ppiDisposable.dispose();
            ppiDisposable = null;
        }
    }

}
