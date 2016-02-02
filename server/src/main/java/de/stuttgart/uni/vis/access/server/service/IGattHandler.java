package de.stuttgart.uni.vis.access.server.service;

import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.os.ParcelUuid;

import java.util.List;
import java.util.UUID;

/**
 * @author Alexander Dridiger
 */
public interface IGattHandler {
    BluetoothGattServer getServer();

    void setServer(BluetoothGattServer server);

    List<UUID> getConstantUuids();

    void setConstantUuids(List<UUID> constantUuids);

    void prepareServer();

    BluetoothGattServerCallback getCallback(ParcelUuid[] uuids);

    BluetoothGattServerCallback getCallback(UUID[] uuids);

    BluetoothGattServerCallback getCallback(ParcelUuid uuid);

    BluetoothGattServerCallback getCallback(UUID uuids);
}
