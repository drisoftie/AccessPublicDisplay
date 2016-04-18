package de.uni.stuttgart.vis.access.client.act;

import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
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


    }

    @Override
    protected void onResuming() {
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

                for (AbstractMap.SimpleEntry<Constants.AdvertiseConst, byte[]> advertEntry : ParserData.parseAdvert(
                        blData.getAdvertisement())) {
                    Button btn = (Button) LayoutInflater.from(ActMulti.this).inflate(R.layout.btn_gatt_category,
                                                                                     (ViewGroup) findViewById(R.id.lyt_multi_select),
                                                                                     false);
                    StringBuilder text = new StringBuilder();
                    switch (advertEntry.getKey()) {
                        case ADVERTISE_WEATHER: {
                            String g = getGattData(Constants.WEATHER.GATT_WEATHER_TODAY.getUuid(), blData);
                            if (g != null) {
                                text.append(App.inst().getString(R.string.weater_today_display));
                                text.append(g).append(System.lineSeparator());
                            }
                            btn.setText(text);
                            text.append(getString(R.string.press_weather));
                            buttons.add(btn);
                            btn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startActivity(new Intent(ActMulti.this, ActWeather.class));
                                }
                            });
                            break;
                        }
                        case ADVERTISE_WEATHER_DATA: {
                            String g = getGattData(Constants.WEATHER.GATT_WEATHER_TODAY.getUuid(), blData);
                            if (g != null) {
                                text.append(App.inst().getString(R.string.weater_today_display));
                                text.append(g).append(System.lineSeparator());
                            } else {
                                text.append(App.inst().getString(R.string.weather_curr_temp));
                                text.append(new DecimalFormat("#.#").format(ParserData.parseByteToFloat(advertEntry.getValue()))).append(
                                        System.lineSeparator());
                                text.append(App.inst().getString(R.string.Celcius));
                            }
                            text.append(getString(R.string.press_weather));
                            btn.setText(text);
                            buttons.add(btn);
                            btn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startActivity(new Intent(ActMulti.this, ActWeather.class));
                                }
                            });
                            break;
                        }
                        case ADVERTISE_TRANSP: {
                            String g = getGattData(Constants.PUBTRANSP.GATT_PUB_TRANSP_METRO.getUuid(), blData);
                            if (g != null) {
                                text.append("");
                                text.append(g).append(System.lineSeparator());
                            }
                            text.append(getString(R.string.press_pubtransp));
                            btn.setText(text);
                            buttons.add(btn);
                            break;
                        }
                        case ADVERTISE_SHOUT: {
                            String g = getGattData(Constants.SHOUT.GATT_SHOUT.getUuid(), blData);
                            if (g != null) {
                                text.append(getString(R.string.newest_shoutouts));
                                text.append(g + System.lineSeparator());
                            }
                            text.append(getString(R.string.press_shoutouts));
                            btn.setText(text);
                            buttons.add(btn);
                            break;
                        }
                        case ADVERTISE_NEWS: {
                            String g = getGattData(Constants.NEWS.GATT_NEWS.getUuid(), blData);
                            if (g != null) {
                                text.append(g);
                                text.append(g).append(System.lineSeparator());
                            }
                            text.append(getString(R.string.press_news));
                            btn.setText(text);
                            buttons.add(btn);
                            break;
                        }
                        case ADVERTISE_NEWS_DATA: {
                            String g = getGattData(Constants.NEWS.GATT_NEWS.getUuid(), blData);
                            if (g != null) {
                                text.append(g);
                                text.append(g).append(System.lineSeparator());
                            } else {
                                text.append(getString(R.string.newest_news));
                                text.append(String.valueOf(advertEntry.getValue())).append(System.lineSeparator());
                            }
                            text.append(getString(R.string.press_news));
                            btn.setText(text);
                            buttons.add(btn);
                            break;
                        }
                        case ADVERTISE_BOOKING: {
                            String g = getGattData(Constants.BOOKING.GATT_BOOKING_WRITE.getUuid(), blData);
                            if (g != null) {
                                text.append(g).append(System.lineSeparator());
                            }
                            text.append(getString(R.string.want_to_book_a_table_or_dish_inside_el_mero_mexicano_press_here)).append(
                                    System.lineSeparator()).append(getString(R.string.press_book));
                            btn.setText(text);
                            btn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startActivity(new Intent(ActMulti.this, ActBooking.class));
                                }
                            });
                            buttons.add(btn);
                            break;
                        }
                        case ADVERTISE_CHAT: {
                            String g = getGattData(Constants.CHAT.GATT_CHAT_WRITE.getUuid(), blData);
                            if (g != null) {
                                text.append(g).append(System.lineSeparator());
                            }
                            text.append(getString(R.string.press_chat_message)).append(System.lineSeparator()).append(
                                    getString(R.string.press_chat));
                            btn.setText(text);
                            btn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startActivity(new Intent(ActMulti.this, ActChat.class));
                                }
                            });
                            buttons.add(btn);
                            break;
                        }
                    }
                }
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
                    }
                }.init(data, buttons));
            }
        });
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
