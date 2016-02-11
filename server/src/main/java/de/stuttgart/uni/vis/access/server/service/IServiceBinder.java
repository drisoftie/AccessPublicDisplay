package de.stuttgart.uni.vis.access.server.service;

/**
 * @author Alexander Dridiger
 */
public interface IServiceBinder {

    void registerServiceListener(IServiceBlListener listener);

    void deregisterServiceListener(IServiceBlListener listener);

    boolean isConnected(IServiceBlListener listener);

    void onBlUserShutdown();
}
