package de.uni.stuttgart.vis.access.client.act;

import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import com.drisoftie.action.async.IGenericAction;

import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.AccessApp;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.helper.IContextProv;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;
import de.uni.stuttgart.vis.access.client.service.bl.IConnWeather;

/**
 * @author Alexander Dridiger
 */
public class ConnGattCommWeather implements IConnGattProvider.IConnGattSubscriber, IConnWeather.IConnWeatherSub {

    private IConnGattProvider blConn;
    private IConnWeather      blWeather;
    private IContextProv      cntxtProv;
    private IViewProv         viewProv;

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

    public void setConn(IConnGattProvider blConnection) {
        this.blConn = blConnection;
        blConnection.registerConnGattSub(this);
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
        viewProv.provideView(R.id.txt_headline_today).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        blWeather.getWeatherInfo(Constants.GATT_WEATHER_TODAY.getUuid());
    }

    @Override
    public void onGattValueReceived(byte[] value) {

    }

    @Override
    public void onGattValueChanged(UUID uuid, byte[] value) {

    }

    @Override
    public void onWeatherInfo(UUID uuid, byte[] value) {
        setText(uuid, value);
    }

    public void onDetach() {
        cntxtProv = null;
        blConn.deregisterConnGattSub(this);
        blWeather.deregisterWeatherSub(this);
    }

    private void setText(UUID uuid, byte[] value) {
        String weather;
        if (Constants.GATT_WEATHER_TODAY.getUuid().equals(uuid)) {
            weather = AccessApp.inst().getString(R.string.info_weather_today, new String(value));
            actionWeatherToday.invokeSelf(weather);
            blWeather.getWeatherInfo(Constants.GATT_WEATHER_TOMORROW.getUuid());
        } else if (Constants.GATT_WEATHER_TOMORROW.getUuid().equals(uuid)) {
            weather = AccessApp.inst().getString(R.string.info_weather_tomorrow, new String(value));
            actionWeatherTomorrow.invokeSelf(weather);
            blWeather.getWeatherInfo(Constants.GATT_WEATHER_DAT.getUuid());
        } else if (Constants.GATT_WEATHER_DAT.getUuid().equals(uuid)) {
            weather = AccessApp.inst().getString(R.string.info_weather_dat, new String(value));
            actionWeatherDat.invokeSelf(weather);
        }
    }
}
