package de.uni.stuttgart.vis.access.client.service;

import java.util.UUID;

/**
 * @author Alexander Dridiger
 */
public interface IServiceBinder {

    void registerServiceListener(IServiceBlListener listener);

    void deRegisterServiceListener(IServiceBlListener listener);

    IConnAdvertScanHandler subscribeBlConnection(UUID uuid, IConnAdvertScanHandler.IConnGattSubscriber subscriber);
}
