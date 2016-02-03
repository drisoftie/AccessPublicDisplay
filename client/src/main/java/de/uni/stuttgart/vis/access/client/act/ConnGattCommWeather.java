package de.uni.stuttgart.vis.access.client.act;

import android.widget.TextView;

import com.drisoftie.action.async.IGenericAction;

import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.AccessApp;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.helper.IContextProv;
import de.uni.stuttgart.vis.access.client.service.IConnAdvertScanHandler;
import de.uni.stuttgart.vis.access.client.service.IConnWeather;

/**
 * @author Alexander Dridiger
 */
public class ConnGattCommWeather implements IConnAdvertScanHandler.IConnGattSubscriber, IConnWeather.IConnWeatherSub {

    private IConnAdvertScanHandler blConn;
    private IConnWeather           blWeather;
    private IContextProv           cntxtProv;
    private IViewProv              viewProv;

    private ActionSetText actionWeatherToday;
    private ActionSetText actionWeatherTomorrow;
    private ActionSetText actionWeatherDat;

    public void setContextProvider(IContextProv prov) {
        this.cntxtProv = prov;
    }

    public void setViewProvider(IViewProv prov) {
        this.viewProv = prov;
        actionWeatherToday = new ActionSetText((TextView) prov.provideView(R.id.txt_weather_today), IGenericAction.class, null);
        actionWeatherTomorrow = new ActionSetText((TextView) prov.provideView(R.id.txt_weather_tomorrow), IGenericAction.class, null);
        actionWeatherDat = new ActionSetText((TextView) prov.provideView(R.id.txt_weather_dat), IGenericAction.class, null);
    }

    public void setConn(IConnAdvertScanHandler blConnection) {
        this.blConn = blConnection;
        blConnection.registerConnSub(this);
        if (blConnection instanceof IConnWeather) {
            blWeather = (IConnWeather) blConnection;
            blWeather.registerWeatherSub(this);
        }
    }

    @Override
    public void onGattReady() {
    }

    @Override
    public void onServicesReady() {
        blWeather.getWeatherInfo(UUID.fromString(Constants.GATT_WEATHER_TODAY));
    }

    @Override
    public void onWeatherInfo(UUID uuid, byte[] value) {
        setText(uuid, value);
    }

    public void onDetach() {
        cntxtProv = null;
        blConn.deregisterConnSub(this);
        blWeather.deregisterWeatherSub(this);
    }

    private void setText(UUID uuid, byte[] value) {
        String weather = null;
        if (UUID.fromString(Constants.GATT_WEATHER_TODAY).equals(uuid)) {
            weather = AccessApp.inst().getString(R.string.info_weather_today, new String(value));
            actionWeatherToday.invokeSelf(weather);
            blWeather.getWeatherInfo(UUID.fromString(Constants.GATT_WEATHER_TOMORROW));
        } else if (UUID.fromString(Constants.GATT_WEATHER_TOMORROW).equals(uuid)) {
            weather = AccessApp.inst().getString(R.string.info_weather_tomorrow, new String(value));
            actionWeatherTomorrow.invokeSelf(weather);
            blWeather.getWeatherInfo(UUID.fromString(Constants.GATT_WEATHER_DAT));
        } else if (UUID.fromString(Constants.GATT_WEATHER_DAT).equals(uuid)) {
            weather = AccessApp.inst().getString(R.string.info_weather_dat, new String(value));
            actionWeatherDat.invokeSelf(weather);
        }
    }
}
