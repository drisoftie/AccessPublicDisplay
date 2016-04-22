package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.util.AccessGatt;
import de.uni.stuttgart.vis.access.client.App;

/**
 * @author Alexander Dridiger
 */
public class ConnShout extends ConnBasePartAdvertScan implements IConnMultiPart {

    Set<String> shoutouts = new HashSet<>();

    public ConnShout() {
        addUuid(Constants.UUID_ADVERT_SERVICE_MULTI.getUuid()).addUuid(Constants.SHOUT.UUID_ADVERT_SERVICE_SHOUT.getUuid()).addUuid(
                Constants.SHOUT.GATT_SERVICE_SHOUT.getUuid()).addUuid(Constants.SHOUT.GATT_SHOUT.getUuid());
        setScanCallback(new BlAdvertScanCallback());
        setGattCallback(new BlGattCallback());
    }

    private void analyzeScanData(ScanResult scanData) {
        boolean start = false;
        //noinspection ConstantConditions
        byte[] advert = scanData.getScanRecord().getServiceData(Constants.UUID_ADVERT_SERVICE_MULTI);
        for (int i = 0; i < advert.length; i++) {
            if (i + 1 < advert.length) {
                byte[] b = new byte[]{advert[i], advert[i + 1]};
                if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_START)) {
                    start = true;
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_SHOUT.getFlag())) {
                    ScanResult foundRes = null;
                    for (ScanResult res : getScanResults()) {
                        if (StringUtils.equals(scanData.getDevice().getAddress(), res.getDevice().getAddress())) {
                            foundRes = res;
                            break;
                        }
                    }
                    if (foundRes != null) {
                        removeScanResult(foundRes.getDevice().getAddress());
                        addScanResult(scanData);
                        for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
                            callback.onRefreshedScanReceived(scanData);
                        }
                    } else {
                        addScanResult(scanData);
                        getConnMulti().contributeNotification(App.string(Constants.AdvertiseConst.ADVERTISE_SHOUT.getDescr()), this);
                        for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
                            callback.onScanResultReceived(scanData);
                        }
                    }
                }
            } else {
                break;
            }
        }
    }

    @Override
    public Object getData() {
        return shoutouts;
    }

    private class BlAdvertScanCallback extends ScanCallbackBase {

        @Override
        protected ParcelUuid getServiceUuid() {
            return Constants.UUID_ADVERT_SERVICE_MULTI;
        }

        @Override
        protected void onReceiveScanData(ScanResult result) {
            analyzeScanData(result);
        }
    }

    private class BlGattCallback extends GattCallbackBase {

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setGattInst(gatt);
                access(new AccessGatt(ConnShout.this) {
                    @Override
                    public void onRun() {
                        getLastGattInst().setCharacteristicNotification(
                                getLastGattInst().getService(Constants.SHOUT.GATT_SERVICE_SHOUT.getUuid())
                                                 .getCharacteristic(Constants.SHOUT.GATT_SHOUT.getUuid()), true);
                    }
                });
                for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                    sub.onServicesReady(getLastGattInst().getDevice().getAddress());
                }
                setServicesDiscovered(true);
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
                            shoutouts.add(new String(characteristic.getValue()));
                        }
                    }
                }
            }
        }
    }
}
