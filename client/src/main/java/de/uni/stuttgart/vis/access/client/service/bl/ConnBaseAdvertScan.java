package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.uni.stuttgart.vis.access.client.helper.INotifyProv;
import de.uni.stuttgart.vis.access.client.helper.ITtsProv;

/**
 * @author Alexander Dridiger
 */
public abstract class ConnBaseAdvertScan implements IConnAdvertScan, IConnGattProvider, IConnAdvertProvider {

    private Queue<AccessGatt>        accessGatts = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService e           = Executors.newSingleThreadScheduledExecutor();
    private BluetoothGatt         lastGattInst;
    private List<UUID>            constantUuids;
    private ScanCallback          cllbckAdvertScan;
    private BluetoothGattCallback cllbckGatt;

    private List<BluetoothDevice>       connDevices    = new ArrayList<>();
    private List<ScanResult>            scanHistory    = new ArrayList<>();
    private List<IConnAdvertSubscriber> subsConnAdvert = new ArrayList<>();
    private List<IConnGattSubscriber>   subsConnGatt   = new ArrayList<>();
    private INotifyProv notifyProv;
    private ITtsProv    ttsProv;

    public Queue<AccessGatt> getAccessGatts() {
        return accessGatts;
    }

    public void setAccessGatts(Queue<AccessGatt> accessGatts) {
        this.accessGatts = accessGatts;
    }

    protected ScheduledExecutorService getExecutor() {
        return e;
    }

    public void setExecutor(ScheduledExecutorService e) {
        this.e = e;
    }

    public void access(AccessGatt access) {
        if (accessGatts.isEmpty()) {
            accessGatts.add(access);
            e.schedule(access, 500, TimeUnit.MILLISECONDS);
        } else {
            accessGatts.add(access);
        }
    }

    public void checkWork() {
        if (!accessGatts.isEmpty()) {
            e.schedule(accessGatts.poll(), 500, TimeUnit.MILLISECONDS);
        }
    }

    public void setGattInst(BluetoothGatt lastGattInst) {
        this.lastGattInst = lastGattInst;
    }

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
    public void setNotifyProv(INotifyProv notifyProv) {
        this.notifyProv = notifyProv;
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
                if (dev.getAddress().equals(device.getAddress())) {
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
    public List<ScanResult> getScanResults() {
        return scanHistory;
    }

    @Override
    public void addScanResult(ScanResult scanResult) {
        if (scanResult.getScanRecord() != null && scanResult.getScanRecord().getServiceData() != null) {
            ScanResult foundRes = null;
            for (ScanResult myRes : scanHistory) {
                if (scanResult.getDevice().getAddress().equals(myRes.getDevice().getAddress())) {
                    if (scanResult.getScanRecord() != null && myRes.getScanRecord() != null && Objects.equals(
                            scanResult.getScanRecord().getServiceData(), myRes.getScanRecord().getServiceData())) {
                        foundRes = myRes;
                        break;
                    }
                }
            }
            if (foundRes != null) {
                scanHistory.remove(foundRes);
                scanHistory.add(scanResult);
            } else {
                scanHistory.add(scanResult);
            }
        }
    }

    @Override
    public void clearScanHistory() {
        scanHistory.clear();
    }

    @Override
    public void removeScanResult(ScanResult scanResult) {
        scanHistory.remove(scanResult);
    }

    @Override
    public List<IConnAdvertSubscriber> getConnAdvertSubscribers() {
        return subsConnAdvert;
    }

    @Override
    public IConnAdvertProvider registerConnectionAdvertSubscriber(UUID uuid, IConnAdvertSubscriber subscriber) {
        if (!subsConnAdvert.contains(subscriber)) {
            subsConnAdvert.add(subscriber);
        }
        return this;
    }

    @Override
    public void registerConnAdvertSub(IConnAdvertSubscriber subscriber) {
        if (!subsConnAdvert.contains(subscriber)) {
            subsConnAdvert.add(subscriber);
        }
    }

    @Override
    public void deregisterConnAdvertSub(IConnAdvertSubscriber subscriber) {
        subsConnAdvert.remove(subscriber);
    }

    @Override
    public IConnGattProvider registerConnectionGattSubscriber(UUID uuid, IConnGattSubscriber subscriber) {
        if (!subsConnGatt.contains(subscriber)) {
            subsConnGatt.add(subscriber);
        }
        return this;
    }

    @Override
    public void registerConnGattSub(IConnGattSubscriber sub) {
        if (!subsConnGatt.contains(sub)) {
            subsConnGatt.add(sub);
        }
    }

    @Override
    public List<IConnGattSubscriber> getConnGattSubscribers() {
        return subsConnGatt;
    }

    @Override
    public void deregisterConnGattSub(IConnGattSubscriber sub) {
        subsConnGatt.remove(sub);
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
        } else {
            connDevices.add(dev);
        }
    }

    @Override
    public void removeConnDevice(BluetoothDevice deviceToRemove) {
        for (BluetoothDevice dev : connDevices) {
            if (dev.getAddress().equals(deviceToRemove.getAddress())) {
                connDevices.remove(dev);
            }
        }
    }

    @Override
    public boolean hasConnDevice(BluetoothDevice deviceToFind) {
        boolean has = false;
        for (BluetoothDevice dev : connDevices) {
            if (dev.getAddress().equals(deviceToFind.getAddress())) {
                has = true;
                break;
            }
        }
        return has;
    }

    public BluetoothGatt getLastGattInst() {
        return lastGattInst;
    }

    public abstract class AccessGatt implements Runnable {

        @Override
        public void run() {
            onRun();
        }

        public abstract void onRun();
    }
}