package de.uni.stuttgart.vis.access.accesstest.brcast;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import de.uni.stuttgart.vis.access.accesstest.service.ServiceScan;

/**
 * @author Alexander Dridiger
 */
public class BrRcvScan extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent stopAdvert = new Intent();
        stopAdvert.setComponent(new ComponentName(context, ServiceScan.class));
        context.stopService(stopAdvert);
    }
}
