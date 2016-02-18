package de.uni.stuttgart.vis.access.client.service;

import java.util.UUID;

import de.uni.stuttgart.vis.access.client.helper.ITtsProv;
import de.uni.stuttgart.vis.access.client.service.bl.IConnAdvertProvider;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;

/**
 * @author Alexander Dridiger
 */
public interface IServiceBinderClient {

    void registerServiceListener(IServiceBlListener listener);

    void deregisterServiceListener(IServiceBlListener listener);

    boolean isConnected(IServiceBlListener listener);

    IConnGattProvider subscribeGattConnection(UUID uuid, IConnGattProvider.IConnGattSubscriber subscriber);

    IConnAdvertProvider subscribeAdvertConnection(UUID uuid, IConnAdvertProvider.IConnAdvertSubscriber subscriber);

    ITtsProv getTtsProvider();
}