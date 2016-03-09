package de.uni.stuttgart.vis.access.client.data;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

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
}
