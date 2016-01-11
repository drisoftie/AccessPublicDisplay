package de.uni.stuttgart.vis.access.client.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.NotificationBuilder;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.act.ActScan;
import de.uni.stuttgart.vis.access.client.act.ActWeather;
import de.uni.stuttgart.vis.access.client.brcast.BrRcvScan;
import de.uni.stuttgart.vis.access.client.brcast.BrRcvStop;

/**
 * Manages BLE Advertising independent of the main app.
 * If the app goes off screen (or gets killed completely) advertising can continue because this
 * Service is maintaining the necessary Callback in memory.
 */
public class ServiceScan extends Service {

    private static final long    SCAN_PERIOD = TimeUnit.MILLISECONDS.convert(3, TimeUnit.MINUTES);
    /**
     * A global variable to let AdvertiserFragment check if the Service is running without needing
     * to start or bind to it.
     * This is the best practice method as defined here:
     * https://groups.google.com/forum/#!topic/android-developers/jEvXMWgbgzE
     */
    public static        boolean running     = false;
    private ScanResult         currDev;
    private BluetoothAdapter   blAdapt;
    private BluetoothLeScanner blLeScanner;
    private ScanCallback       blScanCallback;
    private Handler            handler;
    private Handler            timeoutHandler;
    private Runnable           timeoutRunnable;
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver brdRcvr = new BrdcstReceiver();
    private TtsWrapper tts;
    /**
     * Length of time to allow advertising scanning before automatically shutting off.
     */
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    @Override
    public void onCreate() {
        running = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.intent_advert_value));
        filter.addAction(getString(R.string.intent_weather_get));
        LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvr, filter);
        handler = new Handler();
        blAdapt = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        tts = new TtsWrapper(this);
        // Is Bluetooth supported on this device?
        if (blAdapt != null) {

            // Is Bluetooth turned on?
            if (blAdapt.isEnabled()) {
                blLeScanner = blAdapt.getBluetoothLeScanner();
            }
        }
        checkAndScanLeDevices();
        setTimeout();
        super.onCreate();
    }

    private void checkAndScanLeDevices() {
        if (blLeScanner == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
        } else {
            scanLeDevice(true);
        }
    }

    private void scanLeDevice(boolean enable) {
        if (enable) {
            if (blScanCallback == null) {
                // Will stop the scanning after a set time.
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopScanning();
                        // invalidateOptionsMenu();
                    }
                }, SCAN_PERIOD);
                // Kick off a new scan.
                blScanCallback = new BlScanCallback();
                blLeScanner.startScan(buildScanFilters(), buildScanSettings(), blScanCallback);
                createScanNotification(true);
            } else {
            }
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
        removeNotification();
        if (blScanCallback != null) {
            // Stop the scan, wipe the callback.
            blLeScanner.stopScan(blScanCallback);
            blScanCallback = null;
        }
        // Even if no new results, update 'last seen' times.
        //        rcycAdaptDevices.notifyDataSetChanged();
    }

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE results around you
        //        builder.setServiceUuid(Constants.UUID_SERVICE_WEATHER);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }

    @Override
    public void onDestroy() {
        /**
         * Note that onDestroy is not guaranteed to be called quickly or at all. Services exist at
         * the whim of the system, and onDestroy can be delayed or skipped entirely if memory need
         * is critical.
         */
        running = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(brdRcvr);
        tts.shutDown();
        removeNotification();
        timeoutHandler.removeCallbacks(timeoutRunnable);
        super.onDestroy();
    }

    /**
     * Required for extending service, but this will be a Started Service only, so no need for
     * binding.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Starts a delayed Runnable that will cause the BLE Advertising to timeout and stop after a
     * set amount of time.
     */
    private void setTimeout() {
        timeoutHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                //                Log.d(TAG, "ServiceAdvertise has reached timeout of " + TIMEOUT + " milliseconds, stopping advertising.");
                //                sendFailureIntent(ADVERTISING_TIMED_OUT);
                stopSelf();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }

    private void createScanNotification(boolean read) {
        NotificationCompat.Builder nBuilder = NotificationBuilder.createNotificationBuilder(this, R.id.nid_main,
                                                                                            R.drawable.ic_action_bl_scan, getString(
                        R.string.ntxt_scan), null, ActScan.class);

        NotificationBuilder.addAction(this, nBuilder, R.drawable.ic_action_remove, getString(R.string.nact_stop), BrRcvStop.class,
                                      NotificationBuilder.BROADCAST_RECEIVER);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        nBuilder.setAutoCancel(false);

        Notification n = nBuilder.build();
        mNotificationManager.notify(R.id.nid_main, n);
        startForeground(R.id.nid_main, n);

        if (read) {
            tts.queueRead(getString(R.string.ntxt_scan));
        }
    }

    private void createDisplayNotification(String value) {
        String txtFound      = getString(R.string.ntxt_scan_found);
        String txtFoundDescr = getString(R.string.ntxt_scan_descr, value);

        NotificationCompat.Builder nBuilder = NotificationBuilder.createNotificationBuilder(this, R.id.nid_main,
                                                                                            R.drawable.ic_action_display_visible, txtFound,
                                                                                            txtFoundDescr, ActScan.class);

        Intent showIntent = new Intent(this, BrRcvScan.class);
        showIntent.putExtra(getString(R.string.bndl_bl_show), value);
        NotificationBuilder.addAction(this, nBuilder, R.drawable.ic_action_display_visible, getString(R.string.nact_show), showIntent,
                                      NotificationBuilder.BROADCAST_RECEIVER);

        NotificationBuilder.addAction(this, nBuilder, R.drawable.ic_action_remove, getString(R.string.nact_stop), BrRcvStop.class,
                                      NotificationBuilder.BROADCAST_RECEIVER);

        nBuilder.setAutoCancel(false);

        Notification n = nBuilder.build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(R.id.nid_main, n);
        startForeground(R.id.nid_main, n);

        tts.queueRead(txtFound, txtFoundDescr);
    }

    private void removeNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(R.id.nid_main);
    }

    private class BrdcstReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StringUtils.equals(intent.getAction(), getString(R.string.intent_advert_value))) {
                String advertisement = intent.getStringExtra(getString(R.string.intent_advert_value));
                Intent weatherIntent = new Intent(ServiceScan.this, ActWeather.class);
                weatherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(weatherIntent);
            } else if (StringUtils.equals(intent.getAction(), getString(R.string.intent_weather_get))) {
                currDev.getDevice().connectGatt(ServiceScan.this, false, new BlGattCallback());
                createScanNotification(false);
                currDev = null;
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
                result.getScanRecord().getServiceData().get(Constants.UUID_SERVICE_WEATHER) != null) {
                if (StringUtils.equals(result.getDevice().getAddress(), currDev.getDevice().getAddress())) {
                    String oldData = new String(currDev.getScanRecord().getServiceData().get(Constants.UUID_SERVICE_WEATHER));
                    String newData = new String(result.getScanRecord().getServiceData().get(Constants.UUID_SERVICE_WEATHER));
                    if (!StringUtils.equals(newData, oldData)) {
                        createDisplayNotification(newData);
                    }
                }
            } else {
                if (result.getScanRecord() != null && result.getScanRecord().getServiceData() != null &&
                    result.getScanRecord().getServiceData().get(Constants.UUID_SERVICE_WEATHER) != null) {
                    createDisplayNotification(new String(result.getScanRecord().getServiceData().get(Constants.UUID_SERVICE_WEATHER)));
                }
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

    private class BlGattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService s = gatt.getService(UUID.fromString(Constants.GATT_SERVICE_WEATHER));
                if (s != null) {
                    BluetoothGattCharacteristic weatherC = s.getCharacteristic(UUID.fromString(Constants.GATT_WEATHER_TODAY));
                    gatt.readCharacteristic(weatherC);
                } else {
                    s = gatt.getService(UUID.fromString(Constants.GATT_SERVICE_PUB_TRANSP));
                    BluetoothGattCharacteristic transpC = s.getCharacteristic(UUID.fromString(Constants.GATT_PUB_TRANSP_BUS));
                    gatt.readCharacteristic(transpC);
                }
            } else {
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getValue() != null) {
                    if (UUID.fromString(Constants.GATT_WEATHER_QUERY).equals(characteristic.getUuid())) {

                    } else if (UUID.fromString(Constants.GATT_WEATHER_TODAY).equals(characteristic.getUuid())) {
                        byte[] weather = characteristic.getValue();
                        Intent weatherIntent = new Intent(getString(R.string.intent_gatt_weather));
                        weatherIntent.putExtra(getString(R.string.bndl_gatt_weather_today), weather);
                        LocalBroadcastManager.getInstance(ServiceScan.this).sendBroadcast(weatherIntent);
                        BluetoothGattCharacteristic weatherC = characteristic.getService().getCharacteristic(UUID.fromString(
                                Constants.GATT_WEATHER_TOMORROW));
                        gatt.readCharacteristic(weatherC);
                    } else if (UUID.fromString(Constants.GATT_WEATHER_TOMORROW).equals(characteristic.getUuid())) {
                        byte[] weather = characteristic.getValue();
                        Intent weatherIntent = new Intent(getString(R.string.intent_gatt_weather));
                        weatherIntent.putExtra(getString(R.string.bndl_gatt_weather_tomorrow), weather);
                        LocalBroadcastManager.getInstance(ServiceScan.this).sendBroadcast(weatherIntent);
                        BluetoothGattCharacteristic weatherC = characteristic.getService().getCharacteristic(UUID.fromString(
                                Constants.GATT_WEATHER_DAT));
                        gatt.readCharacteristic(weatherC);
                    } else if (UUID.fromString(Constants.GATT_WEATHER_DAT).equals(characteristic.getUuid())) {
                        byte[] weather = characteristic.getValue();
                        Intent weatherIntent = new Intent(getString(R.string.intent_gatt_weather));
                        weatherIntent.putExtra(getString(R.string.bndl_gatt_weather_dat), weather);
                        LocalBroadcastManager.getInstance(ServiceScan.this).sendBroadcast(weatherIntent);
                        gatt.close();
                    } else if (UUID.fromString(Constants.GATT_PUB_TRANSP_BUS).equals(characteristic.getUuid())) {
                        byte[] transp = characteristic.getValue();
                        Intent transpIntent = new Intent(getString(R.string.intent_gatt_pub_transp));
                        transpIntent.putExtra(getString(R.string.bndl_gatt_pub_transp_bus), transp);
                        LocalBroadcastManager.getInstance(ServiceScan.this).sendBroadcast(transpIntent);
                    } else if (UUID.fromString(Constants.GATT_PUB_TRANSP_METRO).equals(characteristic.getUuid())) {
                        byte[] transp = characteristic.getValue();
                        Intent transpIntent = new Intent(getString(R.string.intent_gatt_pub_transp));
                        transpIntent.putExtra(getString(R.string.bndl_gatt_pub_transp_metro), transp);
                        LocalBroadcastManager.getInstance(ServiceScan.this).sendBroadcast(transpIntent);
                    } else if (UUID.fromString(Constants.GATT_PUB_TRANSP_TRAIN).equals(characteristic.getUuid())) {
                        byte[] transp = characteristic.getValue();
                        Intent transpIntent = new Intent(getString(R.string.intent_gatt_pub_transp));
                        transpIntent.putExtra(getString(R.string.bndl_gatt_pub_transp_metro), transp);
                        LocalBroadcastManager.getInstance(ServiceScan.this).sendBroadcast(transpIntent);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        }
    }
}