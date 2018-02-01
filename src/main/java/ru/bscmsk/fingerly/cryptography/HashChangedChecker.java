package ru.bscmsk.fingerly.cryptography;

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import ru.bscmsk.fingerly.utils.IPrefsStore;

/**
 * Created by mlakatkin on 02.06.2017.
 */

public class HashChangedChecker {

    private final String HASH_KEY = "HASH_KEY";
    private static final String INVALIDATE_BY_BIOMETRIC_ENROLLMENT = "INVALIDATE_BY_BIOMETRIC_ENROLLMENT";
    private final String PASS_REPOSITORY_SALT = "3ae3bec920fa5b28749bb1233f6be7014e32313ce18a3b461fd9e96f9be4d79f";

    private IPrefsStore prefsStore;

    public HashChangedChecker(IPrefsStore sharedPreferences) {
        this.prefsStore = sharedPreferences;
    }

    public void saveHash(String hash, boolean invalidateByEnrolment) {
        try {
            String fingers = CryptographyUtils.getHashed(hash, PASS_REPOSITORY_SALT);
            prefsStore.add(HASH_KEY, fingers);
            prefsStore.add(INVALIDATE_BY_BIOMETRIC_ENROLLMENT, invalidateByEnrolment);
        } catch (NoSuchAlgorithmException |
                UnsupportedEncodingException e) {
            prefsStore.remove(HASH_KEY);
            prefsStore.remove(INVALIDATE_BY_BIOMETRIC_ENROLLMENT);
        }
    }

    public boolean hasSavedHash() {
        if (!prefsStore.contains(HASH_KEY))
            return false;
        return !TextUtils.isEmpty(prefsStore.get(HASH_KEY, String.class, null));
    }

    public boolean isInvalidateByBiometricEnrollmentEnabled() {
        return prefsStore.get(INVALIDATE_BY_BIOMETRIC_ENROLLMENT, boolean.class, true);
    }

    public void removeHash() {
        prefsStore.remove(HASH_KEY);
    }

    public boolean checkHash(String hash) {
        if (TextUtils.isEmpty(hash) || !isInvalidateByBiometricEnrollmentEnabled())
            return false;
        try {
            String oldHash = CryptographyUtils.getHashed(prefsStore.get(HASH_KEY, String.class, null), PASS_REPOSITORY_SALT);
            return hash.equals(oldHash);
        } catch (NoSuchAlgorithmException |
                UnsupportedEncodingException e) {
            return false;
        }
    }

}
