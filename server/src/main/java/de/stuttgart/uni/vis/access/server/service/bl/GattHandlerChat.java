package de.stuttgart.uni.vis.access.server.service.bl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.view.View;

import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.android.AndroidAction;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.util.ScheduleUtil;
import de.stuttgart.uni.vis.access.server.App;
import de.stuttgart.uni.vis.access.server.R;

/**
 * @author Alexander Dridiger
 */
public class GattHandlerChat extends BaseGattHandler {

    private static final String TAG = GattHandlerChat.class.getSimpleName();

    private List<String> chatHierarchy = new ArrayList<>();

    private GattCallback callback = new GattCallback();
    private ActionServicesAdd actionServicesAdd;

    public GattHandlerChat() {
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(Constants.CHAT.GATT_SERVICE_CHAT.getUuid());
        constantUuids.add(Constants.CHAT.GATT_CHAT_NOTIFY.getUuid());
        constantUuids.add(Constants.CHAT.GATT_CHAT_WRITE.getUuid());
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

    private void setNewsInfo() {
//        ProviderNews provider = ProviderNews.inst();
//        provider.createNews();
//        if (provider.hasNewsInfo()) {
//            changeGattChar(Constants.CHAT.GATT_SERVICE_CHAT.getUuid(), Constants.CHAT.GATT_CHAT_NOTIFY.getUuid(),
//                           provider.getFeedNews().getLink());
//        }

    }

    private class GattCallback extends GattCallbackBase {

        @Override
        public void onConnectSuccess(BluetoothDevice device, int status, int newState) {

        }

        @Override
        public void onDisconnected(BluetoothDevice device, int status, int newState) {

        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            setNewsInfo();
            actionServicesAdd.invokeSelf(service.getUuid());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            characteristic.setValue(value);
            chatHierarchy.add(new String(value));
            if (responseNeeded) {
                getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
            ScheduleUtil.scheduleWork(new Runnable() {

                public BluetoothDevice device;
                public byte[] value;

                public Runnable init(BluetoothDevice device, byte[] value) {
                    this.device = device;
                    this.value = value;
                    return this;
                }

                @Override
                public void run() {
                    changeGattChar(Constants.CHAT.GATT_SERVICE_CHAT.getUuid(), Constants.CHAT.GATT_CHAT_NOTIFY.getUuid(), value);

                }
            }.init(device, value), 500, TimeUnit.MILLISECONDS);
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
                BluetoothGattService serviceChat = new BluetoothGattService(Constants.CHAT.GATT_SERVICE_CHAT.getUuid(),
                                                                            BluetoothGattService.SERVICE_TYPE_PRIMARY);
                serviceChat.addCharacteristic(
                        createCharacteristic(Constants.CHAT.GATT_CHAT_WRITE.getUuid(), BluetoothGattCharacteristic.PROPERTY_WRITE,
                                             BluetoothGattCharacteristic.PERMISSION_WRITE,
                                             App.inst().getString(R.string.bl_gatt_char_weather_default).getBytes()));
                serviceChat.addCharacteristic(
                        createCharacteristic(Constants.CHAT.GATT_CHAT_NOTIFY.toString(), BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                                             BluetoothGattCharacteristic.PERMISSION_READ,
                                             App.inst().getString(R.string.bl_advert_cloudy).getBytes()));

                getServer().addService(serviceChat);
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
