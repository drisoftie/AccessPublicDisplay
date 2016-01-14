package de.stuttgart.uni.vis.access.server.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.server.AccessApp;
import de.stuttgart.uni.vis.access.server.R;

/**
 * @author Alexander Dridiger
 */
public class GattHandlerWeather extends BaseGattHandler {


    private GattCallback callback = new GattCallback();

    public GattHandlerWeather() {
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(UUID.fromString(Constants.GATT_SERVICE_WEATHER));
        setConstantUuids(constantUuids);
    }

    @Override
    protected void addServices() {
        BluetoothGattService serviceWeather = new BluetoothGattService(UUID.fromString(Constants.GATT_SERVICE_WEATHER),
                                                                       BluetoothGattService.SERVICE_TYPE_PRIMARY);

        serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_TODAY, BluetoothGattCharacteristic.PROPERTY_READ,
                                                              BluetoothGattCharacteristic.PERMISSION_READ, AccessApp.inst().getString(
                        R.string.bl_advert_cloudy).getBytes()));
        serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_TOMORROW, BluetoothGattCharacteristic.PROPERTY_READ,
                                                              BluetoothGattCharacteristic.PERMISSION_READ, AccessApp.inst().getString(
                        R.string.bl_advert_rainy).getBytes()));
        serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_DAT, BluetoothGattCharacteristic.PROPERTY_READ,
                                                              BluetoothGattCharacteristic.PERMISSION_READ, AccessApp.inst().getString(
                        R.string.bl_advert_sunny).getBytes()));
        serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_QUERY, BluetoothGattCharacteristic.PROPERTY_WRITE,
                                                              BluetoothGattCharacteristic.PERMISSION_WRITE, "blub".getBytes()));

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

    private class GattCallback extends BluetoothGattServerCallback {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
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
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
        }
    }
}
