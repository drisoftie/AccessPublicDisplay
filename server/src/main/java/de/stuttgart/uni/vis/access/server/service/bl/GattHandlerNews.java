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
import de.stuttgart.uni.vis.access.common.domain.RssItem;
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
        constantUuids.add(Constants.NEWS.GATT_SERVICE_NEWS.getUuid());
        constantUuids.add(Constants.NEWS.GATT_NEWS.getUuid());
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
            RssItem n = provider.getFeedNews().getItems().iterator().next();
            changeGattChar(Constants.NEWS.GATT_SERVICE_NEWS.getUuid(), Constants.NEWS.GATT_NEWS.getUuid(), n.getTitle());
        }
    }

    private class GattCallback extends GattCallbackBase {

        @Override
        public void onConnectSuccess(BluetoothDevice device, int status, int newState) {
            setNewsInfo();
        }

        @Override
        public void onDisconnected(BluetoothDevice device, int status, int newState) {

        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            setNewsInfo();
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
                BluetoothGattService serviceNews = new BluetoothGattService(Constants.NEWS.GATT_SERVICE_NEWS.getUuid(),
                                                                            BluetoothGattService.SERVICE_TYPE_PRIMARY);
                serviceNews.addCharacteristic(
                        createCharacteristic(Constants.NEWS.GATT_NEWS.getUuid(), BluetoothGattCharacteristic.PROPERTY_READ,
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
