package de.stuttgart.uni.vis.access.server.service.bl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.view.View;

import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.android.AndroidAction;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.domain.PublicTransport;
import de.stuttgart.uni.vis.access.server.App;
import de.stuttgart.uni.vis.access.server.R;

/**
 * @author Alexander Dridiger
 */
public class GattHandlerPubTransp extends BaseGattHandler {

    private GattCallback callback = new GattCallback();
    private ActionServicesAdd actionServicesAdd;

    public GattHandlerPubTransp() {
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid());
        constantUuids.add(Constants.PUBTRANSP.GATT_PUB_TRANSP_BUS.getUuid());
        constantUuids.add(Constants.PUBTRANSP.GATT_PUB_TRANSP_METRO.getUuid());
        constantUuids.add(Constants.PUBTRANSP.GATT_PUB_TRANSP_TRAIN.getUuid());
        setConstantUuids(constantUuids);
    }

    @Override
    protected void addServices() {
        actionServicesAdd = new ActionServicesAdd(null, IGenericAction.class, null);
        actionServicesAdd.invokeSelf();
    }

    @Override
    public GattCallback getCallback() {
        return callback;
    }

    @Override
    public BluetoothGattServerCallback getCallback(UUID uuid) {
        for (UUID myUuid : getConstantUuids()) {
            if (myUuid.equals(uuid)) {
                return callback;
            }
        }
        return null;
    }

    private void setInfoPubTransp() {
        ProviderPubTransp provider = ProviderPubTransp.inst();
        provider.createTransportInfo();
        if (provider.hasTransportInfo()) {
            StringBuilder infoTranspBus   = new StringBuilder();
            StringBuilder infoTranspMetro = new StringBuilder();
            StringBuilder infoTranspTrain = new StringBuilder();
            for (PublicTransport t : provider.getTransports()) {
                switch (t.getType()) {
                    case BUS:
                        infoTranspBus.append(System.lineSeparator()).append(t.getLine()).append(" ").append(t.getDirection()).append(": ")
                                     .append(t.getTime());
                        break;
                    case METRO:
                        infoTranspMetro.append(System.lineSeparator()).append(t.getLine()).append(" ").append(t.getDirection()).append(": ")
                                       .append(t.getTime());
                        break;
                    case TRAIN:
                        infoTranspTrain.append(System.lineSeparator()).append(t.getLine()).append(" ").append(t.getDirection()).append(": ")
                                       .append(t.getTime());
                        break;
                }
            }
            changeGattChar(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid(), Constants.PUBTRANSP.GATT_PUB_TRANSP_BUS.getUuid(),
                           infoTranspBus.toString());
            if (infoTranspMetro.length() == 0) {
                infoTranspMetro.append(App.inst().getString(R.string.info_transp_no_conn));
            }
            changeGattChar(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid(), Constants.PUBTRANSP.GATT_PUB_TRANSP_METRO.getUuid(),
                           infoTranspMetro.toString());
            changeGattChar(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid(), Constants.PUBTRANSP.GATT_PUB_TRANSP_TRAIN.getUuid(),
                           infoTranspTrain.toString());
        }
    }

    private class GattCallback extends GattCallbackBase {
        @Override
        public void onConnectSuccess(BluetoothDevice device, int status, int newState) {
            setInfoPubTransp();
        }

        @Override
        public void onDisconnected(BluetoothDevice device, int status, int newState) {
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            setInfoPubTransp();
            actionServicesAdd.invokeSelf(service.getUuid());
        }
    }


    private class ActionServicesAdd extends AndroidAction<View, Void, Void, Void, Void> {

        public ActionServicesAdd(View view, Class<?> actionType, String regMethodName) {
            super(view, actionType, regMethodName);
        }

        @Override
        public Object onActionPrepare(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            return null;
        }

        @Override
        public Void onActionDoWork(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            if (ArrayUtils.isEmpty(stripMethodArgs(methodArgs))) {
                BluetoothGattService servicePubTransp = new BluetoothGattService(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid(),
                                                                                 BluetoothGattService.SERVICE_TYPE_PRIMARY);

                servicePubTransp.addCharacteristic(
                        createCharacteristic(Constants.PUBTRANSP.GATT_PUB_TRANSP_BUS.getUuid(), BluetoothGattCharacteristic.PROPERTY_READ,
                                             BluetoothGattCharacteristic.PERMISSION_READ,
                                             App.inst().getString(R.string.bl_advert_bus).getBytes()));
                servicePubTransp.addCharacteristic(
                        createCharacteristic(Constants.PUBTRANSP.GATT_PUB_TRANSP_METRO.getUuid(), BluetoothGattCharacteristic.PROPERTY_READ,
                                             BluetoothGattCharacteristic.PERMISSION_READ,
                                             App.inst().getString(R.string.bl_advert_metro).getBytes()));
                servicePubTransp.addCharacteristic(
                        createCharacteristic(Constants.PUBTRANSP.GATT_PUB_TRANSP_TRAIN.getUuid(), BluetoothGattCharacteristic.PROPERTY_READ,
                                             BluetoothGattCharacteristic.PERMISSION_READ,
                                             App.inst().getString(R.string.bl_advert_train).getBytes()));

                getServer().addService(servicePubTransp);
            } else {
                Object[] args = stripMethodArgs(methodArgs);
                getServicesReadyListener().onFinished((UUID) args[0]);
            }
            return null;
        }

        @Override
        public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2,
                                      Object[] additionalTags) {

        }
    }
}
