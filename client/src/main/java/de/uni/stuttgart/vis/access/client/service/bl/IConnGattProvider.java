package de.uni.stuttgart.vis.access.client.service.bl;

import java.util.UUID;

/**
 * @author Alexander Dridiger
 */
public interface IConnGattProvider {

    void registerConnGattSub(IConnGattSubscriber sub);

    void deregisterConnGattSub(IConnGattSubscriber sub);

    void getGattCharacteristicRead(UUID service, UUID characteristic);

    void writeGattCharacteristic(UUID service, UUID characteristic, byte[] write);

    Object getData();

    interface IConnGattSubscriber {

        void onGattReady(String macAddress);

        void onServicesReady(String macAddress);

        void onGattValueReceived(String macAddress, UUID uuid, byte[] value);

        void onGattValueWriteReceived(String macAddress, UUID uuid, byte[] value);

        void onGattValueChanged(String macAddress, UUID uuid, byte[] value);
    }
}
