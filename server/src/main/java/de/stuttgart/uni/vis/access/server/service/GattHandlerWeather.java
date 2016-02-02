package de.stuttgart.uni.vis.access.server.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.server.AccessApp;
import de.stuttgart.uni.vis.access.server.R;
import zh.wang.android.apis.yweathergetter4a.WeatherInfo;
import zh.wang.android.apis.yweathergetter4a.YahooWeather;
import zh.wang.android.apis.yweathergetter4a.YahooWeatherExceptionListener;
import zh.wang.android.apis.yweathergetter4a.YahooWeatherInfoListener;

/**
 * @author Alexander Dridiger
 */
public class GattHandlerWeather extends BaseGattHandler implements YahooWeatherExceptionListener, YahooWeatherInfoListener {


    private List<BluetoothDevice> connectedDevices = new ArrayList<>();

    private GattCallback callback     = new GattCallback();
    private YahooWeather yahooWeather = YahooWeather.getInstance(5000, 5000, true);

    public GattHandlerWeather() {
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(UUID.fromString(Constants.GATT_SERVICE_WEATHER));
        constantUuids.add(UUID.fromString(Constants.GATT_WEATHER_DAT));
        constantUuids.add(UUID.fromString(Constants.GATT_WEATHER_TODAY));
        constantUuids.add(UUID.fromString(Constants.GATT_WEATHER_TOMORROW));
        constantUuids.add(UUID.fromString(Constants.GATT_WEATHER_QUERY));
        setConstantUuids(constantUuids);

        yahooWeather.setExceptionListener(this);
        searchByPlaceName("Stuttgart");
    }

    @Override
    protected void addServices() {
        BluetoothGattService serviceWeather = new BluetoothGattService(UUID.fromString(Constants.GATT_SERVICE_WEATHER),
                                                                       BluetoothGattService.SERVICE_TYPE_PRIMARY);

        serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_TODAY, BluetoothGattCharacteristic.PROPERTY_READ,
                                                              BluetoothGattCharacteristic.PERMISSION_READ, AccessApp.inst().getString(
                        R.string.bl_advert_cloudy).getBytes()));
        serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_TOMORROW, BluetoothGattCharacteristic.PROPERTY_READ,
                                                              BluetoothGattCharacteristic.PERMISSION_READ, AccessApp.inst().getString(
                        R.string.bl_advert_rainy).getBytes()));
        serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_DAT, BluetoothGattCharacteristic.PROPERTY_READ,
                                                              BluetoothGattCharacteristic.PERMISSION_READ, AccessApp.inst().getString(
                        R.string.bl_advert_sunny).getBytes()));
        serviceWeather.addCharacteristic(createCharacteristic(Constants.GATT_WEATHER_QUERY, BluetoothGattCharacteristic.PROPERTY_WRITE,
                                                              BluetoothGattCharacteristic.PERMISSION_WRITE, "blub".getBytes()));

        getServer().addService(serviceWeather);
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

    @Override
    public void onFailConnection(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onFailParsing(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onFailFindLocation(Exception e) {
        e.printStackTrace();
    }

    private void searchByPlaceName(String location) {
        yahooWeather.setNeedDownloadIcons(true);
        yahooWeather.setUnit(YahooWeather.UNIT.CELSIUS);
        yahooWeather.setSearchMode(YahooWeather.SEARCH_MODE.PLACE_NAME);
        yahooWeather.queryYahooWeatherByPlaceName(AccessApp.inst(), location, this);
    }

    @Override
    public void gotWeatherInfo(WeatherInfo weatherInfo) {
        if (weatherInfo != null) {


            String info = weatherInfo.getTitle() + "\n" + weatherInfo.getWOEIDneighborhood() + ", " + weatherInfo.getWOEIDCounty() + ", " +
                          weatherInfo.getWOEIDState() + ", " + weatherInfo.getWOEIDCountry();
            BluetoothGattCharacteristic c = getServer().getService(UUID.fromString(Constants.GATT_SERVICE_WEATHER)).getCharacteristic(
                    UUID.fromString(Constants.GATT_WEATHER_TODAY));
            c.setValue(weatherInfo.getCurrentText());

            for (BluetoothDevice dev : connectedDevices) {
                getServer().notifyCharacteristicChanged(dev, c, false);
            }
            //            mTvWeather0.setText("====== CURRENT ======" + "\n" +
            //                                "date: " + weatherInfo.getCurrentConditionDate() + "\n" +
            //                                "weather: " + weatherInfo.getCurrentText() + "\n" +
            //                                "temperature in ºC: " + weatherInfo.getCurrentTemp() + "\n" +
            //                                "wind chill: " + weatherInfo.getWindChill() + "\n" +
            //                                "wind direction: " + weatherInfo.getWindDirection() + "\n" +
            //                                "wind speed: " + weatherInfo.getWindSpeed() + "\n" +
            //                                "Humidity: " + weatherInfo.getAtmosphereHumidity() + "\n" +
            //                                "Pressure: " + weatherInfo.getAtmospherePressure() + "\n" +
            //                                "Visibility: " + weatherInfo.getAtmosphereVisibility());
            //            if (weatherInfo.getCurrentConditionIcon() != null) {
            //                mIvWeather0.setImageBitmap(weatherInfo.getCurrentConditionIcon());
            //            }
            //            for (int i = 0; i < YahooWeather.FORECAST_INFO_MAX_SIZE; i++) {
            //                final LinearLayout forecastInfoLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.forecastinfo, null);
            //                final TextView tvWeather = (TextView) forecastInfoLayout.findViewById(R.id.textview_forecast_info);
            //                final WeatherInfo.ForecastInfo forecastInfo = weatherInfo.getForecastInfoList().get(i);
            //                tvWeather.setText("====== FORECAST " + (i + 1) + " ======" + "\n" +
            //                                  "date: " + forecastInfo.getForecastDate() + "\n" +
            //                                  "weather: " + forecastInfo.getForecastText() + "\n" +
            //                                  "low  temperature in ºC: " + forecastInfo.getForecastTempLow() + "\n" +
            //                                  "high temperature in ºC: " + forecastInfo.getForecastTempHigh() + "\n"
            //                                  //						           "low  temperature in ºF: " + forecastInfo.getForecastTempLowF() + "\n" +
            //                                  //				                   "high temperature in ºF: " + forecastInfo.getForecastTempHighF() + "\n"
            //                                 );
            //                final ImageView ivForecast = (ImageView) forecastInfoLayout.findViewById(R.id.imageview_forecast_info);
            //                if (forecastInfo.getForecastConditionIcon() != null) {
            //                    ivForecast.setImageBitmap(forecastInfo.getForecastConditionIcon());
            //                }
            //                mWeatherInfosLayout.addView(forecastInfoLayout);
            //            }
        }
    }

    private class GattCallback extends BluetoothGattServerCallback {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    connectedDevices.add(device);
                    break;
                default:
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        connectedDevices.remove(device);
                    }
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
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
        }
    }
}
