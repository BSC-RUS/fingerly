package ru.bscmsk.fingerly.cryptography;

import android.text.TextUtils;
import android.util.Base64;

import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import ru.bscmsk.fingerly.utils.IPrefsStore;

/**
 * Created by mlakatkin on 02.06.2017.
 */

public class HashChangedChecker {

	private final String HASH_KEY = "HASH_KEY";
	private final String PASS_REPOSITORY_SALT = "3ae3bec920fa5b28749bb1233f6be7014e32313ce18a3b461fd9e96f9be4d79f";

	private IPrefsStore prefsStore;

	public HashChangedChecker(IPrefsStore sharedPreferences) {
		this.prefsStore = sharedPreferences;
	}

	private Cipher initNewCipher(boolean encrypt, String key) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			byte[] sec = sha.digest(key.getBytes());
			sec = Arrays.copyOf(sec, 16);
			SecretKeySpec keySpec = new SecretKeySpec(sec, "AES");
			IvParameterSpec ivParameterSpec = new IvParameterSpec(sec);
			if (encrypt)
				cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);
			else
				cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec);
			return cipher;
		} catch (Exception e) {
			return null;
		}
	}

	public void saveHash(String hash) {
		Cipher cipher = initNewCipher(true, hash + PASS_REPOSITORY_SALT);
		if (cipher != null) {
			try {
				String fingers = Base64.encodeToString(cipher.doFinal(hash.getBytes()), Base64.NO_WRAP);
				prefsStore.add(HASH_KEY, fingers);
			} catch (Exception e) {
				prefsStore.remove(HASH_KEY);
			}
		} else {
			prefsStore.remove(HASH_KEY);
		}
	}

	public boolean hasSavedHash() {
		if (!prefsStore.contains(HASH_KEY))
			return false;
		try {
			return Base64.decode(prefsStore.get(HASH_KEY, String.class, null), Base64.NO_WRAP) != null;
		} catch (Exception e) {
			return false;
		}
	}

	public void removeHash() {
		prefsStore.remove(HASH_KEY);
	}

	public boolean checkHash(String hash) {
		if (TextUtils.isEmpty(hash))
			return false;
		Cipher cipher = initNewCipher(false, hash + PASS_REPOSITORY_SALT);
		if (cipher == null)
			return false;
		String oldFingerEnc = prefsStore.get(HASH_KEY, String.class,null);
		if (oldFingerEnc == null)
			return false;
		try {
			byte[] finger = cipher.doFinal(Base64.decode(oldFingerEnc, Base64.NO_WRAP));
			String oldHash = new String(finger);
			return oldHash.equals(hash);
		} catch (Exception e) {
			return false;
		}
	}

}
