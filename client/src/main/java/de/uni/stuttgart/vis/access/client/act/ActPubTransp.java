package de.uni.stuttgart.vis.access.client.act;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import com.drisoftie.frags.comp.ManagedActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.uni.stuttgart.vis.access.client.R;

public class ActPubTransp extends ManagedActivity {

    private BroadcastReceiver msgReceiver = new BrdcstReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalBroadcastManager.getInstance(this).registerReceiver(msgReceiver, new IntentFilter(getString(R.string.intent_gatt_pub_transp)));
        setContentView(R.layout.act_pub_transport);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResuming() {

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Runnable task = new Runnable() {

            @Override
            public void run() {
                View current = getCurrentFocus();
                if (current != null) {
                    current.clearFocus();
                }
                findViewById(R.id.txt_headline_bus).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }
        };

        final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();

        worker.schedule(task, 1, TimeUnit.SECONDS);

    }

    @Override
    protected void registerComponents() {
        Intent intent = new Intent(getString(R.string.intent_pub_transp_get));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void deregisterComponents() {

    }

    @Override
    protected void onPausing() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(msgReceiver);
    }

    private class BrdcstReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String   transp = null;
            TextView txt     = null;
            if (intent.hasExtra(getString(R.string.bndl_gatt_pub_transp_bus))) {
                findViewById(R.id.txt_headline_bus).setVisibility(View.GONE);
                transp = getString(R.string.info_pub_transp_bus, new String(intent.getByteArrayExtra(getString(
                        R.string.bndl_gatt_pub_transp_bus))));
                txt = ((TextView) findViewById(R.id.txt_bus));
                findViewById(R.id.txt_bus).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            } else if (intent.hasExtra(getString(R.string.bndl_gatt_pub_transp_metro))) {
                findViewById(R.id.txt_headline_metro).setVisibility(View.GONE);
                transp = getString(R.string.info_pub_transp_metro, new String(intent.getByteArrayExtra(getString(
                        R.string.bndl_gatt_pub_transp_metro))));
                txt = ((TextView) findViewById(R.id.txt_metro));
            } else if (intent.hasExtra(getString(R.string.bndl_gatt_pub_transp_train))) {
                findViewById(R.id.txt_headline_train).setVisibility(View.GONE);
                transp = getString(R.string.info_pub_transp_train, new String(intent.getByteArrayExtra(getString(R.string.bndl_gatt_weather_dat))));
                txt = ((TextView) findViewById(R.id.txt_train));
            }
            assert txt != null;
            txt.setText(transp);
        }
    }
}
