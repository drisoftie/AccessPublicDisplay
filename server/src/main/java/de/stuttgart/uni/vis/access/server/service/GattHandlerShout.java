package de.stuttgart.uni.vis.access.server.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.server.AccessApp;
import de.stuttgart.uni.vis.access.server.R;

/**
 * @author Alexander Dridiger
 */
public class GattHandlerShout extends BaseGattHandler {

    private GattCallback callback = new GattCallback();
    private ScheduledExecutorService shouter;
    private Runnable shout = new Runnable() {

        int count = 0;

        @Override
        public void run() {
            if (!getConnDevices().isEmpty()) {
                switch (count % 5) {
                    case 0:
                        changeGattChar(Constants.GATT_SERVICE_SHOUT.getUuid(), Constants.GATT_SHOUT.getUuid(),
                                       "Wall Mart sausages 50% off");
                        break;
                    case 1:
                        changeGattChar(Constants.GATT_SERVICE_SHOUT.getUuid(), Constants.GATT_SHOUT.getUuid(),
                                       "Breuninger trousers up to 30% off");
                        break;
                    case 2:
                        changeGattChar(Constants.GATT_SERVICE_SHOUT.getUuid(), Constants.GATT_SHOUT.getUuid(), "Prime Mark socks 20% off");
                        break;
                    case 3:
                        changeGattChar(Constants.GATT_SERVICE_SHOUT.getUuid(), Constants.GATT_SHOUT.getUuid(),
                                       "Media Markt TVs up to 70% off!");
                        break;
                    case 4:
                        changeGattChar(Constants.GATT_SERVICE_SHOUT.getUuid(), Constants.GATT_SHOUT.getUuid(),
                                       "Sportarena everything 10% off");
                        count = 0;
                        break;
                }
            }
        }
    };

    public GattHandlerShout() {
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(Constants.GATT_SERVICE_SHOUT.getUuid());
        constantUuids.add(Constants.GATT_SHOUT.getUuid());
        setConstantUuids(constantUuids);
    }

    @Override
    protected void addServices() {
        BluetoothGattService serviceWeather = new BluetoothGattService(Constants.GATT_SERVICE_SHOUT.getUuid(),
                                                                       BluetoothGattService.SERVICE_TYPE_PRIMARY);

        serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_SHOUT.toString(),
                                                              BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                                                              BluetoothGattCharacteristic.PERMISSION_READ, AccessApp.inst().getString(
                        R.string.bl_advert_cloudy).getBytes()));
        getServer().addService(serviceWeather);
    }

    @Override
    public BluetoothGattServerCallback getCallback(UUID uuid) {
        for (UUID myUuid : getConstantUuids()) {
            if (myUuid.equals(uuid)) {
                return callback;
            }
        }
        return null;
    }

    private void checkStopShouting() {
        if (getConnDevices().isEmpty()) {
            stopShouting();
        }
    }

    private void stopShouting() {
        shouter.shutdown();
    }

    private void startShouting() {
        shouter = Executors.newSingleThreadScheduledExecutor();
        shouter.scheduleAtFixedRate(shout, 20, 20, TimeUnit.SECONDS);
    }

    private class GattCallback extends BluetoothGattServerCallback {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    getConnDevices().add(device);
                    startShouting();
                    break;
                default:
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        getConnDevices().remove(device);
                        checkStopShouting();
                    }
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            characteristic.setValue(value);
            if (responseNeeded) {
                getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {

        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
        }
    }
}
