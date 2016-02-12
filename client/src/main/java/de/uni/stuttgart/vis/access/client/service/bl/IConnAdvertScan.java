package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import java.util.List;
import java.util.UUID;

import de.uni.stuttgart.vis.access.client.helper.INotifyProv;
import de.uni.stuttgart.vis.access.client.helper.ITtsProv;

/**
 * @author Alexander Dridiger
 */
public interface IConnAdvertScan {

    List<UUID> getConstantUuids();

    void setConstantUuids(List<UUID> constantUuids);

    ScanCallback getScanCallback();

    void setScanCallback(ScanCallback scanCallback);

    BluetoothGattCallback getGattCallback();

    void setGattCallback(BluetoothGattCallback gattCallback);

    INotifyProv getNotifyProv();

    void setNotifyProv(INotifyProv notifyProv);

    ITtsProv getTtsProv();

    void setTtsProv(ITtsProv ttsProv);

    BluetoothGattCallback getGattCallback(UUID... uuids);

    BluetoothGattCallback getGattCallback(List<ParcelUuid> uuids);

    BluetoothGattCallback getGattCallback(ParcelUuid... uuids);

    boolean match(UUID uuid);

    boolean match(UUID... uuids);

    boolean match(ParcelUuid... uuids);

    boolean match(List<ParcelUuid> uuids);

    boolean match(BluetoothDevice device);

    List<ScanResult> getScanResults();

    void addScanResult(ScanResult scanResult);

    void clearScanHistory();

    void removeScanResult(ScanResult scanResult);

    IConnAdvertProvider registerConnectionAdvertSubscriber(IConnAdvertProvider.IConnAdvertSubscriber subscriber);

    List<IConnAdvertProvider.IConnAdvertSubscriber> getConnAdvertSubscribers();

    IConnGattProvider registerConnectionGattSubscriber(UUID uuid, IConnGattProvider.IConnGattSubscriber subscriber);

    List<IConnGattProvider.IConnGattSubscriber> getConnGattSubscribers();

    List<BluetoothDevice> getConnDevices();

    void addConnDevice(BluetoothDevice dev);
}
