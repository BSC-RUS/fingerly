package ru.bscmsk.fingerly.impl.meizu;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;

import com.fingerprints.service.FingerprintManager;

import ru.bscmsk.fingerly.cryptography.CryptographyUtils;
import ru.bscmsk.fingerly.impl.AbstractSpecialTouchId;
import ru.bscmsk.fingerly.interfaces.UnionTouchIdCallback;
import ru.bscmsk.fingerly.utils.AppLogger;
import ru.bscmsk.fingerly.utils.IPrefsStore;

import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ACQUIRED_GOOD;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ERROR_HW_UNAVAILABLE;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ERROR_NO_SPACE;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ERROR_UNABLE_TO_PROCESS;

/**
 * Created by mlakatkin on 08.12.2017.
 */

public class MeizuTouchId extends AbstractSpecialTouchId {
    private static final String PERMISSION = "com.fingerprints.service.ACCESS_FINGERPRINT_MANAGER";
    private final int LOCKOUT_TIMEOUT = 30000;

    protected FingerprintManager fingerManager;
    private static final String MEIZU = "MEIZU";

    public MeizuTouchId(Context context, IPrefsStore prefs) {
        super(context, prefs);
    }

    protected FingerprintManager getFingerPrintManager() {
        if (fingerManager == null) {
            try {
                fingerManager = FingerprintManager.open();
            } catch (Throwable e) {
                AppLogger.d(this, Log.getStackTraceString(e));
                fingerManager = null;
            }
        }
        return fingerManager;
    }

    @Override
    public boolean isApiSupported() {
        boolean b = isMeizu() &&
                getFingerPrintManager() != null &&
                getFingerPrintManager().isSurpport();
        releaseFingerManager();
        return b;
    }

    @Override
    public boolean isPermissionGranted() {
        return ActivityCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void requestFinger(final CipherMode mode, final UnionTouchIdCallback callback) {
        FingerprintManager fingerPrintManager = getFingerPrintManager();
        if (fingerPrintManager == null) {
            callback.onAuthenticationError(FINGERPRINT_ERROR_HW_UNAVAILABLE, "FINGERPRINT_ERROR_HW_UNAVAILABLE");
            return;
        }
        try {
            fingerPrintManager.startIdentify(new FingerprintManager.IdentifyCallback() {
                @Override
                public void onIdentified(int i, boolean b) {
                    makeVibrate();
                    releaseFingerManager();
                    AppLogger.d(this, "onFinished(FINGERPRINT_ACQUIRED_GOOD)");
                    callback.onAuthenticationHelp(FINGERPRINT_ACQUIRED_GOOD, "FINGERPRINT_ACQUIRED_GOOD");
                    String hashedFingers = getHashedFingers();
                    if (mode == CipherMode.DECRYPT) {
                        if (!hashChangedChecker.checkHash(hashedFingers)) {
                            callback.onAuthenticationError(FINGERPRINT_ERROR_NO_SPACE, "The keystore was compromised");
                            return;
                        }
                        callback.onAuthenticationSucceeded(cipherInitializer.getCipher(false, hashedFingers));
                    } else {
                        callback.onAuthenticationSucceeded(cipherInitializer.getCipher(true, hashedFingers));
                    }
                }

                @Override
                public void onNoMatch() {
                    makeVibrate();
                    callback.onAuthenticationError(FINGERPRINT_ERROR_UNABLE_TO_PROCESS, "FINGERPRINT_ERROR_UNABLE_TO_PROCESS");
                    releaseFingerManager();
                    requestFinger(mode, callback);
                }
            }, fingerPrintManager.getIds());

        } catch (
                Exception e)

        {
            callback.onAuthenticationError(FINGERPRINT_ERROR_HW_UNAVAILABLE, "FINGERPRINT_ERROR_HW_UNAVAILABLE");
        }

    }

    @Override
    public void cancel() {
        releaseFingerManager();
    }

    @Override
    public int getLockoutTimeout() {
        return LOCKOUT_TIMEOUT;
    }

    @Override
    public String permissionCode() {
        return PERMISSION;
    }

    @Override
    public boolean hasFingerPrints() {
        boolean b = getFingerPrintManager().getIds() != null &&
                getFingerPrintManager().getIds().length > 0;
        releaseFingerManager();
        return b;
    }

    private void releaseFingerManager() {
        try {
            if (fingerManager != null) {
                fingerManager.release();
                fingerManager = null;
            }
        } catch (Throwable e) {
            AppLogger.d(this, Log.getStackTraceString(e));
        }
    }

    @Override
    protected String getHashedFingers() {
        if (hasFingerPrints()) {
            StringBuilder builder = new StringBuilder();
            int[] fingerPrintIds = getFingerPrintManager().getIds();
            for (int i = 0; i < fingerPrintIds.length; i++) {
                builder.append(fingerPrintIds[i]);
            }
            try {
                return CryptographyUtils.getHashed(builder.toString(), SALT);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private boolean isMeizu() {
        String manufacturer = Build.MANUFACTURER;
        return !TextUtils.isEmpty(manufacturer) && manufacturer.toUpperCase().contains(MEIZU);
    }
}
