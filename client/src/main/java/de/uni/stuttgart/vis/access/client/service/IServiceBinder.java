package de.uni.stuttgart.vis.access.client.service;

/**
 * @author Alexander Dridiger
 */
public interface IServiceBinder {

    void registerServiceListener(IServiceBlListener listener);

    void deRegisterServiceListener(IServiceBlListener listener);
}
