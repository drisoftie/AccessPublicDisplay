package de.stuttgart.uni.vis.access.server.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.drisoftie.action.async.ActionMethod;
import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.RegActionMethod;
import com.drisoftie.action.async.android.AndroidAction;
import com.drisoftie.action.async.handler.IFinishedHandler;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.brcst.BrcstBlAdaptChanged;
import de.stuttgart.uni.vis.access.common.util.ScheduleUtil;
import de.stuttgart.uni.vis.access.server.BuildConfig;
import de.stuttgart.uni.vis.access.server.R;
import de.stuttgart.uni.vis.access.server.act.ActServerAdvertise;
import de.stuttgart.uni.vis.access.server.brcast.BrRcvAdvertisement;
import de.stuttgart.uni.vis.access.server.service.bl.AdvertHandler;
import de.stuttgart.uni.vis.access.server.service.bl.GattServerStateHolder;
import de.stuttgart.uni.vis.access.server.service.bl.IAdvertStartListener;

/**
 * Manages BLE Advertising independent of the main app.
 * If the app goes off screen (or gets killed completely) advertising can continue because this
 * Service is maintaining the necessary Callback in memory.
 */
public class ServiceAdvertise extends Service implements IAdvertStartListener {

    private static final String  TAG           = ServiceAdvertise.class.getSimpleName();
    /**
     * A global variable to let AdvertiserFragment check if the Service is running without needing
     * to start or bind to it.
     * This is the best practice method as defined here:
     * https://groups.google.com/forum/#!topic/android-developers/jEvXMWgbgzE
     */
    public static        boolean running       = false;

    private final        Binder  serviceBinder = new ServiceBinder();
    private BluetoothManager      blManager;
    private BluetoothLeAdvertiser blLeAdvertiser;
    private List<IServiceBlListener> serviceListeners = new ArrayList<>();

    private AdvertHandler            blAdvertHandler;
    private GattServerStateHolder    blGattServerHolder;
    private Handler                  timeoutHandler;
    private Runnable                 timeoutRunnable;
    private ScheduledExecutorService worker;

    private ActionServiceSetup actionServiceSetup;

