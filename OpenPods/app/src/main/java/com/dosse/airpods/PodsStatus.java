package com.dosse.airpods;

/**
 * Decoding the beacon:
 * This was done through reverse engineering. Hopefully it's correct.
 * - The beacon coming from a pair of AirPods contains a manufacturer specific data field n°76 of 27 bytes
 * - We convert this data to a hexadecimal string
 * - The 12th and 13th characters in the string represent the charge of the left and right pods.
 *     Under unknown circumstances, they are right and left instead (see isFlipped).
 *     Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
 * - The 15th character in the string represents the charge of the case.
 *     Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
 * - The 14th character in the string represents the "in charge" status.
 *     Bit 0 (LSB) is the left pod; Bit 1 is the right pod; Bit 2 is the case.
 *     Bit 3 might be case open/closed but I'm not sure and it's not used
 * - The 7th character in the string represents the AirPods model (E=AirPods pro)
 */

public class PodsStatus {

    public static final String MODEL_AIRPODS_NORMAL = "airpods12";
    public static final String MODEL_AIRPODS_PRO = "airpodspro";
    public static final int DISCONNECTED_STATUS = 15;
    public static final int MAX_CONNECTED_STATUS = 10;

    public static final PodsStatus DISCONNECTED = new PodsStatus();

    private int leftStatus = DISCONNECTED_STATUS;
    private int rightStatus = DISCONNECTED_STATUS;
    private int caseStatus = DISCONNECTED_STATUS;
    private boolean chargeL = false;
    private boolean chargeR = false;
    private boolean chargeCase = false;
    private String model = MODEL_AIRPODS_NORMAL;
    private BluetoothDeviceInfo device = null;
    private long timestamp = System.currentTimeMillis();

    public PodsStatus() {

    }

    public PodsStatus(String status, BluetoothDeviceInfo device) {
        if(status == null) {
            return;
        }

        String strLeft = ""; //left airpod (0-10 batt; 15=disconnected)
        String strRight = ""; //right airpod (0-10 batt; 15=disconnected)
        if (isFlipped(status)) {
            strLeft = String.valueOf(status.charAt(12));
            strRight = String.valueOf(status.charAt(13));
        } else {
            strLeft = String.valueOf(status.charAt(13));
            strRight = String.valueOf(status.charAt(12));
        }
        String strCase = String.valueOf(status.charAt(15)); //case (0-10 batt; 15=disconnected)
        String strStatus = String.valueOf(status.charAt(14)); //charge status (bit 0=left; bit 1=right; bit 2=case)
        leftStatus = Integer.parseInt(strLeft, 16);
        rightStatus = Integer.parseInt(strRight, 16);
        caseStatus = Integer.parseInt(strCase, 16);
        int chargeStatus = Integer.parseInt(strStatus, 16);
        chargeL = (chargeStatus & 0b00000001) != 0;
        chargeR = (chargeStatus & 0b00000010) != 0;
        chargeCase = (chargeStatus & 0b00000100) != 0;
        model = (status.charAt(7) == 'E') ? MODEL_AIRPODS_PRO : MODEL_AIRPODS_NORMAL; //detect if these are AirPods pro or regular ones

        this.device = device;
    }

    private static boolean isFlipped(String str) {
        return (Integer.toString(Integer.parseInt(String.valueOf(str.charAt(10)),16) + 0x10,2)).charAt(3) == '0';
    }

    public String getStatusString() {
        return "Left: " + leftStatus + (chargeL ? "+" : "") + " " +
                "Right: " + rightStatus + (chargeR ? "+" : "") + " " +
                "Case: " + caseStatus + (chargeCase ? "+" : "") + " " +
                "Model: " + model;
    }

    public String getLeftStatus() {
        return buildStatus(leftStatus, chargeL);
    }

    public String getRightStatus() {
        return buildStatus(rightStatus, chargeR);
    }

    public String getCaseStatus() {
        return buildStatus(caseStatus, chargeCase);
    }

    private String buildStatus(int status, boolean charge) {
        return (status == MAX_CONNECTED_STATUS ? "100%" :
                status < MAX_CONNECTED_STATUS ? (((status) * 10 + 5) + "%" + (charge ? "+" : "")) : "");
    }

    public boolean isAllDisconnected() {
        return leftStatus == DISCONNECTED_STATUS &&
                rightStatus == DISCONNECTED_STATUS &&
                caseStatus == DISCONNECTED_STATUS;
    }

    public boolean isLeftConnected() {
        return leftStatus <= MAX_CONNECTED_STATUS;
    }

    public boolean isRightConnected() {
        return rightStatus <= MAX_CONNECTED_STATUS;
    }

    public boolean isCaseConnected() {
        return caseStatus <= MAX_CONNECTED_STATUS;
    }

    public boolean isAirpods() {
        return model.equals(MODEL_AIRPODS_NORMAL);
    }

    public boolean isAirpodsPro() {
        return model.equals(MODEL_AIRPODS_PRO);
    }

    public long getTimestamp() {
        return timestamp;
    }

}
