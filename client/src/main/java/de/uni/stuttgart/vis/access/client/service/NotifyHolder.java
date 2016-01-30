package de.uni.stuttgart.vis.access.client.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import de.stuttgart.uni.vis.access.common.NotificationBuilder;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.act.ActScan;
import de.uni.stuttgart.vis.access.client.brcast.BrRcvScan;
import de.uni.stuttgart.vis.access.client.brcast.BrRcvStop;

/**
 * @author Alexander Dridiger
 */
public class NotifyHolder implements INotificationServiceCreator {

    private Service service;

    @Override
    public void setService(Service service) {
        this.service = service;
    }

    public void createScanNotification() {
        NotificationCompat.Builder nBuilder = NotificationBuilder.createNotificationBuilder(service, R.id.nid_main,
                                                                                            R.drawable.ic_action_bl_scan, service.getString(
                        R.string.ntxt_scan), null, ActScan.class);

        NotificationBuilder.addAction(service, nBuilder, R.drawable.ic_action_remove, service.getString(R.string.nact_stop),
                                      BrRcvStop.class, NotificationBuilder.BROADCAST_RECEIVER);

        NotificationManager mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        nBuilder.setAutoCancel(false);

        Notification n = nBuilder.build();
        mNotificationManager.notify(R.id.nid_main, n);
        service.startForeground(R.id.nid_main, n);
    }

    public void createDisplayNotification(String value, int nid) {
        String txtFound = service.getString(R.string.ntxt_scan_found);
        String txtDescr = service.getString(R.string.ntxt_scan_descr, value);

        NotificationCompat.Builder nBuilder = NotificationBuilder.createNotificationBuilder(service, nid,
                                                                                            R.drawable.ic_action_display_visible, txtFound,
                                                                                            txtDescr, ActScan.class);

        Intent showIntent = new Intent(service, BrRcvScan.class);
        showIntent.putExtra(service.getString(R.string.bndl_bl_show), value);
        NotificationBuilder.addAction(service, nBuilder, R.drawable.ic_action_display_visible, service.getString(R.string.nact_show),
                                      showIntent, NotificationBuilder.BROADCAST_RECEIVER);

        NotificationBuilder.addAction(service, nBuilder, R.drawable.ic_action_remove, service.getString(R.string.nact_stop),
                                      BrRcvStop.class, NotificationBuilder.BROADCAST_RECEIVER);

        nBuilder.setAutoCancel(false);

        Notification n = nBuilder.build();

        NotificationManager mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(nid, n);
        service.startForeground(nid, n);

        //        tts.queueRead(txtFound, txtDescr);
    }

    public void removeAllNotifications() {
        NotificationManager mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(R.id.nid_main);
        mNotificationManager.cancel(R.id.nid_weather);
        mNotificationManager.cancel(R.id.nid_pub_transp);
    }

    public void removeNotification(int id) {
        NotificationManager mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(id);
    }
}
