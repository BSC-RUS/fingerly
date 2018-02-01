package ru.bscmsk.fingerly.impl.samsung;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.SparseArray;

import com.samsung.android.sdk.pass.Spass;
import com.samsung.android.sdk.pass.SpassFingerprint;

import ru.bscmsk.fingerly.cryptography.CryptographyUtils;
import ru.bscmsk.fingerly.impl.AbstractSpecialTouchId;
import ru.bscmsk.fingerly.interfaces.UnionTouchIdCallback;
import ru.bscmsk.fingerly.utils.AppLogger;
import ru.bscmsk.fingerly.utils.IPrefsStore;

import static com.samsung.android.sdk.pass.SpassFingerprint.IdentifyListener;
import static com.samsung.android.sdk.pass.SpassFingerprint.STATUS_AUTHENTIFICATION_FAILED;
import static com.samsung.android.sdk.pass.SpassFingerprint.STATUS_BUTTON_PRESSED;
import static com.samsung.android.sdk.pass.SpassFingerprint.STATUS_OPERATION_DENIED;
import static com.samsung.android.sdk.pass.SpassFingerprint.STATUS_QUALITY_FAILED;
import static com.samsung.android.sdk.pass.SpassFingerprint.STATUS_SENSOR_FAILED;
import static com.samsung.android.sdk.pass.SpassFingerprint.STATUS_TIMEOUT_FAILED;
import static com.samsung.android.sdk.pass.SpassFingerprint.STATUS_USER_CANCELLED;
import static com.samsung.android.sdk.pass.SpassFingerprint.STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ACQUIRED_GOOD;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ACQUIRED_INSUFFICIENT;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ACQUIRED_PARTIAL;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ACQUIRED_TOO_SLOW;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ERROR_CANCELED;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ERROR_HW_UNAVAILABLE;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ERROR_LOCKOUT;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ERROR_NO_SPACE;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ERROR_TIMEOUT;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ERROR_UNABLE_TO_PROCESS;

/**
 * Created by mlakatkin on 08.06.2017.
 */

public class SamsungTouchId extends AbstractSpecialTouchId {

    private static final String LOG_TAG = SamsungTouchId.class.getSimpleName();
    private final int LOCKOUT_TIMEOUT = 30000;
    private final String WRITE_USE_APP_FEATURE_SURVEY = "com.samsung.android.providers.context.permission.WRITE_USE_APP_FEATURE_SURVEY";
    private SpassFingerprint mSpassFingerprint;
    private Spass mSpass;
    private boolean isInitialized;
    private boolean needRetryIdentify;
    private boolean needCancel = false;
    private Handler handler;

    public SamsungTouchId(Context context, IPrefsStore prefs) {
        super(context, prefs);
        this.handler = new Handler(context.getMainLooper());
        this.init(context);
    }

    private void init(Context context) {
        if (!isPermissionGranted())
            throw new SecurityException(String.format("caller does not have permission to access fingerprint scanner. Permission code is %s", permissionCode()));
        AppLogger.d(LOG_TAG, "samsung fingerprint api init");
        if (isInitialized)
            return;
        this.mSpass = new Spass();
        try {
            if (isPermissionGranted()) {
                this.mSpass.initialize(context);
                this.isInitialized = true;
            } else
                this.isInitialized = false;
        } catch (Exception e) {
            isInitialized = false;
            AppLogger.d(LOG_TAG, "Sumsung api not initialized cause:\n" + e.toString());
        }
        if (isApiSupported()) {
            mSpassFingerprint = new SpassFingerprint(context);
        }
    }

