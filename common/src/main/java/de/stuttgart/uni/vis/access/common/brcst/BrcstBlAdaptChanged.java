package de.stuttgart.uni.vis.access.common.brcst;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import de.stuttgart.uni.vis.access.common.R;

/**
 * @author Alexander Dridiger
 */
public class BrcstBlAdaptChanged extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        String           action    = intent.getAction();

        // It means the user has changed his bluetooth state.
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

            //noinspection ResourceType
            if (btAdapter.getState() == BluetoothAdapter.STATE_TURNING_OFF) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(
                        new Intent(context.getString(R.string.intent_action_bl_user_changing)));
                return;
            }

            //noinspection ResourceType
            if (btAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(
                        new Intent(context.getString(R.string.intent_action_bl_user_changing)));
            }
        }
    }
}
