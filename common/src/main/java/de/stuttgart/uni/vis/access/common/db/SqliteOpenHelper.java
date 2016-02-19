/*****************************************************************************
 *
 * Copyright 2012-2013 Sony Corporation
 *
 * The information contained here-in is the property of Sony corporation and
 * is not to be disclosed or used without the prior written permission of
 * Sony corporation. This copyright extends to all media in which this
 * information may be preserved including magnetic storage, computer
 * print-out or visual display.
 *
 * Contains proprietary information, copyright and database rights Sony.
 * Decompilation prohibited save as permitted by law. No using, disclosing,
 * reproducing, accessing or modifying without Sony prior written consent.
 *
 ****************************************************************************/

package de.stuttgart.uni.vis.access.common.db;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.sony.eu.stc.nfc.R;

/**
 * Central helper class for the SQLite database.
 *
 * @author Alexander Dridiger
 */
public class SqliteOpenHelper extends OrmLiteSqliteOpenHelper {

    private Context context;

    /**
     * Sets the {@link Context} to the helper.
     *
     * @param context the {@link Context} to set
     */
    public SqliteOpenHelper(Context context) {
        super(context, context.getString(R.string.db_name), null, context.getResources().getInteger(R.integer.dbversion));
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            for (String tablename : context.getResources().getStringArray(R.array.dbtables)) {
                TableUtils.createTable(connectionSource, Class.forName(context.getString(R.string.classprefix, tablename)));
            }
        } catch (SQLException e) {
            Log.e("", e.getMessage());
        } catch (java.sql.SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        deleteAll(connectionSource);
    }

    /**
     * Deletes and recreates all available database tables.
     *
     * @param connectionSource the connection
     */
    public void deleteAll(ConnectionSource connectionSource) {
        try {
            for (String tablename : context.getResources().getStringArray(R.array.dbtables)) {
                TableUtils.dropTable(connectionSource, Class.forName(context.getString(R.string.classprefix, tablename)), true);
            }
            for (String tablename : context.getResources().getStringArray(R.array.dbtables)) {
                TableUtils.createTable(connectionSource, Class.forName(context.getString(R.string.classprefix, tablename)));
            }
        } catch (SQLException e) {
            Log.e("", e.getMessage());
        } catch (java.sql.SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
