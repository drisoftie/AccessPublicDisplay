package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.util.AccessGatt;
import de.uni.stuttgart.vis.access.client.BuildConfig;
import de.uni.stuttgart.vis.access.client.helper.INotifyProv;
import de.uni.stuttgart.vis.access.client.helper.ITtsProv;

/**
 * @author Alexander Dridiger
 */
public abstract class ConnBaseAdvertScan implements IConnAdvertScan, IConnGattProvider, IConnAdvertProvider, AccessGatt.IGatt {

    private Queue<AccessGatt>        accessGatts = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService e           = Executors.newSingleThreadScheduledExecutor();
    private BluetoothGatt         lastGattInst;
    private List<UUID>            constantUuids;
    private ScanCallback          cllbckAdvertScan;
    private BluetoothGattCallback cllbckGatt;

    private List<ScanResult>            scanHistory    = new ArrayList<>();
    private List<IConnAdvertSubscriber> subsConnAdvert = new ArrayList<>();
    private List<IConnGattSubscriber>   subsConnGatt   = new ArrayList<>();
    private INotifyProv notifyProv;
    private ITtsProv    ttsProv;

    @Override
    public List<UUID> getConstantUuids() {
        if (constantUuids == null) {
            constantUuids = new ArrayList<>();
        }
        return constantUuids;
    }

    @Override
    public void setConstantUuids(List<UUID> constantUuids) {
        this.constantUuids = constantUuids;
    }

