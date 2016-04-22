package de.stuttgart.uni.vis.access.server.service.bl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.server.App;
import de.stuttgart.uni.vis.access.server.R;

/**
 * @author Alexander Dridiger
 */
public class GattHandlerShout extends BaseGattHandler {

    private GattCallback callback = new GattCallback();
    private ScheduledExecutorService shouter;
    private Runnable shout = new Runnable() {

        int count = 0;

        @Override
        public void run() {
            if (!getConnDevices().isEmpty()) {
                switch (count % 10) {
                    case 0:
                        changeGattChar(Constants.SHOUT.GATT_SERVICE_SHOUT.getUuid(), Constants.SHOUT.GATT_SHOUT.getUuid(),
                                       App.inst().getString(R.string.info_shout_wallmart));
                        break;
                    case 2:
                        changeGattChar(Constants.SHOUT.GATT_SERVICE_SHOUT.getUuid(), Constants.SHOUT.GATT_SHOUT.getUuid(),
                                       App.inst().getString(R.string.info_shout_breu));
                        break;
                    case 4:
                        changeGattChar(Constants.SHOUT.GATT_SERVICE_SHOUT.getUuid(), Constants.SHOUT.GATT_SHOUT.getUuid(),
                                       App.inst().getString(R.string.info_shout_prime));
                        break;
                    case 6:
                        changeGattChar(Constants.SHOUT.GATT_SERVICE_SHOUT.getUuid(), Constants.SHOUT.GATT_SHOUT.getUuid(),
                                       App.inst().getString(R.string.info_shout_media));
                        break;
                    case 8:
                        changeGattChar(Constants.SHOUT.GATT_SERVICE_SHOUT.getUuid(), Constants.SHOUT.GATT_SHOUT.getUuid(),
                                       App.inst().getString(R.string.info_shout_sport));
                        count = 0;
                        break;
                }
                count++;
            }
        }
    };

    public GattHandlerShout() {
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(Constants.SHOUT.GATT_SERVICE_SHOUT.getUuid());
        constantUuids.add(Constants.SHOUT.GATT_SHOUT.getUuid());
        setConstantUuids(constantUuids);
    }

    @Override
    protected void addServices() {
        BluetoothGattService serviceWeather = new BluetoothGattService(Constants.SHOUT.GATT_SERVICE_SHOUT.getUuid(),
                                                                       BluetoothGattService.SERVICE_TYPE_PRIMARY);

        serviceWeather.addCharacteristic(
                createCharacteristic(Constants.SHOUT.GATT_SHOUT.toString(), BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                                     BluetoothGattCharacteristic.PERMISSION_READ,
                                     App.inst().getString(R.string.bl_advert_cloudy).getBytes()));
        getServer().addService(serviceWeather);
    }

    @Override
    public GattCallback getCallback() {
        return callback;
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

    private void checkStopShouting() {
        if (getConnDevices().isEmpty()) {
            stopShouting();
        }
    }

    private void startShouting() {
        shouter = Executors.newSingleThreadScheduledExecutor();
        shouter.scheduleAtFixedRate(shout, 20, 20, TimeUnit.SECONDS);
    }

    private void stopShouting() {
        shouter.shutdown();
    }

    private class GattCallback extends GattCallbackBase {

        @Override
        public void onConnectSuccess(BluetoothDevice device, int status, int newState) {
            startShouting();
        }

        @Override
        public void onDisconnected(BluetoothDevice device, int status, int newState) {
            checkStopShouting();
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            getServicesReadyListener().onFinished(service.getUuid());
        }
    }
}
