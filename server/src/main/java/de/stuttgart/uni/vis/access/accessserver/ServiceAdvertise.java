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

import de.stuttgart.uni.vis.access.common.Constants;

/**
 * Manages BLE Advertising independent of the main app.
 * If the app goes off screen (or gets killed completely) advertising can continue because this
 * Service is maintaining the necessary Callback in memory.
 */
public class ServiceAdvertise extends Service implements IAdvertisementReceiver {

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
    private              String  advertisement                 = "Weather Forecast";
    private BluetoothManager      blManager;
    private BluetoothLeAdvertiser blLeAdvertiser;
    private BluetoothGattServer   blGattServerWeather;
    private BluetoothGattServer   blGattServerPubTransp;
    private GattCallback          blGattCallback;
    private AdvertiseCallback     blAdvertiseCallback;

    private Handler  timeoutHandler;
    private Runnable timeoutRunnable;
    /**
     * Length of time to allow advertising before automatically shutting off. (10 minutes)
     */
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver brdRcvr;

    @Override
    public void onCreate() {
        running = true;
        initialize();
        startGattServers();
        startAdvertising();
        setTimeout();
        brdRcvr = new BrRcvAdvertRestart(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvr, new IntentFilter(getString(R.string.intent_advert_value)));
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        running = false;
        stopAdvertising();
        blGattServerWeather.close();
        blGattServerPubTransp.close();
        timeoutHandler.removeCallbacks(timeoutRunnable);
        Intent notify = new Intent(getString(R.string.intent_bl_stopped));
        LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(brdRcvr);
        brdRcvr = null;
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
                BluetoothAdapter blAdapt = blManager.getAdapter();
                if (blAdapt != null) {
                    blLeAdvertiser = blAdapt.getBluetoothLeAdvertiser();
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
        timeoutHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "ServiceAdvertise has reached timeout of " + TIMEOUT + " milliseconds, stopping advertising.");
                sendFailureIntent(ADVERTISING_TIMED_OUT);
                stopSelf();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }

    @Override
    public void onNewAdvertisementString(String advert) {
        advertisement = advert;
    }

    /**
     * Starts BLE Advertising.
     */
    private void startAdvertising() {
        Log.d(TAG, "Service: Starting Advertising");

        if (blAdvertiseCallback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData dataWeather = buildAdvertiseDataWeather();
            AdvertiseData dataPubTransp = buildAdvertiseDataPubTransp();
            blAdvertiseCallback = new SampleAdvertiseCallback();

            if (blLeAdvertiser != null) {
                blLeAdvertiser.startAdvertising(settings, dataWeather, blAdvertiseCallback);
                //                blLeAdvertiser.startAdvertising(settings, dataPubTransp, blAdvertiseCallback);
                createStartNotification();
            }
        }
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private AdvertiseData buildAdvertiseDataWeather() {
        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        //        dataBuilder.addServiceUuid(Constants.UUID_SERVICE_WEATHER);

        dataBuilder.addServiceData(Constants.UUID_SERVICE_WEATHER, advertisement.getBytes());

        return dataBuilder.build();
    }

    private AdvertiseData buildAdvertiseDataPubTransp() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        //        dataBuilder.addServiceUuid(Constants.UUID_SERVICE_WEATHER);

        dataBuilder.addServiceData(Constants.UUID_SERVICE_PUB_TRANSP, "Public Transport".getBytes());

        return dataBuilder.build();
    }


    private void createStartNotification() {
        // start notification
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
        nBuilder.setSmallIcon(R.drawable.ic_action_bl_advert);
        nBuilder.setContentTitle(getString(R.string.ntxt_advert_run));
        nBuilder.setContentText(getString(R.string.ntxt_advert_run_descr, advertisement) + " + " + getString(
                R.string.bl_advert_pub_transp));

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

        Intent stopAdvertIntent = new Intent(this, BrRcvAdvertisement.class);
        // PendingIntent contentIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent contentIntent = PendingIntent.getBroadcast(this, 0, stopAdvertIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        nBuilder.addAction(R.drawable.ic_action_remove, getString(R.string.nact_stop), contentIntent);

        nBuilder.setAutoCancel(false);

        Notification n = nBuilder.build();
        mNotificationManager.notify(R.id.nid_main, n);
        startForeground(R.id.nid_main, n);
    }

    private void removeNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(R.id.nid_main);
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

    private void restartAdvertisement() {
        stopAdvertising();
        startAdvertising();
    }

