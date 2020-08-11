package com.dosse.airpods;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;

import static com.dosse.airpods.BluetoothDeviceInfo.checkUUID;
import static com.dosse.airpods.Logger.debug;
import static com.dosse.airpods.Logger.error;
import static com.dosse.airpods.PodsStatusScanCallback.getScanSettings;
import static com.dosse.airpods.PodsStatusScanCallback.getScanFilters;

/**
 * This is the class that does most of the work. It has 3 functions:
 * - Detect when AirPods are detected
 * - Receive beacons from AirPods and decode them (easier said than done thanks to google's autism)
 * - Display the notification with the status
 *
 */
public class PodsService extends Service {

    private BluetoothLeScanner btScanner;
    private PodsStatus status = PodsStatus.DISCONNECTED;

    private static NotificationThread n = null;
    private static boolean maybeConnected = false;

    private BroadcastReceiver btReceiver = null;
    private BroadcastReceiver screenReceiver = null;
    private PodsStatusScanCallback scanCallback = null;

    /**
     * The following method (startAirPodsScanner) creates a bluetoth LE scanner.
     * This scanner receives all beacons from nearby BLE devices (not just your devices!) so we need to do 3 things:
     * - Check that the beacon comes from something that looks like a pair of AirPods
     * - Make sure that it is YOUR pair of AirPods
     * - Decode the beacon to get the status
     *
     * After decoding a beacon, the status is written to status so that the NotificationThread can use the information
     *
     */

    private void startAirPodsScanner() {
        try {
            debug("START SCANNER");
            SharedPreferences prefs=getSharedPreferences("openpods", MODE_PRIVATE);
            boolean batterySaver = prefs.getBoolean("batterySaver",false);
            BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter btAdapter = btManager.getAdapter();
            if (btAdapter == null) {
                debug("No BT");
                return;
            }

            if(btScanner != null && scanCallback != null) {
                btScanner.stopScan(scanCallback);
                scanCallback = null;
            }

            if (!btAdapter.isEnabled()) {
                debug("BT Off");
                return;
            }

            btScanner = btAdapter.getBluetoothLeScanner();
            scanCallback = new PodsStatusScanCallback() {
                @Override
                public void onStatus(PodsStatus newStatus) {
                    status = newStatus;
                }
            };

            btScanner.startScan(getScanFilters(), getScanSettings(batterySaver), scanCallback);
        } catch (Throwable t) {
            error(t);
        }
    }


    private void stopAirPodsScanner(){
        try{
            if(btScanner!=null && scanCallback != null){
                debug("STOP SCANNER");
                btScanner.stopScan(scanCallback);
                scanCallback = null;
            }
            status = PodsStatus.DISCONNECTED;
        }catch (Throwable t){
            error(t);
        }
    }

    private boolean isLocationEnabled(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){
            LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
            return service!=null && service.isLocationEnabled();
        }else{
            try {
                return Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF;
            }catch(Throwable t){
                error(t);
                return true;
            }
        }
    }

    public PodsService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * When the service is created, we register to get as many bluetooth and airpods related events as possible.
     * ACL_CONNECTED and ACL_DISCONNECTED should have been enough, but you never know with android these days.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        try{
            if(btReceiver != null) {
                unregisterReceiver(btReceiver);
                btReceiver = null;
            }
        }catch (Throwable t){
            error(t);
        }
        btReceiver = new BluetoothReceiver() {
            @Override
            public void onStart() {
                //bluetooth turned on, start/restart scanner
                debug("BT ON");
                startAirPodsScanner();
            }

            @Override
            public void onStop() {
                //bluetooth turned off, stop scanner and remove notification
                debug("BT OFF");
                maybeConnected = false;
                stopAirPodsScanner();
            }

            @Override
            public void onConnect(BluetoothDevice bluetoothDevice) {
                if (checkUUID(bluetoothDevice)) { //airpods filter
                    //airpods connected, show notification
                    debug("ACL CONNECTED");
                    maybeConnected = true;
                }
            }

            @Override
            public void onDisconnect(BluetoothDevice bluetoothDevice) {
                if (checkUUID(bluetoothDevice)) { //airpods filter
                    //airpods disconnected, remove notification but leave the scanner going
                    debug("ACL DISCONNECTED");
                    maybeConnected = false;
                }
           }
        };
        try{
            registerReceiver(btReceiver,BluetoothReceiver.buildFilter());
        }catch(Throwable t){
            error(t);
        }
        //this BT Profile Proxy allows us to know if airpods are already connected when the app is started.
        // It also fires an event when BT is turned off, in case the BroadcastReceiver doesn't do its job
        BluetoothAdapter ba=((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        ba.getProfileProxy(getApplicationContext(), new BluetoothListener() {
            @Override
            public boolean onConnect(BluetoothDevice device) {
                debug("BT PROXY SERVICE CONNECTED");
                if(checkUUID(device)){
                    debug("BT PROXY: AIRPODS ALREADY CONNECTED");
                    maybeConnected=true;
                    return true;
                }
                return false;
            }

            @Override
            public void onDisconnect() {
                debug("BT PROXY SERVICE DISCONNECTED ");
                maybeConnected=false;
            }
        },BluetoothProfile.HEADSET);
        if(ba.isEnabled()) {
            //if BT is already on when the app is started, start the scanner without waiting for an event to happen
            startAirPodsScanner();
        }

        //Screen on/off listener to suspend scanning when the screen is off, to save battery
        try{
            if(screenReceiver != null) {
                unregisterReceiver(screenReceiver);
                screenReceiver = null;
            }
        }catch (Throwable t){
            error(t);
        }
        SharedPreferences prefs=getSharedPreferences("openpods", MODE_PRIVATE);
        if(prefs.getBoolean("batterySaver",false)) {
            screenReceiver = new ScreenReceiver() {
                @Override
                public void onStart() {
                    debug( "SCREEN ON");
                    startAirPodsScanner();
                }

                @Override
                public void onStop() {
                    debug( "SCREEN OFF");
                    stopAirPodsScanner();
                }
            };
            try {
                registerReceiver(screenReceiver, ScreenReceiver.buildFilter());
            } catch (Throwable t) {
                error(t);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try{
            if(btReceiver != null) {
                unregisterReceiver(btReceiver);
                btReceiver = null;
            }
        } catch (Throwable t){
            error(t);
        }
        try{
            if(screenReceiver != null) {
                unregisterReceiver(screenReceiver);
                screenReceiver = null;
            }
        } catch (Throwable t){
            error(t);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(n == null || !n.isAlive()){
            n = new NotificationThread(this) {

                @Override
                public boolean isConnected() {
                    return maybeConnected;
                }

                @Override
                public boolean isLocationEnabled() {
                    return PodsService.this.isLocationEnabled();
                }

                @Override
                public PodsStatus getStatus() {
                    return status;
                }
            };
            n.start();
        }
        return START_STICKY;
    }

}
