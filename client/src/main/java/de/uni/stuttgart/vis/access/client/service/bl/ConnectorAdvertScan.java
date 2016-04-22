package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.BuildConfig;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.helper.IContextProv;
import de.uni.stuttgart.vis.access.client.helper.INotifyProv;
import de.uni.stuttgart.vis.access.client.helper.ITtsProv;
import de.uni.stuttgart.vis.access.client.helper.NotifyHolder;
import de.uni.stuttgart.vis.access.client.helper.TtsWrapper;

/**
 * @author Alexander Dridiger
 */
public class ConnectorAdvertScan implements INotifyProv, ITtsProv {

    private static final String TAG = "ConnectorAdvertScan";

    private ScanCallback          cllbckAdvertScan = new BlAdvertScanCallback();
    private BluetoothGattCallback cllbckGatt       = new BlGattCallback();

    private List<IConnAdvertScan> connections = new ArrayList<>();

    private IContextProv cntxtProv;
    private ITtsProv     ttsProv;
    private INotifyProv  notifyProv;

    private ScheduledExecutorService                           executerAliveChecker = Executors.newSingleThreadScheduledExecutor();
    private List<AbstractMap.SimpleEntry<ScanResult, Integer>> scanCounter          = Collections.synchronizedList(
            new ArrayList<AbstractMap.SimpleEntry<ScanResult, Integer>>());

    public ConnectorAdvertScan(IContextProv cntxtProv) {
        this.cntxtProv = cntxtProv;
        connections.add(new ConnMulti());
    }

    public IContextProv getCntxtProv() {
        return cntxtProv;
    }

    public void setCntxtProv(IContextProv cntxtProv) {
        this.cntxtProv = cntxtProv;
    }

    public ITtsProv getTtsProv() {
        return ttsProv;
    }

    public void setTtsProv(ITtsProv ttsProv) {
        this.ttsProv = ttsProv;
        for (IConnAdvertScan conn : connections) {
            conn.setTtsProv(ttsProv);
        }
    }

    public INotifyProv getNotifyProv() {
        return notifyProv;
    }

    public void setNotifyProv(INotifyProv notifyProv) {
        this.notifyProv = notifyProv;
        for (IConnAdvertScan conn : connections) {
            conn.setNotifyProv(notifyProv);
        }
    }

    public ScanCallback getAdvertScanCallback() {
        return cllbckAdvertScan;
    }

    public BluetoothGattCallback getGattCallback() {
        return cllbckGatt;
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

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    public List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        scanFilters.add(getFilter(Constants.UUID_ADVERT_SERVICE_MULTI));
        scanFilters.add(getFilter(Constants.WEATHER.UUID_ADVERT_SERVICE_WEATHER));
        scanFilters.add(getFilter(Constants.PUBTRANSP.UUID_ADVERT_SERVICE_PUB_TRANSP));
        scanFilters.add(getFilter(Constants.SHOUT.UUID_ADVERT_SERVICE_SHOUT));
        scanFilters.add(getFilter(Constants.NEWS.UUID_ADVERT_SERVICE_NEWS));
        scanFilters.add(getFilter(Constants.CHAT.UUID_ADVERT_SERVICE_CHAT));
        scanFilters.add(getFilter(Constants.BOOKING.UUID_ADVERT_SERVICE_BOOKING));

        return scanFilters;
    }

