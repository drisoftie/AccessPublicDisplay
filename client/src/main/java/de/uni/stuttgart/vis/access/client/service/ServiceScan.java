package de.uni.stuttgart.vis.access.client.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.brcst.BrcstBlAdaptChanged;
import de.uni.stuttgart.vis.access.client.App;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.act.ActPubTransp;
import de.uni.stuttgart.vis.access.client.act.ActWeather;
import de.uni.stuttgart.vis.access.client.helper.IContextProv;
import de.uni.stuttgart.vis.access.client.helper.INotifyProv;
import de.uni.stuttgart.vis.access.client.helper.ITtsProv;
import de.uni.stuttgart.vis.access.client.helper.NotifyHolder;
import de.uni.stuttgart.vis.access.client.helper.TtsWrapper;
import de.uni.stuttgart.vis.access.client.service.bl.ConnectorAdvertScan;
import de.uni.stuttgart.vis.access.client.service.bl.IConnAdvertProvider;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;

/**
 * Manages BLE Advertising independent of the main app.
 * If the app goes off screen (or gets killed completely) advertising can continue because this
 * Service is maintaining the necessary Callback in memory.
 */
public class ServiceScan extends Service implements IContextProv, ITtsProv, INotifyProv {

    /**
     * A global variable to let AdvertiserFragment check if the Service is running without needing
     * to start or bind to it.
     * This is the best practice method as defined here:
     * https://groups.google.com/forum/#!topic/android-developers/jEvXMWgbgzE
     */
    public static boolean running = false;

    private final Binder serviceBinder = new ServiceBinder();

    private List<IServiceBlListener> serviceListeners = new ArrayList<>();

    private ConnectorAdvertScan connectorAdvertScan;

    private BluetoothAdapter   blAdapt;
    private BluetoothLeScanner blLeScanner;
    private Handler            handler;

    private Runnable timeoutRunnable;
    private BroadcastReceiver brdRcvr = new BrdcstRcvrService();

    private BroadcastReceiver brcstRcvrBlAdapt = new BrcstBlAdaptChanged();
    private TtsWrapper tts;

