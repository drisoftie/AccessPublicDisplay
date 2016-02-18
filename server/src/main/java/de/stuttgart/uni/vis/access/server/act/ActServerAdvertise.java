package de.stuttgart.uni.vis.access.server.act;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.drisoftie.frags.comp.ManagedActivity;

import org.apache.commons.lang3.StringUtils;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.DialogCreator;
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

        // Activity first started, check if Bluetooth is on
        if (savedInstanceState == null) {
            blAdapt = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        }
    }

    @Override
    protected void onResuming() {
    }

    @Override
    protected void registerComponents() {
        if (blAdapt != null) {
            // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
            // fire an intent to display a dialog asking the user to grant permission to enable it.
            if (blAdapt.isEnabled()) {
                checkBlFunctAdvert();
            } else {
                DialogCreator.createDialogAlert(this, R.string.txt_bl_requ_turn_on, R.string.txt_bl_descr_requ_turn_on, R.string.txt_ok,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                                        startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                                                    }
                                                }, R.string.txt_close, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }, null);
            }
        } else {
            DialogCreator.createDialogAlert(this, R.string.txt_bl_needed_then_stop, R.string.txt_bl_descr_needed_then_stop,
                                            new DialogInterface.OnDismissListener() {
                                                @Override
                                                public void onDismiss(DialogInterface dialog) {
                                                    finish();
                                                }
                                            });
        }
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    checkBlFunctAdvert();
                } else {
                    DialogCreator.createDialogAlert(this, R.string.txt_bl_requ_turn_on, R.string.txt_bl_descr_requ_turn_on, R.string.txt_ok,
                                                    new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                                            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                                                        }
                                                    }, R.string.txt_close, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            }, new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    finish();
                                }
                            });
                }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void checkPermLocation() {
        int permScanCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        switch (permScanCheck) {
            case PackageManager.PERMISSION_GRANTED: {
                startBlServiceConn();
                break;
            }
            case PackageManager.PERMISSION_DENIED: // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

                    DialogCreator.createDialogAlert(this, R.string.txt_bl_loc_req_turn_on, R.string.txt_bl_descr_loc_requ_turn_on,
                                                    R.string.txt_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(ActServerAdvertise.this,
                                                                      new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                                                      getResources().getInteger(R.integer.perm_access_coarse_location));
                                }
                            }, R.string.txt_close, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            }, new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    finish();
                                }
                            });
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                                      getResources().getInteger(R.integer.perm_access_coarse_location));
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == getResources().getInteger(R.integer.perm_access_coarse_location)) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBlServiceConn();
            } else {
                DialogCreator.createDialogAlert(this, R.string.txt_bl_loc_req_turn_on, R.string.txt_bl_descr_loc_turn_on_last,
                                                R.string.txt_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(ActServerAdvertise.this,
                                                                  new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                                                  getResources().getInteger(R.integer.perm_access_coarse_location));
                            }
                        }, R.string.txt_close, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }, new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                finish();
                            }
                        });
            }
        }
    }

    private void startBlServiceConn() {
        startService(new Intent(this, ServiceAdvertise.class));
        bindService(new Intent(this, ServiceAdvertise.class), this, BIND_AUTO_CREATE);
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

    @Override
    public void onBlStarted() {
        actionMenuServer.invokeSelf(false);
    }

    @Override
    public void onBlUserShutdownCompleted() {
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
        removeServiceReferences();
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

    private void stopServiceBlConn() {
        if (isServiceBlConnected()) {
            unbindService(ActServerAdvertise.this);
        }
    }

    private void stopServiceBl() {
        if (isServiceBlConnected()) {
            unbindService(ActServerAdvertise.this);
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
                if (isServiceBlConnected()) {
                    service.onBlUserShutdown();
                    stopServiceBl();
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
