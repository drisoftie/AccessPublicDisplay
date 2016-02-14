package de.uni.stuttgart.vis.access.client.service;

import java.util.UUID;

import de.uni.stuttgart.vis.access.client.helper.ITtsProv;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;

/**
 * @author Alexander Dridiger
 */
public interface IServiceBinder {

    void registerServiceListener(IServiceBlListener listener);

    void deregisterServiceListener(IServiceBlListener listener);

    IConnGattProvider subscribeBlConnection(UUID uuid, IConnGattProvider.IConnGattSubscriber subscriber);

    ITtsProv getTtsProvider();
}