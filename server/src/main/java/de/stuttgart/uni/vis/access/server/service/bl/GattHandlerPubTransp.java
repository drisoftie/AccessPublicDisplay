package de.stuttgart.uni.vis.access.server.service.bl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.view.View;

import com.drisoftie.action.async.android.AndroidAction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.domain.Departure;
import de.stuttgart.uni.vis.access.common.domain.PublicTransport;
import de.stuttgart.uni.vis.access.server.App;
import de.stuttgart.uni.vis.access.server.R;

/**
 * @author Alexander Dridiger
 */
public class GattHandlerPubTransp extends BaseGattHandler {

    private GattCallback callback = new GattCallback();

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
        BluetoothGattService servicePubTransp = new BluetoothGattService(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid(),
                                                                         BluetoothGattService.SERVICE_TYPE_PRIMARY);

        servicePubTransp.addCharacteristic(
                createCharacteristic(Constants.PUBTRANSP.GATT_PUB_TRANSP_BUS.getUuid(), BluetoothGattCharacteristic.PROPERTY_READ,
                                     BluetoothGattCharacteristic.PERMISSION_READ, App.inst().getString(R.string.bl_advert_bus).getBytes()));
        servicePubTransp.addCharacteristic(
                createCharacteristic(Constants.PUBTRANSP.GATT_PUB_TRANSP_METRO.getUuid(), BluetoothGattCharacteristic.PROPERTY_READ,
                                     BluetoothGattCharacteristic.PERMISSION_READ,
                                     App.inst().getString(R.string.bl_advert_metro).getBytes()));
        servicePubTransp.addCharacteristic(
                createCharacteristic(Constants.PUBTRANSP.GATT_PUB_TRANSP_TRAIN.getUuid(), BluetoothGattCharacteristic.PROPERTY_READ,
                                     BluetoothGattCharacteristic.PERMISSION_READ,
                                     App.inst().getString(R.string.bl_advert_train).getBytes()));

        getServer().addService(servicePubTransp);
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
                        infoTranspBus.append(t.getLine()).append(": ").append(t.getTime());
                        break;
                    case METRO:
                        infoTranspMetro.append(t.getLine()).append(": ").append(t.getTime());
                        break;
                    case TRAIN:
                        infoTranspTrain.append(t.getLine()).append(": ").append(t.getTime());
                        break;
                }
            }
            changeGattChar(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid(), Constants.PUBTRANSP.GATT_PUB_TRANSP_BUS.getUuid(),
                           infoTranspBus.toString());
            changeGattChar(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid(), Constants.PUBTRANSP.GATT_PUB_TRANSP_METRO.getUuid(),
                           infoTranspMetro.toString());
            changeGattChar(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid(), Constants.PUBTRANSP.GATT_PUB_TRANSP_TRAIN.getUuid(),
                           infoTranspTrain.toString());
        }
    }

    private class GattCallback extends BluetoothGattServerCallback {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            setInfoPubTransp();
            getServicesReadyListener().onFinished(service.getUuid());
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {

        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
        }
    }


    private class PubTranspAction extends AndroidAction<View, Void, Void, Void, Void> {

        public PubTranspAction(View view, Class<?> actionType, String regMethodName) {
            super(view, actionType, regMethodName);
        }

        @Override
        public Object onActionPrepare(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            return null;
        }

        @Override
        public Void onActionDoWork(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();
            Type collectionType = new TypeToken<Collection<Departure>>() {
            }.getType();

            InputStream is = null;
            try {
                URL               url  = new URL("https://efa-api.asw.io/api/v1/station/5006008/departures/?format=json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                is = conn.getInputStream();

                Reader reader = null;
                reader = new InputStreamReader(is, "UTF-8");

                Collection<Departure> departures = gson.fromJson(reader, collectionType);

                StringBuilder sBus   = new StringBuilder();
                StringBuilder sTrain = new StringBuilder();
                StringBuilder sMetro = new StringBuilder();

                Calendar c       = Calendar.getInstance();
                int      minutes = c.get(Calendar.MINUTE);

                if (departures != null) {
                    for (Departure d : departures) {
                        if (StringUtils.isNumericSpace(d.number)) {
                            sBus.append(d.number).append(":").append(
                                    String.valueOf(Math.min(Integer.valueOf(d.departureTime.minute) - minutes, 0)));
                        } else if (StringUtils.startsWithIgnoreCase(d.number, "s")) {
                            sTrain.append(d.number).append(":").append(
                                    String.valueOf(Math.min(Integer.valueOf(d.departureTime.minute) - minutes, 0)));
                        }
                    }
                }
                changeGattChar(Constants.PUBTRANSP.GATT_SERVICE_PUB_TRANSP.getUuid(), Constants.PUBTRANSP.GATT_PUB_TRANSP_BUS.getUuid(),
                               sBus.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2,
                                      Object[] additionalTags) {

        }
    }
}
