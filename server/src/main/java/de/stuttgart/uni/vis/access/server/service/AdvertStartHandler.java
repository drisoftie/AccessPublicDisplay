package de.stuttgart.uni.vis.access.server.service;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import de.stuttgart.uni.vis.access.common.Constants;

/**
 * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
 * in an Intent to be picked up by AdvertiserFragment and stops this Service.
 */
class AdvertStartHandler extends AdvertiseCallback {

    private static final String TAG = AdvertStartHandler.class.getSimpleName();

    private IAdvertStartListener serviceAdvertise;

    public AdvertStartHandler(IAdvertStartListener serviceAdvertise) {
        this.serviceAdvertise = serviceAdvertise;
    }

    @Override
    public void onStartFailure(int errorCode) {
        super.onStartFailure(errorCode);

        Log.d(AdvertStartHandler.TAG, "Advertising failed: " + errorCode);
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
        Log.i(TAG, "Advertising successfully started");
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

    public interface IAdvertStartListener {

        Context getCntxt();

        void onStartingFailed(int code);

        void onStartingSuccess();
    }
}
