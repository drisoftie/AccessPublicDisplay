package de.uni.stuttgart.vis.access.accesstest.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
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
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.NotificationBuilder;
import de.uni.stuttgart.vis.access.accesstest.Constants;
import de.uni.stuttgart.vis.access.accesstest.R;
import de.uni.stuttgart.vis.access.accesstest.act.ActScan;
import de.uni.stuttgart.vis.access.accesstest.brcast.BrRcvScan;

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
    private SampleScanCallback blScanCallback;
    private Handler            handler;
    private Handler            mHandler;
    private Runnable           timeoutRunnable;
    private TtsWrapper         tts;
    /**
     * Length of time to allow advertising before automatically shutting off. (10 minutes)
     */
    private long              TIMEOUT          = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            //            advertisement = intent.getStringExtra(getString(R.string.intent_advert_value));
            //            restartAdvertisement();
        }
    };

    @Override
    public void onCreate() {
        running = true;
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
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(getString(R.string.bndl_advert_value)));
        super.onCreate();
    }

    private boolean checkAccessibility() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        return am.isEnabled() | am.isTouchExplorationEnabled();
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
                        //                        invalidateOptionsMenu();
                    }
                }, SCAN_PERIOD);
                // Kick off a new scan.
                blScanCallback = new SampleScanCallback();
                blLeScanner.startScan(buildScanFilters(), buildScanSettings(), blScanCallback);
                createScanNotification();
            } else {
            }
        } else {
            stopScanning();
            //            invalidateOptionsMenu();
        }
        //        invalidateOptionsMenu();
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
        builder.setServiceUuid(Constants.Service_UUID);
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
        tts.shutDown();
        removeNotification();
        mHandler.removeCallbacks(timeoutRunnable);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
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
        mHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                //                Log.d(TAG, "ServiceAdvertise has reached timeout of " + TIMEOUT + " milliseconds, stopping advertising.");
                //                sendFailureIntent(ADVERTISING_TIMED_OUT);
                stopSelf();
            }
        };
        mHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }

    private void createScanNotification() {
        NotificationCompat.Builder nBuilder = NotificationBuilder.createNotificationBuilder(this, R.id.nid_main,
                                                                                            R.drawable.ic_action_bl_scan, getString(
                        R.string.ntxt_scan), null, ActScan.class);

        NotificationBuilder.addAction(this, nBuilder, R.drawable.ic_action_remove, getString(R.string.nact_stop), BrRcvScan.class,
                                      NotificationBuilder.BROADCAST_RECEIVER);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        nBuilder.setAutoCancel(false);

        Notification n = nBuilder.build();
        mNotificationManager.notify(R.id.nid_main, n);
        startForeground(R.id.nid_main, n);

        tts.queueRead(getString(R.string.ntxt_scan));
    }

    private void createStartNotification(String value) {
        String txtFound      = getString(R.string.ntxt_scan_found);
        String txtFoundDescr = getString(R.string.ntxt_scan_descr, value);

        NotificationCompat.Builder nBuilder = NotificationBuilder.createNotificationBuilder(this, R.id.nid_main,
                                                                                            R.drawable.ic_action_display_visible, txtFound,
                                                                                            txtFoundDescr, ActScan.class);

        NotificationBuilder.addAction(this, nBuilder, R.drawable.ic_action_display_visible, getString(R.string.nact_show), BrRcvScan.class,
                                      NotificationBuilder.BROADCAST_RECEIVER);

        NotificationBuilder.addAction(this, nBuilder, R.drawable.ic_action_remove, getString(R.string.nact_stop), BrRcvScan.class,
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


    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private class SampleScanCallback extends ScanCallback {

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
            if (currDev != null) {
                if (StringUtils.equals(result.getDevice().getAddress(), currDev.getDevice().getAddress())) {
                    String newData = new String(result.getScanRecord().getServiceData().get(Constants.Service_UUID));
                    if (!StringUtils.equals(newData, new String(currDev.getScanRecord().getServiceData().get(Constants.Service_UUID)))) {
                        createStartNotification(newData);
                    }
                }
            } else {
                createStartNotification(new String(result.getScanRecord().getServiceData().get(Constants.Service_UUID)));
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
}