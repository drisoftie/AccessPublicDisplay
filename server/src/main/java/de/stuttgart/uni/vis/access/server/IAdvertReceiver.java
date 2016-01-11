package de.stuttgart.uni.vis.access.server;

/**
 * @author Alexander Dridiger
 */
public interface IAdvertReceiver {

    void onNewAdvertisementString(String advert);

    void onRestartAdvertisement();
}
