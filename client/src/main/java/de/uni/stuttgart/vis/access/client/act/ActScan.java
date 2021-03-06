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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.drisoftie.action.async.android.AndroidAction;

import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.act.ActBasePerms;
import de.stuttgart.uni.vis.access.common.brcst.BrcstBlAdaptChanged;
import de.stuttgart.uni.vis.access.common.util.ScheduleUtil;
import de.uni.stuttgart.vis.access.client.App;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.data.BlData;
import de.uni.stuttgart.vis.access.client.data.GattData;
import de.uni.stuttgart.vis.access.client.data.HolderBlData;
import de.uni.stuttgart.vis.access.client.data.IBlDataSubscriber;
import de.uni.stuttgart.vis.access.client.helper.SoundPlayer;
import de.uni.stuttgart.vis.access.client.helper.VibratorBuilder;
import de.uni.stuttgart.vis.access.client.service.IServiceBinderClient;
import de.uni.stuttgart.vis.access.client.service.IServiceBlListener;
import de.uni.stuttgart.vis.access.client.service.ServiceScan;
import de.uni.stuttgart.vis.access.client.service.bl.IConnAdvertProvider;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;
import de.uni.stuttgart.vis.access.client.view.AdaptScans;

public class ActScan extends ActBasePerms implements NavigationView.OnNavigationItemSelectedListener, ServiceConnection, IServiceBlListener,
                                                     IConnAdvertProvider.IConnAdvertSubscriber {

    private static final String TAG = "ActScan";
    private RecyclerView               rcycDevices;
    private AdaptScans                 rcycAdaptDevices;
    private RecyclerView.LayoutManager rcycLayoutManager;
    private Menu                       menu;
    private IServiceBinderClient       service;
    private BroadcastReceiver          brcstRcvrBlAdapt;
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
    private GattWeather       gattListenWeather;
    private IConnGattProvider gattProviderWeather;
    private GattShout         gattListenShout;
    private IConnGattProvider gattProviderShout;
    private GattNews          gattListenNews;
    private IConnGattProvider gattProviderNews;
    private GattBooking       gattListenBooking;
    private IConnGattProvider gattProviderBooking;

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
        App.holder().init();
        App.holder().subscribeBlData(new ActionBlDataSub(null, IBlDataSubscriber.class, null).getHandlerImpl(IBlDataSubscriber.class));
        App.holder().access(App.holder().new HolderAccess() {
            @Override
            public void onRun() {
                ActScan.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Iterator<BlData> it = getData();
                        while (it.hasNext()) {
                            BlData b = it.next();
                            if (b.isActive()) {
                                rcycAdaptDevices.getBlData().add(b);
                            }
                        }
                        rcycAdaptDevices.notifyItemInserted(0);
                        checkVisibleScans();
                    }
                });
            }
        });


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
        deregisterGattComponents();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(brdcstRcvr);

        rcycAdaptDevices.getBlData().clear();
        rcycAdaptDevices.notifyDataSetChanged();
    }

    private void deregisterGattComponents() {
        if (gattProviderBooking != null) {
            gattProviderBooking.deregisterConnGattSub(gattListenBooking);
        }
        if (gattProviderNews != null) {
            gattProviderNews.deregisterConnGattSub(gattListenNews);
        }
        if (gattProviderShout != null) {
            gattProviderShout.deregisterConnGattSub(gattListenShout);
        }
        if (gattProviderWeather != null) {
            gattProviderWeather.deregisterConnGattSub(gattListenWeather);
        }
    }

    @Override
    protected void onPausing() {
        unregisterReceiver(brcstRcvrBlAdapt);
    }

    @Override
    public void onConnStopped() {
        invalidateOptionsMenu();
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
        Log.d(TAG, "onScanResultReceived: " + result.getDevice().getAddress());
        findViewById(R.id.txt_headline_displays).setVisibility(View.VISIBLE);
        for (BlData b : rcycAdaptDevices.getBlData()) {
            if (b.isActive() && b.getAddress().equals(result.getDevice().getAddress())) {
                return;
            }
        }
        BlData data = new BlData();
        data.setActive(true);
        data.setAddress(result.getDevice().getAddress());
        data.setRssi(result.getRssi());
        //noinspection ConstantConditions
        data.setAdvertisement(result.getScanRecord().getServiceData(Constants.UUID_ADVERT_SERVICE_MULTI));
        App.holder().access(App.holder().new HolderAccess() {
            private BlData data;
            private ScanResult result;

            public HolderBlData.HolderAccess init(BlData data, ScanResult result) {
                this.data = data;
                this.result = result;
                return this;
            }

            @Override
            public void onRun() {
                Iterator<BlData> i     = getData();
                boolean          found = false;
                while (i.hasNext()) {
                    BlData b = i.next();
                    if (b.getAddress().equals(data.getAddress())) {
                        if (!b.isActive()) {
                            Intent showIntent = new Intent(getString(R.string.intent_advert_gatt_connect));
                            showIntent.putExtra(getString(R.string.bndl_bl_scan_result), result);
                            LocalBroadcastManager.getInstance(ActScan.this).sendBroadcast(showIntent);
                        }
                        b.setActive(true);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Log.d(TAG, "Display not found and will be added: " + data.getAddress());
                    SoundPlayer.playExampleBeep(440, 1, 0);
                    VibratorBuilder.vibrate(VibratorBuilder.SHORT_SHORT);
                    addData(data);
                    Intent showIntent = new Intent(getString(R.string.intent_advert_gatt_connect));
                    showIntent.putExtra(getString(R.string.bndl_bl_scan_result), result);
                    LocalBroadcastManager.getInstance(ActScan.this).sendBroadcast(showIntent);
                }

            }
        }.init(data, result));

        if (gattListenWeather == null) {
            gattProviderWeather = service.subscribeGattConnection(Constants.WEATHER.GATT_SERVICE_WEATHER.getUuid(),
                                                                  gattListenWeather = new GattWeather());
        }
        if (gattListenShout == null) {
            gattProviderShout = service.subscribeGattConnection(Constants.SHOUT.GATT_SERVICE_SHOUT.getUuid(),
                                                                gattListenShout = new GattShout());
        }
        if (gattListenNews == null) {
            gattProviderNews = service.subscribeGattConnection(Constants.NEWS.GATT_SERVICE_NEWS.getUuid(), gattListenNews = new GattNews());
        }
        if (gattListenBooking == null) {
            gattProviderBooking = service.subscribeGattConnection(Constants.BOOKING.GATT_SERVICE_BOOKING.getUuid(),
                                                                  gattListenBooking = new GattBooking());
        }
    }

    @Override
    public void onScanResultsReceived(List<ScanResult> results) {
        getClass();
    }

    @Override
    public void onRefreshedScanReceived(ScanResult result) {
        App.holder().access(App.holder().new HolderAccess() {
            private ScanResult result;

            public HolderBlData.HolderAccess init(ScanResult result) {
                this.result = result;
                return this;
            }

            @Override
            public void onRun() {
                boolean          found = false;
                Iterator<BlData> i     = getData();
                while (i.hasNext()) {
                    BlData b = i.next();
                    if (b.getAddress().equals(result.getDevice().getAddress())) {
                        if (!b.isActive()) {
                            Intent showIntent = new Intent(getString(R.string.intent_advert_gatt_connect));
                            showIntent.putExtra(getString(R.string.bndl_bl_scan_result), result);
                            LocalBroadcastManager.getInstance(ActScan.this).sendBroadcast(showIntent);
                        }
                        b.setActive(true);
                        b.setRssi(result.getRssi());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    BlData data = new BlData();
                    data.setActive(true);
                    data.setAddress(result.getDevice().getAddress());
                    data.setRssi(result.getRssi());
                    //noinspection ConstantConditions
                    data.setAdvertisement(result.getScanRecord().getServiceData(Constants.UUID_ADVERT_SERVICE_MULTI));
                    Log.d(TAG, "Already found Display not found in Data and will be added: " + data.getAddress());
                    SoundPlayer.playExampleBeep(440, 1, 0);
                    VibratorBuilder.vibrate(VibratorBuilder.SHORT_SHORT);
                    addData(data);
                    Intent showIntent = new Intent(getString(R.string.intent_advert_gatt_connect));
                    showIntent.putExtra(getString(R.string.bndl_bl_scan_result), result);
                    LocalBroadcastManager.getInstance(ActScan.this).sendBroadcast(showIntent);
                }
            }
        }.init(result));
    }

    @Override
    public void onRefreshedScansReceived(List<ScanResult> results) {

    }

    @Override
    public void onScanLost(ScanResult lostResult) {
        App.holder().access(App.holder().new HolderAccess() {
            private ScanResult result;

            public HolderBlData.HolderAccess init(ScanResult result) {
                this.result = result;
                return this;
            }

            @Override
            public void onRun() {
                Iterator<BlData> i = getData();
                while (i.hasNext()) {
                    BlData b = i.next();
                    if (b.getAddress().equals(result.getDevice().getAddress())) {
                        b.setActive(false);
                    }
                    ActScan.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < rcycAdaptDevices.getBlData().size(); i++) {
                                BlData b = rcycAdaptDevices.getBlData().get(i);
                                if (!b.isActive() && b.getAddress().equals(result.getDevice().getAddress())) {
                                    rcycAdaptDevices.getBlData().remove(b);
                                    rcycAdaptDevices.notifyDataSetChanged();
                                }
                            }
                        }
                    });
                }
            }
        }.init(lostResult));
    }

    @Override
    public void onScanFailed(int errorCode) {

    }

    private void updateHolderData(String macAddress, UUID uuid, byte[] value) {
        App.holder().access(App.holder().new HolderAccess() {
            public String macAddress;
            public UUID uuid;
            public byte[] value;

            public HolderBlData.HolderAccess init(String macAddress, UUID uuid, byte[] value) {
                this.macAddress = macAddress;
                this.uuid = uuid;
                this.value = value;
                return this;
            }

            @Override
            public void onRun() {
                Iterator<BlData> i = getData();
                while (i.hasNext()) {
                    BlData d = i.next();
                    if (d.getAddress().equals(macAddress)) {
                        boolean found = false;
                        for (GattData g : d.getGattData()) {
                            if (g.getUuid().equals(uuid)) {
                                g.setData(value);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            d.getGattData().add(new GattData(uuid, value));
                        }
                        ActScan.this.runOnUiThread(new Runnable() {
                            public BlData d;

                            public Runnable init(BlData d) {
                                this.d = d;
                                return this;
                            }

                            @Override
                            public void run() {
                                AdaptScans.ViewHolder viewHolder = (AdaptScans.ViewHolder) rcycDevices.findViewHolderForAdapterPosition(
                                        rcycAdaptDevices.getBlData().indexOf(d));
                                if (viewHolder != null) {
                                    viewHolder.parseData(d);
                                }
                            }
                        }.init(d));
                        break;
                    }
                }
            }
        }.init(macAddress, uuid, value));
    }

    private void checkVisibleScans() {
        if (rcycAdaptDevices.getItemCount() > 0) {
            rcycDevices.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.txt_headline_displays)).setText(R.string.txt_range_list);
        }
    }

    private class GattWeather extends GattSub {

        @Override
        public void onServicesReady(String macAddress) {
            gattProviderNews.registerConnGattSub(gattListenWeather);
            gattProviderNews.getGattCharacteristicRead(Constants.WEATHER.GATT_SERVICE_WEATHER.getUuid(),
                                                       Constants.WEATHER.GATT_WEATHER_TODAY.getUuid());
        }

        @Override
        public void onGattValueReceived(String macAddress, UUID uuid, byte[] value) {
            if (Constants.WEATHER.GATT_WEATHER_TODAY.getUuid().equals(uuid)) {
                updateHolderData(macAddress, uuid, value);
            }
        }
    }

    private class GattShout extends GattSub {

        @Override
        public void onGattValueChanged(String macAddress, UUID uuid, byte[] value) {
            if (Constants.SHOUT.GATT_SHOUT.getUuid().equals(uuid)) {
                updateHolderData(macAddress, uuid, value);
            }
        }
    }

    private class GattNews extends GattSub {

        @Override
        public void onServicesReady(String macAddress) {
            gattProviderNews.registerConnGattSub(gattListenNews);
            gattProviderNews.getGattCharacteristicRead(Constants.NEWS.GATT_SERVICE_NEWS.getUuid(), Constants.NEWS.GATT_NEWS.getUuid());
        }

        @Override
        public void onGattValueReceived(String macAddress, UUID uuid, byte[] value) {
            if (Constants.NEWS.GATT_NEWS.getUuid().equals(uuid)) {
                updateHolderData(macAddress, uuid, value);
            }
        }
    }

    private class GattBooking extends GattSub {

        @Override
        public void onServicesReady(String macAddress) {
            gattProviderBooking.registerConnGattSub(gattListenBooking);
            //            gattProviderBooking.writeGattCharacteristic(Constants.BOOKING.GATT_SERVICE_BOOKING.getUuid(),
            //                                                        Constants.BOOKING.GATT_BOOKING_WRITE.getUuid(),
            //                                                        ConstantsBooking.StateBooking.START.getState().getBytes());
        }

        @Override
        public void onGattValueWriteReceived(String macAddress, UUID uuid, byte[] value) {
            if (Constants.BOOKING.GATT_BOOKING_WRITE.getUuid().equals(uuid)) {
                updateHolderData(macAddress, uuid, value);
            }
        }
    }

    private class ActionBlDataSub extends AndroidAction<View, Void, Void, Void, Void> {

        public ActionBlDataSub(View view, Class<?> actionType, String regMethodName) {
            super(view, actionType, regMethodName);
        }

        @Override
        public Object onActionPrepare(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            return null;
        }

        @Override
        public Void onActionDoWork(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            return null;
        }

        @Override
        public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2,
                                      Object[] additionalTags) {
            if (StringUtils.equals(methodName, "onBlDataAdded")) {
                rcycAdaptDevices.getBlData().add((BlData) methodArgs[0]);
                rcycAdaptDevices.notifyItemInserted(0);
                checkVisibleScans();
            }
        }
    }
}
