package de.uni.stuttgart.vis.access.client.act;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import com.drisoftie.frags.comp.ManagedActivity;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.helper.IContextProv;
import de.uni.stuttgart.vis.access.client.service.IServiceBinder;
import de.uni.stuttgart.vis.access.client.service.IServiceBlListener;
import de.uni.stuttgart.vis.access.client.service.ServiceScan;

public class ActWeather extends ManagedActivity implements ServiceConnection, IServiceBlListener, IContextProv, IViewProv {

    private BroadcastReceiver msgReceiver = new BrdcstReceiver();

    private IServiceBinder          service;
    private ConnGattGattCommWeather gattCommunicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalBroadcastManager.getInstance(this).registerReceiver(msgReceiver, new IntentFilter(getString(R.string.intent_gatt_weather)));
        setContentView(R.layout.act_weather);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResuming() {
    }

    @Override
    protected void registerComponents() {
        Runnable task = new Runnable() {

            @Override
            public void run() {
                View current = getCurrentFocus();
                Objects.requireNonNull(current).clearFocus();
                findViewById(R.id.txt_headline_today).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }
        };

        final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
        worker.schedule(task, 1, TimeUnit.SECONDS);

        Intent intent = new Intent(getString(R.string.intent_weather_get));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        bindService(new Intent(this, ServiceScan.class), this, BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder binder) {
        service = (IServiceBinder) binder;
        service.registerServiceListener(this);
        gattCommunicator = new ConnGattGattCommWeather();
        gattCommunicator.setContextProvider(this);
        gattCommunicator.setViewProvider(this);
        gattCommunicator.setConn(service.subscribeBlConnection(UUID.fromString(Constants.GATT_SERVICE_WEATHER), gattCommunicator));
    }

    @Override
    public Context provideContext() {
        return this;
    }

    @Override
    public View provideView(int resId) {
        return findViewById(resId);
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        service = null;
    }

    @Override
    public void onConnStopped() {
        // Deactivate updates to us so that we dont get callbacks no more.
        service.deRegisterServiceListener(this);

        // Finally stop the service
        unbindService(this);
    }

    @Override
    protected void deregisterComponents() {
        // Deactivate updates to us so that we dont get callbacks no more.
        service.deRegisterServiceListener(this);

        // Finally stop the service
        unbindService(this);
        gattCommunicator.onDetach();
        gattCommunicator = null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(msgReceiver);
    }

    @Override
    protected void onPausing() {
    }

    private class BrdcstReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    }
}
