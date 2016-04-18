package de.uni.stuttgart.vis.access.client.act;

import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.List;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.domain.ConstantsBooking;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;

public class ActBooking extends ActGattScan {

    private GattBooking       gattListenBooking;
    private IConnGattProvider gattProviderBooking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_booking);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //        fab.setOnClickListener(new View.OnClickListener() {
        //            @Override
        //            public void onClick(View view) {
        //                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        //            }
        //        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.txt_booking).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gattProviderBooking.writeGattCharacteristic(Constants.BOOKING.GATT_SERVICE_BOOKING.getUuid(),
                                                            Constants.BOOKING.GATT_BOOKING_WRITE.getUuid(),
                                                            ConstantsBooking.StateBooking.START.getState().getBytes());
            }
        });
    }

    @Override
    protected void onResuming() {

    }

    @Override
    protected void onPausing() {

    }

    @Override
    public void onScanResultReceived(ScanResult result) {

    }

    @Override
    public void onScanResultsReceived(List<ScanResult> results) {

    }

    @Override
    public void onRefreshedScanReceived(ScanResult result) {

    }

    @Override
    public void onRefreshedScansReceived(List<ScanResult> results) {

    }

    @Override
    public void onScanLost(ScanResult lostResult) {

    }

    @Override
    public void onScanFailed(int errorCode) {

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        super.onServiceConnected(name, binder);
        if (gattListenBooking == null) {
            gattProviderBooking = service.subscribeGattConnection(Constants.BOOKING.GATT_SERVICE_BOOKING.getUuid(),
                                                                  gattListenBooking = new GattBooking());
        }
        service.getTtsProvider().provideTts().queueRead("Book a table!");
    }

    private class GattBooking extends GattSub {

        @Override
        public void onServicesReady(String macAddress) {
            gattProviderBooking.registerConnGattSub(gattListenBooking);
        }

        @Override
        public void onGattValueReceived(String macAddress, UUID uuid, byte[] value) {
            super.onGattValueReceived(macAddress, uuid, value);
        }

        @Override
        public void onGattValueWriteReceived(String macAddress, UUID uuid, byte[] value) {
            if (Constants.BOOKING.GATT_BOOKING_WRITE.getUuid().equals(uuid)) {
                //                updateHolderData(macAddress, uuid, value);
//                gattProviderBooking.getGattCharacteristicRead(Constants.BOOKING.GATT_SERVICE_BOOKING.getUuid(),
//                                                              Constants.BOOKING.GATT_BOOKING_WRITE.getUuid());
            }
        }

        @Override
        public void onGattValueChanged(String macAddress, UUID uuid, byte[] value) {
            if (Constants.BOOKING.GATT_BOOKING_NOTIFY.getUuid().equals(uuid)) {
                service.getTtsProvider().provideTts().queueRead("Choose Time!!");

            }
        }
    }
}
