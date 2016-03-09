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
package de.stuttgart.uni.vis.access.common;

import android.app.Application;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;

import java.util.Objects;

import de.stuttgart.uni.vis.access.common.db.DaoProvider;

/**
 * Central {@link android.app.Application} class for handling application states. Uses the Singleton Pattern.
 *
 * @author Alexander Dridiger
 */
public abstract class AppBase extends Application {

    /**
     * Singleton instance.
     */
    private static AppBase instance;

    /*-################
     * Database helpers
     * ################*/
    private OrmLiteSqliteOpenHelper ormLiteSqliteOpenHelper;

    @SuppressWarnings("rawtypes")
    private DaoProvider daoProvider;

    /**
     * Delegate method for {@link #getInstance()}
     */
    public static AppBase inst() {
        return getInstance();
    }

    /**
     * @return Singleton instance
     */
    public static AppBase getInstance() {
        return instance;
    }

    /**
     * Delegate method for {@link #inst()#getString}
     */
    public static String string(int id) {
        return inst().getString(id);
    }

    /**
     * Delegate method for {@link #inst()#getString}
     */
    public static String string(int id, Objects... args) {
        return inst().getString(id, new Object[]{args});
    }

    /**
     * Delegate/shortcut method for {@link #getDao(Class)}
     *
     * @return the {@link Dao}
     */
    @SuppressWarnings("unchecked")
    public static <T> Dao<T, Object> dao(Class<T> clazz) {
        return instance.daoProvider.get(clazz);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this; // Singleton pattern

        OpenHelperManager.setOpenHelperClass(getSqliteOpenHelperClass());
        ormLiteSqliteOpenHelper = OpenHelperManager.getHelper(this, getSqliteOpenHelperClass());
        daoProvider = new DaoProvider(ormLiteSqliteOpenHelper.getConnectionSource());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        OpenHelperManager.releaseHelper();
    }


    public abstract Class<? extends OrmLiteSqliteOpenHelper> getSqliteOpenHelperClass();

    /**
     * The database helper.
     *
     * @return the ormlite helper
     */
    public OrmLiteSqliteOpenHelper getSqliteHelper() {
        return ormLiteSqliteOpenHelper;
    }

    /**
     * Returns an appropriate {@link Dao} instance for the given {@link Class}.
     *
     * @param clazz class to get
     * @return returns the appropriate {@link Dao}
     */
    @SuppressWarnings("unchecked")
    public <T> Dao<T, Object> getDao(Class<T> clazz) {
        return daoProvider.get(clazz);
    }
}