    private ScanFilter getFilter(ParcelUuid uuid) {
        return new ScanFilter.Builder().setServiceUuid(uuid).build();
    }

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    public ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }

    public void startingAdvertScan() {
        notifyProv.provideNotify().createScanNotification();
        ttsProv.provideTts().queueRead(cntxtProv.provideContext().getString(R.string.ntxt_scan));
        executerAliveChecker.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < scanCounter.size(); i++) {
                    ScanResult result = scanCounter.get(i).getKey();
                    if (scanCounter.get(i).getValue() == 0) {
                        IConnAdvertScan handler = getConnection(result.getDevice());
                        if (handler != null) {
                            handler.onScanLost(result);
                        }
                    }
                }
                countingOrResetting(false, null);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    public void scanningStopped() {
        notifyProv.provideNotify().removeAllNotifications();
        for (IConnAdvertScan conn : connections) {
            conn.onScanningStopped();
        }
        connections.clear();
    }


    public IConnAdvertProvider subscribeAdvertConnection(UUID uuid, IConnAdvertProvider.IConnAdvertSubscriber subscriber) {
        for (IConnAdvertScan conn : connections) {
            if (conn.match(uuid)) {
                return conn.registerConnectionAdvertSubscriber(uuid, subscriber);
            }
        }
        return null;
    }

    public IConnGattProvider subscribeGattConnection(UUID uuid, IConnGattProvider.IConnGattSubscriber subscriber) {
        for (IConnAdvertScan conn : connections) {
            if (conn.match(uuid)) {
                return conn.registerConnectionGattSubscriber(uuid, subscriber);
            }
        }
        return null;
    }

    public void connectGatt(Parcelable parcelableExtra) {
        if (parcelableExtra instanceof ScanResult) {
            ScanResult result = (ScanResult) parcelableExtra;
            for (IConnAdvertScan conn : connections) {
                if (result.getScanRecord() != null && conn.match(result.getScanRecord().getServiceUuids())) {
                    result.getDevice().connectGatt(getCntxtProv().provideContext(), false, getGattCallback());
                    break;
                }
            }
        }
    }

    public void connectGatt(BluetoothAdapter adapter, String address) {
        adapter.getRemoteDevice(address).connectGatt(getCntxtProv().provideContext(), false, getGattCallback());
    }

    @Override
    public NotifyHolder provideNotify() {
        return getNotifyProv().provideNotify();
    }

    @Override
    public TtsWrapper provideTts() {
        return getTtsProv().provideTts();
    }

    private synchronized void countingOrResetting(boolean counting, ScanResult result) {
        if (counting) {
            boolean found = false;
            for (int i = 0; i < scanCounter.size(); i++) {
                if (scanCounter.get(i).getKey().getDevice().getAddress().equals(result.getDevice().getAddress())) {
                    scanCounter.get(i).setValue(scanCounter.get(i).getValue() + 1);
                    found = true;
                }
            }
            if (!found) {
                scanCounter.add(new AbstractMap.SimpleEntry<>(result, 1));
            }
        } else {
            for (int i = 0; i < scanCounter.size(); i++) {
                scanCounter.get(i).setValue(0);
            }
        }
    }

    /**
     *
     */
    private class BlAdvertScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            ArrayList<IConnAdvertScan> checkedConns = new ArrayList<>(connections);
            for (ScanResult result : results) {
                countingOrResetting(true, result);
                if (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
                    IConnAdvertScan handler = getConnection(result.getScanRecord().getServiceUuids());
                    if (handler != null && checkedConns.contains(handler)) {
                        handler.getScanCallback().onBatchScanResults(results);
                        checkedConns.remove(handler);
                    }
                }
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String address = result.getDevice().getAddress();
            countingOrResetting(true, result);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onScanResult: " + address);
            }
            if (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
                IConnAdvertScan handler = getConnection(result.getScanRecord().getServiceUuids());
                if (handler != null) {
                    handler.getScanCallback().onScanResult(callbackType, result);
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
                            gatt.requestMtu(512);
                    }
                    break;
                case BluetoothGatt.GATT_FAILURE:
                    switch (newState) {
                        case BluetoothProfile.STATE_DISCONNECTED:
                        case BluetoothProfile.STATE_DISCONNECTING:
                            IConnAdvertScan handler = getConnection(gatt.getDevice());
                            if (handler != null) {
                                handler.getGattCallback().onConnectionStateChange(gatt, status, newState);
                            }
                    }
                    break;
                default:
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        IConnAdvertScan handler = getConnection(gatt.getDevice());
                        if (handler != null) {
                            handler.getGattCallback().onConnectionStateChange(gatt, status, newState);
                        }
                    }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                IConnAdvertScan handler = getConnection(gatt.getDevice());
                if (handler != null) {
                    handler.getGattCallback().onServicesDiscovered(gatt, status);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                IConnAdvertScan handler = getConnection(gatt.getDevice());
                if (handler != null) {
                    handler.getGattCallback().onCharacteristicRead(gatt, characteristic, status);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            IConnAdvertScan handler = getConnection(gatt.getDevice());
            if (handler != null) {
                handler.getGattCallback().onCharacteristicChanged(gatt, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            IConnAdvertScan handler = getConnection(gatt.getDevice());
            if (handler != null) {
                handler.getGattCallback().onCharacteristicWrite(gatt, characteristic, status);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            IConnAdvertScan handler = getConnection(gatt.getDevice());
            if (handler != null) {
                handler.getGattCallback().onConnectionStateChange(gatt, status, BluetoothProfile.STATE_CONNECTED);
            }
            if (gatt.getServices() == null || (gatt.getServices() != null && gatt.getServices().isEmpty())) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }
    }
}
