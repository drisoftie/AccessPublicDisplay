package de.uni.stuttgart.vis.access.client.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
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
public class GattScanHandler {

    private BluetoothGattCallback gattCallback;

    private List<IGattScanSubscriber> subscribers = new ArrayList<>();

    public GattScanHandler() {
        gattCallback = new BlGattCallback();
    }

    public BluetoothGattCallback getGattCallback() {
        return gattCallback;
    }

    public void setGattCallback(BluetoothGattCallback gattCallback) {
        this.gattCallback = gattCallback;
    }


    public void registerGattScanSubscriber(IGattScanSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void removeAdvertSubscriber(IGattScanSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    private class BlGattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
                //            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService s = gatt.getService(UUID.fromString(Constants.GATT_SERVICE_WEATHER));
                if (s != null) {
                    BluetoothGattCharacteristic weatherC = s.getCharacteristic(UUID.fromString(Constants.GATT_WEATHER_TODAY));
                    gatt.readCharacteristic(weatherC);
                } else {
                    s = gatt.getService(UUID.fromString(Constants.GATT_SERVICE_PUB_TRANSP));
                    BluetoothGattCharacteristic transpC = s.getCharacteristic(UUID.fromString(Constants.GATT_PUB_TRANSP_BUS));
                    gatt.readCharacteristic(transpC);
                }
                //            } else {
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getValue() != null) {
                    //                    if (UUID.fromString(Constants.GATT_WEATHER_QUERY).equals(characteristic.getUuid())) {
                    //
                    //                    } else
                    if (UUID.fromString(Constants.GATT_WEATHER_TODAY).equals(characteristic.getUuid())) {
                        byte[] weather = characteristic.getValue();
                        Intent weatherIntent = new Intent(AccessApp.string(R.string.intent_gatt_weather));
                        weatherIntent.putExtra(AccessApp.string(R.string.bndl_gatt_weather_today), weather);
                        LocalBroadcastManager.getInstance(AccessApp.inst()).sendBroadcast(weatherIntent);
                        BluetoothGattCharacteristic weatherC = characteristic.getService().getCharacteristic(UUID.fromString(
                                Constants.GATT_WEATHER_TOMORROW));
                        gatt.readCharacteristic(weatherC);
                    } else if (UUID.fromString(Constants.GATT_WEATHER_TOMORROW).equals(characteristic.getUuid())) {
                        byte[] weather = characteristic.getValue();
                        Intent weatherIntent = new Intent(AccessApp.string(R.string.intent_gatt_weather));
                        weatherIntent.putExtra(AccessApp.string(R.string.bndl_gatt_weather_tomorrow), weather);
                        LocalBroadcastManager.getInstance(AccessApp.inst()).sendBroadcast(weatherIntent);
                        BluetoothGattCharacteristic weatherC = characteristic.getService().getCharacteristic(UUID.fromString(
                                Constants.GATT_WEATHER_DAT));
                        gatt.readCharacteristic(weatherC);
                    } else if (UUID.fromString(Constants.GATT_WEATHER_DAT).equals(characteristic.getUuid())) {
                        byte[] weather = characteristic.getValue();
                        Intent weatherIntent = new Intent(AccessApp.string(R.string.intent_gatt_weather));
                        weatherIntent.putExtra(AccessApp.string(R.string.bndl_gatt_weather_dat), weather);
                        LocalBroadcastManager.getInstance(AccessApp.inst()).sendBroadcast(weatherIntent);
                        gatt.close();
                    } else if (UUID.fromString(Constants.GATT_PUB_TRANSP_BUS).equals(characteristic.getUuid())) {
                        byte[] transp = characteristic.getValue();
                        Intent transpIntent = new Intent(AccessApp.string(R.string.intent_gatt_pub_transp));
                        transpIntent.putExtra(AccessApp.string(R.string.bndl_gatt_pub_transp_bus), transp);
                        LocalBroadcastManager.getInstance(AccessApp.inst()).sendBroadcast(transpIntent);
                    } else if (UUID.fromString(Constants.GATT_PUB_TRANSP_METRO).equals(characteristic.getUuid())) {
                        byte[] transp = characteristic.getValue();
                        Intent transpIntent = new Intent(AccessApp.string(R.string.intent_gatt_pub_transp));
                        transpIntent.putExtra(AccessApp.string(R.string.bndl_gatt_pub_transp_metro), transp);
                        LocalBroadcastManager.getInstance(AccessApp.inst()).sendBroadcast(transpIntent);
                    } else if (UUID.fromString(Constants.GATT_PUB_TRANSP_TRAIN).equals(characteristic.getUuid())) {
                        byte[] transp = characteristic.getValue();
                        Intent transpIntent = new Intent(AccessApp.string(R.string.intent_gatt_pub_transp));
                        transpIntent.putExtra(AccessApp.string(R.string.bndl_gatt_pub_transp_train), transp);
                        LocalBroadcastManager.getInstance(AccessApp.inst()).sendBroadcast(transpIntent);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        }
    }
}
