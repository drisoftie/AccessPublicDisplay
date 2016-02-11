package de.uni.stuttgart.vis.access.client.act;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

import com.drisoftie.frags.comp.ManagedActivity;

import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.util.ScheduleUtil;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.service.IServiceBinder;
import de.uni.stuttgart.vis.access.client.service.IServiceBlListener;
import de.uni.stuttgart.vis.access.client.service.ServiceScan;
import de.uni.stuttgart.vis.access.client.view.AdaptScanResults;

public class ActScan extends ManagedActivity
        implements NavigationView.OnNavigationItemSelectedListener, ServiceConnection, IServiceBlListener {

    private RecyclerView               rcycDevices;
    private AdaptScanResults           rcycAdaptDevices;
    private RecyclerView.LayoutManager rcycLayoutManager;
    private Menu                       menu;


    private IServiceBinder   service;
    private BluetoothAdapter blAdapt;

    private BroadcastReceiver brdcstRcvr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(getString(R.string.intent_bl_stopped))) {
                invalidateOptionsMenu();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(brdcstRcvr);

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

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        rcycDevices.setHasFixedSize(true);

        // use a linear layout manager
        rcycLayoutManager = new LinearLayoutManager(this);
        rcycDevices.setLayoutManager(rcycLayoutManager);

        // specify an adapter (see also next example)
        rcycAdaptDevices = new AdaptScanResults();
        rcycDevices.setAdapter(rcycAdaptDevices);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

        if (savedInstanceState == null) {

            blAdapt = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

            // Is Bluetooth supported on this device?
            if (blAdapt != null) {

                // Is Bluetooth turned on?
                if (!blAdapt.isEnabled()) {
                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                }
            } else {
                Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:

                if (resultCode == RESULT_OK) {
                    Runnable task = new Runnable() {

                        @Override
                        public void run() {
                            startService(new Intent(ActScan.this, ServiceScan.class));
                            bindService(new Intent(ActScan.this, ServiceScan.class), ActScan.this, BIND_AUTO_CREATE);
                        }
                    };
                    ScheduleUtil.scheduleWork(task, 3, TimeUnit.SECONDS);
                } else {
                    // User declined to enable Bluetooth, exit the app.
                    Toast.makeText(this, R.string.error_bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResuming() {
    }

    @Override
    protected void registerComponents() {
        if (blAdapt.isEnabled()) {
            Runnable task = new Runnable() {

                @Override
                public void run() {
                    startService(new Intent(ActScan.this, ServiceScan.class));
                }
            };
            bindService(new Intent(ActScan.this, ServiceScan.class), ActScan.this, BIND_AUTO_CREATE);
            ScheduleUtil.scheduleWork(task, 3, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder binder) {
        service = (IServiceBinder) binder;
        service.registerServiceListener(ActScan.this);
        invalidateOptionsMenu();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        service = null;
    }


    @Override
    protected void deregisterComponents() {
        // Deactivate updates to us so that we dont get callbacks no more.
        service.deregisterServiceListener(this);
        unbindService(this);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(brdcstRcvr);
        rcycAdaptDevices.getResults().clear();
        rcycAdaptDevices.notifyDataSetChanged();
    }

    @Override
    protected void onPausing() {

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.act_main, menu);
        this.menu = menu;
        if (service == null) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                rcycAdaptDevices.getResults().clear();
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
}
