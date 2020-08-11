package com.dosse.airpods;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

import static com.dosse.airpods.Logger.error;

public class BluetoothDeviceInfo {
    private final String address;
    private final String name;
    private final boolean uuids;
    private final int battery;

    private static final ParcelUuid[] AIRPODS_UUIDS = {
            ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"),
            ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74")
    };

    public static boolean checkUUID(BluetoothDevice bluetoothDevice){
        if(bluetoothDevice == null) {
            return false;
        }
        ParcelUuid[] uuids = bluetoothDevice.getUuids();
        if(uuids == null) {
            return false;
        }
        for(ParcelUuid u : uuids){
            for(ParcelUuid v : AIRPODS_UUIDS){
                if(u.equals(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    public BluetoothDeviceInfo(BluetoothDevice device) {
        address = device.getAddress();
        name = getName(device);
        uuids = checkUUID(device);
        battery = getBattery(device);
    }

    private String getName(BluetoothDevice device) {
        try {
            return (String) device.getClass().getMethod("getAliasName").invoke(device);
        } catch (Exception e) {
            error(e);
            return device.getName();
        }
    }

    private int getBattery(BluetoothDevice device) {
        try {
            return (int) device.getClass().getMethod("getBatteryLevel").invoke(device);
        } catch (Exception e) {
            error(e);
            return -1;
        }
    }
}
