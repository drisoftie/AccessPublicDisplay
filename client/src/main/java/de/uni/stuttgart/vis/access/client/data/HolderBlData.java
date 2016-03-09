package de.uni.stuttgart.vis.access.client.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Alexander Dridiger
 */
public class HolderBlData {

    private ExecutorService e = Executors.newSingleThreadExecutor();
    private List<IBlDataSubscriber> subs;
    private List<BlData> blDataDb = Collections.synchronizedList(new ArrayList<BlData>());

    private List<IBlDataSubscriber> getSubs() {
        if (subs == null) {
            subs = new ArrayList<>();
        }
        return subs;
    }

    private List<BlData> getBlDataDb() {
        if (blDataDb == null) {
            blDataDb = new ArrayList<>();
        }
        return blDataDb;
    }

    public void subscribeBlData(IBlDataSubscriber sub) {
        getSubs().add(sub);
    }

    private boolean hasData(UUID uuid) {
        for (BlData data : blDataDb) {

        }
        return false;
    }

    public void access(HolderAccess access) {
        e.execute(access);
    }

    public abstract class HolderAccess implements Runnable {

        @Override
        public void run() {
            onRun();
        }

        public abstract void onRun();

        public Iterator<BlData> getData() {
            return getBlDataDb().iterator();
        }

        public boolean addData(BlData data) {
            boolean found = false;
            while (getData().hasNext()) {
                BlData d = getData().next();
                if (d.getAddress().equals(data.getAddress())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                getBlDataDb().add(data);
                for (IBlDataSubscriber s : subs) {
                    s.onBlDataAdded(data);
                }
            }
            return !found;
        }

        public BlData getData(String macAddress) {
            BlData data = null;
            while (getData().hasNext()) {
                BlData d = getData().next();
                if (d.getAddress().equals(macAddress)) {
                    data = d;
                    break;
                }
            }
            return data;
        }
    }
}
