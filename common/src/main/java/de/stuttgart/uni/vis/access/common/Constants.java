/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.stuttgart.uni.vis.access.common;

import android.os.ParcelUuid;

/**
 * Constants for use in the Bluetooth Advertisements sample
 */
public class Constants {

    public static final String ADVERTISING_FAILED            = "com.example.android.bluetoothadvertisements.advertising_failed";
    public static final String ADVERTISING_FAILED_EXTRA_CODE = "failureCode";
    public static final int    ADVERTISING_TIMED_OUT         = 6;

    /**
     * UUID identified with this app - set as Service UUID for BLE Advertisements.
     * <p>
     * Bluetooth requires a certain format for UUIDs associated with Services.
     * The official specification can be found here:
     * {@link https://www.bluetooth.org/en-us/specification/assigned-numbers/service-discovery}
     */
    public static final ParcelUuid UUID_ADVERT_SERVICE_WEATHER = ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid UUID_SERVICE_PUB_TRANSP     = ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fa");

    public static final int REQUEST_ENABLE_BT = 1;

    public static final int    ACTIVITY                   = 123;
    public static final int    SERVICE                    = 213;
    public static final int    BROADCAST_RECEIVER         = 312;
    public static final String GATT_SERVICE_WEATHER       = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static final String GATT_WEATHER_TODAY         = "00fff1-0000-1000-8000-00805f9b34fb";
    public static final String GATT_WEATHER_TOMORROW      = "00fff2-0000-1000-8000-00805f9b34fb";
    public static final String GATT_WEATHER_DAT           = "00fff3-0000-1000-8000-00805f9b34fb";
    public static final String GATT_WEATHER_QUERY         = "00fff4-0000-1000-8000-00805f9b34fb";
    public static final String GATT_SERVICE_PUB_TRANSP    = "0000fff0-0000-1000-8000-00805f9b34fa";
    public static final String GATT_PUB_TRANSP_BUS        = "00fff1-0000-1000-8000-00805f9b34fa";
    public static final String GATT_PUB_TRANSP_METRO      = "00fff2-0000-1000-8000-00805f9b34fa";
    public static final String GATT_PUB_TRANSP_TRAIN      = "00fff3-0000-1000-8000-00805f9b34fa";
    public static final String SERVICE_DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb";
    public static final String SOFTWARE_REVISION_STRING   = "00002A28-0000-1000-8000-00805f9b34fb";

    public enum AdvertiseConst {

        ADVERTISE_WEATHER((byte) 0x10, R.string.bl_advert_weather_info),
        ADVERTISE_TRANSP((byte) 0x20, R.string.bl_advert_pub_transp),
        ADVERTISE_NEWS((byte) 0x30, R.string.bl_advert_news_info),
        ADVERTISE_BOOKING((byte) 0x40, R.string.bl_advert_booking_info);

        public static final byte ADVERTISE_START = (byte) 0xfe;
        public static final byte ADVERTISE_END   = (byte) 0xff;

        private final byte flag;
        private final int  descr;

        AdvertiseConst(byte flag, int descrResString) {
            this.flag = flag;
            this.descr = descrResString;
        }

        public byte getFlag() {
            return flag;
        }

        public int getDescr() {
            return descr;
        }
    }
}
