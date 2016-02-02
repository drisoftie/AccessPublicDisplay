package de.uni.stuttgart.vis.access.client.act;

import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.AccessApp;
import de.uni.stuttgart.vis.access.client.service.IConnAdvertScanHandler;
import de.uni.stuttgart.vis.access.client.service.IConnSubscriber;
import de.uni.stuttgart.vis.access.client.service.IConnWeather;

/**
 * @author Alexander Dridiger
 */
public class ConnGattCommWeather implements IConnSubscriber, IConnWeather.IConnWeatherSub {

    private IConnAdvertScanHandler blConn;
    private IConnWeather           blWeather;

    public void setConn(IConnAdvertScanHandler blConnection) {
        this.blConn = blConnection;
        blConnection.registerConnSub(this);
        if (blConnection instanceof IConnWeather) {
            blWeather = (IConnWeather) blConnection;
            blWeather.registerWeatherSub(this);
            Log.i("SUB", "SUBSCRIBE FOR WEATHER INFO");
        }
    }

    @Override
    public void onGattReady() {
        Log.i("GATT", "GATT IS READY");
        blWeather.getWeatherInfo(UUID.fromString(Constants.GATT_WEATHER_TODAY));
    }

    @Override
    public void onWeatherInfo(UUID uuid, byte[] value) {
        Toast.makeText(AccessApp.inst(), new String(value), Toast.LENGTH_SHORT).show();
    }
}
