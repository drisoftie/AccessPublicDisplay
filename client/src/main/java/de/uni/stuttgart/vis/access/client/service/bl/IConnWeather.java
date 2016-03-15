package de.uni.stuttgart.vis.access.client.service.bl;

import java.util.UUID;

/**
 * @author Alexander Dridiger
 */
public interface IConnWeather extends IConnGattProvider {

    void registerWeatherSub(IConnWeatherSub sub);

    void deregisterWeatherSub(IConnWeatherSub sub);

    void getWeatherInfo(UUID uuid);


    interface IConnWeatherSub extends IConnGattSubscriber {

        void onWeatherInfo(String macAddress, UUID uuid, byte[] value);
    }
}