    @Override
    public IConnAdvertScan addUuid(UUID uuid) {
        getConstantUuids().add(uuid);
        return this;
    }

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
        if (getAccessGatts().isEmpty()) {
            e.schedule(access, 1000, TimeUnit.MILLISECONDS);
        } else {
            getAccessGatts().add(access);
        }
    }

    @Override
    public void checkWork() {
        if (!getAccessGatts().isEmpty()) {
            e.schedule(getAccessGatts().poll(), 1000, TimeUnit.MILLISECONDS);
        }
    }

    public void setGattInst(BluetoothGatt lastGattInst) {
        this.lastGattInst = lastGattInst;
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
        if (!getScanResults().isEmpty()) {
            for (int i = 0; i < getScanResults().size(); i++) {
                ScanResult res = getScanResults().get(i);
                if (res.getDevice().getAddress().equals(device.getAddress())) {
                    return true;
                }
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
            for (ScanResult myRes : getScanResults()) {
                if (scanResult.getDevice().getAddress().equals(myRes.getDevice().getAddress())) {
                    if (scanResult.getScanRecord() != null && myRes.getScanRecord() != null && Objects.equals(
                            scanResult.getScanRecord().getServiceData(), myRes.getScanRecord().getServiceData())) {
                        foundRes = myRes;
                        break;
                    }
                }
            }
            if (foundRes != null) {
                getScanResults().remove(foundRes);
                getScanResults().add(scanResult);
            } else {
                getScanResults().add(scanResult);
            }
        }
    }

    @Override
    public void clearScanHistory() {
        getScanResults().clear();
    }

    @Override
    public void removeScanResult(String macAddress) {
        for (int i = 0; i < getScanResults().size(); i++) {
            ScanResult scan = getScanResults().get(i);
            if (scan.getDevice().getAddress().equals(macAddress)) {
                getScanResults().remove(scan);
                break;
            }
        }
    }

    @Override
    public List<IConnAdvertSubscriber> getConnAdvertSubscribers() {
        return subsConnAdvert;
    }

    @Override
    public IConnAdvertProvider registerConnectionAdvertSubscriber(UUID uuid, IConnAdvertSubscriber subscriber) {
        if (!getConnAdvertSubscribers().contains(subscriber)) {
            getConnAdvertSubscribers().add(subscriber);
        }
        return this;
    }

    @Override
    public void registerConnAdvertSub(IConnAdvertSubscriber subscriber) {
        if (!getConnAdvertSubscribers().contains(subscriber)) {
            getConnAdvertSubscribers().add(subscriber);
            for (ScanResult s : getScanResults()) {
                subscriber.onScanResultReceived(s);
                break;
            }
        }
    }

    @Override
    public void deregisterConnAdvertSub(IConnAdvertSubscriber subscriber) {
        getConnAdvertSubscribers().remove(subscriber);
    }

    @Override
    public IConnGattProvider registerConnectionGattSubscriber(UUID uuid, IConnGattSubscriber subscriber) {
        if (!getConnGattSubscribers().contains(subscriber)) {
            getConnGattSubscribers().add(subscriber);
        }
        return this;
    }

    @Override
    public void registerConnGattSub(IConnGattSubscriber sub) {
        if (!getConnGattSubscribers().contains(sub)) {
            getConnGattSubscribers().add(sub);
        }
    }

    @Override
    public List<IConnGattSubscriber> getConnGattSubscribers() {
        return subsConnGatt;
    }

    @Override
    public void deregisterConnGattSub(IConnGattSubscriber sub) {
        getConnGattSubscribers().remove(sub);
    }

    //    @Override
    //    public void removeConnDevice(BluetoothDevice deviceToRemove) {
    //        for (BluetoothDevice dev : getConnDevices()) {
    //            if (dev.getAddress().equals(deviceToRemove.getAddress())) {
    //                getConnDevices().remove(dev);
    //            }
    //        }
    //    }

    @Override
    public boolean hasConnDevice(BluetoothDevice deviceToFind) {
        if (!getScanResults().isEmpty()) {
            for (int i = 0; i < getScanResults().size(); i++) {
                ScanResult res = getScanResults().get(i);
                if (res.getDevice().getAddress().equals(deviceToFind.getAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void getGattCharacteristicRead(UUID service, UUID characteristic) {
        access(new AccessGatt(this) {

            private UUID service;
            private UUID characteristic;

            public AccessGatt init(UUID service, UUID characteristic) {
                this.service = service;
                this.characteristic = characteristic;
                return this;
            }

            @Override
            public void onRun() {
                BluetoothGattService s = getLastGattInst().getService(service);
                if (s != null) {
                    BluetoothGattCharacteristic c = s.getCharacteristic(characteristic);
                    if (c != null) {
                        getLastGattInst().readCharacteristic(c);
                    }
                }
            }
        }.init(service, characteristic));
    }

    @Override
    public void writeGattCharacteristic(UUID service, UUID characteristic, byte[] write) {
        access(new AccessGatt(this) {

            private byte[] write;
            private UUID service;
            private UUID characteristic;

            public AccessGatt init(UUID service, UUID characteristic, byte[] write) {
                this.service = service;
                this.characteristic = characteristic;
                this.write = write;
                return this;
            }

            @Override
            public void onRun() {
                BluetoothGattService s = getLastGattInst().getService(service);
                if (s != null) {
                    BluetoothGattCharacteristic c = s.getCharacteristic(characteristic);
                    if (c != null) {
                        c.setValue(write);
                        getLastGattInst().writeCharacteristic(c);
                    }
                }
            }
        }.init(service, characteristic, write));
    }

    @Override
    public void onScanningStopped() {
        getAccessGatts().clear();
        getConnAdvertSubscribers().clear();
        getConnGattSubscribers().clear();
        getScanResults().clear();
    }

    @Override
    public void onScanLost(ScanResult result) {
        for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
            callback.onScanLost(result);
        }
        int foundRes = -1;
        for (int i = 0; i < getScanResults().size(); i++) {
            ScanResult res = getScanResults().get(i);
            if (res.getDevice().getAddress().equals(result.getDevice().getAddress())) {
                foundRes = i;
                break;
            }
        }
        if (foundRes > 0) {
            getScanResults().remove(foundRes);
        }
    }

    public BluetoothGatt getLastGattInst() {
        return lastGattInst;
    }


    protected abstract class ScanCallbackBase extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result.getScanRecord() != null && result.getScanRecord().getServiceData() != null &&
                result.getScanRecord().getServiceData().get(getServiceUuid()) != null) {
                switch (callbackType) {
                    case ScanSettings.CALLBACK_TYPE_ALL_MATCHES:
                    case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
                        onReceiveScanData(result);
                        break;
                    case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
                        removeScanResult(result.getDevice().getAddress());
                        for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
                            callback.onScanLost(result);
                        }
                        if (BuildConfig.DEBUG) {
                            Log.d("Scan", "Scan lost");
                        }
                        break;
                }
            }
        }

        protected abstract ParcelUuid getServiceUuid();

        protected abstract void onReceiveScanData(ScanResult result);

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            for (IConnAdvertSubscriber subs : getConnAdvertSubscribers()) {
                subs.onScanFailed(errorCode);
            }
        }
    }

    protected abstract class GattCallbackBase extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    switch (newState) {
                        case BluetoothProfile.STATE_CONNECTED:
                            setGattInst(gatt);
                            for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                                sub.onGattReady(gatt.getDevice().getAddress());
                            }
                    }
                    break;
                case BluetoothGatt.GATT_FAILURE:
                    switch (newState) {
                        case BluetoothProfile.STATE_DISCONNECTED:
                        case BluetoothProfile.STATE_DISCONNECTING:
                            //                            setServicesDiscovered(false);
                    }
                    break;
                default:
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        onDeviceDisconnected(gatt);
                    }
            }
        }

        private void onDeviceDisconnected(BluetoothGatt gatt) {
            removeScanResult(gatt.getDevice().getAddress());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setGattInst(gatt);
                for (UUID uuid : getConstantUuids()) {
                    if (uuid.equals(characteristic.getUuid())) {
                        if (characteristic.getValue() != null) {
                            byte[] value = characteristic.getValue();
                            for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                                sub.onGattValueReceived(getLastGattInst().getDevice().getAddress(), characteristic.getUuid(), value);
                            }
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setGattInst(gatt);
                for (UUID uuid : getConstantUuids()) {
                    if (uuid.equals(characteristic.getUuid())) {
                        if (characteristic.getValue() != null) {
                            byte[] value = characteristic.getValue();
                            for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                                sub.onGattValueWriteReceived(getLastGattInst().getDevice().getAddress(), characteristic.getUuid(), value);
                            }
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            setGattInst(gatt);
            for (UUID uuid : getConstantUuids()) {
                if (uuid.equals(characteristic.getUuid())) {
                    if (characteristic.getValue() != null) {
                        for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                            sub.onGattValueChanged(getLastGattInst().getDevice().getAddress(), characteristic.getUuid(),
                                                   characteristic.getValue());
                        }
                    }
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    }
}