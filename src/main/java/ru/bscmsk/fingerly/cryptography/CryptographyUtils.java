package ru.bscmsk.fingerly.cryptography;

import android.support.annotation.Nullable;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import ru.bscmsk.fingerly.utils.AppLogger;

/**
 * Created by mlakatkin on 02.06.2017.
 */

public class CryptographyUtils {

	private final static String TAG = CryptographyUtils.class.getSimpleName();

	@Nullable
	public static String tryEncrypt(Cipher cipher, String msg) {
		try {
			byte[] encrypted = cipher.doFinal(msg.getBytes());
			return Base64.encodeToString(encrypted, Base64.NO_WRAP);
		} catch (BadPaddingException | IllegalBlockSizeException e) {
			AppLogger.e(CryptographyUtils.class,"Failed to encrypt the data with the generated key." + e.getMessage());
		}

		return null;
	}

	@Nullable
	public static String tryDecrypt(Cipher cipher, String msg) {
		try {
			byte[] decodedMsg = Base64.decode(msg, Base64.NO_WRAP);
			byte[] decryptedMessage = cipher.doFinal(decodedMsg);
			return new String(decryptedMessage);
		} catch (BadPaddingException | IllegalBlockSizeException e) {
			AppLogger.e(CryptographyUtils.class, "Failed to encrypt the data with the generated key." + e.getMessage());
		}
		return null;
	}

	public static String getHashed(String text, String randomSalt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		final MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(randomSalt.getBytes());
		byte[] result = digest.digest(text.getBytes());
		StringBuilder sb = new StringBuilder();
		for (byte b : result) {
			sb.append(String.format("%02X ", b));
		}
		return sb.toString();
	}
}
