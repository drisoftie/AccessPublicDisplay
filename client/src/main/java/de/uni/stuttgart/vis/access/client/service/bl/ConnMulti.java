package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.helper.INotifyProv;
import de.uni.stuttgart.vis.access.client.helper.ITtsProv;

/**
 * @author Alexander Dridiger
 */
public class ConnMulti extends ConnBaseAdvertScan implements IConnMulti {

    private List<IConnAdvertScan>                                 connections       = new ArrayList<>();
    private Map<UUID, List<IConnAdvertScan>>                      cachedConns       = new HashMap<>();
    private Map<String, List<IConnAdvertScan>>                    cachedDeviceConns = new HashMap<>();
    private List<AbstractMap.SimpleEntry<IConnMultiPart, String>> notifyParts       = new ArrayList<>();

    public ConnMulti() {
        ConnWeather weather = new ConnWeather();
        weather.setConnMulti(this);
        weather.setExecutor(getExecutor());
        weather.setAccessGatts(getAccessGatts());
        ConnShout shout = new ConnShout();
        shout.setConnMulti(this);
        shout.setExecutor(getExecutor());
        shout.setAccessGatts(getAccessGatts());
        ConnNews news = new ConnNews();
        news.setConnMulti(this);
        news.setExecutor(getExecutor());
        news.setAccessGatts(getAccessGatts());
        ConnBooking booking = new ConnBooking();
        booking.setConnMulti(this);
        booking.setExecutor(getExecutor());
        booking.setAccessGatts(getAccessGatts());
        connections.add(weather);
        connections.add(shout);
        connections.add(news);
        connections.add(booking);
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(Constants.UUID_ADVERT_SERVICE_MULTI.getUuid());
        constantUuids.add(Constants.WEATHER.UUID_ADVERT_SERVICE_WEATHER.getUuid());
        constantUuids.add(Constants.PUBTRANSP.UUID_ADVERT_SERVICE_PUB_TRANSP.getUuid());
        constantUuids.add(Constants.SHOUT.UUID_ADVERT_SERVICE_SHOUT.getUuid());
        constantUuids.add(Constants.NEWS.UUID_ADVERT_SERVICE_NEWS.getUuid());
        constantUuids.add(Constants.CHAT.UUID_ADVERT_SERVICE_CHAT.getUuid());
        constantUuids.add(Constants.WEATHER.GATT_SERVICE_WEATHER.getUuid());
        constantUuids.add(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid());
        constantUuids.add(Constants.SHOUT.GATT_SERVICE_SHOUT.getUuid());
        constantUuids.add(Constants.NEWS.GATT_SERVICE_NEWS.getUuid());
        constantUuids.add(Constants.CHAT.GATT_SERVICE_CHAT.getUuid());
        constantUuids.add(Constants.BOOKING.GATT_SERVICE_BOOKING.getUuid());
        setConstantUuids(constantUuids);
        setScanCallback(new BlAdvertScanCallback());
        setGattCallback(new BlGattCallback());
    }

    @Override
    public void setNotifyProv(INotifyProv notifyProv) {
        super.setNotifyProv(notifyProv);
        for (IConnAdvertScan handler : connections) {
            handler.setNotifyProv(notifyProv);
        }
    }

    @Override
    public void setTtsProv(ITtsProv ttsProv) {
        super.setTtsProv(ttsProv);
        for (IConnAdvertScan handler : connections) {
            handler.setTtsProv(ttsProv);
        }
    }

    @Override
    public IConnAdvertProvider registerConnectionAdvertSubscriber(UUID uuid, IConnAdvertSubscriber subscriber) {
        for (IConnAdvertScan conn : connections) {
            if (conn.match(uuid)) {
                return conn.registerConnectionAdvertSubscriber(uuid, subscriber);
            }
        }
        return null;
    }

    @Override
    public IConnGattProvider registerConnectionGattSubscriber(UUID uuid, IConnGattSubscriber subscriber) {
        for (IConnAdvertScan conn : connections) {
            if (conn.match(uuid)) {
                return conn.registerConnectionGattSubscriber(uuid, subscriber);
            }
        }
        return null;
    }

