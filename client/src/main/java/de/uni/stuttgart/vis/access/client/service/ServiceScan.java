package de.uni.stuttgart.vis.access.client.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.act.ActPubTransp;
import de.uni.stuttgart.vis.access.client.act.ActWeather;
import de.uni.stuttgart.vis.access.client.helper.INotifyProv;
import de.uni.stuttgart.vis.access.client.helper.ITtsProv;
import de.uni.stuttgart.vis.access.client.helper.NotifyHolder;
import de.uni.stuttgart.vis.access.client.helper.TtsWrapper;

/**
 * Manages BLE Advertising independent of the main app.
 * If the app goes off screen (or gets killed completely) advertising can continue because this
 * Service is maintaining the necessary Callback in memory.
 */
public class ServiceScan extends Service implements IContextProv, ITtsProv, INotifyProv {

    private static final long    SCAN_PERIOD = TimeUnit.MILLISECONDS.convert(3, TimeUnit.MINUTES);
    /**
     * A global variable to let AdvertiserFragment check if the Service is running without needing
     * to start or bind to it.
     * This is the best practice method as defined here:
     * https://groups.google.com/forum/#!topic/android-developers/jEvXMWgbgzE
     */
    public static        boolean running     = false;

    private final Binder serviceBinder = new ServiceBinder();

    private List<IServiceBlListener> serviceListeners = new ArrayList<>();

    private ConnectorAdvertScan connectorAdvertScan;

    private ScanResult         currDev;
    private BluetoothAdapter   blAdapt;
    private BluetoothLeScanner blLeScanner;
    private Handler            handler;
    private Runnable           timeoutRunnable;

    private BroadcastReceiver brdRcvr = new BrdcstRcvrService();

    private TtsWrapper   tts;
    private NotifyHolder notify;

