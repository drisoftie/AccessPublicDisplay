package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.util.ParserData;
import de.uni.stuttgart.vis.access.client.App;
import de.uni.stuttgart.vis.access.client.BuildConfig;
import de.uni.stuttgart.vis.access.client.R;

/**
 * @author Alexander Dridiger
 */
public class ConnWeather extends ConnBaseAdvertScan implements IConnWeather, IConnMultiPart {

    private List<IConnWeatherSub> subs = new ArrayList<>();
    private IConnMulti connMulti;
    private boolean    servicesDiscovered;

    public ConnWeather() {
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(Constants.UUID_ADVERT_SERVICE_MULTI.getUuid());
        constantUuids.add(Constants.UUID_ADVERT_SERVICE_WEATHER.getUuid());
        constantUuids.add(Constants.GATT_SERVICE_WEATHER.getUuid());
        constantUuids.add(Constants.GATT_WEATHER_DAT.getUuid());
        constantUuids.add(Constants.GATT_WEATHER_TODAY.getUuid());
        constantUuids.add(Constants.GATT_WEATHER_TOMORROW.getUuid());
        constantUuids.add(Constants.GATT_WEATHER_QUERY.getUuid());
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
            } else if (b == Constants.AdvertiseConst.ADVERTISE_WEATHER.getFlag()) {
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
                    connMulti.contributeNotification(App.string(Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()), this);
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
            } else if (b == Constants.AdvertiseConst.ADVERTISE_WEATHER_DATA.getFlag()) {
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
                    connMulti.contributeNotification("Current temperature: " + new DecimalFormat("#.#")
                            .format(ParserData.parseByteToFloat(Arrays.copyOfRange(advert, i + 1, i + 5))), this);
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

    @Override
    public void registerWeatherSub(IConnWeatherSub sub) {
        subs.add(sub);
    }

    @Override
    public void deregisterWeatherSub(IConnWeatherSub sub) {
        subs.remove(sub);
    }

    @Override
    public void getWeatherInfo(UUID uuid) {
        access(new AccessGatt() {

            private UUID uuid;

            public AccessGatt init(UUID uuid) {
                this.uuid = uuid;
                return this;
            }

            @Override
            public void onRun() {
                BluetoothGattService        s = getLastGattInst().getService(Constants.GATT_SERVICE_WEATHER.getUuid());
                BluetoothGattCharacteristic c = s.getCharacteristic(uuid);
                if (c != null) {
                    getLastGattInst().readCharacteristic(c);
                }
            }
        }.init(uuid));
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
                            Log.d("Weather", "Scan lost");
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
                    }
                    setServicesDiscovered(false);
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
                for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                    sub.onServicesReady(gatt.getDevice().getAddress());
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
                            byte[] weather = characteristic.getValue();
                            for (IConnWeatherSub sub : subs) {
                                sub.onWeatherInfo(gatt.getDevice().getAddress(), characteristic.getUuid(), weather);
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
            if (Constants.GATT_WEATHER_TODAY.getUuid().equals(characteristic.getUuid())) {
                if (characteristic.getValue() != null) {
                    byte[] weather       = characteristic.getValue();
                    Intent weatherIntent = new Intent(App.string(R.string.intent_gatt_weather));
                    weatherIntent.putExtra(App.string(R.string.bndl_gatt_weather_today), weather);
                    LocalBroadcastManager.getInstance(App.inst()).sendBroadcast(weatherIntent);

                    for (IConnWeatherSub sub : subs) {
                        sub.onWeatherInfo(gatt.getDevice().getAddress(), Constants.GATT_WEATHER_TODAY.getUuid(), weather);
                    }
                }
            }
        }
    }
}
