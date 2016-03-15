package de.uni.stuttgart.vis.access.client.service.bl;

import java.util.UUID;

/**
 * @author Alexander Dridiger
 */
public interface IConnGattProvider {

    void registerConnGattSub(IConnGattSubscriber sub);

    void deregisterConnGattSub(IConnGattSubscriber sub);

    interface IConnGattSubscriber {

        void onGattReady(String macAddress);

        void onServicesReady(String macAddress);

        void onGattValueReceived(String macAddress, byte[] value);

        void onGattValueChanged(String macAddress, UUID uuid, byte[] value);
    }
}