    /**
     * Length of time to allow advertising before automatically shutting off. (10 minutes)
     */
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);

    private BroadcastReceiver brcstRcvrBlAdapt = new BrcstBlAdaptChanged();

    private ActionUserShutdown actionUserShutdown;

    private BroadcastReceiver msgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StringUtils.equals(intent.getAction(), getString(R.string.intent_action_bl_user_stopped)) || StringUtils.equals(
                    intent.getAction(), getString(R.string.intent_bl_stopped))) {
                for (IServiceBlListener l : serviceListeners) {
                    l.onConnStopped();
                }
                stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        Runnable task = new Runnable() {

            @Override
            public void run() {
                actionServiceSetup = new ActionServiceSetup(null, new Class[]{IGenericAction.class, IFinishedHandler.class}, null);
                actionServiceSetup.invokeSelf();
            }
        };
        actionUserShutdown = new ActionUserShutdown(null, IGenericAction.class, RegActionMethod.NONE.method());
        timeoutHandler = new Handler();
        worker = ScheduleUtil.scheduleWork(task, 1, TimeUnit.SECONDS);
    }

    /**
     * Required for extending service, but this will be a Started Service only, so no need for
     * binding.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     * Get references to system Bluetooth objects if we don't have them already.
     */
    private void initialize() {
        if (blLeAdvertiser == null) {
            blManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (blManager != null) {
                BluetoothAdapter blAdapt = blManager.getAdapter();
                if (blAdapt != null) {
                    blLeAdvertiser = blAdapt.getBluetoothLeAdvertiser();
                } else {
                    Toast.makeText(this, getString(R.string.bt_null), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, getString(R.string.bt_null), Toast.LENGTH_LONG).show();
            }
        }

    }

    /**
     * Starts a delayed Runnable that will cause the BLE Advertising to timeout and stop after a
     * set amount of time.
     */
    private void setTimeout() {
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "ServiceAdvertise has reached timeout of " + TIMEOUT + " milliseconds, stopping advertising.");
                sendFailureIntent(Constants.ADVERTISING_TIMED_OUT);
                stopSelf();
                for (IServiceBlListener l : serviceListeners) {
                    l.onConnStopped();
                }
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }

    /**
     * Starts BLE Advertising.
     */
    private void startAdvertising() {
        Log.d(TAG, "Service: Starting Advertising");
        if (blAdvertHandler == null) {
            blAdvertHandler = new AdvertHandler(this);
            AdvertiseSettings settings = blAdvertHandler.buildAdvertiseSettings();
            AdvertiseData dataWeather = blAdvertHandler.buildAdvertiseDataWeather();
            if (blLeAdvertiser != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Advertisement ready");
                }
                blLeAdvertiser.startAdvertising(settings, dataWeather, blAdvertHandler);
                createStartNotification();
            }
        }
    }

    @Override
    public Context getCntxt() {
        return this;
    }

    @Override
    public void onStartingSuccess() {
    }

    @Override
    public void onStartingFailed(int code) {
        stopSelf();
    }

    private void createStartNotification() {
        // start notification
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
        nBuilder.setSmallIcon(R.drawable.ic_action_bl_advert);
        nBuilder.setContentTitle(getString(R.string.ntxt_advert_run));

        nBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getString(Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()) +
                                                                        System.lineSeparator() + getString(
                Constants.AdvertiseConst.ADVERTISE_TRANSP.getDescr()) +
                                                                        System.lineSeparator() + getString(
                Constants.AdvertiseConst.ADVERTISE_SHOUT.getDescr()) +
                                                                        System.lineSeparator() +
                                                                        getString(Constants.AdvertiseConst.ADVERTISE_BOOKING.getDescr())));

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, ActServerAdvertise.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ActServerAdvertise.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(R.id.nid_main, PendingIntent.FLAG_UPDATE_CURRENT);
        nBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.

        Intent stopAdvertIntent = new Intent(this, BrRcvAdvertisement.class);
        // PendingIntent contentIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent contentIntent = PendingIntent.getBroadcast(this, 0, stopAdvertIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        nBuilder.addAction(R.drawable.ic_action_remove, getString(R.string.nact_stop), contentIntent);

        nBuilder.setAutoCancel(false);

        Notification n = nBuilder.build();
        mNotificationManager.notify(R.id.nid_main, n);
        startForeground(R.id.nid_main, n);
    }

    private void removeNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(R.id.nid_main);
    }

    /**
     * Stops BLE Advertising.
     */
    private void stopAdvertising() {
        Log.d(TAG, "Service: Stopping Advertising");
        if (blLeAdvertiser != null) {
            if (blAdvertHandler != null) {
                blLeAdvertiser.stopAdvertising(blAdvertHandler);
            }
            blAdvertHandler = null;
            removeNotification();
        }
    }

    /**
     * Builds and sends a broadcast intent indicating Advertising has failed. Includes the error
     * code as an extra. This is intended to be picked up by the {@code AdvertiserFragment}.
     */
    private void sendFailureIntent(int errorCode) {
        Intent failureIntent = new Intent();
        failureIntent.setAction(Constants.ADVERTISING_FAILED);
        failureIntent.putExtra(Constants.ADVERTISING_FAILED_EXTRA_CODE, errorCode);
        sendBroadcast(failureIntent);
    }

    private void startGattServers() {
        blGattServerHolder = new GattServerStateHolder();
        //noinspection unchecked
        blGattServerHolder.startGatt(blManager, actionServiceSetup.getHandlerImpl(IFinishedHandler.class));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        Intent notify = new Intent(getString(R.string.intent_bl_stopped));
        LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(msgReceiver);

        unregisterReceiver(brcstRcvrBlAdapt);

        msgReceiver = null;
    }

    public class ServiceBinder extends Binder implements IServiceBinder {

        @Override
        public void registerServiceListener(IServiceBlListener listener) {
            serviceListeners.add(listener);
        }

        @Override
        public void deregisterServiceListener(IServiceBlListener listener) {
            serviceListeners.remove(listener);
        }

        @Override
        public boolean isConnected(IServiceBlListener listener) {
            return serviceListeners.contains(listener);
        }

        @Override
        public void onBlUserShutdown() {
            actionUserShutdown.invokeSelf();
        }
    }

    private class ActionUserShutdown extends AndroidAction<View, Void, Void, Void, Void> {

        public ActionUserShutdown(View view, Class<?> actionType, String regMethodName) {
            super(view, actionType, regMethodName);
        }

        @Override
        public Object onActionPrepare(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            return null;
        }

        @Override
        public Void onActionDoWork(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            worker.shutdown();
            stopAdvertising();
            blGattServerHolder.closeServer();
            if (timeoutHandler != null) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
            }
            return null;
        }

        @Override
        public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2,
                                      Object[] additionalTags) {
            for (IServiceBlListener l : serviceListeners) {
                l.onBlUserShutdownCompleted();
            }
        }
    }

    private class ActionServiceSetup extends AndroidAction<View, Void, Void, Void, Void> {

        public ActionServiceSetup(View view, Class<?>[] actionTypes, String regMethodName) {
            super(view, actionTypes, regMethodName);
        }

        @Override
        public Object onActionPrepare(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            return null;
        }

        @Override
        public Void onActionDoWork(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
            if (ActionMethod.INVOKE_ACTION.matches(methodName)) {
                if (ArrayUtils.isEmpty(stripMethodArgs(methodArgs))) {
                    initialize();
                    //noinspection unchecked
                    startGattServers();
                }
            } else if (StringUtils.equals(methodName, "onFinished")) {
                startAdvertising();
                setTimeout();
                IntentFilter filter = new IntentFilter();
                filter.addAction(getString(R.string.intent_action_bl_user_stopped));
                filter.addAction(getString(R.string.intent_advert_value));
                LocalBroadcastManager.getInstance(ServiceAdvertise.this).registerReceiver(msgReceiver, filter);
                registerReceiver(brcstRcvrBlAdapt, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

                for (IServiceBlListener l : serviceListeners) {
                    l.onBlStarted();
                }
            }
            return null;
        }

        @Override
        public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2,
                                      Object[] additionalTags) {
        }
    }
}