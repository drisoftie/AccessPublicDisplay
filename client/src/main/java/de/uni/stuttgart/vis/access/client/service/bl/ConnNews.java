package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.App;
import de.uni.stuttgart.vis.access.client.R;

/**
 * @author Alexander Dridiger
 */
public class ConnNews extends ConnBasePartAdvertScan implements IConnMultiPart {

    public ConnNews() {
        addUuid(Constants.UUID_ADVERT_SERVICE_MULTI.getUuid()).addUuid(Constants.NEWS.UUID_ADVERT_SERVICE_NEWS.getUuid()).addUuid(
                Constants.NEWS.GATT_SERVICE_NEWS.getUuid()).addUuid(Constants.NEWS.GATT_NEWS.getUuid());
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
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_NEWS.getFlag())) {
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
                        getConnMulti().contributeNotification(App.string(Constants.AdvertiseConst.ADVERTISE_NEWS.getDescr()), this);
                        for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
                            callback.onScanResultReceived(scanData);
                        }
                    }
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_NEWS_DATA.getFlag())) {
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
                        getConnMulti().contributeNotification(App.inst().getString(R.string.ntxt_news) + String.valueOf(advert[i + 2]), this);
                        String txtFound = App.string(R.string.ntxt_scan_found);
                        String txtFoundDescr = App.inst().getString(R.string.ntxt_scan_descr,
                                                                    App.string(Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()));
                        //                    getTtsProv().provideTts().queueRead(txtFound, txtFoundDescr);
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
        return null;
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
    }
}
