package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothGattCallback;
import android.os.ParcelUuid;

import java.util.List;
import java.util.UUID;

/**
 * @author Alexander Dridiger
 */
public interface IGattScanHandler {
    List<UUID> getConstantUuids();

    void setConstantUuids(List<UUID> constantUuids);

    boolean match(UUID uuid);

    boolean match(ParcelUuid... uuids);

    boolean match(UUID... uuids);

    boolean match(List<ParcelUuid> uuids);

    BluetoothGattCallback getGattCallback(UUID... uuids);

    BluetoothGattCallback getGattCallback(ParcelUuid... uuids);

    BluetoothGattCallback getGattCallback(List<ParcelUuid> uuids);

    void onGattConnected();

    void onGattDisconnected();
}
