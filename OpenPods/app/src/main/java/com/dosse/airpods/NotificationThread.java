package com.dosse.airpods;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

import static com.dosse.airpods.Logger.debug;
import static com.dosse.airpods.Logger.error;
import static com.dosse.airpods.NotificationBuilder.NOTIFICATION_ID;
import static com.dosse.airpods.NotificationBuilder.TAG;

/**
 * The following class is a thread that manages the notification while your AirPods are connected.
 *
 * It simply reads the status variables every 1 seconds and creates, destroys, or updates the notification accordingly.
 * The notification is shown when BT is on and AirPods are connected. The status is updated every 1 second.
 * Battery% is hidden if we didn't receive a beacon for 30 seconds (screen off for a while)
 *
 * This thread is the reason why we need permission to disable doze. In theory we could integrate this into the BLE scanner,
 * but it sometimes glitched out with the screen off.
 *
 */

public abstract class NotificationThread extends Thread {
    private static final long SLEEP_TIMEOUT = 1000;

    private final NotificationBuilder builder;
    private final NotificationManager mNotifyManager;

    public abstract boolean isConnected();
    public abstract boolean isLocationEnabled();
    public abstract PodsStatus getStatus();

    public NotificationThread(Context context){
        mNotifyManager=(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //on oreo and newer, create a notification channel
            NotificationChannel channel = new NotificationChannel(TAG, TAG, NotificationManager.IMPORTANCE_LOW);
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            mNotifyManager.createNotificationChannel(channel);
        }
        builder = new NotificationBuilder(context);
    }

    public void run(){
        boolean notificationShowing=false;
        while (!Thread.interrupted()){
            PodsStatus status = getStatus();
            if(isConnected() && !(status.isAllDisconnected())
                // && System.currentTimeMillis() - lastSeenConnected < TIMEOUT_CONNECTED
            ){
                if(!notificationShowing){
                    debug("Creating notification");
                    notificationShowing = true;
                }
                debug( status.getStatusString());
                mNotifyManager.notify(NOTIFICATION_ID, builder.build(status, isLocationEnabled()));
            }else{
                if(notificationShowing){
                    debug("Removing notification");
                    notificationShowing = false;
                    continue;
                }
                mNotifyManager.cancel(NOTIFICATION_ID);
            }
            try {
                Thread.sleep(SLEEP_TIMEOUT);
            } catch (InterruptedException e) {
                error(e);
            }
        }
    }
}
