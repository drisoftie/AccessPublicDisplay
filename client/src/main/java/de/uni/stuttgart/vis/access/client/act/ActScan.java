package de.uni.stuttgart.vis.access.client.act;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.act.ActBasePerms;
import de.stuttgart.uni.vis.access.common.brcst.BrcstBlAdaptChanged;
import de.stuttgart.uni.vis.access.common.util.ScheduleUtil;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.data.BlData;
import de.uni.stuttgart.vis.access.client.data.IBlDataSubscriber;
import de.uni.stuttgart.vis.access.client.service.IServiceBinderClient;
import de.uni.stuttgart.vis.access.client.service.IServiceBlListener;
import de.uni.stuttgart.vis.access.client.service.ServiceScan;
import de.uni.stuttgart.vis.access.client.service.bl.IConnAdvertProvider;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;
import de.uni.stuttgart.vis.access.client.view.AdaptScans;

public class ActScan extends ActBasePerms implements NavigationView.OnNavigationItemSelectedListener, ServiceConnection, IServiceBlListener,
                                                     IConnAdvertProvider.IConnAdvertSubscriber, IBlDataSubscriber {

    private RecyclerView               rcycDevices;
    private AdaptScans                 rcycAdaptDevices;
    private RecyclerView.LayoutManager rcycLayoutManager;
    private Menu                       menu;
    private IServiceBinderClient       service;

    private BroadcastReceiver brcstRcvrBlAdapt;

    private BroadcastReceiver brdcstRcvr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(getString(R.string.intent_bl_stopped))) {
                invalidateOptionsMenu();
            } else if (intent.hasExtra(getString(R.string.intent_action_bl_user_stopped))) {
                invalidateOptionsMenu();
            } else if (intent.hasExtra(getString(R.string.intent_action_bl_user_changing))) {

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.intent_bl_stopped));
        filter.addAction(getString(R.string.intent_action_bl_user_stopped));
        filter.addAction(getString(R.string.intent_action_bl_user_changing));
        LocalBroadcastManager.getInstance(this).registerReceiver(brdcstRcvr, filter);

        brcstRcvrBlAdapt = new BrcstBlAdaptChanged();

        setContentView(R.layout.act_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.tst_fab, Snackbar.LENGTH_LONG).setAction(R.string.cd_btn_filter, null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open,
                                                                 R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        rcycDevices = (RecyclerView) findViewById(R.id.my_recycler_view);
        rcycDevices.setHasFixedSize(true);
        rcycLayoutManager = new LinearLayoutManager(this);
        rcycDevices.setLayoutManager(rcycLayoutManager);
        rcycAdaptDevices = new AdaptScans();
        rcycDevices.setAdapter(rcycAdaptDevices);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

        if (savedInstanceState == null) {
            blAdapt = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        invalidateOptionsMenu();
    }

    @Override
    protected void onResuming() {
        registerReceiver(brcstRcvrBlAdapt, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    protected void onBlAdaptStarted() {
        checkPermLocation();
    }

    @Override
    protected void startBlServiceConn() {
        startBlServiceConnDelayed();
    }

    private void startBlServiceConnDelayed() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                startService(new Intent(ActScan.this, ServiceScan.class));
                bindService(new Intent(ActScan.this, ServiceScan.class), ActScan.this, BIND_AUTO_CREATE);
            }
        };
        ScheduleUtil.scheduleWork(task, 3, TimeUnit.SECONDS);
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder binder) {
        service = (IServiceBinderClient) binder;
        service.registerServiceListener(ActScan.this);
        service.subscribeAdvertConnection(Constants.UUID_ADVERT_SERVICE_MULTI.getUuid(), this);
        invalidateOptionsMenu();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        service = null;
    }


    @Override
    protected void deregisterComponents() {
        if (isServiceBlConnected()) {
            // Deactivate updates to us so that we dont get callbacks no more.
            service.deregisterServiceListener(this);
            unbindService(this);
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(brdcstRcvr);

        rcycAdaptDevices.getBlData().clear();
        rcycAdaptDevices.notifyDataSetChanged();
    }

    @Override
    protected void onPausing() {
        unregisterReceiver(brcstRcvrBlAdapt);
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
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private boolean isServiceBlConnected() {
        return service != null && service.isConnected(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.act_main, menu);
        this.menu = menu;
        if (isServiceBlConnected()) {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                rcycAdaptDevices.getBlData().clear();
                rcycAdaptDevices.notifyDataSetChanged();
                if (service == null) {
                    startService(new Intent(this, ServiceScan.class));
                    bindService(new Intent(ActScan.this, ServiceScan.class), ActScan.this, BIND_AUTO_CREATE);
                }
                item.setVisible(false);
                menu.findItem(R.id.menu_stop).setVisible(true);
                break;
            case R.id.menu_stop:
                item.setVisible(false);
                menu.findItem(R.id.menu_scan).setVisible(true);
                //                scanLeDevice(false);
                stopService(new Intent(this, ServiceScan.class));
                // Deactivate updates to us so that we dont get callbacks no more.
                service.deregisterServiceListener(this);

                // Finally stop the service
                unbindService(this);
                service = null;
                break;
        }
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camara) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onScanResultReceived(ScanResult result) {
        findViewById(R.id.txt_headline_displays).setVisibility(View.VISIBLE);
        BlData data = new BlData();
        data.setActive(true);
        data.setAddress(result.getDevice().getAddress());
        //noinspection ConstantConditions
        data.setAdvertisement(result.getScanRecord().getServiceData(Constants.UUID_ADVERT_SERVICE_MULTI));

    }

    @Override
    public void onScanResultsReceived(List<ScanResult> results) {
        findViewById(R.id.txt_headline_displays).setVisibility(View.VISIBLE);
        rcycAdaptDevices.notifyDataSetChanged();
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
    public void onBlDataAdded(BlData data) {

    }

    private class GattWeather implements IConnGattProvider.IConnGattSubscriber {

        @Override
        public void onGattReady() {

        }

        @Override
        public void onServicesReady() {

        }

        @Override
        public void onGattValueReceived(byte[] value) {

        }

        @Override
        public void onGattValueChanged(UUID uuid, byte[] value) {

        }
    }
}
