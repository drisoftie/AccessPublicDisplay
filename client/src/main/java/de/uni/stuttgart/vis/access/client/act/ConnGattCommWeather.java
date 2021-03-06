package de.uni.stuttgart.vis.access.client.act;

import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import com.drisoftie.action.async.IGenericAction;

import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.App;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.helper.IContextProv;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;

/**
 * @author Alexander Dridiger
 */
public class ConnGattCommWeather implements IConnGattProvider.IConnGattSubscriber {

    private IConnGattProvider blConn;
    private IContextProv      provCntxt;
    private IViewProv         provView;

    private ActionSetText actionWeatherToday;
    private ActionSetText actionWeatherTomorrow;
    private ActionSetText actionWeatherDat;

    public void setContextProvider(IContextProv prov) {
        this.provCntxt = prov;
    }

    public void setViewProvider(IViewProv prov) {
        this.provView = prov;
        actionWeatherToday = new ActionSetText((TextView) prov.provideView(R.id.txt_weather_today), IGenericAction.class, null);
        actionWeatherTomorrow = new ActionSetText((TextView) prov.provideView(R.id.txt_weather_tomorrow), IGenericAction.class, null);
        actionWeatherDat = new ActionSetText((TextView) prov.provideView(R.id.txt_weather_dat), IGenericAction.class, null);
    }

    public void setConn(IConnGattProvider blConnection) {
        this.blConn = blConnection;
        blConnection.registerConnGattSub(this);
    }

    @Override
    public void onGattReady(String macAddress) {

    }

    @Override
    public void onServicesReady(String macAddress) {
        provView.provideView(R.id.txt_headline_today).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        blConn.getGattCharacteristicRead(Constants.WEATHER.GATT_SERVICE_WEATHER.getUuid(), Constants.WEATHER.GATT_WEATHER_TODAY.getUuid());
    }

    @Override
    public void onGattValueReceived(String macAddress, UUID uuid, byte[] value) {
        setText(uuid, value);
    }

    @Override
    public void onGattValueWriteReceived(String macAddress, UUID uuid, byte[] value) {

    }

    @Override
    public void onGattValueChanged(String macAddress, UUID uuid, byte[] value) {

    }

    public void onDetach() {
        provCntxt = null;
        blConn.deregisterConnGattSub(this);
    }

    private void setText(UUID uuid, byte[] value) {
        String weather;
        if (Constants.WEATHER.GATT_WEATHER_TODAY.getUuid().equals(uuid)) {
            weather = App.inst().getString(R.string.info_weather_today, new String(value));
            actionWeatherToday.invokeSelf(weather);
            blConn.getGattCharacteristicRead(Constants.WEATHER.GATT_SERVICE_WEATHER.getUuid(),
                                             Constants.WEATHER.GATT_WEATHER_TOMORROW.getUuid());
        } else if (Constants.WEATHER.GATT_WEATHER_TOMORROW.getUuid().equals(uuid)) {
            weather = App.inst().getString(R.string.info_weather_tomorrow, new String(value));
            actionWeatherTomorrow.invokeSelf(weather);
            blConn.getGattCharacteristicRead(Constants.WEATHER.GATT_SERVICE_WEATHER.getUuid(),
                                             Constants.WEATHER.GATT_WEATHER_DAT.getUuid());
        } else if (Constants.WEATHER.GATT_WEATHER_DAT.getUuid().equals(uuid)) {
            weather = App.inst().getString(R.string.info_weather_dat, new String(value));
            actionWeatherDat.invokeSelf(weather);
        }
    }
}
