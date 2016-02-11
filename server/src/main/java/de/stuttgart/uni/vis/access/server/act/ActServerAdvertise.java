package de.stuttgart.uni.vis.access.server.act;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.android.AndroidAction;
import com.drisoftie.frags.comp.ManagedActivity;

import org.apache.commons.lang3.StringUtils;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.server.BuildConfig;
import de.stuttgart.uni.vis.access.server.R;
import de.stuttgart.uni.vis.access.server.service.IServiceBinder;
import de.stuttgart.uni.vis.access.server.service.IServiceBlListener;
import de.stuttgart.uni.vis.access.server.service.ServiceAdvertise;

public class ActServerAdvertise extends ManagedActivity
        implements ServiceConnection, IServiceBlListener, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = ActServerAdvertise.class.getSimpleName();

    private BluetoothAdapter blAdapt;
    private Menu             menu;
    private IServiceBinder   service;

    private AndroidAction<View, Void, Void, Void, Void> actionMenuStart = new AndroidAction<View, Void, Void, Void, Void>(new View[0],
                                                                                                                          IGenericAction.class,
                                                                                                                          "") {
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
            createMenuStart();
        }
    };

    private AndroidAction<View, Void, Void, Void, Void> actionMenuEnd = new AndroidAction<View, Void, Void, Void, Void>(new View[0],
                                                                                                                        IGenericAction.class,
                                                                                                                        "") {
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
            createMenuStop();
        }
    };

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver msgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StringUtils.equals(intent.getAction(), getString(R.string.intent_bl_stopped))) {
                removeServiceReferences();
                actionMenuStart.invokeSelf();
            } else if (StringUtils.equals(intent.getAction(), getString(R.string.intent_action_bl_user_stopped))) {
                stopServiceBl();
                removeServiceReferences();
                actionMenuStart.invokeSelf();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);

        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.intent_bl_stopped));
        filter.addAction(getString(R.string.intent_action_bl_user_stopped));
        LocalBroadcastManager.getInstance(this).registerReceiver(msgReceiver, filter);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open,
                                                                 R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Activity first started, check if Bluetooth is on
        if (savedInstanceState == null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Instance state is null, starting bluetooth stack");
            }
            blAdapt = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

            // Is Bluetooth supported on this device?
            if (blAdapt != null) {
                // Is Bluetooth turned on?
                if (blAdapt.isEnabled()) {
                    // Are Bluetooth Advertisements supported on this device?
                    if (!blAdapt.isMultipleAdvertisementSupported()) {
                        // Bluetooth Advertisements are not supported.
                        showErrorText(R.string.bt_ads_not_supported);
                    }
                } else {
                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                }
            } else {
                // Bluetooth is not supported.
                showErrorText(R.string.bt_not_supported);
            }
        }
    }

    @Override
    protected void onResuming() {
    }

    @Override
    protected void registerComponents() {
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!blAdapt.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        } else {
            startBlServiceConn();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:

                if (resultCode == RESULT_OK) {

                    // Bluetooth is now Enabled, are Bluetooth Advertisements supported on
                    // this device?
                    if (blAdapt.isMultipleAdvertisementSupported()) {

                        // Everything is supported and enabled, load the fragments.
                        startBlServiceConn();

                    } else {

                        // Bluetooth Advertisements are not supported.
                        showErrorText(R.string.bt_ads_not_supported);
                    }
                } else {

                    // User declined to enable Bluetooth, exit the app.
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void deregisterComponents() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(msgReceiver);
        if (isServiceBlConnected()) {
            stopServiceBlConn();
            removeServiceReferences();
        }
    }

    @Override
    protected void onPausing() {
    }

    private void startBlServiceConn() {
        startService(new Intent(this, ServiceAdvertise.class));
        bindService(new Intent(this, ServiceAdvertise.class), this, BIND_AUTO_CREATE);
    }

    @Override
    public void onBlStarted() {
        actionMenuEnd.invokeSelf();
//        createMenuStart();
    }

    private void createMenuStart() {
        if (menu != null) {
            menu.findItem(R.id.menu_refresh).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_advertise).setVisible(true);
        }
    }

    private void createMenuStop() {
        if (menu != null) {
            menu.findItem(R.id.menu_refresh).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_advertise).setVisible(false);
        }
    }

    @Override
    public void onConnStopped() {
        stopServiceBl();
        removeServiceReferences();
        actionMenuStart.invokeSelf();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = (IServiceBinder) binder;
        service.registerServiceListener(this);
        actionMenuEnd.invokeSelf();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
        actionMenuStart.invokeSelf();
    }

    private void showErrorText(int messageId) {

        //        TextView view = (TextView) findViewById(R.id.error_textview);
        //        view.setText(getString(messageId));
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

    public void removeServiceReferences() {
        if (service != null) {
            service.deregisterServiceListener(ActServerAdvertise.this);
            service = null;
        }
    }

    private void stopServiceBlConn() {
        if (isServiceBlConnected()) {
            unbindService(ActServerAdvertise.this);
        }
    }

    private void stopServiceBl() {
        if (isServiceBlConnected()) {
            unbindService(ActServerAdvertise.this);
            stopService(new Intent(ActServerAdvertise.this, ServiceAdvertise.class));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.act_main, menu);
        this.menu = menu;
        menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        if (ServiceAdvertise.running) {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_advertise).setVisible(false);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_advertise).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_advertise:
                startBlServiceConn();
                menu.findItem(R.id.menu_advertise).setVisible(false);
                menu.findItem(R.id.menu_stop).setVisible(true);
                menu.findItem(R.id.menu_refresh).setVisible(true);
                menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
                break;
            case R.id.menu_stop:
                stopServiceBl();
                menu.findItem(R.id.menu_advertise).setVisible(true);
                menu.findItem(R.id.menu_stop).setVisible(false);
                menu.findItem(R.id.menu_refresh).setVisible(true);
                menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
                break;
        }
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
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
