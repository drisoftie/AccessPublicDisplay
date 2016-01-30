package de.uni.stuttgart.vis.access.client.service;

import android.app.Service;

/**
 * @author Alexander Dridiger
 */
public interface INotificationServiceCreator extends INotificationCreator {

    void setService(Service service);
}
