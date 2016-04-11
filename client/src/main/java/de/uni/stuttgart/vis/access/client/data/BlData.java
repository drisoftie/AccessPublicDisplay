package de.uni.stuttgart.vis.access.client.data;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Alexander Dridiger
 */
public class BlData extends ClientEntityBase {

    @DatabaseField
    private boolean active;

    @DatabaseField
    private String advertUuid;

    @DatabaseField
    private String address;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] advertisement;

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private String[] uuids;

    @DatabaseField
    private int rssi;

    @ForeignCollectionField(eager = true)
    private Collection<GattData> gattData;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getAdvertUuid() {
        return advertUuid;
    }

    public void setAdvertUuid(String advertUuid) {
        this.advertUuid = advertUuid;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public byte[] getAdvertisement() {
        return advertisement;
    }

    public void setAdvertisement(byte[] advertisement) {
        this.advertisement = advertisement;
    }

    public String[] getUuids() {
        return uuids;
    }

    public void setUuids(String[] uuids) {
        this.uuids = uuids;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public Collection<GattData> getGattData() {
        if (gattData == null) {
            gattData = new ArrayList<>();
        }
        return gattData;
    }

    public void setGattData(Collection<GattData> gattData) {
        this.gattData = gattData;
    }
}
