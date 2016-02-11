package de.uni.stuttgart.vis.access.client.service.bl;

import java.util.UUID;

/**
 * @author Alexander Dridiger
 */
public interface IConnWeather {

    void registerWeatherSub(IConnWeatherSub sub);

    void deregisterWeatherSub(IConnWeatherSub sub);

    void getWeatherInfo(UUID uuid);


    interface IConnWeatherSub {

        void onWeatherInfo(UUID uuid, byte[] value);
    }
}
