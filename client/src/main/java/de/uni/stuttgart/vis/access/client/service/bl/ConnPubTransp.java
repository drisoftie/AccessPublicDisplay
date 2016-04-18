package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.App;

/**
 * @author Alexander Dridiger
 */
public class ConnPubTransp extends ConnBasePartAdvertScan implements IConnMultiPart {


    public ConnPubTransp() {
        addUuid(Constants.UUID_ADVERT_SERVICE_MULTI.getUuid()).addUuid(Constants.PUBTRANSP.UUID_ADVERT_SERVICE_PUB_TRANSP.getUuid())
                                                              .addUuid(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid()).addUuid(
                Constants.PUBTRANSP.GATT_PUB_TRANSP_BUS.getUuid()).addUuid(Constants.PUBTRANSP.GATT_PUB_TRANSP_METRO.getUuid()).addUuid(
                Constants.PUBTRANSP.GATT_PUB_TRANSP_TRAIN.getUuid());
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
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_TRANSP.getFlag())) {
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
                        getConnMulti().contributeNotification(App.string(Constants.AdvertiseConst.ADVERTISE_TRANSP.getDescr()), this);
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
                for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                    sub.onServicesReady(getLastGattInst().getDevice().getAddress());
                }
                setServicesDiscovered(true);
            }
        }
    }
}
