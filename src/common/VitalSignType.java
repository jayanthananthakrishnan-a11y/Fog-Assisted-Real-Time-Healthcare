package common;

/**
 * Standard vital sign stream identifiers for healthcare monitoring.
 */
public class VitalSignType {
    public static final String HEART_RATE = "HEART_RATE";
    public static final String SPO2 = "SPO2";
    public static final String TEMPERATURE = "TEMPERATURE";
    public static final String BLOOD_PRESSURE = "BLOOD_PRESSURE";
    public static final String RESPIRATION = "RESPIRATION";
    public static final String ECG_HRV = "ECG_HRV";

    public static final String[] ALL_TYPES = {
        HEART_RATE,
        SPO2,
        TEMPERATURE,
        BLOOD_PRESSURE
    };

    public static final String[] EXTENDED_DAG_TYPES = {
        HEART_RATE,
        SPO2,
        TEMPERATURE,
        BLOOD_PRESSURE,
        RESPIRATION,
        ECG_HRV
    };
}
