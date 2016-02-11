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

    public static final int REQUEST_ENABLE_BT = 1;

    public static final ParcelUuid UUID_ADVERT_SERVICE_MULTI      = ParcelUuid.fromString("0000b81a-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid UUID_ADVERT_SERVICE_WEATHER    = ParcelUuid.fromString("0000b81b-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid UUID_ADVERT_SERVICE_PUB_TRANSP = ParcelUuid.fromString("0000b81c-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid UUID_ADVERT_SERVICE_SHOUT      = ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb");

    public static final int        ACTIVITY                = 123;
    public static final int        SERVICE                 = 213;
    public static final int        BROADCAST_RECEIVER      = 312;
    public static final ParcelUuid GATT_SERVICE_WEATHER    = ParcelUuid.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid GATT_WEATHER_TODAY      = ParcelUuid.fromString("00fff1-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid GATT_WEATHER_TOMORROW   = ParcelUuid.fromString("00fff2-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid GATT_WEATHER_DAT        = ParcelUuid.fromString("00fff3-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid GATT_WEATHER_QUERY      = ParcelUuid.fromString("00fff4-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid GATT_SERVICE_PUB_TRANSP = ParcelUuid.fromString("0000eff0-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid GATT_PUB_TRANSP_BUS     = ParcelUuid.fromString("00eff1-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid GATT_PUB_TRANSP_METRO   = ParcelUuid.fromString("00eff2-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid GATT_PUB_TRANSP_TRAIN   = ParcelUuid.fromString("00eff3-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid GATT_SERVICE_SHOUT      = ParcelUuid.fromString("0000dff0-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid GATT_SHOUT              = ParcelUuid.fromString("00dff1-0000-1000-8000-00805f9b34fb");


    public static final ParcelUuid SERVICE_DEVICE_INFORMATION = ParcelUuid.fromString("0000180a-0000-1000-8000-00805f9b34ff");
    public static final ParcelUuid SOFTWARE_REVISION_STRING   = ParcelUuid.fromString("00002A28-0000-1000-8000-00805f9b34ff");

    public enum AdvertiseConst {

        ADVERTISE_WEATHER((byte) 0x10, R.string.bl_advert_weather_info),
        ADVERTISE_TRANSP((byte) 0x20, R.string.bl_advert_pub_transp),
        ADVERTISE_NEWS((byte) 0x30, R.string.bl_advert_news_info),
        ADVERTISE_BOOKING((byte) 0x40, R.string.bl_advert_booking_info),
        ADVERTISE_SHOUT((byte) 0x50, R.string.bl_advert_shout);

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
