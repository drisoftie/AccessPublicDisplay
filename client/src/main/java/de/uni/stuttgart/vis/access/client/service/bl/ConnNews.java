package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.App;
import de.uni.stuttgart.vis.access.client.BuildConfig;
import de.uni.stuttgart.vis.access.client.R;

/**
 * @author Alexander Dridiger
 */
public class ConnNews extends ConnBaseAdvertScan implements IConnMultiPart {

    private IConnMulti connMulti;
    private boolean    servicesDiscovered;

    public ConnNews() {
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(Constants.UUID_ADVERT_SERVICE_MULTI.getUuid());
        constantUuids.add(Constants.NEWS.UUID_ADVERT_SERVICE_NEWS.getUuid());
        constantUuids.add(Constants.NEWS.GATT_SERVICE_NEWS.getUuid());
        constantUuids.add(Constants.NEWS.GATT_NEWS.getUuid());
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

    @Override
    public void setServicesDiscovered(boolean discovered) {
        servicesDiscovered = discovered;
    }

    @Override
    public boolean hasServicesDiscovered() {
        return servicesDiscovered;
    }

    private void analyzeScanData(ScanResult scanData) {
        boolean start = false;
        //noinspection ConstantConditions
        byte[] advert = scanData.getScanRecord().getServiceData(Constants.UUID_ADVERT_SERVICE_MULTI);
        for (int i = 0; i < advert.length; i++) {
            byte b = advert[i];
            if (b == Constants.AdvertiseConst.ADVERTISE_START) {
                start = true;
            } else if (b == Constants.AdvertiseConst.ADVERTISE_NEWS.getFlag()) {
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
                    for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
                        callback.onRefreshedScanReceived(scanData);
                    }
                } else {
                    addScanResult(scanData);
                    connMulti.contributeNotification(App.string(Constants.AdvertiseConst.ADVERTISE_NEWS.getDescr()), this);
                    for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
                        callback.onScanResultReceived(scanData);
                    }
                }
            } else if (b == Constants.AdvertiseConst.ADVERTISE_NEWS_DATA.getFlag()) {
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
                    for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
                        callback.onRefreshedScanReceived(scanData);
                    }
                } else {
                    addScanResult(scanData);
                    connMulti.contributeNotification("News: " + String.valueOf(advert[i + 1]), this);
                    String txtFound = App.string(R.string.ntxt_scan_found);
                    String txtFoundDescr = App.inst().getString(R.string.ntxt_scan_descr,
                                                                App.string(Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()));
                    //                    getTtsProv().provideTts().queueRead(txtFound, txtFoundDescr);
                    for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
                        callback.onScanResultReceived(scanData);
                    }
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
                        if (BuildConfig.DEBUG) {
                            Log.d("Shout", "Scan lost");
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
                            setServicesDiscovered(false);
                    }
                    break;
                default:
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        setServicesDiscovered(false);
                    }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setGattInst(gatt);
                //                access(new AccessGatt() {
                //                    @Override
                //                    public void onRun() {
                //                        getLastGattInst().setCharacteristicNotification(getLastGattInst().getService(Constants.GATT_SERVICE_NEWS.getUuid())
                //                                                                                         .getCharacteristic(Constants.GATT_NEWS.getUuid()),
                //                                                                        true);
                //                    }
                //                });
                for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                    sub.onServicesReady(getLastGattInst().getDevice().getAddress());
                }
                setServicesDiscovered(true);
            }
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
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            setGattInst(gatt);
            if (Constants.NEWS.GATT_NEWS.getUuid().equals(characteristic.getUuid())) {
                if (characteristic.getValue() != null) {
                    for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                        sub.onGattValueChanged(getLastGattInst().getDevice().getAddress(), characteristic.getUuid(),
                                               characteristic.getValue());
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
