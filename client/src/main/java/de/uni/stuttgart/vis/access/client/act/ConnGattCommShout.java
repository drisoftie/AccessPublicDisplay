package de.uni.stuttgart.vis.access.client.act;

import java.util.UUID;

import de.uni.stuttgart.vis.access.client.helper.IContextProv;
import de.uni.stuttgart.vis.access.client.helper.ITtsProv;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;

/**
 * @author Alexander Dridiger
 */
public class ConnGattCommShout implements IConnGattProvider.IConnGattSubscriber {

    private IConnGattProvider blConn;
    private IContextProv      provCntxt;
    private IViewProv         provView;
    private ITtsProv          provTts;

    public void setContextProvider(IContextProv prov) {
        this.provCntxt = prov;
    }

    public void setViewProvider(IViewProv prov) {
        this.provView = prov;
    }

    public void setTtsProvider(ITtsProv provTts) {
        this.provTts = provTts;
    }

    public void setConn(IConnGattProvider blConnection) {
        this.blConn = blConnection;
        blConnection.registerConnGattSub(this);
    }

    @Override
    public void onGattReady() {
    }

    @Override
    public void onServicesReady() {
    }

    @Override
    public void onGattValueReceived(byte[] value) {
        provTts.provideTts().queueRead("We have a new sale here!");
        provTts.provideTts().queueRead(new String(value));
    }

    @Override
    public void onGattValueChanged(UUID uuid, byte[] value) {
        provTts.provideTts().queueRead("We have a new sale here!");
        provTts.provideTts().queueRead(new String(value));
    }

    public void onDetach() {
        provCntxt = null;
        blConn.deregisterConnGattSub(this);
    }
}