    /**
     * Length of time to allow advertising scanning before automatically shutting off.
     */
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.intent_advert_value));
        filter.addAction(getString(R.string.intent_weather_get));
        filter.addAction(getString(R.string.intent_action_bl_user_stopped));
        LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvr, filter);

        notify = new NotifyHolder();
        notify.setService(this);
        handler = new Handler();
        blAdapt = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        tts = new TtsWrapper(this);

        connectorAdvertScan = new ConnectorAdvertScan(this);
        connectorAdvertScan.setNotifyProv(this);
        connectorAdvertScan.setTtsProv(this);

        // Is Bluetooth supported on this device?
        if (blAdapt != null) {
            // Is Bluetooth turned on?
            if (blAdapt.isEnabled()) {
                blLeScanner = blAdapt.getBluetoothLeScanner();
            }
        }
        checkAndScanLeDevices();
    }

    /**
     * Required for extending service, but this will be a Started Service only, so no need for
     * binding.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private void checkAndScanLeDevices() {
        if (blLeScanner == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
        } else {
            setTimeout();
            scanLeDevice(true);
        }
    }

    private void scanLeDevice(boolean enable) {
        if (enable) {
            // Will stop the scanning after a set time.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                    // invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            // Kick off a new scan.

            blLeScanner.startScan(connectorAdvertScan.buildScanFilters(), connectorAdvertScan.buildScanSettings(),
                                  connectorAdvertScan.getAdvertScanCallback());
            connectorAdvertScan.startingAdvertScan();
            //            } else {
        } else {
            stopScanning();
            // invalidateOptionsMenu();
        }
        // invalidateOptionsMenu();
    }

    /**
     * Stop scanning for BLE Advertisements.
     */
    public void stopScanning() {
        connectorAdvertScan.scanningStopped();
        // Stop the scan, wipe the callback.
        blLeScanner.stopScan(connectorAdvertScan.getAdvertScanCallback());
    }

    /**
     * Starts a delayed Runnable that will cause the BLE Advertising to timeout and stop after a
     * set amount of time.
     */
    private void setTimeout() {
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                //                Log.d(TAG, "ServiceAdvertise has reached timeout of " + TIMEOUT + " milliseconds, stopping advertising.");
                //                sendFailureIntent(ADVERTISING_TIMED_OUT);
                stopSelf();
            }
        };
        handler.postDelayed(timeoutRunnable, TIMEOUT);
    }

    @Override
    public Context provideContext() {
        return this;
    }

    @Override
    public TtsWrapper provideTts() {
        return tts;
    }

    @Override
    public NotifyHolder provideNotify() {
        return notify;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        running = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(brdRcvr);
        tts.shutDown();
        handler.removeCallbacks(timeoutRunnable);
        stopScanning();
        notify.setService(null);
        notify = null;
        connectorAdvertScan = null;

        for (IServiceBlListener l : serviceListeners) {
            l.onConnStopped();
        }
    }

    private class BrdcstRcvrService extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StringUtils.equals(intent.getAction(), getString(R.string.intent_advert_value))) {
                connectorAdvertScan.connectGatt(intent.getParcelableExtra(getString(R.string.bndl_bl_scan_result)));
                String advertisement = intent.getStringExtra(getString(R.string.bndl_bl_show));
                if (getString(Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()).equals(advertisement)) {
                    Intent weatherIntent = new Intent(ServiceScan.this, ActWeather.class);
                    weatherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(weatherIntent);
                } else if (getString(Constants.AdvertiseConst.ADVERTISE_TRANSP.getDescr()).equals(advertisement)) {
                    Intent weatherIntent = new Intent(ServiceScan.this, ActPubTransp.class);
                    weatherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(weatherIntent);
                }

            } else if (StringUtils.equals(intent.getAction(), getString(R.string.intent_weather_get))) {
                //                currDev.getDevice().connectGatt(ServiceScan.this, false, connectorAdvertScan.getGattCallback());
                //                connectorAdvertScan.connectGatt(intent.getParcelableExtra(getString(R.string.bndl_bl_show)));
                notify.createScanNotification();
                currDev = null;
            } else if (StringUtils.equals(intent.getAction(), getString(R.string.intent_action_bl_user_stopped))) {
                for (IServiceBlListener l : serviceListeners) {
                    l.onConnStopped();
                }
                stopSelf();
            }
        }
    }

    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private class BlScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            //            for (ScanResult result : results) {
            //                rcycAdaptDevices.getResults().add(result);
            //            }
            //            rcycAdaptDevices.notifyDataSetChanged();
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (currDev != null && result.getScanRecord() != null && result.getScanRecord().getServiceData() != null &&
                result.getScanRecord().getServiceData().get(Constants.UUID_ADVERT_SERVICE_WEATHER) != null) {
                //                if (StringUtils.equals(result.getDevice().getAddress(), currDev.getDevice().getAddress())) {
                //                    String oldData = new String(currDev.getScanRecord().getServiceData().get(Constants.UUID_ADVERT_SERVICE_WEATHER));
                //                    String newData = new String(result.getScanRecord().getServiceData().get(Constants.UUID_ADVERT_SERVICE_WEATHER));
                //                    if (!StringUtils.equals(newData, oldData)) {
                notify.removeNotification(R.id.nid_main);
                //                        boolean start = false;
                //                        for (byte b : newData.getBytes()) {
                //                            if (b == Constants.AdvertiseConst.ADVERTISE_START) {
                //                                start = true;
                //                            } else if (b == Constants.AdvertiseConst.ADVERTISE_BOOKING.getFlag()) {
                //                            } else if (b == Constants.AdvertiseConst.ADVERTISE_NEWS.getFlag()) {
                //                            } else if (b == Constants.AdvertiseConst.ADVERTISE_TRANSP.getFlag()) {
                //                                //                                createDisplayNotification(getString(Constants.AdvertiseConst.ADVERTISE_TRANSP.getDescr()),
                //                                //                                                          R.id.nid_pub_transp);
                //                            } else if (b == Constants.AdvertiseConst.ADVERTISE_WEATHER.getFlag()) {
                //                                notify.createDisplayNotification(getString(Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()),
                //                                                                 R.id.nid_weather);
                //                            } else if (b == Constants.AdvertiseConst.ADVERTISE_END) {
                //                                break;
                //                            }
                //                        }
                //                    }
                //                }
            } else {
                //                String newData = new String(result.getScanRecord().getServiceData().get(Constants.UUID_ADVERT_SERVICE_WEATHER));
                notify.removeNotification(R.id.nid_main);
                //                boolean start = false;
                //                for (byte b : newData.getBytes()) {
                //                    if (b == Constants.AdvertiseConst.ADVERTISE_START) {
                //                        start = true;
                //                    } else if (b == Constants.AdvertiseConst.ADVERTISE_BOOKING.getFlag()) {
                //                    } else if (b == Constants.AdvertiseConst.ADVERTISE_NEWS.getFlag()) {
                //                    } else if (b == Constants.AdvertiseConst.ADVERTISE_TRANSP.getFlag()) {
                //                        //                        createDisplayNotification(getString(Constants.AdvertiseConst.ADVERTISE_TRANSP.getDescr()),
                //                        //                                                  R.id.nid_pub_transp);
                //                    } else if (b == Constants.AdvertiseConst.ADVERTISE_WEATHER.getFlag()) {
                //                        notify.createDisplayNotification(getString(Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()),
                //                                                         R.id.nid_weather);
                //                    } else if (b == Constants.AdvertiseConst.ADVERTISE_END) {
                //                        break;
                //                    }
                //                }
            }
            currDev = result;
            //            rcycAdaptDevices.getResults().add(result);
            //            rcycAdaptDevices.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            //            Toast.makeText(ActScan.this, "Scan failed with error: " + errorCode, Toast.LENGTH_LONG).show();
        }
    }


    public class ServiceBinder extends Binder implements IServiceBinder {

        @Override
        public void registerServiceListener(IServiceBlListener listener) {
            serviceListeners.add(listener);
        }

        @Override
        public void deRegisterServiceListener(IServiceBlListener listener) {
            serviceListeners.remove(listener);
        }

        @Override
        public IConnAdvertScanHandler subscribeBlConnection(UUID uuid, IConnSubscriber subscriber) {
            return connectorAdvertScan.subscribeBlConnection(uuid, subscriber);
        }
    }
}