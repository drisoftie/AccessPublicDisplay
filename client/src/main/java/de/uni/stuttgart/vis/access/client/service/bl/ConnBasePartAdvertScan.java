package de.uni.stuttgart.vis.access.client.service.bl;

/**
 * @author Alexander Dridiger
 */
public abstract class ConnBasePartAdvertScan extends ConnBaseAdvertScan implements IConnMultiPart {

    private IConnMulti connMulti;
    private boolean    servicesDiscovered;

    @Override
    public IConnMulti getConnMulti() {
        return connMulti;
    }

    @Override
    public void setConnMulti(IConnMulti connMulti) {
        this.connMulti = connMulti;
    }

    @Override
    public IConnAdvertScan getAdvertScan() {
        return this;
    }

    @Override
    public void setServicesDiscovered(boolean discovered) {
        servicesDiscovered = discovered;
    }

    @Override
    public boolean hasServicesDiscovered() {
        return servicesDiscovered;
    }
}