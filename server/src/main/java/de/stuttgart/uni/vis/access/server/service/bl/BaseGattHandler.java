package de.stuttgart.uni.vis.access.server.service.bl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.ParcelUuid;
import android.util.Log;

import com.drisoftie.action.async.handler.IFinishedHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.util.AccessGatt;
import de.stuttgart.uni.vis.access.server.BuildConfig;

/**
 * @author Alexander Dridiger
 */
public abstract class BaseGattHandler implements IGattHandler, AccessGatt.IGatt {

    private static final String TAG = BaseGattHandler.class.getSimpleName();

    private Queue<AccessGatt>        accessGatts = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService e           = Executors.newSingleThreadScheduledExecutor();

    private BluetoothGattServer server;
    private List<UUID>          constantUuids;
    private List<BluetoothDevice> connectedDevices = new ArrayList<>();
    private IFinishedHandler<UUID> readyListener;

    @Override
    public BluetoothGattServer getServer() {
        return server;
    }

    @Override
    public void setServer(BluetoothGattServer server) {
        this.server = server;
    }

    @Override
    public List<UUID> getConstantUuids() {
        return constantUuids;
    }

    @Override
    public void setConstantUuids(List<UUID> constantUuids) {
        this.constantUuids = constantUuids;
    }

    @Override
    public List<BluetoothDevice> getConnDevices() {
        return connectedDevices;
    }

    @Override
    public void checkAndAddDevice(BluetoothDevice device) {
        int foundDevice = -1;
        for (int i = 0; i < getConnDevices().size(); i++) {
            BluetoothDevice dev = getConnDevices().get(i);
            if (dev.getAddress().equals(device.getAddress())) {
                foundDevice = i;
            }
        }
        if (foundDevice == -1) {
            getConnDevices().add(device);
        } else {
            getConnDevices().set(foundDevice, device);
        }
    }

    @Override
    public void checkAndRemoveDevice(BluetoothDevice device) {
        int foundDevice = -1;
        for (int i = 0; i < getConnDevices().size(); i++) {
            BluetoothDevice dev = getConnDevices().get(i);
            if (dev.getAddress().equals(device.getAddress())) {
                foundDevice = i;
            }
        }
        if (foundDevice > -1) {
            getConnDevices().remove(foundDevice);
        }
    }

    @Override
    public void prepareServer() {
        server.clearServices();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Start Device Info");
        }
        addDeviceInfoService(server);
    }

    @Override
    public void prepareServices(IFinishedHandler<UUID> readyListener) {
        this.readyListener = readyListener;
        addServices();
    }

    @Override
    public void checkWork() {
        if (!accessGatts.isEmpty()) {
            e.schedule(accessGatts.poll(), 500, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public IFinishedHandler<UUID> getServicesReadyListener() {
        return readyListener;
    }

    protected void addDeviceInfoService(BluetoothGattServer gattServer) {
        if (null == gattServer) {
            return;
        }
        //
        // device info
        //
        BluetoothGattCharacteristic softwareVerCharacteristic = new BluetoothGattCharacteristic(
                Constants.SOFTWARE_REVISION_STRING.getUuid(), BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattService deviceInfoService = new BluetoothGattService(Constants.SERVICE_DEVICE_INFORMATION.getUuid(),
                                                                          BluetoothGattService.SERVICE_TYPE_PRIMARY);

        softwareVerCharacteristic.setValue("0.0.1".getBytes());

        deviceInfoService.addCharacteristic(softwareVerCharacteristic);
        gattServer.addService(deviceInfoService);
    }

    protected abstract void addServices();

    protected BluetoothGattCharacteristic createCharacteristic(String uuid, int property, int permission, byte[] value) {
        return createCharacteristic(UUID.fromString(uuid), property, permission, value);
    }

    protected BluetoothGattCharacteristic createCharacteristic(UUID uuid, int property, int permission, byte[] value) {
        BluetoothGattCharacteristic c = new BluetoothGattCharacteristic(uuid, property, permission);
        c.setValue(value);
        return c;
    }

    @Override
    public void changeGattChar(UUID servUuid, UUID charUuid, String value) {
        BluetoothGattCharacteristic c = getServer().getService(servUuid).getCharacteristic(charUuid);
        c.setValue(value);
        for (BluetoothDevice dev : getConnDevices()) {
            getServer().notifyCharacteristicChanged(dev, c, false);
        }
    }

    @Override
    public void changeGattChar(UUID servUuid, UUID charUuid, byte[] value) {
        BluetoothGattCharacteristic c = getServer().getService(servUuid).getCharacteristic(charUuid);
        c.setValue(value);
        for (BluetoothDevice dev : getConnDevices()) {
            getServer().notifyCharacteristicChanged(dev, c, false);
        }
    }

    @Override
    public BluetoothGattServerCallback getCallback(ParcelUuid[] uuids) {
        for (ParcelUuid uuid : uuids) {
            BluetoothGattServerCallback callback = getCallback(uuid);
            if (callback != null) {
                return callback;
            }
        }
        return null;
    }

    @Override
    public BluetoothGattServerCallback getCallback(UUID[] uuids) {
        for (UUID uuid : uuids) {
            BluetoothGattServerCallback callback = getCallback(uuid);
            if (callback != null) {
                return callback;
            }
        }
        return null;
    }

    @Override
    public BluetoothGattServerCallback getCallback(ParcelUuid uuid) {
        return getCallback(uuid.getUuid());
    }

    protected abstract class GattCallbackBase extends BluetoothGattServerCallback {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    checkAndAddDevice(device);
                    onConnectSuccess(device, status, newState);
                    break;
                default:
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        checkAndRemoveDevice(device);
                        onDisconnected(device, status, newState);
                    }
            }
        }

        public abstract void onConnectSuccess(BluetoothDevice device, int status, int newState);

        public abstract void onDisconnected(BluetoothDevice device, int status, int newState);

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
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
        }
    }
}
