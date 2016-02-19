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
package de.stuttgart.uni.vis.access.common.domain;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.Dao.CreateOrUpdateStatus;
import com.j256.ormlite.field.DatabaseField;

import java.sql.SQLException;

import de.stuttgart.uni.vis.access.common.R;

/**
 * Basic persistance class providing an ID for the database. Used as parent for all other entities. <br>
 * Provides means to access the database in easy ways.
 *
 * @author Alexander Dridiger
 */
public abstract class BaseEntity {

    @DatabaseField(generatedId = true)
    private Integer id;

    /**
     * @return the entity id in the database
     */
    public Integer getId() {
        return id;
    }

    public abstract <T> Dao<T, Object> dao(Class<T> clazz);

    public abstract Context cntxt();

    /**
     * Creates a new database entry with the data provided by the {@code entity} argument inside the database table associated with the
     * {@code clazz} argument.
     *
     * @param clazz  {@link Class} associated with a database table
     * @param entity data to be created
     * @return returns the dao for the provided {@code clazz}
     */
    public <DatabaseTableClass extends BaseEntity> Dao<DatabaseTableClass, ?> createEntity(Class<DatabaseTableClass> clazz,
                                                                                           DatabaseTableClass entity) {
        Dao<DatabaseTableClass, ?> dao = dao(clazz);
        create(dao, entity);
        return dao;
    }

    /**
     * Creates a new database entry with the data provided by {@code this} inside the database table associated with the {@code clazz}
     * argument.
     *
     * @param clazz indicator of the class
     */
    public <DatabaseTableClass extends BaseEntity> void createSelf(Class<DatabaseTableClass> clazz) {
        @SuppressWarnings("unchecked") DatabaseTableClass self = (DatabaseTableClass) this;
        Dao<DatabaseTableClass, ?>                        dao  = dao(clazz);
        create(dao, self);
    }

    /**
     * Updates data provided by the {@code entity} argument inside the database table associated with the {@code clazz} argument.
     *
     * @param clazz  {@link Class} associated with a database table
     * @param entity data to be updated
     * @return returns the dao for the provided {@code clazz}
     */
    public <DatabaseTableClass extends BaseEntity> Dao<DatabaseTableClass, ?> updateEntity(Class<DatabaseTableClass> clazz,
                                                                                           DatabaseTableClass entity) {
        Dao<DatabaseTableClass, ?> dao = dao(clazz);
        update(dao, entity);
        return dao;
    }

    /**
     * Creates or updates data provided by the {@code this} inside the database table associated with the {@code clazz} argument.
     *
     * @param clazz {@link Class} associated with a database table
     */
    public <DatabaseTableClass extends BaseEntity> void createOrUpdateSelf(Class<DatabaseTableClass> clazz) {
        // if (clazz.equals(CecCluster.class)) {
        // CecCluster self = (CecCluster) this;
        // Dao<CecCluster, ?> dao = NcApplication.dao(CecCluster.class);
        //
        // self.setIdentifier(self.getName());
        // self.setUuid(self.getUuid());
        //
        // createOrUpdate(dao, self);
        //
        // for (CecDevice device : self.getDevices()) {
        // device.createOrUpdateSelf(CecDevice.class);
        // }
        // } else if (clazz.equals(CecDevice.class)) {
        // CecDevice self = (CecDevice) this;
        // Dao<CecDevice, ?> dao = NcApplication.dao(CecDevice.class);
        //
        // CreateOrUpdateStatus status = createOrUpdate(dao, self);
        //
        // if (status.isCreated()) {
        // update(NcApplication.dao(CecCluster.class), self.getParentCluster());
        // }
        // } else if (clazz.equals(UpnpDevice.class)) {
        // UpnpDevice self = (UpnpDevice) this;
        // Dao<UpnpDevice, ?> dao = NcApplication.dao(UpnpDevice.class);
        // createOrUpdate(dao, self);
        // } else {
        // @SuppressWarnings("unchecked")
        // DatabaseTableClass self = (DatabaseTableClass) this;
        // Dao<DatabaseTableClass, ?> dao = NcApplication.dao(clazz);
        // createOrUpdate(dao, self);
        // }
    }

    /**
     * Refreshes {@code this} object with data from the database. The data is retrieved from the database table associated with the
     * {@code clazz} argument and the appropriate {@code id} inside {@code this} object.
     *
     * @param clazz {@link Class} associated with a database table
     */
    public <DatabaseTableClass extends BaseEntity> void refreshSelf(Class<DatabaseTableClass> clazz) {
        @SuppressWarnings("unchecked") DatabaseTableClass self = (DatabaseTableClass) this;
        if (self.getId() != null) {
            Dao<DatabaseTableClass, ?> dao = dao(clazz);
            refresh(dao, self);
        }
    }

