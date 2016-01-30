package de.uni.stuttgart.vis.access.client.service;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.stuttgart.uni.vis.access.common.Constants;

/**
 * @author Alexander Dridiger
 */
public class AdvertScanHandler {

    private ScanCallback scanCallback;
    private Map<String, ScanResult> scanHistory = new HashMap<>();
    private List<IAdvertSubscriber> subscribers = new ArrayList<>();

    public AdvertScanHandler() {
        scanCallback = new BlScanCallback();
    }

    public ScanCallback getScanCallback() {
        return scanCallback;
    }

    public void setScanCallback(ScanCallback scanCallback) {
        this.scanCallback = scanCallback;
    }

    public boolean addScanResult(String address, ScanResult scanResult) {
        return scanHistory.put(address, scanResult) == null;
    }

    public void clearScanHistory() {
        scanHistory.clear();
    }

    public void removeScanResult(String address) {
        scanHistory.remove(address);
    }

    public void registerAdvertSubscriber(IAdvertSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void removeAdvertSubscriber(IAdvertSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    public List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE results around you
        //        builder.setServiceUuid(Constants.UUID_ADVERT_SERVICE_WEATHER);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    public ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }

    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private class BlScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            List<ScanResult> newScans       = null;
            List<ScanResult> refreshedScans = null;
            for (ScanResult result : results) {
                if (result.getDevice().getAddress() != null) {
                    boolean isNewDevice = addScanResult(result.getDevice().getAddress(), result);
                    if (isNewDevice) {
                        if (newScans == null) {
                            newScans = new ArrayList<>();
                        }
                        newScans.add(result);
                    } else {
                        if (refreshedScans == null) {
                            refreshedScans = new ArrayList<>();
                        }
                        refreshedScans.add(result);
                    }
                }
            }
            for (IAdvertSubscriber callback : subscribers) {
                callback.onScanResultsReceived(newScans);
                callback.onRefreshedScansReceived(refreshedScans);
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
                        if (result.getDevice().getAddress() != null) {
                            boolean isNewDevice = addScanResult(result.getDevice().getAddress(), result);
                            if (isNewDevice) {
                                for (IAdvertSubscriber callback : subscribers) {
                                    callback.onScanResultReceived(result);
                                }
                            } else {
                                for (IAdvertSubscriber callback : subscribers) {
                                    callback.onRefreshedScanReceived(result);
                                }
                            }
                        }
                        break;
                    case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
                        removeScanResult(result.getDevice().getAddress());
                        for (IAdvertSubscriber callback : subscribers) {
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
            for (IAdvertSubscriber subs : subscribers) {
                subs.onScanFailed(errorCode);
            }
            //            Toast.makeText(ActScan.this, "Scan failed with error: " + errorCode, Toast.LENGTH_LONG).show();
        }
    }
}
