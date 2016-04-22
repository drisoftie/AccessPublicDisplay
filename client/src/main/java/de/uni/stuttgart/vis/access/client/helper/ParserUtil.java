package de.uni.stuttgart.vis.access.client.helper;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.util.ParserData;
import de.uni.stuttgart.vis.access.client.App;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.data.BlData;
import de.uni.stuttgart.vis.access.client.data.GattData;

/**
 * @author Alexander Dridiger
 */
public class ParserUtil {

    public static String getGattData(UUID uuid, BlData data) {
        String value = null;
        for (GattData g : data.getGattData()) {
            if (g.getUuid().equals(uuid)) {
                value = new String(g.getData());
                break;
            }
        }
        return value;
    }

    public static String parseWeather(byte[] value) {
        return parseWeather(new String(value));
    }

    public static String parseWeather(String value) {
        StringBuilder weather = new StringBuilder();
        String[]      data    = StringUtils.split(value, ';');
        for (int i = 0; i < data.length; i++) {
            switch (i) {
                case 0:
                    weather.append(data[i]);
                    break;
                case 1:
                    weather.append(" ").append(new DecimalFormat("#.#").format(ParserData.fahrenheitToCelcius(Float.valueOf(data[i]))));
                    weather.append(App.inst().getString(R.string.txt_to));
                    break;
                case 2:
                    weather.append(new DecimalFormat("#.#").format(ParserData.fahrenheitToCelcius(Float.valueOf(data[i]))));
                    weather.append(" Celcius");
                    break;
            }
        }
        return weather.toString();
    }
}
