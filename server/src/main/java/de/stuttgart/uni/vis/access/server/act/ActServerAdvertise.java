package de.stuttgart.uni.vis.access.server.act;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.RegActionMethod;
import com.drisoftie.action.async.android.AndroidAction;

import org.apache.commons.lang3.StringUtils;

import de.stuttgart.uni.vis.access.common.DialogCreator;
import de.stuttgart.uni.vis.access.common.act.ActBasePerms;
import de.stuttgart.uni.vis.access.server.R;
import de.stuttgart.uni.vis.access.server.service.IServiceBinder;
import de.stuttgart.uni.vis.access.server.service.IServiceBlListener;
import de.stuttgart.uni.vis.access.server.service.ServiceBlConn;

public class ActServerAdvertise extends ActBasePerms
        implements ServiceConnection, IServiceBlListener, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = ActServerAdvertise.class.getSimpleName();

    public  boolean        ok;
    private Menu           menu;
    private IServiceBinder service;

    private ActionMenuServer actionMenuServer = new ActionMenuServer(null, IGenericAction.class, RegActionMethod.NONE.method());

    private BroadcastReceiver msgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StringUtils.equals(intent.getAction(), getString(R.string.intent_bl_stopped))) {
                removeServiceReferences();
                actionMenuServer.invokeSelf(true);
            } else if (StringUtils.equals(intent.getAction(), getString(R.string.intent_action_bl_user_stopped))) {
                stopServiceBl();
                removeServiceReferences();
                actionMenuServer.invokeSelf(true);
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
    }

    @Override
    protected void onResuming() {
    }

    @Override
    protected void onBlAdaptStarted() {
        checkBlFunctAdvert();
    }

    private void checkBlFunctAdvert() {
        // Bluetooth is now Enabled, are Bluetooth Advertisements supported on
        // this device?
        if (blAdapt.isMultipleAdvertisementSupported()) {
            checkPermLocation();
        } else {
            DialogCreator.createDialogAlert(this, R.string.txt_bl_adv_needed, R.string.txt_bl_descr_adv_needed, R.string.txt_ok, null,
                                            new DialogInterface.OnDismissListener() {
                                                @Override
                                                public void onDismiss(DialogInterface dialog) {
                                                    finish();
                                                }
                                            });
        }
    }

    @Override
    protected void startBlServiceConn() {
        startService(new Intent(this, ServiceBlConn.class));
        bindService(new Intent(this, ServiceBlConn.class), this, BIND_AUTO_CREATE);
    }

    @Override
    protected void deregisterComponents() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(msgReceiver);
        if (isServiceBlConnected()) {
            stopServiceBl();
        }
    }

    @Override
    protected void onPausing() {
    }

    @Override
    public void onBlStarted() {
        actionMenuServer.invokeSelf(false);
    }

    @Override
    public void onBlUserShutdownCompleted() {
        stopServiceBl();
        actionMenuServer.invokeSelf(true);
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
        actionMenuServer.invokeSelf(true);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = (IServiceBinder) binder;
        service.registerServiceListener(this);
        actionMenuServer.invokeSelf(false);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
        actionMenuServer.invokeSelf(true);
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

    private void stopServiceBl() {
        if (isServiceBlConnected()) {
            unbindService(ActServerAdvertise.this);
        }
        removeServiceReferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.act_main, menu);
        this.menu = menu;
        menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        if (ServiceBlConn.running) {
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
                if (isServiceBlConnected()) {
                    service.onBlUserShutdown();
                }
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

    private class ActionMenuServer extends AndroidAction<View, Void, Void, Void, Void> {

        public ActionMenuServer(View view, Class<?> actionType, String regMethodName) {
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
            Object[] args = stripMethodArgs(methodArgs);
            if ((Boolean) args[0]) {
                createMenuStart();
            } else {
                createMenuStop();
            }
        }
    }
}
