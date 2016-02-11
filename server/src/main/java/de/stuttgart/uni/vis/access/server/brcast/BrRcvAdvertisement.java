package de.stuttgart.uni.vis.access.server.brcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import de.stuttgart.uni.vis.access.server.R;

/**
 * @author Alexander Dridiger
 */
public class BrRcvAdvertisement extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(context.getString(R.string.intent_action_bl_user_stopped)));
    }
}