    @Override
    public void onRestartAdvertisement() {
        restartAdvertisement();
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

    private void startGattServers() {
        blGattCallback = new GattCallback();
        startGattServerWeather();
        startGattServerPubTransp();
    }

    private void startGattServerWeather() {
        blGattServerWeather = blManager.openGattServer(this, blGattCallback);
        blGattServerWeather.clearServices();
        addDeviceInfoService(blGattServerWeather);

        BluetoothGattService serviceWeather = new BluetoothGattService(UUID.fromString(Constants.GATT_SERVICE_WEATHER),
                                                                       BluetoothGattService.SERVICE_TYPE_PRIMARY);

        serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_TODAY, BluetoothGattCharacteristic.PROPERTY_READ,
                                                              BluetoothGattCharacteristic.PERMISSION_READ, getString(
                        R.string.bl_advert_cloudy).getBytes()));
        serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_TOMORROW, BluetoothGattCharacteristic.PROPERTY_READ,
                                                              BluetoothGattCharacteristic.PERMISSION_READ, getString(
                        R.string.bl_advert_rainy).getBytes()));
        serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_DAT, BluetoothGattCharacteristic.PROPERTY_READ,
                                                              BluetoothGattCharacteristic.PERMISSION_READ, getString(
                        R.string.bl_advert_sunny).getBytes()));
        serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_QUERY, BluetoothGattCharacteristic.PROPERTY_WRITE,
                                                              BluetoothGattCharacteristic.PERMISSION_WRITE, "blub".getBytes()));

        blGattServerWeather.addService(serviceWeather);
    }

    private void startGattServerPubTransp() {
        blGattServerPubTransp = blManager.openGattServer(this, blGattCallback);
        blGattServerPubTransp.clearServices();
        addDeviceInfoService(blGattServerPubTransp);

        BluetoothGattService servicePubTransp = new BluetoothGattService(UUID.fromString(Constants.GATT_SERVICE_PUB_TRANSP),
                                                                        BluetoothGattService.SERVICE_TYPE_PRIMARY);

        servicePubTransp.addCharacteristic(createCharacteristic(Constants.GATT_PUB_TRANSP_BUS, BluetoothGattCharacteristic.PROPERTY_READ,
                                                               BluetoothGattCharacteristic.PERMISSION_READ, getString(
                        R.string.bl_advert_bus).getBytes()));
        servicePubTransp.addCharacteristic(createCharacteristic(Constants.GATT_PUB_TRANSP_METRO, BluetoothGattCharacteristic.PROPERTY_READ,
                                                               BluetoothGattCharacteristic.PERMISSION_READ, getString(
                        R.string.bl_advert_metro).getBytes()));
        servicePubTransp.addCharacteristic(createCharacteristic(Constants.GATT_PUB_TRANSP_TRAIN, BluetoothGattCharacteristic.PROPERTY_READ,
                                                               BluetoothGattCharacteristic.PERMISSION_READ, getString(
                        R.string.bl_advert_train).getBytes()));

        blGattServerPubTransp.addService(servicePubTransp);
    }

    private BluetoothGattCharacteristic createCharacteristic(String uuid, int property, int permission, byte[] value) {
        BluetoothGattCharacteristic c = new BluetoothGattCharacteristic(UUID.fromString(uuid), property, permission);
        c.setValue(value);
        return c;
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

    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private class SampleAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            Log.d(TAG, "Advertising failed: " + errorCode);
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    Toast.makeText(ServiceAdvertise.this, "Already started", Toast.LENGTH_SHORT).show();
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    Toast.makeText(ServiceAdvertise.this, "Data too large", Toast.LENGTH_SHORT).show();
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    Toast.makeText(ServiceAdvertise.this, "Unsupported feature", Toast.LENGTH_SHORT).show();
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    Toast.makeText(ServiceAdvertise.this, "Internal error", Toast.LENGTH_SHORT).show();
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    Toast.makeText(ServiceAdvertise.this, "Too many advertisers", Toast.LENGTH_SHORT).show();
                    break;
            }
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
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            byte[] value = characteristic.getValue();
            Log.d("GattServer", "Our gatt characteristic was read: " + new String(value));
            if (UUID.fromString(Constants.GATT_SERVICE_WEATHER).equals(characteristic.getService().getUuid())) {
                blGattServerWeather.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            } else if (UUID.fromString(Constants.GATT_SERVICE_PUB_TRANSP).equals(characteristic.getService().getUuid())) {
                blGattServerPubTransp.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
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