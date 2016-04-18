package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.Arrays;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.util.ParserData;
import de.uni.stuttgart.vis.access.client.App;
import de.uni.stuttgart.vis.access.client.R;

/**
 * @author Alexander Dridiger
 */
public class ConnWeather extends ConnBasePartAdvertScan implements IConnMultiPart {

    private static final String TAG = "ConnWeather";

    public ConnWeather() {
        addUuid(Constants.UUID_ADVERT_SERVICE_MULTI.getUuid()).addUuid(Constants.WEATHER.UUID_ADVERT_SERVICE_WEATHER.getUuid()).addUuid(
                Constants.WEATHER.GATT_SERVICE_WEATHER.getUuid()).addUuid(Constants.WEATHER.GATT_WEATHER_DAT.getUuid()).addUuid(
                Constants.WEATHER.GATT_WEATHER_TODAY.getUuid()).addUuid(Constants.WEATHER.GATT_WEATHER_TOMORROW.getUuid()).addUuid(
                Constants.WEATHER.GATT_WEATHER_QUERY.getUuid());
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
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_WEATHER.getFlag())) {
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
                        getConnMulti().contributeNotification(App.string(Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()), this);
                        //                    getNotifyProv().provideNotify().createDisplayNotification(App.string(
                        //                            Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()), App.string(
                        //                            Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()), scanData, R.id.nid_main);
                        String txtFound = App.string(R.string.ntxt_scan_found);
                        String txtFoundDescr = App.inst().getString(R.string.ntxt_scan_descr,
                                                                    App.string(Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()));
                        //                    getTtsProv().provideTts().queueRead(txtFound, txtFoundDescr);
                        for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
                            callback.onScanResultReceived(scanData);
                        }
                    }
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_WEATHER_DATA.getFlag())) {
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
                        getConnMulti().contributeNotification("Current temperature: " + new DecimalFormat("#.#")
                                .format(ParserData.parseByteToFloat(Arrays.copyOfRange(advert, i + 2, i + 6))), this);
                        String txtFound = App.string(R.string.ntxt_scan_found);
                        String txtFoundDescr = App.inst().getString(R.string.ntxt_scan_descr,
                                                                    App.string(Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()));
                        //                    getTtsProv().provideTts().queueRead(txtFound, txtFoundDescr);
                        for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
                            callback.onScanResultReceived(scanData);
                        }
                    }
                }
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

    private class BlGattCallback extends BluetoothGattCallback {

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setGattInst(gatt);
                for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                    sub.onServicesReady(gatt.getDevice().getAddress());
                }
                setServicesDiscovered(true);
            }
        }
    }
}
