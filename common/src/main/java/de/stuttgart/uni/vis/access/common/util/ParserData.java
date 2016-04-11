package de.stuttgart.uni.vis.access.common.util;

import java.nio.ByteBuffer;

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
}
