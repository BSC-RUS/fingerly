package ru.bscmsk.fingerly.cryptography;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import ru.bscmsk.fingerly.utils.AppLogger;
import ru.bscmsk.fingerly.utils.IPrefsStore;

/**
 * Created by mlakatkin on 02.06.2017.
 */

public class CipherInitializer {

    private final String CIPHER_KEY = "CIPHER_KEY";
    private final String IV_KEY = "CIPHER_KEY";
    private final String CIPHER_KEY_ALIAS = "FP_KEY";
    private static final String ANDROID_KEY_STORE_NAME = "AndroidKeyStore";

    private static final String ALGORITHM_RSA = "RSA";
    private static final String ALGORITHM_AES = "AES";

    private static final String RSA_MODE = "RSA/ECB/PKCS1Padding";
    private static final String AES_MODE = "AES/CBC/PKCS5Padding";


    private IPrefsStore prefsStorage;
    private Context context;
    private KeyStore keyStore;

    public CipherInitializer(Context context, IPrefsStore prefsStorage) {
        this.context = context;
        this.prefsStorage = prefsStorage;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_NAME);
            keyStore.load(null);
        } catch (Exception e) {
            AppLogger.d(this, "can't init keystore exception ", e);
        }
    }

    public boolean hasCipherKey() {
        try {
            return keyStore.containsAlias(CIPHER_KEY_ALIAS) &&
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
            keyStore.deleteEntry(CIPHER_KEY_ALIAS);
        } catch (Exception e) {
            AppLogger.d(this, "can't removeCipherKey ", e);
        }
    }

    public void generateNewKey(int keyLen, boolean invalidate) {
        try {
            prefsStorage.remove(CIPHER_KEY);
            prefsStorage.remove(IV_KEY);
            keyStore.deleteEntry(CIPHER_KEY_ALIAS);
            generateRsa();
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(CIPHER_KEY_ALIAS, null);
            if (privateKeyEntry == null)
                throw new Exception("can't create secure keypair");
            PublicKey publicKey = privateKeyEntry.getCertificate().getPublicKey();
            if (publicKey == null)
                throw new Exception("can't create secure keypair");
            byte[] aesKey = generateAES(keyLen);
            aesKey = rsaEncrypt(aesKey, publicKey);
            String aesEncrypted = Base64.encodeToString(aesKey, Base64.NO_WRAP);
            prefsStorage.add(CIPHER_KEY, aesEncrypted);
            if (!invalidate) {
                byte[] iv = generateAES(128);// generate random IV 128 bit
                iv = rsaEncrypt(iv, publicKey);
                String ivEncrypted = Base64.encodeToString(iv, Base64.NO_WRAP);
                prefsStorage.add(IV_KEY, ivEncrypted);
            }
        } catch (Exception e) {
            AppLogger.d(this, "generateNewKey failed cause ", e);
        }
    }

    private KeyPair generateRsa() {
        // Generate a key pair for encryption
        Calendar start = Calendar.getInstance(); //from now
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 30); // key for 30 years
        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(CIPHER_KEY_ALIAS)
                .setSubject(new X500Principal("CN=" + CIPHER_KEY_ALIAS))
                .setSerialNumber(BigInteger.valueOf(Math.abs(CIPHER_KEY_ALIAS.hashCode())))
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build();
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM_RSA, ANDROID_KEY_STORE_NAME);
            kpg.initialize(spec);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            AppLogger.d(this, "failed to create secure key cause ", e);
            return null;

        }

    }

    public Cipher getCipher(boolean encrypt, String fingerHash, boolean invalidate) {
        KeyStore.PrivateKeyEntry privateKeyEntry;
        try {
            privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(CIPHER_KEY_ALIAS, null);
        } catch (Exception e) {
            AppLogger.d(this, "cant' read secure storage cause ", e);
            return null;
        }
        try {
            SecretKey keySpec = new SecretKeySpec(getAesKey(privateKeyEntry.getPrivateKey()), ALGORITHM_AES);
            IvParameterSpec ivParameterSpec;
            if (invalidate)
                ivParameterSpec = new IvParameterSpec(getIV(fingerHash));
            else
                ivParameterSpec = new IvParameterSpec(readIV(privateKeyEntry.getPrivateKey()));

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

    private byte[] readIV(PrivateKey privateKey) throws Exception {
        String base64 = prefsStorage.get(IV_KEY, String.class, null);
        byte[] encrypted = Base64.decode(base64, Base64.NO_WRAP);
        return rsaDecrypt(encrypted, privateKey);
    }

    private byte[] getAesKey(PrivateKey privateKey) throws Exception {
        String base64 = prefsStorage.get(CIPHER_KEY, String.class, null);
        byte[] encrypted = Base64.decode(base64, Base64.NO_WRAP);
        return rsaDecrypt(encrypted, privateKey);
    }


    private byte[] generateAES(int keyLen) {
        byte[] key = new byte[keyLen / 8];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);
        return key;
    }

    private byte[] rsaEncrypt(byte[] secret, PublicKey publicKey) throws Exception {
        Cipher inputCipher = Cipher.getInstance(RSA_MODE);
        inputCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return inputCipher.doFinal(secret);
    }

    private byte[] rsaDecrypt(byte[] secret, PrivateKey privateKey) throws Exception {
        Cipher inputCipher = Cipher.getInstance(RSA_MODE);
        inputCipher.init(Cipher.DECRYPT_MODE, privateKey);
        return inputCipher.doFinal(secret);
    }
}
