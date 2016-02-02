package de.stuttgart.uni.vis.access.server.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.server.AccessApp;

/**
 * @author Alexander Dridiger
 */
public class GattServerStateHolder {


    private GattCallback        blGattCallback;
    private BluetoothGattServer blGattServerWeather;
    private BluetoothGattServer blGattServerPubTransp;

    private List<IGattHandler> blGattHandler = new ArrayList<>();

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
        GattHandlerWeather handler = new GattHandlerWeather();
        blGattServerWeather = blManager.openGattServer(AccessApp.inst(), blGattCallback);
        handler.setServer(blGattServerWeather);
        handler.prepareServer();
        blGattHandler.add(handler);
    }

    private void startGattServerPubTransp(BluetoothManager blManager) {
        GattHandlerPubTransp handler = new GattHandlerPubTransp();
        blGattServerPubTransp = blManager.openGattServer(AccessApp.inst(), blGattCallback);
        handler.setServer(blGattServerPubTransp);
        handler.prepareServer();
        blGattHandler.add(handler);
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


        softwareVerCharacteristic.setValue("0.0.1".getBytes());

        deviceInfoService.addCharacteristic(softwareVerCharacteristic);
        gattServer.addService(deviceInfoService);
    }

    private BluetoothGattServerCallback getHandlerCallback(ParcelUuid... uuids) {
        for (IGattHandler handler : blGattHandler) {
            BluetoothGattServerCallback callback = handler.getCallback(uuids);
            if (callback != null) {
                return callback;
            }
        }
        return null;
    }

    private BluetoothGattServerCallback getHandlerCallback(UUID... uuids) {
        for (IGattHandler handler : blGattHandler) {
            BluetoothGattServerCallback callback = handler.getCallback(uuids);
            if (callback != null) {
                return callback;
            }
        }
        return null;
    }

    private class GattCallback extends BluetoothGattServerCallback {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d("GattServer", "Our gatt server connection state changed, new state ");
            Log.d("GattServer", Integer.toString(newState));
            BluetoothGattServerCallback callback = getHandlerCallback(device.getUuids());
            if (callback != null) {
                callback.onConnectionStateChange(device, status, newState);
            }
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.d("GattServer", "Our gatt server service was added.");
            BluetoothGattServerCallback callback = getHandlerCallback(service.getUuid());
            if (callback != null) {
                callback.onServiceAdded(status, service);
            }
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            byte[] value = characteristic.getValue();
            Log.d("GattServer", "Our gatt characteristic was read: " + new String(value));
            BluetoothGattServerCallback callback = getHandlerCallback(characteristic.getUuid());
            if (callback != null) {
                callback.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }
            //             byte[] value = characteristic.getValue();
            //            Log.d("GattServer", "Our gatt characteristic was read: " + new String(value));
            //            if (UUID.fromString(Constants.GATT_SERVICE_WEATHER).equals(characteristic.getService().getUuid())) {
            //                blGattServerWeather.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            //            } else if (UUID.fromString(Constants.GATT_SERVICE_PUB_TRANSP).equals(characteristic.getService().getUuid())) {
            //                blGattServerPubTransp.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            //            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d("GattServer", "We have received a write request for one of our hosted characteristics");
            Log.d("GattServer", "data = " + new String(value));
            BluetoothGattServerCallback callback = getHandlerCallback(characteristic.getUuid());
            if (callback != null) {
                callback.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            Log.d("GattServer", "onNotificationSent");
            BluetoothGattServerCallback callback = getHandlerCallback(device.getUuids());
            if (callback != null) {
                callback.onNotificationSent(device, status);
            }
            super.onNotificationSent(device, status);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.d("GattServer", "Our gatt server descriptor was read.");
            BluetoothGattServerCallback callback = getHandlerCallback(descriptor.getUuid());
            if (callback != null) {
                callback.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d("GattServer", "Our gatt server descriptor was written.");
            BluetoothGattServerCallback callback = getHandlerCallback(descriptor.getUuid());
            if (callback != null) {
                callback.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Log.d("GattServer", "Our gatt server on execute write.");
            BluetoothGattServerCallback callback = getHandlerCallback(device.getUuids());
            if (callback != null) {
                callback.onExecuteWrite(device, requestId, execute);
            }
            super.onExecuteWrite(device, requestId, execute);
        }
    }
}