    /**
     * Deletes {@code this} from the database and all data it's related to.
     *
     * @param clazz {@link Class} associated with a database table
     */
    public <DatabaseTableClass extends BaseEntity> void deleteEntity(Class<DatabaseTableClass> clazz, DatabaseTableClass entity) {
        Dao<DatabaseTableClass, Object> dao = dao(clazz);
        try {
            if (entity.getId() == null || !dao.idExists(entity.getId())) {
                return;
            }
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        // if (clazz.equals(CecCluster.class)) {
        // CecCluster cluster = (CecCluster) entity;
        // for (int i = 0; i < cluster.getDevices().size(); i++) {
        // cluster.getDevices().remove(0);
        // }
        // delete(dao, entity);
        // } else if (clazz.equals(CecDevice.class)) {
        // CecDevice device = (CecDevice) entity;
        // CecCluster cluster = device.getParentCluster();
        // device.setParentCluster(null);
        // cluster.getDevices().remove(device);
        // delete(dao, entity);
        // device.updateEntity(CecCluster.class, device.getParentCluster());
        // } else {
        // delete(dao, entity);
        // }
    }

    /**
     * Deletes {@code this} from the database and all data it's related to.
     *
     * @param clazz {@link Class} associated with a database table
     */
    public <DatabaseTableClass extends BaseEntity> void deleteSelf(Class<DatabaseTableClass> clazz) throws SQLException {
        // if (clazz.equals(CecCluster.class)) {
        // CecCluster self = (CecCluster) this;
        // Dao<CecCluster, ?> dao = NcApplication.dao(CecCluster.class);
        // for (CecDevice device : self.getDevices()) {
        // try {
        // device.deleteSelf(CecDevice.class);
        // } catch (SQLException e) {
        // e.printStackTrace();
        // }
        // }
        // delete(dao, self);
        // } else if (clazz.equals(CecDevice.class)) {
        // CecDevice self = (CecDevice) this;
        // delete(NcApplication.dao(CecDevice.class), self);
        // } else {
        // @SuppressWarnings("unchecked")
        // DatabaseTableClass self = (DatabaseTableClass) this;
        // delete(NcApplication.dao(clazz), self);
        // }
    }

    private <T extends BaseEntity> void create(Dao<T, ?> dao, T entity) {
        try {
            Log.v(cntxt().getString(R.string.log_ormlite), cntxt().getString(R.string.log_ormlite_creating, this.getClass().getName()));
            int rows = dao.create(entity);
            Log.v(cntxt().getString(R.string.log_ormlite), cntxt().getResources().getQuantityString(
                    R.plurals.log_ormlite_number_of_rows_created, rows, rows));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private <T extends BaseEntity> void update(Dao<T, ?> dao, T entity) {
        try {
            Log.v(cntxt().getString(R.string.log_ormlite), cntxt().getString(R.string.log_ormlite_updating, this.getClass().getName()));
            int rows = dao.update(entity);
            Log.v(cntxt().getString(R.string.log_ormlite), cntxt().getString(R.string.log_ormlite_updated, rows));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private <T extends BaseEntity> void refresh(Dao<T, ?> dao, T entity) {
        // Context c = NdefApp.inst();
        try {
            // Log.v(c.getString(R.string.nc_log_ormlite), c.getString(R.string.nc_log_ormlite_refreshing, this.getClass().getName()));
            dao.refresh(entity);
            // Log.v(c.getString(R.string.nc_log_ormlite), c.getString(R.string.nc_log_ormlite_refreshed, this.getClass().getName()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private <T extends BaseEntity> void delete(Dao<T, ?> dao, T entity) {
        try {
            Log.v(cntxt().getString(R.string.log_ormlite), cntxt().getString(R.string.log_ormlite_deleting, this.getClass().getName()));
            int rows = dao.delete(entity);
            Log.v(cntxt().getString(R.string.log_ormlite), cntxt().getResources().getQuantityString(
                    R.plurals.log_ormlite_number_of_rows_deleted, rows, rows));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private <T extends BaseEntity> CreateOrUpdateStatus createOrUpdate(Dao<T, ?> dao, T entity) {
        CreateOrUpdateStatus status = null;
        Context              c      = cntxt();
        try {
            Log.v(c.getString(R.string.log_ormlite), c.getString(R.string.log_ormlite_creating_updating, this.getClass().getName()));
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
}
