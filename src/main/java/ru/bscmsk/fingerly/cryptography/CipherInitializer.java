package ru.bscmsk.fingerly.cryptography;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import ru.bscmsk.fingerly.utils.IPrefsStore;

/**
 * Created by mlakatkin on 02.06.2017.
 */

public class CipherInitializer {

	private final String CIPHER_KEY = "CIPHER_KEY";
	private final String PASS_REPOSITORY_SALT = "1c3c123932880750cf05a415622a7f7f532b36c6c2c151964f60265b1c5b2dd3";
	private IPrefsStore prefsStorage;
	private SecretKey key;

	public CipherInitializer(IPrefsStore prefsStorage) {
		this.prefsStorage = prefsStorage;
	}

	public boolean hasCipherKey() {
		if ((!prefsStorage.contains(CIPHER_KEY)))
			return false;
		try {
			return Base64.decode(prefsStorage.get(CIPHER_KEY, String.class, null), Base64.NO_WRAP) != null;
		} catch (Exception e) {
			return false;
		}
	}

	public void removeCipherKey() {
		prefsStorage.remove(CIPHER_KEY);
		key = null;
	}

	public void generateNewKey(String hash, int keyLen) {
		try {
			prefsStorage.remove(CIPHER_KEY);
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(keyLen);
			key = keyGen.generateKey();

		} catch (NoSuchAlgorithmException e) {
			this.key = null;
		}
		if (key != null) {
			Cipher cipher = initNewCipher(true, hash + PASS_REPOSITORY_SALT);
			if (cipher == null) {
				key = null;
				return;
			}
			try {
				String secretAes = Base64.encodeToString(cipher.doFinal(key.getEncoded()), Base64.NO_WRAP);
				prefsStorage.add(CIPHER_KEY, secretAes);
			} catch (Exception e) {
				key = null;
			}
		}
	}

	public Cipher getCipher(boolean encrypt, String key) {
		if (this.key == null && !tryReadOldCipher(key))
			return null;
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			IvParameterSpec ivParameterSpec = new IvParameterSpec(this.key.getEncoded());
			if (encrypt)
				cipher.init(Cipher.ENCRYPT_MODE, this.key, ivParameterSpec);
			else
				cipher.init(Cipher.DECRYPT_MODE, this.key, ivParameterSpec);
			return cipher;
		} catch (Exception e) {
			return null;
		}
	}


	private boolean tryReadOldCipher(String hash) {
		Cipher cipher = initNewCipher(false, hash + PASS_REPOSITORY_SALT);
		if (cipher == null)
			return false;
		try {
			String fromShp = prefsStorage.get(CIPHER_KEY, String.class, null);
			if (fromShp == null)
				return false;
			byte[] keyBytes = cipher.doFinal(Base64.decode(fromShp, Base64.NO_WRAP));
			if (keyBytes != null)
				key = new SecretKeySpec(keyBytes, "AES");
			return key != null;
		} catch (Exception e) {
			key = null;
			return false;
		}
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
}
