package de.uni.stuttgart.vis.access.client.act;

import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.android.AndroidAction;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.App;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.helper.ParserUtil;
import de.uni.stuttgart.vis.access.client.service.ServiceScan;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;

public class ActWeather extends ActGattScan {

    private GattWeather       gattListenWeather;
    private IConnGattProvider gattProviderWeather;
    private ActionGattSetup   actionGatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_weather);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        actionGatt = new ActionGattSetup(null, IGenericAction.class, null);
    }

    @Override
    protected void onResuming() {
    }

    @Override
    protected void registerComponents() {
        Intent intent = new Intent(getString(R.string.intent_weather_get));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        bindService(new Intent(this, ServiceScan.class), this, BIND_AUTO_CREATE);
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        super.onServiceConnected(name, binder);
        if (gattListenWeather == null) {
            gattProviderWeather = service.subscribeGattConnection(Constants.WEATHER.GATT_SERVICE_WEATHER.getUuid(),
                                                                  gattListenWeather = new GattWeather());
            gattProviderWeather.getGattCharacteristicRead(Constants.WEATHER.GATT_SERVICE_WEATHER.getUuid(),
                                                          Constants.WEATHER.GATT_WEATHER_TODAY.getUuid());
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        service = null;
    }

    @Override
    protected void onPausing() {
    }

    @Override
    void deregisterGattComponents() {
        gattProviderWeather.deregisterConnGattSub(gattListenWeather);
    }

    @Override
    public void onScanResultReceived(ScanResult result) {

    }

    @Override
    public void onScanResultsReceived(List<ScanResult> results) {

    }

    @Override
    public void onRefreshedScanReceived(ScanResult result) {

    }

    @Override
    public void onRefreshedScansReceived(List<ScanResult> results) {

    }

    @Override
    public void onScanLost(ScanResult lostResult) {

    }

    @Override
    public void onScanFailed(int errorCode) {

    }

    private class ActionGattSetup extends AndroidAction<View, Void, Void, Void, Void> {

        public ActionGattSetup(View view, Class<?> actionType, String regMethodName) {
            super(view, actionType, regMethodName);
        }

        @Override
        public Object onActionPrepare(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            return null;
        }

        @Override
        public Void onActionDoWork(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            Object[] args = stripMethodArgs(methodArgs);
            UUID     uuid = (UUID) args[1];
            if (Constants.WEATHER.GATT_WEATHER_TODAY.getUuid().equals(uuid)) {
                gattProviderWeather.getGattCharacteristicRead(Constants.WEATHER.GATT_SERVICE_WEATHER.getUuid(),
                                                              Constants.WEATHER.GATT_WEATHER_TOMORROW.getUuid());
            } else if (Constants.WEATHER.GATT_WEATHER_TOMORROW.getUuid().equals(uuid)) {
                gattProviderWeather.getGattCharacteristicRead(Constants.WEATHER.GATT_SERVICE_WEATHER.getUuid(),
                                                              Constants.WEATHER.GATT_WEATHER_DAT.getUuid());
            }
            return null;
        }

        @Override
        public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2,
                                      Object[] additionalTags) {
            Object[] args    = stripMethodArgs(methodArgs);
            byte[]   value   = (byte[]) args[0];
            UUID     uuid    = (UUID) args[1];
            int      id      = 0;
            String   weather = null;
            if (Constants.WEATHER.GATT_WEATHER_TODAY.getUuid().equals(uuid)) {
                weather = App.inst().getString(R.string.info_weather_today, ParserUtil.parseWeather(value));
                id = R.id.txt_weather_today;
            } else if (Constants.WEATHER.GATT_WEATHER_TOMORROW.getUuid().equals(uuid)) {
                weather = App.inst().getString(R.string.info_weather_tomorrow, ParserUtil.parseWeather(value));
                id = R.id.txt_weather_tomorrow;
            } else if (Constants.WEATHER.GATT_WEATHER_DAT.getUuid().equals(uuid)) {
                weather = App.inst().getString(R.string.info_weather_dat, ParserUtil.parseWeather(value));
                id = R.id.txt_weather_dat;
            }
            if (id > 0) {
                ((TextView) findViewById(id)).setText(weather);
            }
        }
    }

    private class GattWeather implements IConnGattProvider.IConnGattSubscriber {

        @Override
        public void onGattReady(String macAddress) {
        }

        @Override
        public void onServicesReady(String macAddress) {
            gattProviderWeather.registerConnGattSub(gattListenWeather);
            gattProviderWeather.getGattCharacteristicRead(Constants.WEATHER.GATT_SERVICE_WEATHER.getUuid(),
                                                          Constants.WEATHER.GATT_WEATHER_TODAY.getUuid());
        }

        @Override
        public void onGattValueReceived(String macAddress, UUID uuid, byte[] value) {
            actionGatt.invokeSelf(value, uuid);
        }

        @Override
        public void onGattValueWriteReceived(String macAddress, UUID uuid, byte[] value) {

        }

        @Override
        public void onGattValueChanged(String macAddress, UUID uuid, byte[] value) {

        }
    }
}
