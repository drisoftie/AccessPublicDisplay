package de.uni.stuttgart.vis.access.client.service;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.AccessApp;
import de.uni.stuttgart.vis.access.client.R;

/**
 * @author Alexander Dridiger
 */
public class ConnWeather extends ConnBaseAdvertScan implements IConnWeather {

    private BluetoothGatt lastGattInst;

    private List<IConnWeatherSub> subs = new ArrayList<>();

    public ConnWeather() {
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(Constants.UUID_ADVERT_SERVICE_WEATHER.getUuid());
        constantUuids.add(UUID.fromString(Constants.GATT_SERVICE_WEATHER));
        constantUuids.add(UUID.fromString(Constants.GATT_WEATHER_DAT));
        constantUuids.add(UUID.fromString(Constants.GATT_WEATHER_TODAY));
        constantUuids.add(UUID.fromString(Constants.GATT_WEATHER_TOMORROW));
        constantUuids.add(UUID.fromString(Constants.GATT_WEATHER_QUERY));
        setConstantUuids(constantUuids);
        setScanCallback(new BlAdvertScanCallback());
        setGattCallback(new BlGattCallback());
    }

    private void analyzeScanData(ScanResult scanData) {
        boolean start = false;
        for (byte b : scanData.getScanRecord().getServiceData(Constants.UUID_ADVERT_SERVICE_WEATHER)) {
            if (b == Constants.AdvertiseConst.ADVERTISE_START) {
                start = true;
            } else if (b == Constants.AdvertiseConst.ADVERTISE_BOOKING.getFlag()) {
            } else if (b == Constants.AdvertiseConst.ADVERTISE_NEWS.getFlag()) {
            } else if (b == Constants.AdvertiseConst.ADVERTISE_TRANSP.getFlag()) {
                //                                createDisplayNotification(getString(Constants.AdvertiseConst.ADVERTISE_TRANSP.getDescr()),
                //                                                          R.id.nid_pub_transp);
            } else if (b == Constants.AdvertiseConst.ADVERTISE_WEATHER.getFlag()) {
                getNotifyProv().provideNotify().createDisplayNotification(AccessApp.string(
                                                                                  Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()),
                                                                          scanData, R.id.nid_weather);
                String txtFound = AccessApp.string(R.string.ntxt_scan_found);
                String txtFoundDescr = AccessApp.inst().getString(R.string.ntxt_scan_descr, AccessApp.string(
                        Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()));
                //                getTtsProv().provideTts().queueRead(txtFound, txtFoundDescr);
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
        BluetoothGattService        s = lastGattInst.getService(UUID.fromString(Constants.GATT_SERVICE_WEATHER));
        BluetoothGattCharacteristic c = s.getCharacteristic(uuid);
        if (c != null) {
            lastGattInst.readCharacteristic(c);
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
                result.getScanRecord().getServiceData().get(Constants.UUID_ADVERT_SERVICE_WEATHER) != null) {
                switch (callbackType) {
                    case ScanSettings.CALLBACK_TYPE_ALL_MATCHES:
                    case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
                        addScanResult(result);
                        analyzeScanData(result);
                        break;
                    case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
                        removeScanResult(result);
                        for (IAdvertSubscriber callback : getSubscribers()) {
                            callback.onScanLost(result);
                        }
                        break;
                }

                //                String newData = new String(result.getScanRecord().getServiceData().get(Constants.UUID_ADVERT_SERVICE_WEATHER));
                //                //                removeNotification(R.id.nid_main);
                //                for (byte b : newData.getBytes()) {
                //                    if (b == Constants.AdvertiseConst.ADVERTISE_START) {
                //                    } else if (b == Constants.AdvertiseConst.ADVERTISE_BOOKING.getFlag()) {
                //                    } else if (b == Constants.AdvertiseConst.ADVERTISE_NEWS.getFlag()) {
                //                    } else if (b == Constants.AdvertiseConst.ADVERTISE_TRANSP.getFlag()) {
                //                        //                                createDisplayNotification(getString(Constants.AdvertiseConst.ADVERTISE_TRANSP.getDescr()),
                //                        //                                                          R.id.nid_pub_transp);
                //                    } else if (b == Constants.AdvertiseConst.ADVERTISE_WEATHER.getFlag()) {
                //                        //                        createDisplayNotification(getString(Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()), R.id.nid_weather);
                //                    } else if (b == Constants.AdvertiseConst.ADVERTISE_END) {
                //                        break;
                //                    }
                //                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            for (IAdvertSubscriber subs : getSubscribers()) {
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
                            for (IConnGattSubscriber sub : getConnSubscribers()) {
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
                for (IConnGattSubscriber sub : getConnSubscribers()) {
                    sub.onServicesReady();
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                lastGattInst = gatt;
                if (characteristic.getValue() != null) {
                    byte[] weather = characteristic.getValue();
                    for (IConnWeatherSub sub : subs) {
                        sub.onWeatherInfo(characteristic.getUuid(), weather);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            lastGattInst = gatt;
            if (UUID.fromString(Constants.GATT_WEATHER_TODAY).equals(characteristic.getUuid())) {
                if (characteristic.getValue() != null) {
                    byte[] weather = characteristic.getValue();
                    Intent weatherIntent = new Intent(AccessApp.string(R.string.intent_gatt_weather));
                    weatherIntent.putExtra(AccessApp.string(R.string.bndl_gatt_weather_today), weather);
                    LocalBroadcastManager.getInstance(AccessApp.inst()).sendBroadcast(weatherIntent);

                    for (IConnWeatherSub sub : subs) {
                        sub.onWeatherInfo(UUID.fromString(Constants.GATT_WEATHER_TODAY), weather);
                    }
                }
            }
        }
    }
}
