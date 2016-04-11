package de.uni.stuttgart.vis.access.client.helper;

import android.os.Handler;
import android.os.Looper;

/**
 * @author Alexander Dridiger
 */
public class PosterUi {

    public static void postOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}
