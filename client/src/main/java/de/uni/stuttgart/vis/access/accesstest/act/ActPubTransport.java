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

public class ActPubTransport extends AppCompatActivity {

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
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(msgReceiver);
    }

    private class BrdcstReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String   weather = null;
            TextView txt     = null;
            if (intent.hasExtra(getString(R.string.bndl_gatt_pub_transp_bus))) {
                weather = new String(intent.getByteArrayExtra(getString(R.string.bndl_gatt_pub_transp_bus)));
                txt = ((TextView) findViewById(R.id.txt_bus));
            } else if (intent.hasExtra(getString(R.string.bndl_gatt_pub_transp_metro))) {
                weather = new String(intent.getByteArrayExtra(getString(R.string.bndl_gatt_pub_transp_metro)));
                txt = ((TextView) findViewById(R.id.txt_metro));
            } else if (intent.hasExtra(getString(R.string.bndl_gatt_pub_transp_train))) {
                weather = new String(intent.getByteArrayExtra(getString(R.string.bndl_gatt_pub_transp_train)));
                txt = ((TextView) findViewById(R.id.txt_train));
            }
            assert txt != null;
            txt.setText(weather);
        }
    }
}
