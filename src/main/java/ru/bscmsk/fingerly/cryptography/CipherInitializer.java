package ru.bscmsk.fingerly.cryptography;

import android.content.Context;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import ru.bscmsk.fingerly.utils.AppLogger;
import ru.bscmsk.fingerly.utils.IPrefsStore;

/**
 * Created by mlakatkin on 02.06.2017.
 */

public class CipherInitializer {

    private final String CIPHER_KEY = "CIPHER_KEY";
    private final String IV_KEY = "IV_KEY";
    private final String CIPHER_KEY_ALIAS = "FP_KEY";

    private static final String ALGORITHM_AES = "AES";
    private static final String AES_MODE = "AES/CBC/PKCS5Padding";


    private IPrefsStore prefsStorage;
    private Context context;

    public CipherInitializer(Context context, IPrefsStore prefsStorage) {
        this.context = context;
        this.prefsStorage = prefsStorage;
        try {

        } catch (Exception e) {
            AppLogger.d(this, "can't init keystore exception ", e);
        }
    }

    public boolean hasCipherKey() {
        try {
            return KeyStoreHelper.isSigningKey(CIPHER_KEY_ALIAS) &&
                    prefsStorage.contains(CIPHER_KEY) && prefsStorage.contains(IV_KEY);
        } catch (Exception e) {
            AppLogger.d(this, "can't check hasCipherKey ", e);
            return false;
        }
    }

    public void removeCipherKey() {
        prefsStorage.remove(CIPHER_KEY);
        prefsStorage.remove(IV_KEY);
        try {
            KeyStoreHelper.remoeKey(CIPHER_KEY_ALIAS);
        } catch (Exception e) {
            AppLogger.d(this, "can't removeCipherKey ", e);
        }
    }

    public void generateNewKey(int keyLen, boolean invalidate) {
        try {
            prefsStorage.remove(CIPHER_KEY);
            prefsStorage.remove(IV_KEY);
            KeyStoreHelper.remoeKey(CIPHER_KEY_ALIAS);
            KeyStoreHelper.createKeys(context, CIPHER_KEY_ALIAS);

            byte[] aesKey = generateAES(keyLen);
            aesKey = KeyStoreHelper.encrypt(CIPHER_KEY_ALIAS, aesKey);
            String aesEncrypted = Base64.encodeToString(aesKey, Base64.NO_WRAP);
            prefsStorage.add(CIPHER_KEY, aesEncrypted);
            if (!invalidate) {
                byte[] iv = generateAES(128);// generate random IV 128 bit
                iv = KeyStoreHelper.encrypt(CIPHER_KEY_ALIAS, iv);
                String ivEncrypted = Base64.encodeToString(iv, Base64.NO_WRAP);
                prefsStorage.add(IV_KEY, ivEncrypted);
            }
        } catch (Exception e) {
            AppLogger.d(this, "generateNewKey failed cause ", e);
        }
    }

    public Cipher getCipher(boolean encrypt, String fingerHash, boolean invalidate) {
        try {
            SecretKey keySpec = new SecretKeySpec(getAesKey(), ALGORITHM_AES);
            IvParameterSpec ivParameterSpec;
            if (invalidate)
                ivParameterSpec = new IvParameterSpec(getIV(fingerHash));
            else
                ivParameterSpec = new IvParameterSpec(readIV());

            Cipher cipher = Cipher.getInstance(AES_MODE);
            if (encrypt)
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);
            else
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec);
            return cipher;
        } catch (Exception e) {
            AppLogger.d(this, "fail at cipher init modeEncrypt: " + encrypt + " cause ", e);
            return null;
        }
    }


    private byte[] getIV(String hash) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] sec = sha.digest(hash.getBytes());
            return Arrays.copyOf(sec, 16); //128 bit
        } catch (Exception e) {
            AppLogger.d(this, "getIV fail ", e);
            return new byte[16]; //128 bit
        }
    }

    private byte[] readIV() throws Exception {
        String base64 = prefsStorage.get(IV_KEY, String.class, null);
        byte[] encrypted = Base64.decode(base64, Base64.NO_WRAP);
        return KeyStoreHelper.decrypt(CIPHER_KEY_ALIAS, encrypted);
    }

    private byte[] getAesKey() throws Exception {
        String base64 = prefsStorage.get(CIPHER_KEY, String.class, null);
        byte[] encrypted = Base64.decode(base64, Base64.NO_WRAP);
        return KeyStoreHelper.decrypt(CIPHER_KEY_ALIAS, encrypted);
    }


    private byte[] generateAES(int keyLen) {
        byte[] key = new byte[keyLen / 8];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);
        return key;
    }
}