    @Override
    public void contributeNotification(String notificationDetails, IConnMultiPart part) {
        StringBuilder builder = new StringBuilder();
        boolean       found   = false;
        for (AbstractMap.SimpleEntry<IConnMultiPart, String> p : notifyParts) {
            if (p.getKey().equals(part)) {
                p.setValue(notificationDetails);
                found = true;
            }
            builder.append(p.getValue()).append(System.lineSeparator());
        }
        if (!found) {
            notifyParts.add(new AbstractMap.SimpleEntry<>(part, notificationDetails));
            builder.append(notificationDetails).append(System.lineSeparator());
        }
        builder.deleteCharAt(builder.length() - 1);
        getNotifyProv().provideNotify().createDisplayNotification("Some new infos!", builder.toString(),
                                                                  Constants.UUID_ADVERT_SERVICE_MULTI, part.getAdvertScan().getScanResults()
                                                                                                           .get(part.getAdvertScan()
                                                                                                                    .getScanResults()
                                                                                                                    .size() - 1),
                                                                  R.id.nid_main);
    }

    private IConnAdvertScan getConnection(UUID uuid) {
        for (IConnAdvertScan handler : connections) {
            if (handler.match(uuid)) {
                return handler;
            }
        }
        return null;
    }

    private IConnAdvertScan getConnection(UUID... uuids) {
        for (IConnAdvertScan handler : connections) {
            if (handler.match(uuids)) {
                return handler;
            }
        }
        return null;
    }

    private IConnAdvertScan getConnection(ParcelUuid... uuids) {
        for (IConnAdvertScan handler : connections) {
            if (handler.match(uuids)) {
                return handler;
            }
        }
        return null;
    }

    private IConnAdvertScan getConnection(List<ParcelUuid> uuids) {
        for (IConnAdvertScan handler : connections) {
            if (handler.match(uuids)) {
                return handler;
            }
        }
        return null;
    }

    private IConnAdvertScan getConnection(BluetoothDevice device) {
        for (IConnAdvertScan handler : connections) {
            if (handler.match(device)) {
                return handler;
            }
        }
        return null;
    }

    private List<IConnAdvertScan> getCachedConns(UUID uuid) {
        return cachedConns.get(uuid);
    }

    private List<IConnAdvertScan> getConnections(UUID uuid) {
        List<IConnAdvertScan> conns = getCachedConns(uuid);
        if (conns != null && !conns.isEmpty()) {
            return conns;
        }
        conns = new ArrayList<>();
        for (IConnAdvertScan handler : connections) {
            if (handler.match(uuid)) {
                conns.add(handler);
            }
        }
        cachedConns.put(uuid, conns);
        return conns;
    }


    private List<IConnAdvertScan> getConnections(List<ParcelUuid> uuids) {
        Set<IConnAdvertScan> connSet = new HashSet<>();
        for (ParcelUuid uuid : uuids) {
            List<IConnAdvertScan> conns = getCachedConns(uuid.getUuid());
            if (conns != null) {
                connSet.addAll(conns);
            }
        }
        if (!connSet.isEmpty()) {
            return new ArrayList<>(connSet);
        }
        List<IConnAdvertScan> conns = new ArrayList<>();
        for (ParcelUuid uuid : uuids) {
            for (IConnAdvertScan handler : connections) {
                if (handler.match(uuid) && !conns.contains(handler)) {
                    conns.add(handler);
                    if (cachedConns.containsKey(uuid.getUuid())) {
                        cachedConns.get(uuid.getUuid()).add(handler);
                    } else {
                        ArrayList<IConnAdvertScan> cs = new ArrayList<>();
                        cs.add(handler);
                        cachedConns.put(uuid.getUuid(), cs);
                    }
                }
            }
        }
        return conns;
    }

    private List<IConnAdvertScan> getCachedDeviceConns(BluetoothDevice device) {
        return cachedDeviceConns.get(device.getAddress());
    }

    private List<IConnAdvertScan> getConnections(BluetoothDevice device) {
        List<IConnAdvertScan> conns = getCachedDeviceConns(device);
        if (conns != null && !conns.isEmpty()) {
            return conns;
        }
        conns = new ArrayList<>();
        for (IConnAdvertScan handler : connections) {
            if (handler.match(device)) {
                conns.add(handler);
            }
        }
        cachedDeviceConns.put(device.getAddress(), conns);
        return conns;
    }

