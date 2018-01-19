package ru.bscmsk.fingerlydemo.service;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import javax.crypto.Cipher;

import ru.bscmsk.fingerly.TouchIdFactory;
import ru.bscmsk.fingerly.cryptography.CryptographyUtils;
import ru.bscmsk.fingerly.interfaces.IUnionTouchId;
import ru.bscmsk.fingerly.interfaces.UnionTouchIdCallback;
import ru.bscmsk.fingerly.utils.IPrefsStore;
import ru.bscmsk.fingerlydemo.util.PreferencesStorage;

import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ERROR_CANCELED;
import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ERROR_LOCKOUT;

/**
 * Created by mlakatkin on 07.12.2017.
 */

public class FingerPrintService implements IFingerPrintService {

	private static final String SECRET_MSG = "SECRET_MSG";

	private IUnionTouchId touchId;
	private Timer reInitTouchTimeout;
	private IPrefsStore prefs;
	private boolean cancelByUser = false;
	private Handler handler;

	private static IFingerPrintService fingerPrintService;

	public static IFingerPrintService getService(Context context) {
		if (fingerPrintService == null)
			fingerPrintService = new FingerPrintService(context, new PreferencesStorage(context));
		return fingerPrintService;
	}

	protected FingerPrintService(Context context, PreferencesStorage prefs) {
		this.prefs = prefs;
		this.touchId = TouchIdFactory.getTouchId(context, prefs);
		this.handler = new Handler();
	}

	@Override
	public boolean isFingerPrintAvailable() {
		return touchId.isApiSupported();
	}

	@Override
	public IPrefsStore getPrefs() {
		return prefs;
	}

	@Override
	public void cancel() {
		cancelByUser = true;
		touchId.cancel();
		if (reInitTouchTimeout != null)
			reInitTouchTimeout.cancel();
	}

	@Override
	public void reset() {
		touchId.clearKeystore();
		prefs.remove(SECRET_MSG);
	}

	@Override
	public boolean hasFingerPrints() {
		return touchId.hasFingerPrints();
	}

	@Override
	public boolean hasEncryptedSecret() {
		return hasFingerPrints() &&
			!TextUtils.isEmpty(prefs.get(SECRET_MSG, String.class, ""));
	}

	@Override
	public boolean isStorageCompromised() {
		return touchId.isKeyPermanentlyInvalidated();
	}

	@Override
	public boolean checkPermission() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || touchId.isPermissionGranted();
	}

	@Override
	public String getPermissionCode() {
		return touchId.permissionCode();
	}

	@Override
	public long getTimeout() {
		return touchId.getLockoutTimeout();
	}

	@Override
	public void createFingerRequest(IFingerPrintCallback callback) {
		reset();
		touchId.createKey();
		recognizeFinger(IUnionTouchId.CipherMode.ENCRYPT, callback);
	}

	@Override
	public void authFingerRequest(IFingerPrintCallback callback) {
		recognizeFinger(IUnionTouchId.CipherMode.DECRYPT, callback);
	}

	@Override
	public void setFingerSecret(Cipher cipher, String text) throws ExecutionException {
		String encryptedMessage = CryptographyUtils.tryEncrypt(cipher, text);
		if (encryptedMessage == null)
			throw new IllegalArgumentException("Error while encrypt message");

		prefs.add(SECRET_MSG, encryptedMessage);
	}

	@Nullable
	@Override
	public String getEncryptedMessage() {
		return prefs.get(SECRET_MSG, String.class, null);
	}

	@Override
	@Nullable
	public String getFingerSecret(Cipher cipher) {
		String secretMessage = prefs.get(SECRET_MSG, String.class, "");
		return CryptographyUtils.tryDecrypt(cipher, secretMessage);
	}

	private void recognizeFinger(final IUnionTouchId.CipherMode cipherMode, @NonNull final IFingerPrintCallback callback) {
		touchId.requestFinger(cipherMode, new UnionTouchIdCallback() {
			@Override
			public void onAuthenticationError(int errMsgId, CharSequence errString) {
				if (errMsgId == FINGERPRINT_ERROR_CANCELED) {
					if (cancelByUser)
						callback.onCancelled();
					else
						handler.postDelayed(() -> recognizeFinger(cipherMode, callback), 200);
					return;
				}

				if (errMsgId == FINGERPRINT_ERROR_LOCKOUT) {
					reInitTouchTimeout = new Timer();
					reInitTouchTimeout.schedule(new TimerTask() {
						int epsTime = touchId.getLockoutTimeout();

						@Override
						public void run() {
							callback.onTicketFingerLocked(epsTime);
							epsTime -= 1000;
							if (epsTime < 0) {
								reInitTouchTimeout.cancel();
								callback.onFinishFingerLocked();
							}
						}
					}, 0, 1000);
				} else {
					callback.onFingerIncorrect();
				}
			}

			@Override
			public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
				callback.onHelpResult(helpMsgId);
			}

			@Override
			public void onAuthenticationSucceeded(Cipher fingerCipher) {
				callback.onFingerRecognized(fingerCipher);
			}
		});
	}
}
