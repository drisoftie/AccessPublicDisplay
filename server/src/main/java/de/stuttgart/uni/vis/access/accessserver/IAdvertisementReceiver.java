package de.stuttgart.uni.vis.access.accessserver;

/**
 * @author Alexander Dridiger
 */
public interface IAdvertisementReceiver {

    void onNewAdvertisementString(String advert);

    void onRestartAdvertisement();
}
