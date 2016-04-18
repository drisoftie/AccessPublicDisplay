package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.graphics.BitmapFactory;
import android.os.ParcelUuid;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.util.AccessGatt;
import de.uni.stuttgart.vis.access.client.App;
import de.uni.stuttgart.vis.access.client.data.ChatMessage;

/**
 * @author Alexander Dridiger
 */
public class ConnChat extends ConnBasePartAdvertScan implements IConnMultiPart {

    private List<ChatMessage> chatHistory = new ArrayList<>();

    public ConnChat() {
        addUuid(Constants.UUID_ADVERT_SERVICE_MULTI.getUuid()).addUuid(Constants.CHAT.UUID_ADVERT_SERVICE_CHAT.getUuid()).addUuid(
                Constants.CHAT.GATT_SERVICE_CHAT.getUuid()).addUuid(Constants.CHAT.GATT_CHAT_WRITE.getUuid()).addUuid(
                Constants.CHAT.GATT_CHAT_NOTIFY.getUuid());
        setScanCallback(new BlAdvertScanCallback());
        setGattCallback(new BlGattCallback());
    }

    private void analyzeScanData(ScanResult scanData) {
        boolean start = false;
        //noinspection ConstantConditions
        byte[] advert = scanData.getScanRecord().getServiceData(Constants.UUID_ADVERT_SERVICE_MULTI);
        for (int i = 0; i < advert.length; i++) {
            if (i + 1 < advert.length) {
                byte[] b = new byte[]{advert[i], advert[i + 1]};
                if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_START)) {
                    start = true;
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_CHAT.getFlag())) {
                    ScanResult foundRes = null;
                    for (ScanResult res : getScanResults()) {
                        if (StringUtils.equals(scanData.getDevice().getAddress(), res.getDevice().getAddress())) {
                            foundRes = res;
                            break;
                        }
                    }
                    if (foundRes != null) {
                        removeScanResult(foundRes.getDevice().getAddress());
                        addScanResult(scanData);
                        for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
                            callback.onRefreshedScanReceived(scanData);
                        }
                    } else {
                        addScanResult(scanData);
                        getConnMulti().contributeNotification(App.string(Constants.AdvertiseConst.ADVERTISE_CHAT.getDescr()), this);
                        for (IConnAdvertSubscriber callback : getConnAdvertSubscribers()) {
                            callback.onScanResultReceived(scanData);
                        }
                    }
                }
            } else {
                break;
            }
        }
    }

    private class BlAdvertScanCallback extends ScanCallbackBase {

        @Override
        protected ParcelUuid getServiceUuid() {
            return Constants.UUID_ADVERT_SERVICE_MULTI;
        }

        @Override
        protected void onReceiveScanData(ScanResult result) {
            analyzeScanData(result);
        }
    }

    private class BlGattCallback extends GattCallbackBase {

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setGattInst(gatt);
                for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                    sub.onServicesReady(getLastGattInst().getDevice().getAddress());
                }
                setServicesDiscovered(true);
                access(new AccessGatt(ConnChat.this) {
                    @Override
                    public void onRun() {
                        getLastGattInst().setCharacteristicNotification(
                                getLastGattInst().getService(Constants.CHAT.GATT_SERVICE_CHAT.getUuid())
                                                 .getCharacteristic(Constants.CHAT.GATT_CHAT_NOTIFY.getUuid()), true);
                    }
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            setGattInst(gatt);
            for (UUID uuid : getConstantUuids()) {
                if (uuid.equals(characteristic.getUuid())) {
                    if (characteristic.getValue() != null) {
                        new ActionParse(characteristic.getValue(), characteristic.getUuid()).start();
                    }
                }
            }
        }
    }

    private class ActionParse extends Thread {

        private final byte[] value;
        private final UUID   uuid;

        ActionParse(byte[] value, UUID uuid) {
            this.value = value;
            this.uuid = uuid;
        }

        @Override
        public void run() {
            int index = -1;
            for (int i = 0; i < value.length; i++) {
                byte val = value[i];
                if (val == ':') {
                    index = i;
                    break;
                }
            }
            long    stamp = Long.parseLong(new String(Arrays.copyOfRange(value, 0, index)));
            boolean found = false;
            for (int i = 0; i < chatHistory.size(); i++) {
                ChatMessage msg = chatHistory.get(i);
                if (msg.timestamp == stamp) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                ChatMessage msg = parseMsg(value, index);
                msg.timestamp = stamp;
                chatHistory.add(msg);
                for (IConnGattSubscriber sub : getConnGattSubscribers()) {
                    sub.onGattValueChanged(getLastGattInst().getDevice().getAddress(), uuid, msg.value);
                }
            }
        }

        private ChatMessage parseMsg(byte[] value, int indexColon) {
            ChatMessage newMsg = new ChatMessage();
            newMsg.value = value;
            if (value.length > 200) {
                byte[] picData = Arrays.copyOfRange(value, indexColon + 1, value.length);
                newMsg.pic = BitmapFactory.decodeByteArray(picData, 0, picData.length);
            } else {
                newMsg.message = new String(Arrays.copyOfRange(value, indexColon + 1, value.length));
            }
            return newMsg;
        }
    }
}
