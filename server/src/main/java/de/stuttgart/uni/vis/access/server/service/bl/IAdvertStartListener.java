package de.stuttgart.uni.vis.access.server.service.bl;

import android.content.Context;

/**
 * @author Alexander Dridiger
 */
public interface IAdvertStartListener {

    Context getCntxt();

    void onStartingFailed(int code);

    void onStartingSuccess();
}