    @Override
    public boolean match(BluetoothDevice device) {
        for (IConnAdvertScan handler : connections) {
            if (handler.match(device)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addConnDevice(BluetoothDevice dev) {
        IConnAdvertScan handler = getConnection(dev);
        if (handler != null) {
            handler.addConnDevice(dev);
        }
    }

    @Override
    public boolean hasConnDevice(BluetoothDevice deviceToFind) {
        IConnAdvertScan handler = getConnection(deviceToFind);
        if (handler != null) {
            return true;
        }
        return false;
    }

    @Override
    public void onScanningStopped() {
        for (IConnAdvertScan conn : connections) {
            conn.onScanningStopped();
        }
    }

    @Override
    public void onScanLost(ScanResult result) {
        if (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
            List<IConnAdvertScan> handler = getConnections(result.getScanRecord().getServiceUuids());
            for (IConnAdvertScan h : handler) {
                // don't send results to a handler multiple times
                h.onScanLost(result);
            }
        }
    }

    private class BlAdvertScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            ArrayList<IConnAdvertScan> checkedConns = new ArrayList<>(connections);
            for (ScanResult result : results) {
                if (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
                    List<IConnAdvertScan> handler = getConnections(result.getScanRecord().getServiceUuids());
                    for (IConnAdvertScan h : handler) {
                        // don't send results to a handler multiple times
                        if (checkedConns.contains(h)) {
                            h.getScanCallback().onBatchScanResults(results);
                            checkedConns.remove(h);
                        }
                    }
                }
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
                List<IConnAdvertScan> handler = getConnections(result.getScanRecord().getServiceUuids());
                for (IConnAdvertScan h : handler) {
                    // don't send results to a handler multiple times
                    h.getScanCallback().onScanResult(callbackType, result);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            for (IConnAdvertScan conn : connections) {
                conn.getScanCallback().onScanFailed(errorCode);
            }
        }
    }

    /**
     *
     */
    private class BlGattCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    switch (newState) {
                        case BluetoothProfile.STATE_CONNECTED:
                            List<IConnAdvertScan> handler = getConnections(gatt.getDevice());
                            for (IConnAdvertScan h : handler) {
                                h.getGattCallback().onConnectionStateChange(gatt, status, newState);
                            }
                            addConnDevice(gatt.getDevice());
                    }
                    break;
                case BluetoothGatt.GATT_FAILURE:
                    switch (newState) {
                        case BluetoothProfile.STATE_DISCONNECTED:
                        case BluetoothProfile.STATE_DISCONNECTING:
                            List<IConnAdvertScan> handler = getConnections(gatt.getDevice());
                            for (IConnAdvertScan h : handler) {
                                h.getGattCallback().onConnectionStateChange(gatt, status, newState);
                            }
                            cachedDeviceConns.remove(gatt.getDevice().getAddress());
                            removeConnDevice(gatt.getDevice());
                    }
                    break;
                default:
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        List<IConnAdvertScan> handler = getConnections(gatt.getDevice());
                        for (IConnAdvertScan h : handler) {
                            h.getGattCallback().onConnectionStateChange(gatt, status, newState);
                        }
                        cachedDeviceConns.remove(gatt.getDevice().getAddress());
                    }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            List<IConnAdvertScan> handler = getConnections(gatt.getDevice());
            for (IConnAdvertScan h : handler) {
                h.getGattCallback().onServicesDiscovered(gatt, status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            List<IConnAdvertScan> handler = getConnections(gatt.getDevice());
            for (IConnAdvertScan h : handler) {
                h.getGattCallback().onCharacteristicRead(gatt, characteristic, status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            List<IConnAdvertScan> handler = getConnections(gatt.getDevice());
            for (IConnAdvertScan h : handler) {
                h.getGattCallback().onCharacteristicWrite(gatt, characteristic, status);
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            List<IConnAdvertScan> handler = getConnections(gatt.getDevice());
            for (IConnAdvertScan h : handler) {
                h.getGattCallback().onCharacteristicChanged(gatt, characteristic);
            }
        }
    }
}
