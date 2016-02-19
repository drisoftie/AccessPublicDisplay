package de.stuttgart.uni.vis.access.server.service.bl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;

import com.drisoftie.action.async.ActionMethod;
import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.android.AndroidAction;
import com.drisoftie.action.async.handler.IFinishedHandler;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.server.App;
import de.stuttgart.uni.vis.access.server.BuildConfig;

/**
 * @author Alexander Dridiger
 */
public class GattServerStateHolder {

    private static final String TAG = GattServerStateHolder.class.getSimpleName();

    private GattCallback        blGattCallback;
    private BluetoothGattServer blGattServer;

    private List<IGattHandler> blGattHandler = new ArrayList<>();

    private ActionServicesAdd         actionServicesAdd;
    private IFinishedHandler<Integer> finishedHandler;

    public void startGatt(BluetoothManager blManager, IFinishedHandler<Integer> finishedHandler) {
        this.finishedHandler = finishedHandler;
        blGattCallback = new GattCallback();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Start Weather + Pubtransp + Shout");
        }
        startGattServer(blManager);
        actionServicesAdd = new ActionServicesAdd(null, new Class[]{IGenericAction.class, IFinishedHandler.class}, null);
        actionServicesAdd.invokeSelf();
    }

    public void closeServer() {
        blGattServer.close();
    }

    private void startGattServer(BluetoothManager blManager) {
        blGattServer = blManager.openGattServer(App.inst(), blGattCallback);
    }

    private void startGattServerWeather() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Start Weather");
        }
        GattHandlerWeather handler = new GattHandlerWeather();
        handler.setServer(blGattServer);
        handler.prepareServer();
        //noinspection unchecked
        handler.prepareServices(actionServicesAdd.getHandlerImpl(IFinishedHandler.class));
        blGattHandler.add(handler);
    }

    private void startGattServerPubTransp() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Start Pubtransp");
        }
        GattHandlerPubTransp handler = new GattHandlerPubTransp();
        handler.setServer(blGattServer);
        //noinspection unchecked
        handler.prepareServices(actionServicesAdd.getHandlerImpl(IFinishedHandler.class));
        blGattHandler.add(handler);
    }

    private void startGattServerShout() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Start Shout");
        }
        GattHandlerShout handler = new GattHandlerShout();
        handler.setServer(blGattServer);
        //noinspection unchecked
        handler.prepareServices(actionServicesAdd.getHandlerImpl(IFinishedHandler.class));
        blGattHandler.add(handler);
    }

    private BluetoothGattServerCallback getHandlerCallback(ParcelUuid... uuids) {
        for (IGattHandler handler : blGattHandler) {
            BluetoothGattServerCallback callback = handler.getCallback(uuids);
            if (callback != null) {
                return callback;
            }
        }
        return null;
    }

    private BluetoothGattServerCallback getHandlerCallback(UUID... uuids) {
        for (IGattHandler handler : blGattHandler) {
            BluetoothGattServerCallback callback = handler.getCallback(uuids);
            if (callback != null) {
                return callback;
            }
        }
        return null;
    }

    private class GattCallback extends BluetoothGattServerCallback {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d("GattServer", "Our gatt server connection state changed, new state ");
            Log.d("GattServer", Integer.toString(newState));
            for (IGattHandler h : blGattHandler) {
                h.getCallback().onConnectionStateChange(device, status, newState);
            }
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.d("GattServer", "Our gatt server service was added.");
            BluetoothGattServerCallback callback = getHandlerCallback(service.getUuid());
            if (callback != null) {
                callback.onServiceAdded(status, service);
            }
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            byte[] value = characteristic.getValue();
            Log.d("GattServer", "Our gatt characteristic was read: " + new String(value));
            BluetoothGattServerCallback callback = getHandlerCallback(characteristic.getUuid());
            if (callback != null) {
                callback.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }
            //             byte[] value = characteristic.getValue();
            //            Log.d("GattServer", "Our gatt characteristic was read: " + new String(value));
            //            if (UUID.fromString(Constants.GATT_SERVICE_WEATHER).equals(characteristic.getService().getUuid())) {
            //                blGattServerWeather.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            //            } else if (UUID.fromString(Constants.GATT_SERVICE_PUB_TRANSP).equals(characteristic.getService().getUuid())) {
            //                blGattServerPubTransp.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            //            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d("GattServer", "We have received a write request for one of our hosted characteristics");
            Log.d("GattServer", "data = " + new String(value));
            BluetoothGattServerCallback callback = getHandlerCallback(characteristic.getUuid());
            if (callback != null) {
                callback.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            Log.d("GattServer", "onNotificationSent");
            for (IGattHandler h : blGattHandler) {
                h.getCallback().onNotificationSent(device, status);
            }
            super.onNotificationSent(device, status);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.d("GattServer", "Our gatt server descriptor was read.");
            BluetoothGattServerCallback callback = getHandlerCallback(descriptor.getUuid());
            if (callback != null) {
                callback.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d("GattServer", "Our gatt server descriptor was written.");
            BluetoothGattServerCallback callback = getHandlerCallback(descriptor.getUuid());
            if (callback != null) {
                callback.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Log.d("GattServer", "Our gatt server on execute write.");
            for (IGattHandler h : blGattHandler) {
                h.getCallback().onExecuteWrite(device, requestId, execute);
            }
            super.onExecuteWrite(device, requestId, execute);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
        }
    }


    private class ActionServicesAdd extends AndroidAction<View, Void, Void, Void, Void> {

        public ActionServicesAdd(View view, Class<?>[] actionTypes, String regMethodName) {
            super(view, actionTypes, regMethodName);
        }

        @Override
        public Object onActionPrepare(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            return null;
        }

        @Override
        public Void onActionDoWork(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            if (ActionMethod.INVOKE_ACTION.matches(methodName)) {
                if (ArrayUtils.isEmpty(stripMethodArgs(methodArgs))) {
                    startGattServerWeather();
                }
            } else if (StringUtils.equals(methodName, "onFinished")) {
                if (Constants.GATT_SERVICE_WEATHER.getUuid().equals(methodArgs[0])) {
                    startGattServerPubTransp();
                } else if (Constants.GATT_SERVICE_PUB_TRANSP.getUuid().equals(methodArgs[0])) {
                    startGattServerShout();
                } else if (Constants.GATT_SERVICE_SHOUT.getUuid().equals(methodArgs[0])) {
                    finishedHandler.onFinished(null);
                }
            }
            return null;
        }

        @Override
        public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2,
                                      Object[] additionalTags) {
        }
    }
}
