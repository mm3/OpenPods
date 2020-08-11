package com.dosse.airpods;

import android.app.Notification;
import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;

public class NotificationBuilder {
    public static final String TAG="AirPods";
    public static final long TIMEOUT_CONNECTED = 30000;
    public static final int NOTIFICATION_ID = 1;

    private final RemoteViews notificationBig;
    private final RemoteViews notificationSmall;
    private final RemoteViews locationDisabledBig;
    private final RemoteViews locationDisabledSmall;
    private final NotificationCompat.Builder mBuilder;

    public NotificationBuilder(Context context) {
        notificationBig = new RemoteViews(context.getPackageName(), R.layout.status_big);
        notificationSmall = new RemoteViews(context.getPackageName(), R.layout.status_small);
        locationDisabledBig = new RemoteViews(context.getPackageName(), R.layout.location_disabled_big);
        locationDisabledSmall = new RemoteViews(context.getPackageName(), R.layout.location_disabled_small);
        mBuilder = new NotificationCompat.Builder(context, TAG);
        mBuilder.setShowWhen(false);
        mBuilder.setOngoing(true);
        mBuilder.setSmallIcon(R.mipmap.notification_icon);
    }

    public Notification build(PodsStatus status, boolean location) {
        if(location || (status.isAllDisconnected() && isFreshStatus(status))) {
            mBuilder.setCustomContentView(notificationSmall);
            mBuilder.setCustomBigContentView(notificationBig);
        }else{
            mBuilder.setCustomContentView(locationDisabledSmall);
            mBuilder.setCustomBigContentView(locationDisabledBig);
        }
        if(status.isAirpods()){
            notificationBig.setImageViewResource(R.id.leftPodImg, status.isLeftConnected() ? R.drawable.pod : R.drawable.pod_disconnected);
            notificationBig.setImageViewResource(R.id.rightPodImg, status.isRightConnected() ? R.drawable.pod : R.drawable.pod_disconnected);
            notificationBig.setImageViewResource(R.id.podCaseImg, status.isCaseConnected() ? R.drawable.pod_case : R.drawable.pod_case_disconnected);
            notificationSmall.setImageViewResource(R.id.leftPodImg, status.isLeftConnected() ? R.drawable.pod : R.drawable.pod_disconnected);
            notificationSmall.setImageViewResource(R.id.rightPodImg, status.isRightConnected() ? R.drawable.pod : R.drawable.pod_disconnected);
            notificationSmall.setImageViewResource(R.id.podCaseImg, status.isCaseConnected() ? R.drawable.pod_case : R.drawable.pod_case_disconnected);
        }else if(status.isAirpodsPro()){
            notificationBig.setImageViewResource(R.id.leftPodImg, status.isLeftConnected() ? R.drawable.podpro : R.drawable.podpro_disconnected);
            notificationBig.setImageViewResource(R.id.rightPodImg, status.isRightConnected() ? R.drawable.podpro : R.drawable.podpro_disconnected);
            notificationBig.setImageViewResource(R.id.podCaseImg, status.isCaseConnected() ? R.drawable.podpro_case : R.drawable.podpro_case_disconnected);
            notificationSmall.setImageViewResource(R.id.leftPodImg, status.isLeftConnected() ? R.drawable.podpro : R.drawable.podpro_disconnected);
            notificationSmall.setImageViewResource(R.id.rightPodImg, status.isRightConnected() ? R.drawable.podpro : R.drawable.podpro_disconnected);
            notificationSmall.setImageViewResource(R.id.podCaseImg, status.isCaseConnected() ? R.drawable.podpro_case : R.drawable.podpro_case_disconnected);
        }
        if(isFreshStatus(status)) {
            notificationBig.setViewVisibility(R.id.leftPodText, View.VISIBLE);
            notificationBig.setViewVisibility(R.id.rightPodText, View.VISIBLE);
            notificationBig.setViewVisibility(R.id.podCaseText, View.VISIBLE);
            notificationBig.setViewVisibility(R.id.leftPodUpdating, View.INVISIBLE);
            notificationBig.setViewVisibility(R.id.rightPodUpdating, View.INVISIBLE);
            notificationBig.setViewVisibility(R.id.podCaseUpdating, View.INVISIBLE);
            notificationSmall.setViewVisibility(R.id.leftPodText, View.VISIBLE);
            notificationSmall.setViewVisibility(R.id.rightPodText, View.VISIBLE);
            notificationSmall.setViewVisibility(R.id.podCaseText, View.VISIBLE);
            notificationSmall.setViewVisibility(R.id.leftPodUpdating, View.INVISIBLE);
            notificationSmall.setViewVisibility(R.id.rightPodUpdating, View.INVISIBLE);
            notificationSmall.setViewVisibility(R.id.podCaseUpdating, View.INVISIBLE);
            notificationBig.setTextViewText(R.id.leftPodText, status.getLeftStatus());
            notificationBig.setTextViewText(R.id.rightPodText, status.getRightStatus());
            notificationBig.setTextViewText(R.id.podCaseText, status.getCaseStatus());
            notificationSmall.setTextViewText(R.id.leftPodText, status.getLeftStatus());
            notificationSmall.setTextViewText(R.id.rightPodText, status.getRightStatus());
            notificationSmall.setTextViewText(R.id.podCaseText, status.getCaseStatus());
        }else{
            notificationBig.setViewVisibility(R.id.leftPodText, View.INVISIBLE);
            notificationBig.setViewVisibility(R.id.rightPodText, View.INVISIBLE);
            notificationBig.setViewVisibility(R.id.podCaseText, View.INVISIBLE);
            notificationBig.setViewVisibility(R.id.leftPodUpdating, View.VISIBLE);
            notificationBig.setViewVisibility(R.id.rightPodUpdating, View.VISIBLE);
            notificationBig.setViewVisibility(R.id.podCaseUpdating, View.VISIBLE);
            notificationSmall.setViewVisibility(R.id.leftPodText, View.INVISIBLE);
            notificationSmall.setViewVisibility(R.id.rightPodText, View.INVISIBLE);
            notificationSmall.setViewVisibility(R.id.podCaseText, View.INVISIBLE);
            notificationSmall.setViewVisibility(R.id.leftPodUpdating, View.VISIBLE);
            notificationSmall.setViewVisibility(R.id.rightPodUpdating, View.VISIBLE);
            notificationSmall.setViewVisibility(R.id.podCaseUpdating, View.VISIBLE);
        }
        return mBuilder.build();
    }

    private boolean isFreshStatus(PodsStatus status) {
        return System.currentTimeMillis() - status.getTimestamp() < TIMEOUT_CONNECTED;
    }
}
