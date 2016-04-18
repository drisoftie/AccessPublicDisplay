package de.uni.stuttgart.vis.access.client.act;

import java.util.UUID;

import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;

/**
 * @author Alexander Dridiger
 */
public class GattSub implements IConnGattProvider.IConnGattSubscriber {

    @Override
    public void onGattReady(String macAddress) {

    }

    @Override
    public void onServicesReady(String macAddress) {

    }

    @Override
    public void onGattValueReceived(String macAddress, UUID uuid, byte[] value) {

    }

    @Override
    public void onGattValueWriteReceived(String macAddress, UUID uuid, byte[] value) {

    }

    @Override
    public void onGattValueChanged(String macAddress, UUID uuid, byte[] value) {

    }
}
