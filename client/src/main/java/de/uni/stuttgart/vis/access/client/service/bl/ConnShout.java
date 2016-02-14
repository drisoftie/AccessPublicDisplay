package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.AccessApp;

/**
 * @author Alexander Dridiger
 */
public class ConnShout extends ConnBaseAdvertScan implements IConnMultiPart {

    private BluetoothGatt lastGattInst;
    private IConnMulti    connMulti;

    public ConnShout() {
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(Constants.UUID_ADVERT_SERVICE_MULTI.getUuid());
        constantUuids.add(Constants.UUID_ADVERT_SERVICE_SHOUT.getUuid());
        constantUuids.add(Constants.GATT_SERVICE_SHOUT.getUuid());
        constantUuids.add(Constants.GATT_SHOUT.getUuid());
        setConstantUuids(constantUuids);
        setScanCallback(new BlAdvertScanCallback());
        setGattCallback(new BlGattCallback());
    }

    @Override
    public void setConnMulti(IConnMulti connMulti) {
        this.connMulti = connMulti;
    }

    @Override
    public IConnAdvertScan getAdvertScan() {
        return this;
    }

    private void analyzeScanData(ScanResult scanData) {
        boolean start = false;
        for (byte b : scanData.getScanRecord().getServiceData(Constants.UUID_ADVERT_SERVICE_MULTI)) {
            if (b == Constants.AdvertiseConst.ADVERTISE_START) {
                start = true;
            } else if (b == Constants.AdvertiseConst.ADVERTISE_SHOUT.getFlag()) {
                ScanResult foundRes = null;
                for (ScanResult res : getScanResults()) {
                    if (StringUtils.equals(scanData.getDevice().getAddress(), res.getDevice().getAddress())) {
                        foundRes = res;
                        break;
                    }
                }
                if (foundRes != null) {
                    removeScanResult(foundRes);
                    addScanResult(scanData);
                } else {
                    addScanResult(scanData);
                    connMulti.contributeNotification(AccessApp.string(Constants.AdvertiseConst.ADVERTISE_SHOUT.getDescr()), this);
                }
            } else if (b == Constants.AdvertiseConst.ADVERTISE_END) {
                start = false;
                break;
            }
        }
    }

    private class BlAdvertScanCallback extends ScanCallback {

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
                result.getScanRecord().getServiceData().get(Constants.UUID_ADVERT_SERVICE_MULTI) != null) {
                switch (callbackType) {
                    case ScanSettings.CALLBACK_TYPE_ALL_MATCHES:
                    case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
                        analyzeScanData(result);
                        break;
                    case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
                        removeScanResult(result);
                        for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
                            callback.onScanLost(result);
                        }
                        break;
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            for (IConnAdvertSubscriber subs : getConnAdvertSubscribers()) {
                subs.onScanFailed(errorCode);
            }
        }
    }

    private class BlGattCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    switch (newState) {
                        case BluetoothProfile.STATE_CONNECTED:
                            lastGattInst = gatt;
                            for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                                sub.onGattReady();
                            }
                    }
                    break;
                case BluetoothGatt.GATT_FAILURE:
                    switch (newState) {
                        case BluetoothProfile.STATE_DISCONNECTED:
                        case BluetoothProfile.STATE_DISCONNECTING:
                    }
                    break;
                default:
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                lastGattInst = gatt;
                gatt.setCharacteristicNotification(gatt.getService(Constants.GATT_SERVICE_SHOUT.getUuid()).getCharacteristic(
                        Constants.GATT_SHOUT.getUuid()), true);
                for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                    sub.onServicesReady();
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                lastGattInst = gatt;
                for (UUID uuid : getConstantUuids()) {
                    if (uuid.equals(characteristic.getUuid())) {
                        if (characteristic.getValue() != null) {
                            byte[] value = characteristic.getValue();
                            for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                                sub.onGattValueReceived(value);
                            }
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            lastGattInst = gatt;
            if (Constants.GATT_SHOUT.getUuid().equals(characteristic.getUuid())) {
                if (characteristic.getValue() != null) {
                    for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                        sub.onGattValueChanged(characteristic.getUuid(), characteristic.getValue());
                    }
                }
            }
        }
    }
}
