/**
 * **************************************************************************
 * <p/>
 * Copyright 2012-2013 Sony Corporation
 * <p/>
 * The information contained here-in is the property of Sony corporation and
 * is not to be disclosed or used without the prior written permission of
 * Sony corporation. This copyright extends to all media in which this
 * information may be preserved including magnetic storage, computer
 * print-out or visual display.
 * <p/>
 * Contains proprietary information, copyright and database rights Sony.
 * Decompilation prohibited save as permitted by law. No using, disclosing,
 * reproducing, accessing or modifying without Sony prior written consent.
 * <p/>
 * **************************************************************************
 */
package de.stuttgart.uni.vis.access.common.util;

/**
 * @author Alexander Dridiger
 */
public class Hex {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes, boolean bigEndian) {
        if (bytes != null && bytes.length > 0) {
            char[] hexChars = new char[bytes.length * 2];
//        if (bigEndian) {
            for (int j = 0; j < bytes.length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
//        } else {
//            for (int j = bytes.length - 1; j >= 0; j--) {
//                int v = bytes[j] & 0xFF;
//                hexChars[j * 2] = hexArray[v >>> 4];
//                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
//            }
//        }
            return new String(hexChars);
        }
        return null;
    }
}
