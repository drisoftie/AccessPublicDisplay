package de.uni.stuttgart.vis.access.accesstest.act;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import de.uni.stuttgart.vis.access.accesstest.R;

public class ActWeather extends AppCompatActivity {

    private BroadcastReceiver msgReceiver = new BrdcstReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalBroadcastManager.getInstance(this).registerReceiver(msgReceiver, new IntentFilter(getString(R.string.intent_gatt_weather)));
        setContentView(R.layout.act_weather);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        init();
    }

    private void init() {

    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(msgReceiver);
    }

    private class BrdcstReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String   weather = null;
            TextView txt     = null;
            if (intent.hasExtra(getString(R.string.bndl_gatt_weather_today))) {
                weather = new String(intent.getByteArrayExtra(getString(R.string.bndl_gatt_weather_today)));
                txt = ((TextView) findViewById(R.id.txt_weather_today));
            } else if (intent.hasExtra(getString(R.string.bndl_gatt_weather_tomorrow))) {
                weather = new String(intent.getByteArrayExtra(getString(R.string.bndl_gatt_weather_tomorrow)));
                txt = ((TextView) findViewById(R.id.txt_weather_tomorrow));
            } else if (intent.hasExtra(getString(R.string.bndl_gatt_weather_dat))) {
                weather = new String(intent.getByteArrayExtra(getString(R.string.bndl_gatt_weather_dat)));
                txt = ((TextView) findViewById(R.id.txt_weather_dat));
            }
            assert txt != null;
            txt.setText(weather);
        }
    }
}
