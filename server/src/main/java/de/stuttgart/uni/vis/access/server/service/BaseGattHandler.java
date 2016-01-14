package de.stuttgart.uni.vis.access.server.service;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;

import java.util.List;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;

/**
 * @author Alexander Dridiger
 */
public abstract class BaseGattHandler {

    private BluetoothGattServer server;
    private List<UUID>          constantUuids;

    public BluetoothGattServer getServer() {
        return server;
    }

    public void setServer(BluetoothGattServer server) {
        this.server = server;
    }

    public List<UUID> getConstantUuids() {
        return constantUuids;
    }

    public void setConstantUuids(List<UUID> constantUuids) {
        this.constantUuids = constantUuids;
    }

    public void prepareServer() {
        server.clearServices();
        addDeviceInfoService(server);

        addServices();
    }

    protected void addDeviceInfoService(BluetoothGattServer gattServer) {
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

    protected abstract void addServices();

    protected BluetoothGattCharacteristic createCharacteristic(String uuid, int property, int permission, byte[] value) {
        BluetoothGattCharacteristic c = new BluetoothGattCharacteristic(UUID.fromString(uuid), property, permission);
        c.setValue(value);
        return c;
    }


    public BluetoothGattServerCallback getCallback(ParcelUuid[] uuids) {
        for (ParcelUuid uuid : uuids) {
            BluetoothGattServerCallback callback = getCallback(uuid);
            if (callback != null) {
                return callback;
            }
        }
        return null;
    }

    public BluetoothGattServerCallback getCallback(UUID[] uuids) {
        for (UUID uuid : uuids) {
            BluetoothGattServerCallback callback = getCallback(uuid);
            if (callback != null) {
                return callback;
            }
        }
        return null;
    }

    public BluetoothGattServerCallback getCallback(ParcelUuid uuid) {
        return getCallback(uuid.getUuid());
    }

    public abstract BluetoothGattServerCallback getCallback(UUID uuids);
}
