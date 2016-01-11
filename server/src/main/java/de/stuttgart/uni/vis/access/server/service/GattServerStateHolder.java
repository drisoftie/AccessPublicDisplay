package de.stuttgart.uni.vis.access.server.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.util.Log;

import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.server.AccessApp;
import de.stuttgart.uni.vis.access.server.R;

/**
 * @author Alexander Dridiger
 */
public class GattServerStateHolder {


    private GattCallback        blGattCallback;
    private BluetoothGattServer blGattServerWeather;
    private BluetoothGattServer blGattServerPubTransp;

    public void startGatt(BluetoothManager blManager) {
        blGattCallback = new GattCallback();
        startGattServerWeather(blManager);
        startGattServerPubTransp(blManager);
    }

    public void closeServer() {
        blGattServerWeather.close();
        blGattServerPubTransp.close();
    }

    private void startGattServerWeather(BluetoothManager blManager) {
        blGattServerWeather = blManager.openGattServer(AccessApp.inst(), blGattCallback);
        blGattServerWeather.clearServices();
        addDeviceInfoService(blGattServerWeather);

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

        blGattServerWeather.addService(serviceWeather);
    }

    private void startGattServerPubTransp(BluetoothManager blManager) {
        blGattServerPubTransp = blManager.openGattServer(AccessApp.inst(), blGattCallback);
        blGattServerPubTransp.clearServices();
        addDeviceInfoService(blGattServerPubTransp);

        BluetoothGattService servicePubTransp = new BluetoothGattService(UUID.fromString(Constants.GATT_SERVICE_PUB_TRANSP),
                                                                         BluetoothGattService.SERVICE_TYPE_PRIMARY);

        servicePubTransp.addCharacteristic(createCharacteristic(Constants.GATT_PUB_TRANSP_BUS, BluetoothGattCharacteristic.PROPERTY_READ,
                                                                BluetoothGattCharacteristic.PERMISSION_READ, AccessApp.inst().getString(
                        R.string.bl_advert_bus).getBytes()));
        servicePubTransp.addCharacteristic(createCharacteristic(Constants.GATT_PUB_TRANSP_METRO, BluetoothGattCharacteristic.PROPERTY_READ,
                                                                BluetoothGattCharacteristic.PERMISSION_READ, AccessApp.inst().getString(
                        R.string.bl_advert_metro).getBytes()));
        servicePubTransp.addCharacteristic(createCharacteristic(Constants.GATT_PUB_TRANSP_TRAIN, BluetoothGattCharacteristic.PROPERTY_READ,
                                                                BluetoothGattCharacteristic.PERMISSION_READ, AccessApp.inst().getString(
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
        BluetoothGattCharacteristic softwareVerCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(
                Constants.SOFTWARE_REVISION_STRING), BluetoothGattCharacteristic.PROPERTY_READ,
                                                                                                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattService deviceInfoService = new BluetoothGattService(UUID.fromString(Constants.SERVICE_DEVICE_INFORMATION),
                                                                          BluetoothGattService.SERVICE_TYPE_PRIMARY);


        softwareVerCharacteristic.setValue(new String("0.0.1").getBytes());

        deviceInfoService.addCharacteristic(softwareVerCharacteristic);
        gattServer.addService(deviceInfoService);
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
