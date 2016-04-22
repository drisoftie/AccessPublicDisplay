package de.uni.stuttgart.vis.access.client.act;

import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.android.AndroidAction;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.service.ServiceScan;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;

public class ActNews extends ActGattScan {

    private GattNews          gattListenNews;
    private IConnGattProvider gattProviderNews;
    private ActionGattSetup   actionGatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_news);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        actionGatt = new ActionGattSetup(null, IGenericAction.class, null);
    }

    @Override
    protected void onResuming() {
    }

    @Override
    protected void registerComponents() {
        Intent intent = new Intent(getString(R.string.intent_weather_get));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        bindService(new Intent(this, ServiceScan.class), this, BIND_AUTO_CREATE);
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        super.onServiceConnected(name, binder);
        if (gattListenNews == null) {
            gattProviderNews = service.subscribeGattConnection(Constants.NEWS.GATT_SERVICE_NEWS.getUuid(), gattListenNews = new GattNews());

            gattProviderNews.getGattCharacteristicRead(Constants.NEWS.GATT_SERVICE_NEWS.getUuid(), Constants.NEWS.GATT_NEWS.getUuid());
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        service = null;
    }

    @Override
    protected void onPausing() {
    }

    @Override
    void deregisterGattComponents() {
        gattProviderNews.deregisterConnGattSub(gattListenNews);
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

    private class ActionGattSetup extends AndroidAction<View, Void, Void, Void, Void> {

        public ActionGattSetup(View view, Class<?> actionType, String regMethodName) {
            super(view, actionType, regMethodName);
        }

        @Override
        public Object onActionPrepare(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            return null;
        }

        @Override
        public Void onActionDoWork(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            Object[] args = stripMethodArgs(methodArgs);
            UUID     uuid = (UUID) args[1];
            if (Constants.NEWS.GATT_NEWS.getUuid().equals(uuid)) {
                //                                gattProviderNews.getGattCharacteristicRead(Constants.NEWS.GATT_SERVICE_NEWS.getUuid(),
                //                                                                           Constants.NEWS.GATT_NEWS.getUuid());
            }
            return null;
        }

        @Override
        public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2,
                                      Object[] additionalTags) {
            Object[] args  = stripMethodArgs(methodArgs);
            byte[]   value = (byte[]) args[0];
            UUID     uuid  = (UUID) args[1];
            int      id    = 0;
            String   news  = null;
            if (Constants.NEWS.GATT_NEWS.getUuid().equals(uuid)) {
                news = new String(value);
                id = R.id.txt_info_news;
                //            } else if (Constants.NEWS.GATT_WEATHER_TOMORROW.getUuid().equals(uuid)) {
                //                weather = App.inst().getString(R.string.info_weather_tomorrow, ParserUtil.parseWeather(value));
                //                id = R.id.txt_weather_tomorrow;
                //            } else if (Constants.NEWS.GATT_WEATHER_DAT.getUuid().equals(uuid)) {
                //                weather = App.inst().getString(R.string.info_weather_dat, ParserUtil.parseWeather(value));
                //                id = R.id.txt_weather_dat;
            }
            if (id > 0) {
                TextView txt = (TextView) findViewById(id);
                txt.setText(news + " Bitte doppelt klicken zum Lesen");
                txt.setOnClickListener(new View.OnClickListener() {
                    public String query;

                    public View.OnClickListener init(String query) {
                        this.query = query;
                        return this;
                    }

                    @Override
                    public void onClick(View v) {
                        try {
                            query = URLEncoder.encode(query, "utf-8");
                            String url    = "http://www.google.com/search?q=" + query;
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(url));
                            startActivity(intent);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }.init(news));

            }
        }
    }

    private class GattNews implements IConnGattProvider.IConnGattSubscriber {

        @Override
        public void onGattReady(String macAddress) {
        }

        @Override
        public void onServicesReady(String macAddress) {
            gattProviderNews.registerConnGattSub(gattListenNews);
        }

        @Override
        public void onGattValueReceived(String macAddress, UUID uuid, byte[] value) {
            actionGatt.invokeSelf(value, uuid);
        }

        @Override
        public void onGattValueWriteReceived(String macAddress, UUID uuid, byte[] value) {

        }

        @Override
        public void onGattValueChanged(String macAddress, UUID uuid, byte[] value) {

        }
    }
}
