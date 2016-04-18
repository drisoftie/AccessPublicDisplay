package de.stuttgart.uni.vis.access.common.util;

/**
 * @author Alexander Dridiger
 */
public abstract class AccessGatt implements Runnable {

    private IGatt gatt;

    public AccessGatt(IGatt gatt) {
        this.gatt = gatt;
    }

    @Override
    public void run() {
        onRun();
        gatt.checkWork();
    }

    public abstract void onRun();

    public interface IGatt {
        void checkWork();
    }
}
