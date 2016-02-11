package de.uni.stuttgart.vis.access.client.service.bl;

import java.util.UUID;

/**
 * @author Alexander Dridiger
 */
public interface IConnGattProvider {

    void registerConnGattSub(IConnGattSubscriber sub);

    void deregisterConnGattSub(IConnGattSubscriber sub);

    interface IConnGattSubscriber {

        void onGattReady();

        void onServicesReady();

        void onGattValueReceived(byte[] value);

        void onGattValueChanged(UUID uuid, byte[] value);
    }
}
