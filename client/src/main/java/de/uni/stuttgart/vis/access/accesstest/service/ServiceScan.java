package de.uni.stuttgart.vis.access.accesstest.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
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
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    public static final  String  ADVERTISING_FAILED            = "com.example.android.bluetoothadvertisements.advertising_failed";
    public static final  String  ADVERTISING_FAILED_EXTRA_CODE = "failureCode";
    private static final long    SCAN_PERIOD                   = TimeUnit.MILLISECONDS.convert(3, TimeUnit.MINUTES);
    private static final String  TAG                           = ServiceScan.class.getSimpleName();
    /**
     * A global variable to let AdvertiserFragment check if the Service is running without needing
     * to start or bind to it.
     * This is the best practice method as defined here:
     * https://groups.google.com/forum/#!topic/android-developers/jEvXMWgbgzE
     */
    public static        boolean running                       = false;
    private ScanResult            currDev;
    private boolean               scanning;
    private BluetoothManager      blManager;
    private BluetoothAdapter      blAdapt;
    private BluetoothLeScanner    blLeScanner;
    private ScanCallback          blScanCallback;
    private BluetoothLeAdvertiser blluetoothLeAdvertiser;
    private BluetoothGattServer   blGattServer;
    private AdvertiseCallback     blAdvertiseCallback;
    private Handler               handler;
    private Handler               mHandler;
    private Runnable              timeoutRunnable;
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
            restartAdvertisement();
        }
    };

    private void restartAdvertisement() {
        stopAdvertising();
        startAdvertising();
    }

    @Override
    public void onCreate() {
        running = true;


        blAdapt = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        // Is Bluetooth supported on this device?
        if (blAdapt != null) {

            // Is Bluetooth turned on?
            if (blAdapt.isEnabled()) {
                blLeScanner = blAdapt.getBluetoothLeScanner();
            }
        }
        checkAndScanLeDevices();

        initialize();
        startAdvertising();
        //        startGattServer();
        setTimeout();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(getString(R.string.bndl_advert_value)));
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
                        //                        invalidateOptionsMenu();
                    }
                }, SCAN_PERIOD);
                scanning = true;
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
        scanning = false;
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
        removeNotification();
        stopAdvertising();
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
     * Get references to system Bluetooth objects if we don't have them already.
     */
    private void initialize() {
        if (blluetoothLeAdvertiser == null) {
            blManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (blManager != null) {
                BluetoothAdapter mBluetoothAdapter = blManager.getAdapter();
                if (mBluetoothAdapter != null) {
                    blluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                } else {
                    //                    Toast.makeText(this, getString(R.string.bt_null), Toast.LENGTH_LONG).show();
                }
            } else {
                //                Toast.makeText(this, getString(R.string.bt_null), Toast.LENGTH_LONG).show();
            }
        }

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

    /**
     * Starts BLE Advertising.
     */
    private void startAdvertising() {
        //        Log.d(TAG, "Service: Starting Advertising");
        //
        //        if (blAdvertiseCallback == null) {
        //            AdvertiseSettings settings = buildAdvertiseSettings();
        //            AdvertiseData data = buildAdvertiseData();
        //            blAdvertiseCallback = new SampleAdvertiseCallback();
        //
        //            if (blluetoothLeAdvertiser != null) {
        //                blluetoothLeAdvertiser.startAdvertising(settings, data, blAdvertiseCallback);
        //                createStartNotification();
        //            }
        //        }
    }

    private void createScanNotification() {
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
        nBuilder.setContentTitle(getString(R.string.ntxt_scan));

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, ActScan.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ActScan.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(R.id.nid_main, PendingIntent.FLAG_UPDATE_CURRENT);
        nBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.

        Intent stopAdvertIntent = new Intent(this, BrRcvScan.class);
        // PendingIntent contentIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent contentIntent = PendingIntent.getBroadcast(this, 0, stopAdvertIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        nBuilder.addAction(-1, getString(R.string.nact_stop), contentIntent);

        nBuilder.setAutoCancel(false);

        Notification n = nBuilder.build();
        mNotificationManager.notify(R.id.nid_main, n);
        startForeground(R.id.nid_main, n);
    }

    private void createStartNotification(String value) {
        // start notification
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
        nBuilder.setContentTitle(getString(R.string.ntxt_scan_found));
        nBuilder.setContentText(getString(R.string.ntxt_scan_descr, value));

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, ActScan.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ActScan.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(R.id.nid_main, PendingIntent.FLAG_UPDATE_CURRENT);
        nBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.

        Intent getAdvertInfoIntent = new Intent(this, BrRcvScan.class);
        // PendingIntent contentIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent infoIntent = PendingIntent.getBroadcast(this, 0, getAdvertInfoIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        nBuilder.addAction(-1, getString(R.string.nact_show), infoIntent);

        Intent stopAdvertIntent = new Intent(this, BrRcvScan.class);
        // PendingIntent contentIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent contentIntent = PendingIntent.getBroadcast(this, 0, stopAdvertIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        nBuilder.addAction(-1, getString(R.string.nact_stop), contentIntent);

        nBuilder.setAutoCancel(false);

        Notification n = nBuilder.build();
        mNotificationManager.notify(R.id.nid_main, n);
        startForeground(R.id.nid_main, n);
    }

    /**
     * Stops BLE Advertising.
     */
    private void stopAdvertising() {
        //        Log.d(TAG, "Service: Stopping Advertising");
        //        if (blluetoothLeAdvertiser != null) {
        //            blluetoothLeAdvertiser.stopAdvertising(blAdvertiseCallback);
        //            blAdvertiseCallback = null;
        //            removeNotification();
        //        }
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
                        removeNotification();
                        createStartNotification(newData);
                    }
                }
            } else {
                removeNotification();
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