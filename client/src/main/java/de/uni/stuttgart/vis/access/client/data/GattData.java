package de.uni.stuttgart.vis.access.client.data;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

import java.util.UUID;

/**
 * @author Alexander Dridiger
 */
public class GattData extends ClientEntityBase {

    @DatabaseField(dataType = DataType.UUID)
    private UUID uuid;
    private byte[] data;

    public GattData() {
    }

    public GattData(UUID uuid, byte[] data) {
        this.uuid = uuid;
        this.data = data;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
