package fr.hnl.capacitorpluginpolar;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.google.gson.Gson;

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
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        }
)
public class CapacitorPluginPolar extends Plugin {

    private static final int REQUEST_ENABLE_BT = 420;
    private static final String PERMISSION_DENIED_ERROR = "Unable to access Bluetooth, user denied permission request";

    private final static String TAG = CapacitorPluginPolar.class.getSimpleName() + "HNL Polar ->";

    public PolarBleApi api;
    Disposable ecgDisposable;
    Disposable accDisposable;
    Disposable ppgDisposable;
    Disposable ppiDisposable;

    @PluginMethod()
    public void connect(PluginCall call) {
        final String deviceId = call.getString("deviceId");

        /* Polar device's id */

        if (!call.getData().has("deviceId")) {
            call.reject("Must provide deviceId");
            return;
        } else {
            Log.i(TAG, "Request connection to DEVICE_ID: " + deviceId);
        }

        /* PERMISSIONS */
        // Check if all the required permissions has been granted
        // @see https://capacitor.ionicframework.com/docs/plugins/android#permissions
        if (!hasRequiredPermissions()) {
                        Log.d(TAG, "Not permitted. Asking permission...");
            //            saveCall(call);
            //            pluginRequestAllPermissions();
        } else {
            Log.d(TAG, "BT permission OK");
        }

        Context ctx = this.getActivity().getApplicationContext();

        /* API IMPLEMENTATION */

        // Load the default api implementation and add callback.
        try {
            api = PolarBleApiDefaultImpl.defaultImplementation(ctx, PolarBleApi.ALL_FEATURES);
            api.setPolarFilter(false);

            api.setApiLogger(new PolarBleApi.PolarBleApiLogger() {
                @Override
                public void message(String s) {
                    Log.d(TAG, s);
                }
            });

            Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo());

            setPolarApiCallBack(deviceId);

        } catch (Exception e) {
            e.printStackTrace();
            call.reject(e.getLocalizedMessage(), e);
        }

        /* CONNECT */

        try {
            api.connectToDevice(deviceId);
        } catch (PolarInvalidArgument a) {
            a.printStackTrace();
            call.reject(a.getLocalizedMessage(), a);
        }

        /* RESULT */

        JSObject ret = new JSObject();
        ret.put("value", deviceId);
        call.success(ret);
    }


    @SuppressWarnings("MissingPermission")
    private void setPolarApiCallBack(final String deviceId) {
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

                JSObject ret = new JSObject();
                ret.put("value", "CONNECTED");
                notifyListeners("deviceConnectionStateEvent", ret);
            }

            @Override
            public void deviceConnecting(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "CONNECTING: " + polarDeviceInfo.deviceId);

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
                startOhrPPIStreaming(deviceId);
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
                Log.i(TAG, "HR value: " + data.hr + " rrsMs: " + data.rrsMs + " rr: " + data.rrs + " contact: " + data.contactStatus + "," + data.contactStatusSupported);

                try {
                    // Java Objects to JSON
                    // TODO export to generic method
                    Gson gson = new Gson();

                    String jsonStr = gson.toJson(data);
                    Log.d(TAG, "retJSONString = " + jsonStr);

                    JSONObject jsonObject = new JSONObject(jsonStr);
                    Log.d(TAG, "jsonObject = " + jsonObject.toString());

                    JSObject ret = JSObject.fromJSONObject(jsonObject);
                    Log.d(TAG, "ret = " + ret.toString());

                    notifyListeners("hrNotificationReceived", ret);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "JSONException =" + e.getLocalizedMessage());
                }
            }

            @Override
            public void polarFtpFeatureReady(String s) {
                Log.d(TAG, "FTP ready = " + s);
            }
        });
    }

    //@SuppressWarnings("MissingPermission")
    private void startOhrPPIStreaming(String deviceId) {
        Log.d(TAG, "startOhrPPIStreaming on DEVICE_ID " + deviceId);
        if (ppiDisposable == null) {
            Log.d(TAG, "startOhrPPIStreaming ppiDisposable");
            ppiDisposable = api.startOhrPPIStreaming(deviceId).observeOn(AndroidSchedulers.mainThread()).subscribe(
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
                                Log.d(TAG, "retJSONString = " + retJSONString);

                                JSONObject retTmp = new JSONObject(retJSONString);
                                Log.d(TAG, "retTmp = " + retTmp.toString());

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
            Log.d(TAG, "startOhrPPIStreaming - init ppiDisposable");
            Log.d(TAG, ppiDisposable.toString());
        } else {
            Log.d(TAG, "startOhrPPIStreaming - dispose not null");
            ppiDisposable.dispose();
            ppiDisposable = null;
        }
    }

    @PluginMethod()
    public void disconnect(PluginCall call) {
        String deviceId = call.getString("deviceId");

        // TODO check connection state before trigger deconnect

        Log.i(TAG, "disconnecting From Device.... " + deviceId);
        try {
            api.disconnectFromDevice(deviceId);
            Log.d(TAG, "disconnected From Device " + deviceId);

            /* RESULT */

            JSObject ret = new JSObject();
            ret.put("value", deviceId);
            call.success(ret);

        } catch (PolarInvalidArgument polarInvalidArgument) {
            polarInvalidArgument.printStackTrace();
            call.reject(polarInvalidArgument.getLocalizedMessage(), polarInvalidArgument);
        }
    }

    /**
     * @see https://capacitor.ionicframework.com/docs/plugins/android#permissions
     */
    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.i(TAG, "handling request perms result");

        PluginCall savedCall = getSavedCall();
        if (savedCall == null) {
            Log.i(TAG, "No stored plugin call for permissions request result");
            return;
        }

        for (int result : grantResults) {
            Log.i(TAG, "grantResults result : " + result);
            if (result == PackageManager.PERMISSION_DENIED) {
                savedCall.error("User denied permission");
                return;
            }
        }


        for (int i = 0; i < grantResults.length; i++) {
            int result = grantResults[i];
            String perm = permissions[i];
            Log.i(TAG, "grantResults result : " + result);
            if (result == PackageManager.PERMISSION_DENIED) {
                Log.d(getLogTag(), "User denied permission: " + perm);
                savedCall.error(PERMISSION_DENIED_ERROR);
                return;
            }
        }

        if (requestCode == REQUEST_ENABLE_BT) {
            Log.i(TAG, "We got the permission to ");
            // TODO
        }

        if (savedCall.getMethodName().equals("connect")) {
            Log.i(TAG, "savedCall after permission permission = connect");
            connect(savedCall);
        }
    }

}
