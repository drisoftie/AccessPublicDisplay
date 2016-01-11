package de.stuttgart.uni.vis.access.server.brcast;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import de.stuttgart.uni.vis.access.server.service.ServiceAdvertise;

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