    @Override
    public boolean isApiSupported() {
        if (!isPermissionGranted())
            throw new SecurityException(String.format("caller does not have permission to access fingerprint scanner. Permission code is %s", permissionCode()));
       return mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT);
    }

    @Override
    public boolean isPermissionGranted() {
        return ActivityCompat.checkSelfPermission(context, WRITE_USE_APP_FEATURE_SURVEY) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void requestFinger(final CipherMode mode, final UnionTouchIdCallback callback) {
        if (!isApiSupported()) {
            callback.onAuthenticationError(FINGERPRINT_ERROR_HW_UNAVAILABLE, "FINGERPRINT_ERROR_HW_UNAVAILABLE");
            return;
        }
        cancel();
        handler.post(new Runnable() {
            @Override
            public void run() {
                requestFingerInternal(initInternalCallBack(mode, callback), callback);
            }
        });
    }

    private void requestFingerInternal(IdentifyListener listener, final UnionTouchIdCallback callback) {
        AppLogger.d(LOG_TAG, "MSG_AUTH handled");
        try {
            needCancel = true;
            mSpassFingerprint.startIdentify(listener);
        } catch (final Exception e) {
            AppLogger.d(LOG_TAG, "startIdentify exception: " + Log.getStackTraceString(e));
            if (e.getMessage().equals("Identify request is denied because a previous request is still in progress.")) {
                cancel();
                callback.onAuthenticationError(FINGERPRINT_ERROR_CANCELED, e.getMessage());

            } else if (e.getMessage().equals("Identify request is denied because 5 identify attempts are failed."))
                callback.onAuthenticationError(FINGERPRINT_ERROR_LOCKOUT, e.getMessage());
            else {
                callback.onAuthenticationError(FINGERPRINT_ERROR_HW_UNAVAILABLE, e.getMessage());
            }
        }
    }

    @Override
    public void cancel() {
        if (!needCancel)
            return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    needCancel = false;
                    needRetryIdentify = false;

                    mSpassFingerprint.cancelIdentify();
                } catch (Exception e) {
                    AppLogger.d(LOG_TAG, Log.getStackTraceString(e));
                }
            }
        });
    }

    @Override
    public int getLockoutTimeout() {
        return LOCKOUT_TIMEOUT;
    }

    @Override
    public String permissionCode() {
        return WRITE_USE_APP_FEATURE_SURVEY;
    }

    @Override
    public boolean hasFingerPrints() {
        return isApiSupported() && mSpassFingerprint.hasRegisteredFinger();
    }

    private boolean isFingerHashSupported() {
        return mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_UNIQUE_ID);
    }

    @Override
    protected String getHashedFingers() {
        if (hasFingerPrints()) {
            StringBuilder builder = new StringBuilder();
            SparseArray<String> fingerPrintIds = isFingerHashSupported() ? mSpassFingerprint.getRegisteredFingerprintUniqueID() :
                    mSpassFingerprint.getRegisteredFingerprintName();
            for (int i = 0; i < fingerPrintIds.size(); i++) {
                builder.append(fingerPrintIds.keyAt(i) + fingerPrintIds.valueAt(i));
            }
            try {
                return CryptographyUtils.getHashed(builder.toString(), SALT);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private SpassFingerprint.IdentifyListener initInternalCallBack(final CipherMode mode, final UnionTouchIdCallback callback) {
        return new SpassFingerprint.IdentifyListener() {
            boolean isOnCompliteFirst = false;

            @Override
            public void onFinished(final int eventStatus) {
                needCancel = false;
                if (eventStatus != STATUS_USER_CANCELLED && eventStatus != STATUS_TIMEOUT_FAILED) {
                    makeVibrate();
                }
                if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS) {
                    AppLogger.d(LOG_TAG, "onFinished(FINGERPRINT_ACQUIRED_GOOD)");
                    needRetryIdentify = false;
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
                } else {
                    needRetryIdentify = eventStatus != STATUS_USER_CANCELLED;
                    postErrorStatus(eventStatus, callback);
                    if (isOnCompliteFirst)
                        reInit();
                }
            }

            @Override
            public void onReady() {
                AppLogger.d(LOG_TAG, "IdentifyListener.onReady()");
            }

            @Override
            public void onStarted() {
                AppLogger.d(LOG_TAG, "IdentifyListener.onStarted()");
            }

            @Override
            public void onCompleted() {
                isOnCompliteFirst = true;
                AppLogger.d(LOG_TAG, "IdentifyListener.onCompleted()");
                reInit();
            }

            private void reInit() {
                AppLogger.d(LOG_TAG, "needRetryIdentify: " + needRetryIdentify);
                if (needRetryIdentify) {
                    AppLogger.d(LOG_TAG, "reInit touchId");
                    needRetryIdentify = false;
                    final IdentifyListener listener = this;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isOnCompliteFirst = false;
                            requestFingerInternal(listener, callback);
                        }
                    }, 100);
                }
            }
        };
    }

    private void postErrorStatus(int eventStatus, UnionTouchIdCallback callback) {
        switch (eventStatus) {
            case STATUS_USER_CANCELLED:
                callback.onAuthenticationError(FINGERPRINT_ERROR_CANCELED, "STATUS_USER_CANCELLED");
                AppLogger.d(LOG_TAG, "onFinished(STATUS_USER_CANCELLED)");
                break;
            case STATUS_TIMEOUT_FAILED:
                callback.onAuthenticationError(FINGERPRINT_ERROR_TIMEOUT, "STATUS_TIMEOUT_FAILED");
                AppLogger.d(LOG_TAG, "onFinished(STATUS_TIMEOUT_FAILED)");
                break;
            case STATUS_QUALITY_FAILED:
                callback.onAuthenticationError(FINGERPRINT_ERROR_UNABLE_TO_PROCESS, "STATUS_QUALITY_FAILED");
                AppLogger.d(LOG_TAG, "onFinished(STATUS_QUALITY_FAILED)");
                break;
            case STATUS_SENSOR_FAILED:
                callback.onAuthenticationHelp(FINGERPRINT_ACQUIRED_INSUFFICIENT, "STATUS_SENSOR_FAILED");
                AppLogger.d(LOG_TAG, "onFinished(STATUS_SENSOR_FAILED)");
                break;
            case STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE:
                callback.onAuthenticationHelp(FINGERPRINT_ACQUIRED_PARTIAL, "STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE");
                AppLogger.d(LOG_TAG, "onFinished(STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE)");
                break;
            case STATUS_BUTTON_PRESSED:
                callback.onAuthenticationHelp(FINGERPRINT_ACQUIRED_TOO_SLOW, "STATUS_BUTTON_PRESSED");
                AppLogger.d(LOG_TAG, "onFinished(STATUS_BUTTON_PRESSED)");
                break;
            case STATUS_AUTHENTIFICATION_FAILED:
            case STATUS_OPERATION_DENIED:
                callback.onAuthenticationError(FINGERPRINT_ERROR_UNABLE_TO_PROCESS, "STATUS_AUTHENTIFICATION_FAILED | STATUS_OPERATION_DENIED");
                AppLogger.d(LOG_TAG, "onFinished(STATUS_AUTHENTIFICATION_FAILED or STATUS_OPERATION_DENIED)");
                break;
        }
    }
}
