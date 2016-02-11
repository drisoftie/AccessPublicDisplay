package de.stuttgart.uni.vis.access.server.service;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.server.BuildConfig;

/**
 * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
 * in an Intent to be picked up by AdvertiserFragment and stops this Service.
 */
class AdvertHandler extends AdvertiseCallback {

    private static final String TAG = AdvertHandler.class.getSimpleName();

    private IAdvertStartListener serviceAdvertise;

    public AdvertHandler(IAdvertStartListener serviceAdvertise) {
        this.serviceAdvertise = serviceAdvertise;
    }

    @Override
    public void onStartFailure(int errorCode) {
        super.onStartFailure(errorCode);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Advertising failed: " + errorCode);
        }
        switch (errorCode) {
            case ADVERTISE_FAILED_ALREADY_STARTED:
                Toast.makeText(serviceAdvertise.getCntxt(), "Already started", Toast.LENGTH_SHORT).show();
                break;
            case ADVERTISE_FAILED_DATA_TOO_LARGE:
                Toast.makeText(serviceAdvertise.getCntxt(), "Data too large", Toast.LENGTH_SHORT).show();
                break;
            case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                Toast.makeText(serviceAdvertise.getCntxt(), "Unsupported feature", Toast.LENGTH_SHORT).show();
                break;
            case ADVERTISE_FAILED_INTERNAL_ERROR:
                Toast.makeText(serviceAdvertise.getCntxt(), "Internal error", Toast.LENGTH_SHORT).show();
                break;
            case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                Toast.makeText(serviceAdvertise.getCntxt(), "Too many advertisers", Toast.LENGTH_SHORT).show();
                break;
        }
        Intent failureIntent = new Intent();
        failureIntent.setAction(Constants.ADVERTISING_FAILED);
        failureIntent.putExtra(Constants.ADVERTISING_FAILED_EXTRA_CODE, errorCode);

        LocalBroadcastManager.getInstance(serviceAdvertise.getCntxt()).sendBroadcast(failureIntent);

        serviceAdvertise.onStartingFailed(errorCode);
    }

    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        super.onStartSuccess(settingsInEffect);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Advertising successfully started");
        }
        serviceAdvertise.onStartingSuccess();
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    public AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    public AdvertiseData buildAdvertiseDataWeather() {
        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        //        dataBuilder.addServiceUuid(Constants.UUID_ADVERT_SERVICE_WEATHER);

        //        dataBuilder.addServiceData(Constants.UUID_ADVERT_SERVICE_WEATHER, advertisement.getBytes());
        dataBuilder.addServiceUuid(Constants.UUID_ADVERT_SERVICE_MULTI);
        dataBuilder.addServiceData(Constants.UUID_ADVERT_SERVICE_MULTI, new byte[]{Constants.AdvertiseConst.ADVERTISE_START,
                Constants.AdvertiseConst.ADVERTISE_WEATHER.getFlag(), Constants.AdvertiseConst.ADVERTISE_TRANSP.getFlag(),
                Constants.AdvertiseConst.ADVERTISE_SHOUT.getFlag(), Constants.AdvertiseConst.ADVERTISE_END});
        return dataBuilder.build();
    }

    public interface IAdvertStartListener {

        Context getCntxt();

        void onStartingFailed(int code);

        void onStartingSuccess();
    }
}
