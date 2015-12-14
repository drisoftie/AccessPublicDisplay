package de.stuttgart.uni.vis.access.accessserver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * @author Alexander Dridiger
 */
public class BrRcvAdvertisement extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent stopAdvert = new Intent();
        stopAdvert.setComponent(new ComponentName(context, ServiceAdvertise.class));
        context.stopService(stopAdvert);
    }
}
