package de.uni.stuttgart.vis.access.client.brcast;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import de.uni.stuttgart.vis.access.client.service.ServiceScan;

/**
 * @author Alexander Dridiger
 */
public class BrRcvStop extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent stopAdvert = new Intent();
        stopAdvert.setComponent(new ComponentName(context, ServiceScan.class));
        context.stopService(stopAdvert);
    }
}
