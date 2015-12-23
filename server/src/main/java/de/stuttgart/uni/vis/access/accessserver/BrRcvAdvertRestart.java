package de.stuttgart.uni.vis.access.accessserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Alexander Dridiger
 */
public class BrRcvAdvertRestart extends BroadcastReceiver {

    private IAdvertisementReceiver receiver;

    public BrRcvAdvertRestart(IAdvertisementReceiver receiver) {
        super();
        this.receiver = receiver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (StringUtils.equals(context.getString(R.string.intent_advert_value), intent.getAction())) {
            receiver.onNewAdvertisementString(intent.getStringExtra(context.getString(R.string.bndl_advert_value)));
            receiver.onRestartAdvertisement();
        }
    }
}
