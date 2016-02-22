/*****************************************************************************
 * Copyright 2012-2013 Sony Corporation
 * <p/>
 * The information contained here-in is the property of Sony corporation and
 * is not to be disclosed or used without the prior written permission of
 * Sony corporation. This copyright extends to all media in which this
 * information may be preserved including magnetic storage, computer
 * print-out or visual display.
 * <p/>
 * Contains proprietary information, copyright and database rights Sony.
 * Decompilation prohibited save as permitted by law. No using, disclosing,
 * reproducing, accessing or modifying without Sony prior written consent.
 ****************************************************************************/
package de.stuttgart.uni.vis.access.common.db;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.Dao.CreateOrUpdateStatus;

import java.sql.SQLException;

import de.stuttgart.uni.vis.access.common.R;
import de.stuttgart.uni.vis.access.common.domain.BaseEntity;

/**
 * Saving, updating, deleting domain model instances.
 *
 * @author Alexander Dridiger
 */
public class DomainHelper {

    /**
     * Creates or updates data provided by the {@code this} inside the database table associated with the {@code clazz} argument.
     *
     * @param clazz  {@link Class} associated with a database table
     * @param entity the entity to create or update
     */
    public static synchronized <DatabaseT extends BaseEntity> void createOrUpdate(Class<DatabaseT> clazz, DatabaseT entity) {
        //        if (clazz.equals(BaseEntity.class)) {
        //            Dao<NdefEntity, ?> dao = NdefApp.dao(BaseEntity.class);
        //            NdefEntity ndef = (NdefEntity) entity;
        //            createOrUpdate(dao, ndef);
        //            for (NdefSignature sig : ndef.getSignatures()) {
        //                createOrUpdate(NdefSignature.class, sig);
        //            }
        //        } else {
        //            Dao<DatabaseT, ?> dao = NdefApp.dao(clazz);
        //            createOrUpdate(dao, entity);
        //        }
    }

    private static <DatabaseT extends BaseEntity> CreateOrUpdateStatus createOrUpdate(Context c, Dao<DatabaseT, ?> dao, DatabaseT entity) {
        CreateOrUpdateStatus status = null;
        try {
            Log.v(c.getString(R.string.log_ormlite), c.getString(R.string.log_ormlite_creating_updating, entity.getClass().getName()));
            status = dao.createOrUpdate(entity);
            if (status.isCreated()) {
                Log.v(c.getString(R.string.log_ormlite), c.getResources().getQuantityString(R.plurals.log_ormlite_number_of_rows_created,
                                                                                            status.getNumLinesChanged(),
                                                                                            status.getNumLinesChanged()));
            } else if (status.isUpdated()) {
                Log.v(c.getString(R.string.log_ormlite), c.getString(R.string.log_ormlite_updated, status.getNumLinesChanged()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return status;
    }

    public static synchronized <DatabaseT extends BaseEntity> void deleteEntity(Class<DatabaseT> clazz, DatabaseT entity) {
        //        if (clazz.equals(NdefEntity.class)) {
        //            Dao<NdefEntity, ?> dao = NdefApp.dao(NdefEntity.class);
        //            NdefEntity ndef = (NdefEntity) entity;
        //            deleteEntity(dao, ndef);
        //        } else {
        //            Dao<DatabaseT, ?> dao = NdefApp.dao(clazz);
        //            deleteEntity(dao, entity);
        //        }
    }

    public static <DatabaseT extends BaseEntity> void deleteCascade(Class<DatabaseT> clazz, DatabaseT entity) {
        //        if (clazz.equals(NdefEntity.class)) {
        //            Dao<NdefEntity, ?> dao = NdefApp.dao(NdefEntity.class);
        //            NdefEntity ndef = (NdefEntity) entity;
        //            deleteEntity(dao, ndef);
        //            for (NdefSignature sig : ndef.getSignatures()) {
        //                deleteEntity(NdefSignature.class, sig);
        //            }
        //        } else {
        //            Dao<DatabaseT, ?> dao = NdefApp.dao(clazz);
        //            deleteEntity(dao, entity);
        //        }
    }

    private static <DatabaseT extends BaseEntity> void deleteEntity(Dao<DatabaseT, ?> dao, DatabaseT entity) {
        //        Context c = NdefApp.inst();
        //        try {
        //            Log.v(c.getString(R.string.log_ormlite), c.getString(R.string.log_ormlite_deleting, entity.getClass().getName()));
        //            int rows = dao.delete(entity);
        //            Log.v(
        //                    c.getString(R.string.log_ormlite), c.getResources()
        //                                                        .getQuantityString(R.plurals.log_ormlite_number_of_rows_deleted, rows, rows));
        //        } catch (SQLException e) {
        //            e.printStackTrace();
        //        }
    }
}
