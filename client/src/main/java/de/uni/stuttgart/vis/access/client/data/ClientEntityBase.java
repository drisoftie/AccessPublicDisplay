package de.uni.stuttgart.vis.access.client.data;

import android.content.Context;

import com.j256.ormlite.dao.Dao;

import de.stuttgart.uni.vis.access.common.domain.BaseEntity;
import de.uni.stuttgart.vis.access.client.App;

/**
 * @author Alexander Dridiger
 */
public class ClientEntityBase extends BaseEntity {

    @Override
    public <T> Dao<T, Object> dao(Class<T> clazz) {
        return App.dao(clazz);
    }

    @Override
    public Context cntxt() {
        return App.inst();
    }
}
