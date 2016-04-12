package de.uni.stuttgart.vis.access.client.act;

import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.act.ActBasePerms;
import de.stuttgart.uni.vis.access.common.util.ScheduleUtil;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.service.IServiceBinderClient;
import de.uni.stuttgart.vis.access.client.service.IServiceBlListener;
import de.uni.stuttgart.vis.access.client.service.ServiceScan;
import de.uni.stuttgart.vis.access.client.service.bl.IConnAdvertProvider;

public class ActMulti extends ActBasePerms implements ServiceConnection, IServiceBlListener, IConnAdvertProvider.IConnAdvertSubscriber {

    private static final String TAG = "ActMulti";

    private IServiceBinderClient service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_multi);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResuming() {

    }

    @Override
    protected void deregisterComponents() {

    }

    @Override
    protected void onPausing() {

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

    @Override
    public void onConnStopped() {
        // Deactivate updates to us so that we dont get callbacks no more.
        service.deregisterServiceListener(this);

        // Finally stop the service
        unbindService(this);
        service = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = (IServiceBinderClient) binder;
        service.registerServiceListener(ActMulti.this);
        service.subscribeAdvertConnection(Constants.UUID_ADVERT_SERVICE_MULTI.getUuid(), this);
        invalidateOptionsMenu();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    @Override
    protected void startBlServiceConn() {
        startBlServiceConnDelayed();
    }

    private void startBlServiceConnDelayed() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                startService(new Intent(ActMulti.this, ServiceScan.class));
                bindService(new Intent(ActMulti.this, ServiceScan.class), ActMulti.this, BIND_AUTO_CREATE);
            }
        };
        ScheduleUtil.scheduleWork(task, 3, TimeUnit.SECONDS);
    }

    @Override
    protected void onBlAdaptStarted() {
        checkPermLocation();
    }
}
