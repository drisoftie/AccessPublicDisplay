package de.uni.stuttgart.vis.access.client.brcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import de.uni.stuttgart.vis.access.client.R;

/**
 * @author Alexander Dridiger
 */
public class BrRcvStop extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //        Intent stopAdvert = new Intent();
        //        stopAdvert.setComponent(new ComponentName(context, ServiceScan.class));
        //        context.stopService(stopAdvert);
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(context.getString(R.string.intent_action_bl_user_stopped)));
    }
}
