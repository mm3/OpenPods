package com.dosse.airpods;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import static com.dosse.airpods.Logger.debug;

public abstract class ScreenReceiver extends BroadcastReceiver {

    public abstract void onStart();
    public abstract void onStop();

    public static IntentFilter buildFilter() {
        IntentFilter screenIntentFilter = new IntentFilter();
        screenIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        return screenIntentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            onStop();
        } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            onStart();
        }
    }

}
