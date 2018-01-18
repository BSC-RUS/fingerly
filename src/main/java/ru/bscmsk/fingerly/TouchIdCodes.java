package ru.bscmsk.fingerly;

/**
 * Created by mlakatkin on 05.06.2017.
 */

public class TouchIdCodes {
	//help codes
	public static final int FINGERPRINT_ACQUIRED_GOOD = 0; //The image acquired was good.
	public static final int FINGERPRINT_ACQUIRED_PARTIAL = 1; //Only a partial fingerprint image was detected.
	public static final int FINGERPRINT_ACQUIRED_INSUFFICIENT = 2; // The fingerprint image was too noisy to process due to a detected condition.
	public static final int FINGERPRINT_ACQUIRED_IMAGER_DIRTY = 3; //	The fingerprint image was too noisy due to suspected or detected dirt on the sensor.
	public static final int FINGERPRINT_ACQUIRED_TOO_SLOW = 4; //The fingerprint image was unreadable due to lack of motion.
	public static final int FINGERPRINT_ACQUIRED_TOO_FAST = 5; //The fingerprint image was incomplete due to quick motion.
	//error codes
	public static final int FINGERPRINT_ERROR_HW_UNAVAILABLE = 1; //The hardware is unavailable.
	public static final int FINGERPRINT_ERROR_UNABLE_TO_PROCESS = 2; //Error state returned when the sensor was unable to process the current image.
	public static final int FINGERPRINT_ERROR_TIMEOUT = 3; // Error state returned when the current request has been running too long.
	public static final int FINGERPRINT_ERROR_NO_SPACE = 4; //Error state returned for operations like enrollment; the operation cannot be completed because there's not enough storage remaining to complete the operation.
	public static final int FINGERPRINT_ERROR_CANCELED = 5; //The operation was canceled because the fingerprint sensor is unavailable.
	public static final int FINGERPRINT_ERROR_LOCKOUT = 7; //The operation was canceled because the API is locked out due to too many attempts.
}


