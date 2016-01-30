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

public class ActWeather extends ManagedActivity {

    private BroadcastReceiver msgReceiver = new BrdcstReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalBroadcastManager.getInstance(this).registerReceiver(msgReceiver, new IntentFilter(getString(R.string.intent_gatt_weather)));
        setContentView(R.layout.act_weather);
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
                findViewById(R.id.txt_headline_today).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }
        };

        final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();

        worker.schedule(task, 1, TimeUnit.SECONDS);

    }

    @Override
    protected void registerComponents() {
        Intent intent = new Intent(getString(R.string.intent_weather_get));
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
            String   weather = null;
            TextView txt     = null;
            if (intent.hasExtra(getString(R.string.bndl_gatt_weather_today))) {
                findViewById(R.id.txt_headline_today).setVisibility(View.GONE);
                weather = getString(R.string.info_weather_today, new String(intent.getByteArrayExtra(getString(
                        R.string.bndl_gatt_weather_today))));
                txt = ((TextView) findViewById(R.id.txt_weather_today));
                txt.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            } else if (intent.hasExtra(getString(R.string.bndl_gatt_weather_tomorrow))) {
                findViewById(R.id.txt_headline_tomorrow).setVisibility(View.GONE);
                weather = getString(R.string.info_weather_tomorrow, new String(intent.getByteArrayExtra(getString(
                        R.string.bndl_gatt_weather_tomorrow))));
                txt = ((TextView) findViewById(R.id.txt_weather_tomorrow));
            } else if (intent.hasExtra(getString(R.string.bndl_gatt_weather_dat))) {
                findViewById(R.id.txt_headline_dat).setVisibility(View.GONE);
                weather = getString(R.string.info_weather_dat, new String(intent.getByteArrayExtra(getString(R.string.bndl_gatt_weather_dat))));
                txt = ((TextView) findViewById(R.id.txt_weather_dat));
            }
            assert txt != null;
            txt.setText(weather);
        }
    }
}