    private NotifyHolder notify;
    /**
     * Length of time to allow advertising scanning before automatically shutting off.
     */
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;

        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.intent_advert_gatt_connect));
        filter.addAction(getString(R.string.intent_advert_gatt_connect_weather));
        filter.addAction(getString(R.string.intent_action_bl_user_stopped));
        filter.addAction(getString(R.string.intent_action_bl_user_changing));
        LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvr, filter);

        registerReceiver(brcstRcvrBlAdapt, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        notify = new NotifyHolder();
        notify.setService(this);
        handler = new Handler();

        tts = new TtsWrapper(this);

        connectorAdvertScan = new ConnectorAdvertScan(this);
        connectorAdvertScan.setNotifyProv(this);
        connectorAdvertScan.setTtsProv(this);

        blAdapt = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        blLeScanner = blAdapt.getBluetoothLeScanner();
        checkAndScanLeDevices();
    }

    /**
     * Required for extending service, but this will be a Started Service only, so no need for
     * binding.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private void checkAndScanLeDevices() {
        if (blLeScanner == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
        } else {
            setTimeout();
            scanLeDevice(true);
        }
    }

    private void scanLeDevice(boolean enable) {
        if (enable) {
            blLeScanner.startScan(connectorAdvertScan.buildScanFilters(), connectorAdvertScan.buildScanSettings(),
                                  connectorAdvertScan.getAdvertScanCallback());
            connectorAdvertScan.startingAdvertScan();
            //            } else {
        } else {
            stopScanning();
            // invalidateOptionsMenu();
        }
        // invalidateOptionsMenu();
    }

    /**
     * Stop scanning for BLE Advertisements.
     */
    public void stopScanning() {
        connectorAdvertScan.scanningStopped();
        // Stop the scan, wipe the callback.
        blLeScanner.stopScan(connectorAdvertScan.getAdvertScanCallback());
    }

    /**
     * Starts a delayed Runnable that will cause the BLE Advertising to timeout and stop after a
     * set amount of time.
     */
    private void setTimeout() {
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                //                Log.d(TAG, "ServiceAdvertise has reached timeout of " + TIMEOUT + " milliseconds, stopping advertising.");
                //                sendFailureIntent(ADVERTISING_TIMED_OUT);
                stopSelf();
            }
        };
        handler.postDelayed(timeoutRunnable, TIMEOUT);
    }

    @Override
    public Context provideContext() {
        return this;
    }

    @Override
    public TtsWrapper provideTts() {
        return tts;
    }

    @Override
    public NotifyHolder provideNotify() {
        return notify;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        running = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(brdRcvr);
        unregisterReceiver(brcstRcvrBlAdapt);

        tts.shutDown();
        handler.removeCallbacks(timeoutRunnable);
        stopScanning();
        notify.setService(null);
        notify = null;
        connectorAdvertScan = null;

        App.holder().shutdown();

        for (IServiceBlListener l : serviceListeners) {
            l.onConnStopped();
        }
    }

    private class BrdcstRcvrService extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StringUtils.equals(intent.getAction(), getString(R.string.intent_advert_gatt_connect_weather))) {
                if (intent.getParcelableExtra(getString(R.string.bndl_bl_scan_result)) != null) {
                    connectorAdvertScan.connectGatt(intent.getParcelableExtra(getString(R.string.bndl_bl_scan_result)));
                } else if (intent.getStringExtra(getString(R.string.bndl_bl_address)) != null) {
                    connectorAdvertScan.connectGatt(blAdapt, intent.getStringExtra(getString(R.string.bndl_bl_address)));
                }
                ParcelUuid startIntent = intent.getParcelableExtra(getString(R.string.bndl_bl_show));
                if (Constants.UUID_ADVERT_SERVICE_MULTI.getUuid().equals(startIntent.getUuid())) {
                    Intent weatherIntent = new Intent(ServiceScan.this, ActWeather.class);
                    weatherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(weatherIntent);
                } else if (Constants.UUID_ADVERT_SERVICE_WEATHER.getUuid().equals(startIntent.getUuid())) {
                    Intent weatherIntent = new Intent(ServiceScan.this, ActPubTransp.class);
                    weatherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(weatherIntent);
                }
            } else if (StringUtils.equals(intent.getAction(), getString(R.string.intent_advert_gatt_connect))) {
                connectorAdvertScan.connectGatt(intent.getParcelableExtra(getString(R.string.bndl_bl_scan_result)));
            } else if (StringUtils.equals(intent.getAction(), getString(R.string.intent_action_bl_user_stopped))) {
                for (IServiceBlListener l : serviceListeners) {
                    l.onConnStopped();
                }
                stopSelf();
            }
        }
    }

    public class ServiceBinder extends Binder implements IServiceBinderClient {

        @Override
        public void registerServiceListener(IServiceBlListener listener) {
            serviceListeners.add(listener);
        }

        @Override
        public void deregisterServiceListener(IServiceBlListener listener) {
            serviceListeners.remove(listener);
        }

        @Override
        public boolean isConnected(IServiceBlListener listener) {
            return serviceListeners.contains(listener);
        }

        @Override
        public IConnGattProvider subscribeGattConnection(UUID uuid, IConnGattProvider.IConnGattSubscriber subscriber) {
            return connectorAdvertScan.subscribeGattConnection(uuid, subscriber);
        }

        @Override
        public IConnAdvertProvider subscribeAdvertConnection(UUID uuid, IConnAdvertProvider.IConnAdvertSubscriber subscriber) {
            return connectorAdvertScan.subscribeAdvertConnection(uuid, subscriber);
        }

        @Override
        public ITtsProv getTtsProvider() {
            return ServiceScan.this;
        }
    }
}