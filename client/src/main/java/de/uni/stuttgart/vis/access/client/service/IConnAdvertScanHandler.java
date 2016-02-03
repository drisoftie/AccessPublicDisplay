package de.uni.stuttgart.vis.access.client.service;

/**
 * @author Alexander Dridiger
 */
public interface IConnAdvertScanHandler {

    void registerConnSub(IConnGattSubscriber sub);

    void deregisterConnSub(IConnGattSubscriber sub);

    interface IConnGattSubscriber {

        void onGattReady();

        void onServicesReady();
    }
}
