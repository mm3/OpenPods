package com.dosse.airpods;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.dosse.airpods.Logger.debug;
import static com.dosse.airpods.Logger.error;

public abstract class PodsStatusScanCallback extends ScanCallback {

    public static final long RECENT_BEACONS_MAX_T_NS = 10000000000L; //10s

    public static final int AIRPOD_MANUFACTURER = 76;
    public static final int AIRPOD_DATA_LENGTH = 27;
    public static final int MIN_RSSI = -60;

    private List<ScanResult> recentBeacons = new ArrayList<>();

    public abstract void onStatus(PodsStatus status);

    public static ScanSettings getScanSettings(boolean save) {
        if(save) {
            return new ScanSettings.Builder().setScanMode(0).setReportDelay(0).build();
        }else{
            return new ScanSettings.Builder().setScanMode(2).setReportDelay(2).build();
        }
    }

    public static List<ScanFilter> getScanFilters() {
        byte[] manufacturerData = new byte[AIRPOD_DATA_LENGTH];
        byte[] manufacturerDataMask = new byte[AIRPOD_DATA_LENGTH];

        manufacturerData[0] = 7;
        manufacturerData[1] = 25;

        manufacturerDataMask[0] = -1;
        manufacturerDataMask[1] = -1;

        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setManufacturerData(AIRPOD_MANUFACTURER, manufacturerData, manufacturerDataMask);
        return Collections.singletonList(builder.build());
    }

    @Override
    public void onBatchScanResults(List<ScanResult> scanResults) {
        for (ScanResult result : scanResults) {
            onScanResult(-1, result);
        }
        super.onBatchScanResults(scanResults);
    }

    /**
     * This scanner receives all beacons from nearby BLE devices (not just your devices!) so we need to do 3 things:
     * - Check that the beacon comes from something that looks like a pair of AirPods
     * - Make sure that it is YOUR pair of AirPods
     * - Decode the beacon to get the status
     *
     * On a normal OS, we would use the bluetooth address of the device to filter out beacons from other devices.
     * UNFORTUNATELY, someone at google was so concerned about privacy (yea, as if they give a shit)
     * that he decided it was a good idea to not allow access to the bluetooth address of incoming BLE beacons.
     * As a result, we have no reliable way to make sure that the beacon comes from YOUR airpods
     * and not the guy sitting next to you on the bus.
     * What we did to workaround this issue is this:
     * - When a beacon arrives that looks like a pair of AirPods,
     *     look at the other beacons received in the last 10 seconds and get the strongest one
     * - If the strongest beacon's fake address is the same as this, use this beacon; otherwise use the strongest beacon
     * - Filter for signals stronger than -60db
     * - Decode...
     *
     */

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        try {
            if (!isAirpodResult(result)) {
                return;
            }
            result.getDevice().getAddress();
            debug(result.getRssi() + "db");
            debug(decodeResult(result));
            result = getBestResult(result);
            if(result == null || result.getRssi() < MIN_RSSI) {
                return;
            }
            BluetoothDeviceInfo device = new BluetoothDeviceInfo(result.getDevice());
            PodsStatus status = new PodsStatus(decodeResult(result), device);
            onStatus(status);
        } catch (Throwable t) {
            error(t);
        }
    }

    private ScanResult getBestResult(ScanResult result) {
        recentBeacons.add(result);
        ScanResult strongestBeacon = null;
        for(int i = 0; i < recentBeacons.size(); i++){
            if(SystemClock.elapsedRealtimeNanos() - recentBeacons.get(i).getTimestampNanos() > RECENT_BEACONS_MAX_T_NS){
                recentBeacons.remove(i--);
                continue;
            }
            if(strongestBeacon == null || strongestBeacon.getRssi() < recentBeacons.get(i).getRssi()) {
                strongestBeacon = recentBeacons.get(i);
            }
        }
        if(strongestBeacon != null && Objects.equals(strongestBeacon.getDevice().getAddress(), result.getDevice().getAddress())) {
            strongestBeacon = result;
        }
        return strongestBeacon;
    }

    private static boolean isAirpodResult(ScanResult result) {
        return result != null && result.getScanRecord() != null
                && isDataValid(result.getScanRecord().getManufacturerSpecificData(AIRPOD_MANUFACTURER));
    }

    private static boolean isDataValid(byte[] data) {
        return data != null && data.length == AIRPOD_DATA_LENGTH;
    }

    private static String decodeResult(ScanResult result) {
        if(result != null && result.getScanRecord() != null) {
            byte[] data = result.getScanRecord().getManufacturerSpecificData(AIRPOD_MANUFACTURER);
            if(isDataValid(data)) {
                return decodeHex(data);
            }
        };
        return null;
    }

    private static final char[] hexCharset = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    private static String decodeHex(byte[] bArr) {
        char[] ret = new char[bArr.length * 2];
        for (int i = 0; i < bArr.length; i++) {
            int b = bArr[i] & 0xFF;
            ret[i*2] = hexCharset[b >>> 4];
            ret[i*2+1] = hexCharset[b & 0x0F];
        }
        return new String(ret);
    }
}
