package de.stuttgart.uni.vis.access.common.util;

import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.stuttgart.uni.vis.access.common.Constants;

/**
 * @author Alexander Dridiger
 */
public class ParserData {
    public static byte[] parseFloatToByte(float toByte) {
        return ByteBuffer.allocate(4).putFloat(toByte).array();
    }

    public static float parseByteToFloat(byte[] toFloat) {
        return ByteBuffer.wrap(toFloat).getFloat();
    }

    public static List<AbstractMap.SimpleEntry<Constants.AdvertiseConst, byte[]>> parseAdvert(byte[] advert) {
        List<AbstractMap.SimpleEntry<Constants.AdvertiseConst, byte[]>> uuids = new ArrayList<>();
        for (int i = 0; i < advert.length; i++) {
            if (i + 1 < advert.length) {
                byte[] b = new byte[]{advert[i], advert[i + 1]};
                if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_WEATHER.getFlag())) {
                    uuids.add(new AbstractMap.SimpleEntry<>(Constants.AdvertiseConst.ADVERTISE_WEATHER, new byte[]{}));
                    i++;
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_WEATHER_DATA.getFlag())) {
                    uuids.add(new AbstractMap.SimpleEntry<>(Constants.AdvertiseConst.ADVERTISE_WEATHER_DATA,
                                                            Arrays.copyOfRange(advert, i + 2, i + 6)));
                    i = i + 5;
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_TRANSP.getFlag())) {
                    uuids.add(new AbstractMap.SimpleEntry<>(Constants.AdvertiseConst.ADVERTISE_TRANSP, new byte[]{}));
                    i++;
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_SHOUT.getFlag())) {
                    uuids.add(new AbstractMap.SimpleEntry<>(Constants.AdvertiseConst.ADVERTISE_SHOUT, new byte[]{}));
                    i++;
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_NEWS.getFlag())) {
                    uuids.add(new AbstractMap.SimpleEntry<>(Constants.AdvertiseConst.ADVERTISE_NEWS, new byte[]{}));
                    i++;
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_NEWS_DATA.getFlag())) {
                    uuids.add(new AbstractMap.SimpleEntry<>(Constants.AdvertiseConst.ADVERTISE_NEWS_DATA, new byte[]{advert[i + 2]}));
                    i = i + 2;
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_BOOKING.getFlag())) {
                    uuids.add(new AbstractMap.SimpleEntry<>(Constants.AdvertiseConst.ADVERTISE_BOOKING, new byte[]{}));
                    i++;
                } else if (Arrays.equals(b, Constants.AdvertiseConst.ADVERTISE_CHAT.getFlag())) {
                    uuids.add(new AbstractMap.SimpleEntry<>(Constants.AdvertiseConst.ADVERTISE_CHAT, new byte[]{}));
                    i++;
                }
            } else {
                break;
            }
        }

        return uuids;
    }

    public static float fahrenheitToCelcius(float fahrenheit) {
        return (fahrenheit - 32) * 5.f / 9.f;
    }


}
