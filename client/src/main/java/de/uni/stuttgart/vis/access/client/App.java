/*****************************************************************************
 * Copyright 2012-2013 Sony Corporation
 * <p>
 * The information contained here-in is the property of Sony corporation and
 * is not to be disclosed or used without the prior written permission of
 * Sony corporation. This copyright extends to all media in which this
 * information may be preserved including magnetic storage, computer
 * print-out or visual display.
 * <p>
 * Contains proprietary information, copyright and database rights Sony.
 * Decompilation prohibited save as permitted by law. No using, disclosing,
 * reproducing, accessing or modifying without Sony prior written consent.
 ****************************************************************************/
package de.uni.stuttgart.vis.access.client;

import de.stuttgart.uni.vis.access.common.AppBase;

/**
 * Central {@link android.app.Application} class for handling application states. Uses the Singleton Pattern.
 *
 * @author Alexander Dridiger
 */
public class App extends AppBase {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
