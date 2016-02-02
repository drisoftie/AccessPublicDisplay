package de.uni.stuttgart.vis.access.client.service;

import java.util.UUID;

/**
 * @author Alexander Dridiger
 */
public interface IConnWeather {

    void registerWeatherSub(IConnWeatherSub sub);

    void getWeatherInfo(UUID uuid);


    public interface IConnWeatherSub {

        void onWeatherInfo(UUID uuid, byte[] value);
    }
}
