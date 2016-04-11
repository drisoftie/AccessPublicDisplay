package de.stuttgart.uni.vis.access.server.service.bl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.view.View;

import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.android.AndroidAction;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.server.App;
import de.stuttgart.uni.vis.access.server.R;

/**
 * @author Alexander Dridiger
 */
public class GattHandlerNews extends BaseGattHandler {

    private static final String TAG = GattHandlerNews.class.getSimpleName();

    private GattCallback callback = new GattCallback();
    private ActionServicesAdd actionServicesAdd;

    public GattHandlerNews() {
        ArrayList<UUID> constantUuids = new ArrayList<>();
        constantUuids.add(Constants.GATT_SERVICE_NEWS.getUuid());
        constantUuids.add(Constants.GATT_NEWS.getUuid());
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
        ProviderNews provider = ProviderNews.inst();
        provider.createNews();
        if (provider.hasNewsInfo()) {
            changeGattChar(Constants.GATT_SERVICE_NEWS.getUuid(), Constants.GATT_NEWS.getUuid(),
                           provider.getFeedNews().getItems().iterator().next().getDescription());
        }
    }

    private class GattCallback extends BluetoothGattServerCallback {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    getConnDevices().add(device);
                    break;
                default:
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        getConnDevices().remove(device);
                    }
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            setNewsInfo();
            actionServicesAdd.invokeSelf(service.getUuid());
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
            characteristic.setValue(value);
            if (responseNeeded) {
                getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
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
            getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            getServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
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
                BluetoothGattService serviceNews = new BluetoothGattService(Constants.GATT_SERVICE_NEWS.getUuid(),
                                                                            BluetoothGattService.SERVICE_TYPE_PRIMARY);
                serviceNews.addCharacteristic(createCharacteristic(Constants.GATT_NEWS.getUuid(), BluetoothGattCharacteristic.PROPERTY_READ,
                                                                   BluetoothGattCharacteristic.PERMISSION_READ,
                                                                   App.inst().getString(R.string.bl_gatt_char_weather_default).getBytes()));
                getServer().addService(serviceNews);
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
