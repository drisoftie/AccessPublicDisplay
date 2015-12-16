package de.uni.stuttgart.vis.access.accesstest.brcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import de.uni.stuttgart.vis.access.accesstest.R;

/**
 * @author Alexander Dridiger
 */
public class BrRcvScan extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent showIntent = new Intent(context.getString(R.string.intent_advert_value));
        showIntent.putExtras(intent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(showIntent);
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);
    }
}
