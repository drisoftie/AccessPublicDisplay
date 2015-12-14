package de.stuttgart.uni.vis.access.accessserver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages BLE Advertising independent of the main app.
 * If the app goes off screen (or gets killed completely) advertising can continue because this
 * Service is maintaining the necessary Callback in memory.
 */
public class ServiceAdvertise extends Service {

    public static final  String  ADVERTISING_FAILED            = "com.example.android.bluetoothadvertisements.advertising_failed";
    public static final  String  ADVERTISING_FAILED_EXTRA_CODE = "failureCode";
    public static final  int     ADVERTISING_TIMED_OUT         = 6;
    private static final String  TAG                           = ServiceAdvertise.class.getSimpleName();
    /**
     * A global variable to let AdvertiserFragment check if the Service is running without needing
     * to start or bind to it.
     * This is the best practice method as defined here:
     * https://groups.google.com/forum/#!topic/android-developers/jEvXMWgbgzE
     */
    public static        boolean running                       = false;
    private String advertisement = "Hello";
    private BluetoothManager      blManager;
    private BluetoothLeAdvertiser blLeAdvertiser;
    private BluetoothGattServer   blGattServer;
    private GattCallback          blGattCallback;
    private AdvertiseCallback     blAdvertiseCallback;

    private Handler  mHandler;
    private Runnable timeoutRunnable;
    /**
     * Length of time to allow advertising before automatically shutting off. (10 minutes)
     */
    private long              TIMEOUT          = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            advertisement = intent.getStringExtra(getString(R.string.intent_advert_value));
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
        initialize();
        startAdvertising();
        startGattServer();
        setTimeout();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(getString(R.string.bndl_advert_value)));
        super.onCreate();
    }

    private void startGattServer() {
        blGattCallback = new GattCallback();
        blGattServer = blManager.openGattServer(this, blGattCallback);
        addDeviceInfoService(blGattServer);

        final String SERVICE_A   = "0000fff0-0000-1000-8000-00805f9b34fb";
        final String CHAR_READ_1 = "00fff1-0000-1000-8000-00805f9b34fb";
        final String CHAR_READ_2 = "00fff2-0000-1000-8000-00805f9b34fb";
        final String CHAR_WRITE  = "00fff3-0000-1000-8000-00805f9b34fb";


        blGattServer.clearServices();

        BluetoothGattCharacteristic read1Characteristic = new BluetoothGattCharacteristic(UUID.fromString(CHAR_READ_1),
                                                                                          BluetoothGattCharacteristic.PROPERTY_READ,
                                                                                          BluetoothGattCharacteristic.PERMISSION_READ);
        byte[] bytes = getString(R.string.bl_advert_val).getBytes();
        read1Characteristic.setValue(bytes);

        BluetoothGattCharacteristic read2Characteristic = new BluetoothGattCharacteristic(UUID.fromString(CHAR_READ_2),
                                                                                          BluetoothGattCharacteristic.PROPERTY_READ,
                                                                                          BluetoothGattCharacteristic.PERMISSION_READ);

        bytes = getString(R.string.bl_advert_val2).getBytes();

        read2Characteristic.setValue(bytes);


        BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(CHAR_WRITE),
                                                                                          BluetoothGattCharacteristic.PROPERTY_WRITE,
                                                                                          BluetoothGattCharacteristic.PERMISSION_WRITE);
        writeCharacteristic.setValue("blub".getBytes());


        BluetoothGattService aService = new BluetoothGattService(UUID.fromString(SERVICE_A), BluetoothGattService.SERVICE_TYPE_PRIMARY);


        aService.addCharacteristic(read1Characteristic);
        aService.addCharacteristic(read2Characteristic);
        aService.addCharacteristic(writeCharacteristic);

        // Add notify characteristic here !!!

        blGattServer.addService(aService);
    }

    @Override
    public void onDestroy() {
        /**
         * Note that onDestroy is not guaranteed to be called quickly or at all. Services exist at
         * the whim of the system, and onDestroy can be delayed or skipped entirely if memory need
         * is critical.
         */
        running = false;
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
        if (blLeAdvertiser == null) {
            blManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (blManager != null) {
                BluetoothAdapter mBluetoothAdapter = blManager.getAdapter();
                if (mBluetoothAdapter != null) {
                    blLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                } else {
                    Toast.makeText(this, getString(R.string.bt_null), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, getString(R.string.bt_null), Toast.LENGTH_LONG).show();
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
                Log.d(TAG, "ServiceAdvertise has reached timeout of " + TIMEOUT + " milliseconds, stopping advertising.");
                sendFailureIntent(ADVERTISING_TIMED_OUT);
                stopSelf();
            }
        };
        mHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }

    /**
     * Starts BLE Advertising.
     */
    private void startAdvertising() {
        Log.d(TAG, "Service: Starting Advertising");

        if (blAdvertiseCallback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData data = buildAdvertiseData();
            blAdvertiseCallback = new SampleAdvertiseCallback();

            if (blLeAdvertiser != null) {
                blLeAdvertiser.startAdvertising(settings, data, blAdvertiseCallback);
                createStartNotification();
            }
        }
    }

    private void createStartNotification() {
        // start notification
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
        nBuilder.setSmallIcon(R.drawable.ic_action_bl_advert);
        nBuilder.setContentTitle(getString(R.string.ntxt_advert_run));
        nBuilder.setContentText(getString(R.string.ntxt_advert_run_descr, advertisement));

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, ActServerAdvertise.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ActServerAdvertise.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(R.id.nid_main, PendingIntent.FLAG_UPDATE_CURRENT);
        nBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.

        Intent stopAdvertIntent = new Intent(this, RecvAdvertisement.class);
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
        Log.d(TAG, "Service: Stopping Advertising");
        if (blLeAdvertiser != null) {
            blLeAdvertiser.stopAdvertising(blAdvertiseCallback);
            blAdvertiseCallback = null;
            removeNotification();
        }
    }

    private void addDeviceInfoService(BluetoothGattServer gattServer) {
        if (null == gattServer) {
            return;
        }
        //
        // device info
        //
        final String SERVICE_DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb";
        final String SOFTWARE_REVISION_STRING   = "00002A28-0000-1000-8000-00805f9b34fb";

        BluetoothGattCharacteristic softwareVerCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(SOFTWARE_REVISION_STRING),
                                                                                                BluetoothGattCharacteristic.PROPERTY_READ,
                                                                                                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattService deviceInfoService = new BluetoothGattService(UUID.fromString(SERVICE_DEVICE_INFORMATION),
                                                                          BluetoothGattService.SERVICE_TYPE_PRIMARY);


        softwareVerCharacteristic.setValue(new String("0.0.1").getBytes());

        deviceInfoService.addCharacteristic(softwareVerCharacteristic);
        gattServer.addService(deviceInfoService);
    }

    private void removeNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(R.id.nid_main);
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private AdvertiseData buildAdvertiseData() {

        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(Constants.Service_UUID);
        dataBuilder.setIncludeDeviceName(true);

        /* For example - this will cause advertising to fail (exceeds size limit) */
        String failureData = advertisement;
        dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());

        return dataBuilder.build();
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    /**
     * Builds and sends a broadcast intent indicating Advertising has failed. Includes the error
     * code as an extra. This is intended to be picked up by the {@code AdvertiserFragment}.
     */
    private void sendFailureIntent(int errorCode) {
        Intent failureIntent = new Intent();
        failureIntent.setAction(ADVERTISING_FAILED);
        failureIntent.putExtra(ADVERTISING_FAILED_EXTRA_CODE, errorCode);
        sendBroadcast(failureIntent);
    }


    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private class SampleAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            Log.d(TAG, "Advertising failed");
            sendFailureIntent(errorCode);
            stopSelf();

        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            //            Log.i(TAG, "Advertising successfully started");
        }
    }

    private class GattCallback extends BluetoothGattServerCallback {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d("GattServer", "Our gatt server connection state changed, new state ");
            Log.d("GattServer", Integer.toString(newState));
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.d("GattServer", "Our gatt server service was added.");
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            Log.d("GattServer", "Our gatt characteristic was read.");
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            blGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d("GattServer", "We have received a write request for one of our hosted characteristics");
            Log.d("GattServer", "data = " + value.toString());
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            Log.d("GattServer", "onNotificationSent");
            super.onNotificationSent(device, status);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.d("GattServer", "Our gatt server descriptor was read.");
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d("GattServer", "Our gatt server descriptor was written.");
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Log.d("GattServer", "Our gatt server on execute write.");
            super.onExecuteWrite(device, requestId, execute);
        }
    }
}