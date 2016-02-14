package de.uni.stuttgart.vis.access.client.act;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.drisoftie.action.async.ActionMethod;
import com.drisoftie.action.async.IGenericAction;
import com.drisoftie.action.async.RegActionMethod;
import com.drisoftie.action.async.android.AndroidAction;
import com.drisoftie.frags.comp.ManagedActivity;

import java.util.Objects;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.helper.IContextProv;
import de.uni.stuttgart.vis.access.client.helper.ITtsProv;
import de.uni.stuttgart.vis.access.client.helper.TtsWrapper;
import de.uni.stuttgart.vis.access.client.service.IServiceBinder;
import de.uni.stuttgart.vis.access.client.service.IServiceBlListener;
import de.uni.stuttgart.vis.access.client.service.ServiceScan;

public class ActWeather extends ManagedActivity implements ServiceConnection, IServiceBlListener, IContextProv, IViewProv, ITtsProv {

    private BroadcastReceiver msgReceiver = new BrdcstReceiver();

    private IServiceBinder      service;
    private ConnGattCommWeather gattCommWeather;
    private ConnGattCommShout   gattCommShout;
    private ActionGattSetup     actionGattSetup;
    private ITtsProv            ttsProvider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalBroadcastManager.getInstance(this).registerReceiver(msgReceiver, new IntentFilter(getString(R.string.intent_gatt_weather)));
        setContentView(R.layout.act_weather);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionGattSetup = new ActionGattSetup(null, IGenericAction.class, RegActionMethod.NONE.method());


        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResuming() {
    }

    @Override
    protected void registerComponents() {
        Intent intent = new Intent(getString(R.string.intent_weather_get));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        bindService(new Intent(this, ServiceScan.class), this, BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder binder) {
        service = (IServiceBinder) binder;
        service.registerServiceListener(this);
        ttsProvider = service.getTtsProvider();
        actionGattSetup.invokeSelf();
    }

    @Override
    public Context provideContext() {
        return this;
    }

    @Override
    public View provideView(int resId) {
        return findViewById(resId);
    }

    @Override
    public TtsWrapper provideTts() {
        return ttsProvider.provideTts();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        service = null;
    }

    @Override
    public void onConnStopped() {
        // Deactivate updates to us so that we dont get callbacks no more.
        service.deregisterServiceListener(this);
        // Finally stop the service
        unbindService(this);
        service = null;
    }

    @Override
    protected void deregisterComponents() {
        // Deactivate updates to us so that we dont get callbacks no more.
        service.deregisterServiceListener(this);

        unbindService(this);
        gattCommWeather.onDetach();
        gattCommWeather = null;
        gattCommShout.onDetach();
        gattCommShout = null;
        ttsProvider = null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(msgReceiver);
    }

    @Override
    protected void onPausing() {
    }

    private class BrdcstReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
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
            if (ActionMethod.INVOKE_ACTION.matches(methodName)) {
                gattCommWeather = new ConnGattCommWeather();
                gattCommWeather.setContextProvider(ActWeather.this);
                gattCommWeather.setViewProvider(ActWeather.this);
                gattCommWeather.setConn(service.subscribeBlConnection(Constants.GATT_SERVICE_WEATHER.getUuid(), gattCommWeather));

                gattCommShout = new ConnGattCommShout();
                gattCommShout.setContextProvider(ActWeather.this);
                gattCommShout.setViewProvider(ActWeather.this);
                gattCommShout.setTtsProvider(ActWeather.this);
                gattCommShout.setConn(service.subscribeBlConnection(Constants.GATT_SERVICE_SHOUT.getUuid(), gattCommShout));
            }
            return null;
        }

        @Override
        public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2,
                                      Object[] additionalTags) {

        }
    }
}
