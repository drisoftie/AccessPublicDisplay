package de.uni.stuttgart.vis.access.client.service.bl;

/**
 * @author Alexander Dridiger
 */
public interface IConnMultiPart {

    IConnMulti getConnMulti();

    void setConnMulti(IConnMulti connMulti);

    IConnAdvertScan getAdvertScan();

    boolean hasServicesDiscovered();

    void setServicesDiscovered(boolean discovered);
}
