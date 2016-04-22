package de.uni.stuttgart.vis.access.client.act;

import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.android.AndroidAction;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.App;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.helper.ParserUtil;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;

public class ActPubTransp extends ActGattScan {

    private GattPubTransp     gattListenTransp;
    private IConnGattProvider gattProviderTransp;
    private ActionGattSetup   actionGatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_pub_transport);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        actionGatt = new ActionGattSetup(null, IGenericAction.class, null);
    }

    @Override
    protected void onResuming() {
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        super.onServiceConnected(name, binder);
        if (gattListenTransp == null) {
            gattProviderTransp = service.subscribeGattConnection(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid(),
                                                                 gattListenTransp = new GattPubTransp());
            gattProviderTransp.getGattCharacteristicRead(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid(),
                                                         Constants.PUBTRANSP.GATT_PUB_TRANSP_BUS.getUuid());
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        service = null;
    }

    @Override
    void deregisterGattComponents() {
        gattProviderTransp.deregisterConnGattSub(gattListenTransp);
    }

    @Override
    protected void onPausing() {

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
            if (Constants.PUBTRANSP.GATT_PUB_TRANSP_BUS.getUuid().equals(uuid)) {
                gattProviderTransp.getGattCharacteristicRead(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid(),
                                                             Constants.PUBTRANSP.GATT_PUB_TRANSP_TRAIN.getUuid());
            } else if (Constants.PUBTRANSP.GATT_PUB_TRANSP_TRAIN.getUuid().equals(uuid)) {
                gattProviderTransp.getGattCharacteristicRead(Constants.PUBTRANSP.GATT_PUB_TRANSP_BUS.getUuid(),
                                                             Constants.PUBTRANSP.GATT_PUB_TRANSP_METRO.getUuid());
            }
            return null;
        }

        @Override
        public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2,
                                      Object[] additionalTags) {
            Object[] args    = stripMethodArgs(methodArgs);
            byte[]   value   = (byte[]) args[0];
            UUID     uuid    = (UUID) args[1];
            int      id      = 0;
            String   weather = null;
            if (Constants.PUBTRANSP.GATT_PUB_TRANSP_BUS.getUuid().equals(uuid)) {
                weather = App.inst().getString(R.string.info_pub_transp_bus, ParserUtil.parseWeather(value));
                id = R.id.txt_bus;
            } else if (Constants.PUBTRANSP.GATT_PUB_TRANSP_METRO.getUuid().equals(uuid)) {
                weather = App.inst().getString(R.string.info_pub_transp_metro, ParserUtil.parseWeather(value));
                id = R.id.txt_metro;
            } else if (Constants.PUBTRANSP.GATT_PUB_TRANSP_TRAIN.getUuid().equals(uuid)) {
                weather = App.inst().getString(R.string.info_pub_transp_train, ParserUtil.parseWeather(value));
                id = R.id.txt_train;
            }
            if (id > 0) {
                ((TextView) findViewById(id)).setText(weather);
            }
        }
    }

    private class GattPubTransp implements IConnGattProvider.IConnGattSubscriber {

        @Override
        public void onGattReady(String macAddress) {
        }

        @Override
        public void onServicesReady(String macAddress) {
            gattProviderTransp.registerConnGattSub(gattListenTransp);
            gattProviderTransp.getGattCharacteristicRead(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid(),
                                                         Constants.PUBTRANSP.GATT_PUB_TRANSP_BUS.getUuid());
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
