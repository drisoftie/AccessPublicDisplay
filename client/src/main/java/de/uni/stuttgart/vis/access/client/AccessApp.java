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

import android.app.Application;

import java.util.Objects;

/**
 * Central {@link android.app.Application} class for handling application states. Uses the Singleton Pattern.
 *
 * @author Alexander Dridiger
 */
public class AccessApp extends Application {

    /**
     * Singleton instance.
     */
    private static AccessApp instance;

    //    /*-################
    //     * Database helpers
    //     * ################*/
    //    private SqliteOpenHelper ormLiteSqliteOpenHelper;
    //    @SuppressWarnings("rawtypes")
    //    private DaoProvider      daoProvider;
    //
    //    private DomainHolder domainHolder;
    //
    //    private NdefConstructor ndefConstructor;

    /**
     * Delegate method for {@link #getInstance()}
     */
    public static AccessApp inst() {
        return getInstance();
    }

    /**
     * @return Singleton instance
     */
    public static AccessApp getInstance() {
        return instance;
    }

    public static String string(int id) {
        return inst().getString(id);
    }

    public static String string(int id, Objects... args) {
        return inst().getString(id, new Object[]{args});
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this; // Singleton pattern

        //        OpenHelperManager.setOpenHelperClass(SqliteOpenHelper.class);
        //        ormLiteSqliteOpenHelper = OpenHelperManager.getHelper(this, SqliteOpenHelper.class);
        //        daoProvider = new DaoProvider(ormLiteSqliteOpenHelper.getConnectionSource());
        //
        //        domainHolder = new DomainHolder();
        //        ndefConstructor = new NdefConstructor();
        //+
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        //        OpenHelperManager.releaseHelper();
    }

    /**
     //     * The database helper.
     //     *
     //     * @return
     //     */
    //    public SqliteOpenHelper getSqliteHelper() {
    //        return ormLiteSqliteOpenHelper;
    //    }

    //    /**
    //     * Returns an appropriate {@link Dao} instance for the given {@link Class}.
    //     *
    //     * @param clazz
    //     * @return
    //     */
    //    @SuppressWarnings("unchecked")
    //    public <T> Dao<T, Object> getDao(Class<T> clazz) {
    //        return daoProvider.get(clazz);
    //    }

    //    /**
    //     * Delegate/shortcut method for {@link #getDao(Class)}
    //     *
    //     * @return the {@link Dao}
    //     */
    //    @SuppressWarnings("unchecked")
    //    public static <T> Dao<T, Object> dao(Class<T> clazz) {
    //        return instance.daoProvider.get(clazz);
    //    }

    //    /**
    //     * @return
    //     */
    //    public DomainHolder getDomainHolder() {
    //        return domainHolder;
    //    }
    //
    //    public static DomainHolder domain() {
    //        return instance.getDomainHolder();
    //    }

    //    /**
    //     * @return
    //     */
    //    public boolean isDomainLoaded() {
    //        return domainHolder.isLoaded();
    //    }
    //
    //    /**
    //     * @param entities
    //     */
    //    public void loadDomain(List<NdefEntity> entities) {
    //        domainHolder.provideDomainLoader().loadDomain(entities);
    //    }
    //
    //    /**
    //     * @return
    //     */
    //    public NdefConstructor getNdefConstructor() {
    //        return ndefConstructor;
    //    }
    //
    //    /**
    //     * @return singleton constructor
    //     */
    //    public static NdefConstructor constructor() {
    //        return instance.getNdefConstructor();
    //    }
}
