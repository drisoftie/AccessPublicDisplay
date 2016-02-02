package de.uni.stuttgart.vis.access.client.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.uni.stuttgart.vis.access.client.helper.INotifyProv;
import de.uni.stuttgart.vis.access.client.helper.ITtsProv;

/**
 * @author Alexander Dridiger
 */
public abstract class ConnBaseAdvertScan implements IConnAdvertScan, IConnAdvertScanHandler {

    private List<UUID>            constantUuids;
    private ScanCallback          cllbckAdvertScan;
    private BluetoothGattCallback cllbckGatt;

    private List<BluetoothDevice>   connDevices = new ArrayList<>();
    private List<ScanResult>        scanHistory = new ArrayList<>();
    private List<IAdvertSubscriber> subscribers = new ArrayList<>();
    private List<IConnSubscriber>   subsConn    = new ArrayList<>();
    private INotifyProv notifyProv;
    private ITtsProv    ttsProv;

    @Override
    public List<UUID> getConstantUuids() {
        return constantUuids;
    }

    @Override
    public void setConstantUuids(List<UUID> constantUuids) {
        this.constantUuids = constantUuids;
    }

    @Override
    public ScanCallback getScanCallback() {
        return cllbckAdvertScan;
    }

    @Override
    public void setScanCallback(ScanCallback scanCallback) {
        this.cllbckAdvertScan = scanCallback;
    }

    @Override
    public BluetoothGattCallback getGattCallback() {
        return cllbckGatt;
    }

    @Override
    public void setGattCallback(BluetoothGattCallback gattCallback) {
        this.cllbckGatt = gattCallback;
    }

    @Override
    public INotifyProv getNotifyProv() {
        return notifyProv;
    }

    @Override
    public ITtsProv getTtsProv() {
        return ttsProv;
    }

    @Override
    public void setTtsProv(ITtsProv ttsProv) {
        this.ttsProv = ttsProv;
    }

    @Override
    public void setNotifyProv(INotifyProv notifyProv) {
        this.notifyProv = notifyProv;
    }

    @Override
    public BluetoothGattCallback getGattCallback(UUID... uuids) {
        if (match(uuids)) {
            return cllbckGatt;
        }
        return null;
    }

    @Override
    public BluetoothGattCallback getGattCallback(List<ParcelUuid> uuids) {
        if (match(uuids)) {
            return cllbckGatt;
        }
        return null;
    }

    @Override
    public BluetoothGattCallback getGattCallback(ParcelUuid... uuids) {
        if (match(uuids)) {
            return cllbckGatt;
        }
        return null;
    }

    @Override
    public boolean match(UUID uuid) {
        for (UUID myUuid : constantUuids) {
            if (myUuid.equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean match(UUID... uuids) {
        for (UUID uuid : uuids) {
            if (match(uuid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean match(ParcelUuid... uuids) {
        for (ParcelUuid uuid : uuids) {
            if (match(uuid.getUuid())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean match(List<ParcelUuid> uuids) {
        for (ParcelUuid uuid : uuids) {
            if (match(uuid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean match(BluetoothDevice device) {
        if (!connDevices.isEmpty()) {
            for (BluetoothDevice dev : connDevices) {
                if (dev.getAddress().equals(device)) {
                    return true;
                }
            }
        }
        for (ScanResult res : scanHistory) {
            if (res.getDevice() != null && res.getDevice().getAddress().equals(device.getAddress())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addScanResult(ScanResult scanResult) {
        scanHistory.add(scanResult);
    }

    @Override
    public void clearScanHistory() {
        scanHistory.clear();
    }

    @Override
    public void removeScanResult(ScanResult scanResult) {
        scanHistory.remove(scanResult);
    }

    public List<IAdvertSubscriber> getSubscribers() {
        return subscribers;
    }

    public void registerAdvertSubscriber(IAdvertSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void removeAdvertSubscriber(IAdvertSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    @Override
    public IConnAdvertScanHandler registerConnectionSubscriber(IConnSubscriber subscriber) {
        subsConn.add(subscriber);
        return this;
    }

    @Override
    public void registerConnSub(IConnSubscriber sub) {
        subsConn.add(sub);
    }

    @Override
    public List<IConnSubscriber> getConnSubscribers() {
        return subsConn;
    }

    @Override
    public List<BluetoothDevice> getConnDevices() {
        return connDevices;
    }

    @Override
    public void addConnDevice(BluetoothDevice dev) {
        BluetoothDevice foundDev = null;
        for (BluetoothDevice myDev : connDevices) {
            if (dev.getAddress().equals(myDev.getAddress())) {
                foundDev = myDev;
            }
        }
        if (foundDev != null) {
            connDevices.remove(foundDev);
            connDevices.add(dev);
        }
    }
}
