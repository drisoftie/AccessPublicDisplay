package de.stuttgart.uni.vis.access.server.service.bl;

import android.content.res.Resources;

import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.DailyForecast;
import net.aksingh.owmjapis.OpenWeatherMap;

import org.json.JSONException;

import java.io.IOException;

import de.stuttgart.uni.vis.access.server.App;
import de.stuttgart.uni.vis.access.server.R;

/**
 * @author Alexander Dridiger
 */
public class ProviderWeather {

    private static ProviderWeather inst;

    private CurrentWeather currWeather;
    private DailyForecast  forecast;

    private ProviderWeather() {
        inst = this;
    }

    public static ProviderWeather inst() {
        if (inst == null) {
            new ProviderWeather();
        }
        return inst;
    }

    public boolean hasWeatherInfo() {
        return (currWeather != null && currWeather.hasMainInstance()) && (forecast != null && forecast.hasForecastCount());
    }

    public CurrentWeather getCurrWeather() {
        return currWeather;
    }

    public DailyForecast getForecast() {
        return forecast;
    }

    public void createForecasts() {
        OpenWeatherMap owm = new OpenWeatherMap(App.string(R.string.ow_api));
        owm.setLang(OpenWeatherMap.Language.fromLangCode(Resources.getSystem().getConfiguration().locale.getLanguage()));
        try {
            currWeather = owm.currentWeatherByCityName("Stuttgart", "de");
            forecast = owm.dailyForecastByCityName("Stuttgart", "de", (byte) 2);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}
