package ru.bscmsk.fingerlydemo.service;

import android.support.annotation.Nullable;
import ru.bscmsk.fingerly.utils.IPrefsStore;

import javax.crypto.Cipher;
import java.util.concurrent.ExecutionException;

/**
 * Created by mlakatkin on 07.12.2017.
 */

public interface IFingerPrintService {

	boolean isFingerPrintAvailable();

	IPrefsStore getPrefs();

	void cancel();

	void reset();

	boolean isStorageCompromised();

	boolean hasFingerPrints();

	long getTimeout();

	boolean checkPermission();

	String getPermissionCode();

	void createFingerRequest(IFingerPrintCallback callback);

	void authFingerRequest(IFingerPrintCallback callback);

	boolean hasEncryptedSecret();

	void setFingerSecret(Cipher cipher, String text) throws ExecutionException;

	@Nullable
	String getEncryptedMessage();

	@Nullable
	String getFingerSecret(Cipher cipher);
}
