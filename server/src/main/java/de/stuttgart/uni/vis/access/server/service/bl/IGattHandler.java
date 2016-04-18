package de.stuttgart.uni.vis.access.server.service.bl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.os.ParcelUuid;

import com.drisoftie.action.async.handler.IFinishedHandler;

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

    List<BluetoothDevice> getConnDevices();

    void checkAndAddDevice(BluetoothDevice device);

    void checkAndRemoveDevice(BluetoothDevice device);

    void prepareServer();

    void prepareServices(IFinishedHandler<UUID> readyListener);

    IFinishedHandler<UUID> getServicesReadyListener();

    void changeGattChar(UUID servUuid, UUID charUuid, String value);

    void changeGattChar(UUID servUuid, UUID charUuid, byte[] value);

    BluetoothGattServerCallback getCallback();

    BluetoothGattServerCallback getCallback(ParcelUuid[] uuids);

    BluetoothGattServerCallback getCallback(UUID[] uuids);

    BluetoothGattServerCallback getCallback(ParcelUuid uuid);

    BluetoothGattServerCallback getCallback(UUID uuids);
}
