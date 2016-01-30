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
public class GattHandlerPubTransp extends BaseGattHandler {

    private GattCallback callback = new GattCallback();

    public GattHandlerPubTransp() {
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(UUID.fromString(Constants.GATT_SERVICE_PUB_TRANSP));
        constantUuids.add(UUID.fromString(Constants.GATT_PUB_TRANSP_BUS));
        constantUuids.add(UUID.fromString(Constants.GATT_PUB_TRANSP_METRO));
        constantUuids.add(UUID.fromString(Constants.GATT_PUB_TRANSP_TRAIN));
        setConstantUuids(constantUuids);
    }

    @Override
    protected void addServices() {
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

        getServer().addService(servicePubTransp);
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
