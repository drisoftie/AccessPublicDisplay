package de.stuttgart.uni.vis.access.server.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.res.Resources;
import android.view.View;

import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.android.AndroidAction;

import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.DailyForecast;
import net.aksingh.owmjapis.OpenWeatherMap;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.server.AccessApp;
import de.stuttgart.uni.vis.access.server.R;

/**
 * @author Alexander Dridiger
 */
public class GattHandlerWeather extends BaseGattHandler {

    private static final String TAG = GattHandlerWeather.class.getSimpleName();

    private GattCallback callback = new GattCallback();
    private CurrentWeather    currWeather;
    private DailyForecast     forecast;
    private ActionServicesAdd actionServicesAdd;

    public GattHandlerWeather() {
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(Constants.GATT_SERVICE_WEATHER.getUuid());
        constantUuids.add(Constants.GATT_WEATHER_DAT.getUuid());
        constantUuids.add(Constants.GATT_WEATHER_TODAY.getUuid());
        constantUuids.add(Constants.GATT_WEATHER_TOMORROW.getUuid());
        constantUuids.add(Constants.GATT_WEATHER_QUERY.getUuid());
        setConstantUuids(constantUuids);
    }

    @Override
    protected void addServices() {
        actionServicesAdd = new ActionServicesAdd(null, IGenericAction.class, null);
        actionServicesAdd.invokeSelf();
    }

    @Override
    public BluetoothGattServerCallback getCallback(UUID uuid) {
        for (UUID myUuid : getConstantUuids()) {
            if (myUuid.equals(uuid)) {
                return callback;
            }
        }
        return null;
    }

    private class GattCallback extends BluetoothGattServerCallback {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    getConnDevices().add(device);
                    break;
                default:
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        getConnDevices().remove(device);
                    }
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            new WeatherAction(null, IGenericAction.class, null).invokeSelf();
            actionServicesAdd.invokeSelf(service.getUuid());
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            characteristic.setValue(value);
            if (responseNeeded) {
                getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {

        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
        }
    }


    private class WeatherAction extends AndroidAction<View, Void, Void, Void, Void> {

        public WeatherAction(View view, Class<?> actionType, String regMethodName) {
            super(view, actionType, regMethodName);
        }

        @Override
        public Object onActionPrepare(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            return null;
        }

        @Override
        public Void onActionDoWork(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            OpenWeatherMap owm = new OpenWeatherMap(OpenWeatherMap.Units.IMPERIAL, OpenWeatherMap.Language.fromLangCode(
                    Resources.getSystem().getConfiguration().locale.getLanguage()), AccessApp.string(R.string.ow_api));
            try {
                currWeather = owm.currentWeatherByCityName("Stuttgart", "de");
                forecast = owm.dailyForecastByCityName("Stuttgart", "de", (byte) 2);

                changeGattChar(Constants.GATT_SERVICE_WEATHER.getUuid(), Constants.GATT_WEATHER_TODAY.getUuid(),
                               currWeather.getWeatherInstance(0).getWeatherDescription());
                for (int i = 0; i < forecast.getForecastCount(); i++) {
                    String descr = forecast.getForecastInstance(i).getWeatherInstance(0).getWeatherDescription();
                    switch (i) {
                        case 0:
                            changeGattChar(Constants.GATT_SERVICE_WEATHER.getUuid(), Constants.GATT_WEATHER_TODAY.getUuid(), descr);
                            break;
                        case 1:
                            changeGattChar(Constants.GATT_SERVICE_WEATHER.getUuid(), Constants.GATT_WEATHER_TOMORROW.getUuid(), descr);
                            break;
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2,
                                      Object[] additionalTags) {

        }
    }


    private class ActionServicesAdd extends AndroidAction<View, Void, Void, Void, Void> {

        public ActionServicesAdd(View view, Class<?> actionType, String regMethodName) {
            super(view, actionType, regMethodName);
        }

        @Override
        public Object onActionPrepare(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            return null;
        }

        @Override
        public Void onActionDoWork(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            if (ArrayUtils.isEmpty(stripMethodArgs(methodArgs))) {
                BluetoothGattService serviceWeather = new BluetoothGattService(Constants.GATT_SERVICE_WEATHER.getUuid(),
                                                                               BluetoothGattService.SERVICE_TYPE_PRIMARY);
                serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_TODAY.toString(),
                                                                      BluetoothGattCharacteristic.PROPERTY_READ,
                                                                      BluetoothGattCharacteristic.PERMISSION_READ,
                                                                      AccessApp.inst().getString(R.string.bl_advert_cloudy).getBytes()));
                serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_TOMORROW.toString(),
                                                                      BluetoothGattCharacteristic.PROPERTY_READ,
                                                                      BluetoothGattCharacteristic.PERMISSION_READ,
                                                                      AccessApp.inst().getString(R.string.bl_advert_rainy).getBytes()));
                serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_DAT.toString(),
                                                                      BluetoothGattCharacteristic.PROPERTY_READ,
                                                                      BluetoothGattCharacteristic.PERMISSION_READ,
                                                                      AccessApp.inst().getString(R.string.bl_advert_sunny).getBytes()));
                serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_QUERY.toString(),
                                                                      BluetoothGattCharacteristic.PROPERTY_WRITE,
                                                                      BluetoothGattCharacteristic.PERMISSION_WRITE, "blub".getBytes()));
                getServer().addService(serviceWeather);
            } else {
                Object[] args = stripMethodArgs(methodArgs);
                getServicesReadyListener().onFinished((UUID) args[0]);
            }
            return null;
        }

        @Override
        public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2,
                                      Object[] additionalTags) {

        }
    }
}
