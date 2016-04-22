package de.uni.stuttgart.vis.access.client.act;

import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.android.AndroidAction;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.domain.ConstantsBooking;
import de.stuttgart.uni.vis.access.common.util.ScheduleUtil;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;

public class ActBooking extends ActGattScan {

    private GattBooking       gattListenBooking;
    private IConnGattProvider gattProviderBooking;

    private ConstantsBooking.StateBooking state = ConstantsBooking.StateBooking.START;
    private ActionGattSetup actionGatt;

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

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.txt_booking).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gattProviderBooking != null) {
                    gattProviderBooking.writeGattCharacteristic(Constants.BOOKING.GATT_SERVICE_BOOKING.getUuid(),
                                                                Constants.BOOKING.GATT_BOOKING_WRITE.getUuid(),
                                                                state.getState().getBytes());
                    switch (state) {
                        case START:
                            break;
                        case PERSONS:
                            state = ConstantsBooking.StateBooking.TIME;
                            findViewById(R.id.edttxt_booking).setVisibility(View.GONE);
                            findViewById(R.id.tmepck_book).setVisibility(View.VISIBLE);
                            break;
                        case TIME:
                            state = ConstantsBooking.StateBooking.DISH;
                            findViewById(R.id.tmepck_book).setVisibility(View.GONE);
                            findViewById(R.id.spin_food).setVisibility(View.VISIBLE);
                            break;
                        case DISH:
                            findViewById(R.id.spin_food).setVisibility(View.GONE);
                            findViewById(R.id.edttxt_booking).setVisibility(View.VISIBLE);
                            ScheduleUtil.scheduleWork(new Runnable() {
                                @Override
                                public void run() {
                                    service.getTtsProvider().provideTts().queueRead("Fertig gebucht! Neuer Tisch kann gebucht werden.");
                                }
                            }, 500, TimeUnit.MILLISECONDS);
                            state = ConstantsBooking.StateBooking.START;
                            break;
                        case FINISH:
                            state = ConstantsBooking.StateBooking.START;
                            break;
                    }
                }
            }
        });

        actionGatt = new ActionGattSetup(null, IGenericAction.class, null);
    }

    @Override
    protected void onResuming() {

    }

    @Override
    protected void onPausing() {

    }

    @Override
    void deregisterGattComponents() {
        gattProviderBooking.deregisterConnGattSub(gattListenBooking);
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
        gattProviderBooking.writeGattCharacteristic(Constants.BOOKING.GATT_SERVICE_BOOKING.getUuid(),
                                                    Constants.BOOKING.GATT_BOOKING_WRITE.getUuid(),
                                                    ConstantsBooking.StateBooking.START.getState().getBytes());
        service.getTtsProvider().provideTts().queueRead(getString(de.uni.stuttgart.vis.access.client.R.string.ntxt_book_table));
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
                switch (state) {
                    case START:
                        ScheduleUtil.scheduleWork(new Runnable() {
                            @Override
                            public void run() {
                                service.getTtsProvider().provideTts().queueRead("Anzahl Personen eingeben");
                            }
                        }, 500, TimeUnit.MILLISECONDS);
                        state = ConstantsBooking.StateBooking.PERSONS;
                        break;
                    case PERSONS:
                        break;
                    case TIME:
                        ScheduleUtil.scheduleWork(new Runnable() {
                            @Override
                            public void run() {
                                service.getTtsProvider().provideTts().queueRead(getString(R.string.info_book_choose_time));
                            }
                        }, 500, TimeUnit.MILLISECONDS);

                        break;
                    case DISH:
                        ScheduleUtil.scheduleWork(new Runnable() {
                            @Override
                            public void run() {
                                service.getTtsProvider().provideTts().queueRead("Essen wählen");
                            }
                        }, 500, TimeUnit.MILLISECONDS);
                        break;
                    case FINISH:
                        break;
                }
                actionGatt.invokeSelf(state);
            }
        }

        @Override
        public void onGattValueChanged(String macAddress, UUID uuid, byte[] value) {
            if (Constants.BOOKING.GATT_BOOKING_NOTIFY.getUuid().equals(uuid)) {

            }
        }
    }

    private class ActionGattSetup extends AndroidAction<View, Void, Void, Void, Void> {

        public ActionGattSetup(View view, Class<?> actionType, String regMethodName) {
            super(view, actionType, regMethodName);
        }

        @Override
        public Object onActionPrepare(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            return null;
        }

        @Override
        public Void onActionDoWork(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            return null;
        }

        @Override
        public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2,
                                      Object[] additionalTags) {
        }
    }
}
