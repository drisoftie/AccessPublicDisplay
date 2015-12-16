package de.stuttgart.uni.vis.access.common;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;

import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Alexander Dridiger
 */
public class NotificationBuilder {

    public static final int ACTIVITY           = 0;
    public static final int SERVICE            = 1;
    public static final int BROADCAST_RECEIVER = 2;

    public static NotificationCompat.Builder createNotificationBuilder(Context c, @IdRes int id, @DrawableRes int icon,
                                                                       @Nullable String title, @Nullable String subTitle,
                                                                       @NonNull Class<?> intentClass) {
        // start notification
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(c);
        nBuilder.setSmallIcon(icon);
        if (StringUtils.isNotEmpty(title)) {
            nBuilder.setContentTitle(title);
        }
        if (StringUtils.isNotEmpty(subTitle)) {
            nBuilder.setContentText(subTitle);
        }

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(c, intentClass);

        // The stack builder object will contain an artificial back stack for the started Activity.
        // This ensures that navigating backward from the Activity leads out of your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(c);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(intentClass);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT);
        nBuilder.setContentIntent(resultPendingIntent);
        return nBuilder;
    }

    public static void addAction(Context c, NotificationCompat.Builder nBuilder, @DrawableRes int icon, String action, Class<?> intentClass,
                                 @ComponentType int type) {
        Intent nClickIntent = new Intent(c, intentClass);
        addAction(c, nBuilder, icon, action, nClickIntent, type);
    }

    public static void addAction(Context c, NotificationCompat.Builder nBuilder, @DrawableRes int icon, String action, Intent intent,
                                 @ComponentType int type) {
        PendingIntent nActionIntent = null;
        switch (type) {
            case ACTIVITY:
                nActionIntent = PendingIntent.getActivity(c, Constants.ACTIVITY, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                break;
            case SERVICE:
                nActionIntent = PendingIntent.getService(c, Constants.SERVICE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                break;
            case BROADCAST_RECEIVER:
                nActionIntent = PendingIntent.getBroadcast(c, Constants.BROADCAST_RECEIVER, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                break;
        }
        nBuilder.addAction(icon, action, nActionIntent);
    }

    @IntDef({ACTIVITY, SERVICE, BROADCAST_RECEIVER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ComponentType {
    }
}
