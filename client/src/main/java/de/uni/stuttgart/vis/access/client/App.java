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

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;

import de.stuttgart.uni.vis.access.common.AppBase;
import de.uni.stuttgart.vis.access.client.data.HolderBlData;
import de.uni.stuttgart.vis.access.client.db.SqliteOpenHelper;

/**
 * Central {@link android.app.Application} class for handling application states. Uses the Singleton Pattern.
 *
 * @author Alexander Dridiger
 */
public class App extends AppBase {

    private static App inst;

    private HolderBlData holder;

    public static App inst() {
        return inst.getInst();
    }

    public static HolderBlData holder() {
        return inst().holder;
    }

    public App getInst() {
        return inst;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        inst = this;

        holder = new HolderBlData();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }


    @Override
    public Class<? extends OrmLiteSqliteOpenHelper> getSqliteOpenHelperClass() {
        return SqliteOpenHelper.class;
    }

    public HolderBlData getHolder() {
        return holder;
    }
}
