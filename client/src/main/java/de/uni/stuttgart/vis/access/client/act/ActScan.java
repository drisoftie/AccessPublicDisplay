package de.uni.stuttgart.vis.access.client.act;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.RegActionMethod;
import com.drisoftie.action.async.android.AndroidAction;
import com.drisoftie.frags.comp.ManagedActivity;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.DialogCreator;
import de.stuttgart.uni.vis.access.common.util.ScheduleUtil;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.service.IServiceBinderClient;
import de.uni.stuttgart.vis.access.client.service.IServiceBlListener;
import de.uni.stuttgart.vis.access.client.service.ServiceScan;
import de.uni.stuttgart.vis.access.client.service.bl.IConnAdvertProvider;
import de.uni.stuttgart.vis.access.client.view.AdaptScanResults;

public class ActScan extends ManagedActivity
        implements NavigationView.OnNavigationItemSelectedListener, ServiceConnection, IServiceBlListener,
                   IConnAdvertProvider.IConnAdvertSubscriber {

    public  boolean                    ok;
    private RecyclerView               rcycDevices;
    private AdaptScanResults           rcycAdaptDevices;
    private RecyclerView.LayoutManager rcycLayoutManager;
    private Menu                       menu;
    private IServiceBinderClient       service;
    private BluetoothAdapter           blAdapt;
    private ActionListResults          actionListResults;
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
        rcycDevices.setHasFixedSize(true);
        rcycLayoutManager = new LinearLayoutManager(this);
        rcycDevices.setLayoutManager(rcycLayoutManager);
        rcycAdaptDevices = new AdaptScanResults();
        rcycDevices.setAdapter(rcycAdaptDevices);

        actionListResults = new ActionListResults(rcycDevices, IGenericAction.class, RegActionMethod.NONE.method());

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

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
                checkPermLocation();
            } else {
                ok = false;
                DialogCreator.createDialogAlert(this, R.string.txt_bl_requ_turn_on, R.string.txt_bl_descr_requ_turn_on, R.string.txt_ok,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        ok = true;
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
                                if (!ok) {
                                    finish();
                                }
                            }
                        });
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    checkPermLocation();
                } else {
                    ok = false;
                    DialogCreator.createDialogAlert(this, R.string.txt_bl_requ_turn_on, R.string.txt_bl_descr_requ_turn_on, R.string.txt_ok,
                                                    new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            ok = true;
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
                                    if (!ok) {
                                        finish();
                                    }
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
                startBlServiceConnDelayed();
                break;
            }
            case PackageManager.PERMISSION_DENIED: // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    ok = false;
                    DialogCreator.createDialogAlert(this, R.string.txt_bl_loc_req_turn_on, R.string.txt_bl_descr_loc_requ_turn_on,
                                                    R.string.txt_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ok = true;
                                    ActivityCompat.requestPermissions(ActScan.this,
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
                                    if (!ok) {
                                        finish();
                                    }
                                }
                            });
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                                      getResources().getInteger(R.integer.perm_access_coarse_location));
                }
                break;
        }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == getResources().getInteger(R.integer.perm_access_coarse_location)) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBlServiceConnDelayed();
            } else {
                ok = false;
                DialogCreator.createDialogAlert(this, R.string.txt_bl_loc_req_turn_on, R.string.txt_bl_descr_loc_turn_on_last,
                                                R.string.txt_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ok = true;
                                ActivityCompat.requestPermissions(ActScan.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
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
                                if (!ok) {
                                    finish();
                                }
                            }
                        });
            }
        }
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

    private boolean isServiceBlConnected() {
        return service != null && service.isConnected(this);
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

    @Override
    public void onScanResultReceived(ScanResult result) {
        rcycAdaptDevices.getResults().add(result);
        rcycAdaptDevices.notifyDataSetChanged();
    }

    @Override
    public void onScanResultsReceived(List<ScanResult> results) {
        rcycAdaptDevices.getResults().addAll(results);
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

    private class ActionListResults extends AndroidAction<View, Void, Void, Void, Void> {

        public ActionListResults(View view, Class<?> actionType, String regMethodName) {
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

        }
    }
}
