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

import android.database.SQLException;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;

import java.util.HashMap;
import java.util.Map;

import de.stuttgart.uni.vis.access.common.domain.BaseEntity;

/**
 * Provides {@link Dao}s for certain {@link Class}es.
 * 
 * @author Alexander Dridiger
 */
public class DaoProvider<T extends BaseEntity> {

	private Map<Class<T>, Dao<T, ?>> daos = new HashMap<>();

	private ConnectionSource conn;

	public DaoProvider(ConnectionSource conn) {
		this.conn = conn;
	}

	/**
	 * Gets a {@link Dao} for a {@link Class} mapped in the database. Caches formerly invoked {@link Dao}s.
	 * 
	 * @param clazz
	 *            the {@link Class}
	 * @return {@link Dao} for the given {@link Class}
	 */
	public Dao<T, ?> get(Class<T> clazz) {
		if (!daos.containsKey(clazz)) {
			try {
				Dao<T, ?> dao = DaoManager.createDao(conn, clazz);
				daos.put(clazz, dao);
			} catch (SQLException e) {
				Log.e("", e.getMessage());
			} catch (java.sql.SQLException e) {
				e.printStackTrace();
			}
		}
		return daos.get(clazz);
	}
}
