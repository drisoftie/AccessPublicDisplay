package de.uni.stuttgart.vis.access.client.act;

import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.act.ActBasePerms;
import de.stuttgart.uni.vis.access.common.util.ParserData;
import de.stuttgart.uni.vis.access.common.util.ScheduleUtil;
import de.uni.stuttgart.vis.access.client.App;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.data.BlData;
import de.uni.stuttgart.vis.access.client.data.GattData;
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

        //        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //        fab.setOnClickListener(new View.OnClickListener() {
        //            @Override
        //            public void onClick(View view) {
        //                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        //            }
        //        });
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        App.holder().access(App.holder().new HolderAccess() {
            @Override
            public void onRun() {
                List<AbstractMap.SimpleEntry<Object, List<GattData>>> data   = new ArrayList<>();
                Iterator<BlData>                                      iter   = getData();
                BlData                                                blData = null;
                while (iter.hasNext()) {
                    BlData d = iter.next();
                    if (getIntent().hasExtra(getString(R.string.bndl_bl_address))) {
                        if (d.getAddress().equals(getIntent().getStringExtra(getString(R.string.bndl_bl_address)))) {
                            blData = d;
                            break;
                        }
                    } else {

                    }
                }
                List<TextView> buttons = new ArrayList<>();
                for (int i = 0; i < blData.getAdvertisement().length; i++) {
                    byte          b    = blData.getAdvertisement()[i];
                    StringBuilder text = new StringBuilder();
                    if (b == Constants.AdvertiseConst.ADVERTISE_WEATHER.getFlag()) {
                        String g = getGattData(Constants.WEATHER.GATT_WEATHER_TODAY.getUuid(), blData);
                        if (g != null) {
                            text.append("Todays weather is: ");
                            text.append(g).append(System.lineSeparator());
                        }
                        text.append("Press here for more weather information.");
                        Button btn = (Button) LayoutInflater.from(ActMulti.this).inflate(R.layout.btn_gatt_category,
                                                                                         (ViewGroup) findViewById(R.id.lyt_multi_select),
                                                                                         false);
                        btn.setText(text);
                        buttons.add(btn);
                    } else if (b == Constants.AdvertiseConst.ADVERTISE_WEATHER_DATA.getFlag()) {
                        String g = getGattData(Constants.WEATHER.GATT_WEATHER_TODAY.getUuid(), blData);
                        if (g != null) {
                            text.append("Todays weather is: ");
                            text.append(g + System.lineSeparator());
                        } else {
                            text.append("Current temperature is: ");
                            text.append(new DecimalFormat("#.#").format(ParserData.parseByteToFloat(
                                    Arrays.copyOfRange(blData.getAdvertisement(), i + 1, i + 5))) + System.lineSeparator());
                        }
                        text.append("Press here for more weather information.");
                        Button btn = (Button) LayoutInflater.from(ActMulti.this).inflate(R.layout.btn_gatt_category,
                                                                                         (ViewGroup) findViewById(R.id.lyt_multi_select),
                                                                                         false);
                        btn.setText(text);
                        buttons.add(btn);
                    } else if (b == Constants.AdvertiseConst.ADVERTISE_TRANSP.getFlag()) {
                        String g = getGattData(Constants.PUBTRANSP.GATT_PUB_TRANSP_METRO.getUuid(), blData);
                        if (g != null) {
                            text.append("");
                            text.append(g + System.lineSeparator());
                        }
                        text.append("Press here for more public transport information.");
                        Button btn = (Button) LayoutInflater.from(ActMulti.this).inflate(R.layout.btn_gatt_category,
                                                                                         (ViewGroup) findViewById(R.id.lyt_multi_select),
                                                                                         false);
                        btn.setText(text);
                        buttons.add(btn);
                    } else if (b == Constants.AdvertiseConst.ADVERTISE_SHOUT.getFlag()) {
                        String g = getGattData(Constants.SHOUT.GATT_SHOUT.getUuid(), blData);
                        if (g != null) {
                            text.append("Newest shout: ");
                            text.append(g + System.lineSeparator());
                        }
                        text.append("Press here for more shouts.");
                        Button btn = (Button) LayoutInflater.from(ActMulti.this).inflate(R.layout.btn_gatt_category,
                                                                                         (ViewGroup) findViewById(R.id.lyt_multi_select),
                                                                                         false);
                        btn.setText(text);
                        buttons.add(btn);
                    } else if (b == Constants.AdvertiseConst.ADVERTISE_NEWS.getFlag()) {
                        String g = getGattData(Constants.NEWS.GATT_NEWS.getUuid(), blData);
                        if (g != null) {
                            text.append(g);
                            text.append(g + System.lineSeparator());
                        }
                        text.append("Press here for more news information.");
                        Button btn = (Button) LayoutInflater.from(ActMulti.this).inflate(R.layout.btn_gatt_category,
                                                                                         (ViewGroup) findViewById(R.id.lyt_multi_select),
                                                                                         false);
                        btn.setText(text);
                        buttons.add(btn);
                    } else if (b == Constants.AdvertiseConst.ADVERTISE_NEWS_DATA.getFlag()) {
                        String g = getGattData(Constants.NEWS.GATT_NEWS.getUuid(), blData);
                        if (g != null) {
                            text.append(g);
                            text.append(g).append(System.lineSeparator());
                        } else {
                            text.append("Current amount of news: ");
                            text.append(String.valueOf(blData.getAdvertisement()[i + 1])).append(System.lineSeparator());
                        }
                        text.append("Press here for more news information.");
                        Button btn = (Button) LayoutInflater.from(ActMulti.this).inflate(R.layout.btn_gatt_category,
                                                                                         (ViewGroup) findViewById(R.id.lyt_multi_select),
                                                                                         false);
                        btn.setText(text);
                        buttons.add(btn);
                    } else if (b == Constants.AdvertiseConst.ADVERTISE_BOOKING.getFlag()) {
                        String g = getGattData(Constants.BOOKING.GATT_BOOKING_WRITE.getUuid(), blData);
                        if (g != null) {
                            text.append(g).append(System.lineSeparator());
                        }
                        text.append("Press here for more booking information.");
                        Button btn = (Button) LayoutInflater.from(ActMulti.this).inflate(R.layout.btn_gatt_category,
                                                                                         (ViewGroup) findViewById(R.id.lyt_multi_select),
                                                                                         false);
                        btn.setText(text);
                        buttons.add(btn);
                    }
                }
                //                data.addAll(
                //                        getDataStructured(d, Constants.BOOKING.UUIDS, Constants.CHAT.UUIDS, Constants.NEWS.UUIDS, Constants.PUBTRANSP.UUIDS,
                //                                          Constants.SHOUT.UUIDS, Constants.WEATHER.UUIDS));
                ActMulti.this.runOnUiThread(new Runnable() {
                    public List<TextView> buttons;
                    public List<AbstractMap.SimpleEntry<Object, List<GattData>>> data;

                    public Runnable init(List<AbstractMap.SimpleEntry<Object, List<GattData>>> data, List<TextView> buttons) {
                        this.data = data;
                        this.buttons = buttons;
                        return this;
                    }

                    @Override
                    public void run() {
                        LinearLayout lytMulti = (LinearLayout) findViewById(R.id.lyt_multi_select);

                        for (TextView b : buttons) {
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                                                   LinearLayout.LayoutParams.WRAP_CONTENT);
                            layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                            layoutParams.setMargins(0, 20, 0, 20);
                            lytMulti.addView(b, layoutParams);
                        }

                        //                        for (AbstractMap.SimpleEntry<Object, List<GattData>> entry : data) {
                        //                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        //                                                                                                   LinearLayout.LayoutParams.WRAP_CONTENT);
                        //                            TextView btn = new TextView(ActMulti.this);
                        //                            layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                        //                            layoutParams.setMargins(0, 20, 0, 20);
                        //                            boolean start = true;
                        //                            for (GattData data : entry.getValue()) {
                        //                                if (start) {
                        //                                    start = false;
                        //                                    btn.setText("");
                        //                                } else {
                        //                                    btn.append("\n");
                        //                                }
                        //                                btn.append(new String(data.getData()));
                        //                            }
                        //                            lytMulti.addView(btn, layoutParams);
                        //                        }
                    }
                }.init(data, buttons));
            }
        });
    }

    private List<AbstractMap.SimpleEntry<Object, List<GattData>>> getDataStructured(BlData d, ParcelUuid[]... uuids) {
        List<AbstractMap.SimpleEntry<Object, List<GattData>>> data = new ArrayList<>();
        for (ParcelUuid[] uuidsPackage : uuids) {
            for (GattData g : d.getGattData()) {
                if (match(uuidsPackage, g.getUuid())) {
                    AbstractMap.SimpleEntry<Object, List<GattData>> dataEntry = findInData(data, uuidsPackage);
                    if (dataEntry == null) {
                        dataEntry = new AbstractMap.SimpleEntry<Object, List<GattData>>(uuidsPackage, new ArrayList<GattData>());
                    }
                    dataEntry.getValue().add(g);
                    data.add(dataEntry);
                }
            }
        }
        return data;
    }

    private boolean match(ParcelUuid[] uuids, UUID toFind) {
        for (ParcelUuid uuid : uuids) {
            if (uuid.getUuid().equals(toFind)) {
                return true;
            }
        }
        return false;
    }

    private AbstractMap.SimpleEntry<Object, List<GattData>> findInData(List<AbstractMap.SimpleEntry<Object, List<GattData>>> data,
                                                                       Object key) {
        for (AbstractMap.SimpleEntry<Object, List<GattData>> entry : data) {
            if (entry.equals(key)) {
                return entry;
            }
        }
        return null;
    }


    @Override
    protected void onResuming() {

    }

    private String getGattData(UUID uuid, BlData data) {
        String value = null;
        for (GattData g : data.getGattData()) {
            if (g.getUuid().equals(uuid)) {
                value = new String(g.getData());
                break;
            }
        }
        return value;
    }

    @Override
    protected void deregisterComponents() {
        if (isServiceBlConnected()) {
            // Deactivate updates to us so that we dont get callbacks no more.
            service.deregisterServiceListener(this);
            unbindService(this);
        }
    }

    @Override
    protected void onPausing() {
    }

    private boolean isServiceBlConnected() {
        return service != null && service.isConnected(this);
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
