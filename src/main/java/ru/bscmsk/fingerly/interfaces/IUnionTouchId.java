package ru.bscmsk.fingerly.interfaces;

/**
 * Created by mlakatkin on 01.06.2017.
 */

/**
 * API for working with different implementations of hardware fingerprint API
 */
public interface IUnionTouchId {

    /**
     * Create a storage key
     */
    void createKey(boolean invalidatedByBiometricEnrollment);

    /**
     * Verify API support on the current device
     *
     * @return true/false
     */
    boolean isApiSupported();

    /**
     * Checking user permissions to access the fingerprint scanner on the current device
     *
     * @return true/false
     */
    boolean isPermissionGranted();

    /**
     * Method on activate fingerprint scanner
     *
     * @param mode     enum for encrypt/decrypt
     * @param callback callback on result authentication by fingerprint
     */
    void requestFinger(CipherMode mode, UnionTouchIdCallback callback);

    /**
     * Method on cancel current fingerprint request
     */
    void cancel();

    /**
     * Method on get lockout timeout in milliseconds
     *
     * @return lockout timeout in milliseconds
     */
    int getLockoutTimeout();

    /**
     * Return permission code name
     *
     * @return permission name
     */
    String permissionCode();

    /**
     * Method for verifying the integrity of a fingerprint store
     *
     * @return true/false
     */
    boolean isKeyPermanentlyInvalidated();

    /**
     * Method for checking for fingerprints on the device
     *
     * @return true/false
     */
    boolean hasFingerPrints();

    /**
     * Remove a saved storage key
     */
    void clearKeystore();

    /**
     * Enum for encrypt/decrypt
     */
    enum CipherMode {
        ENCRYPT, DECRYPT
    }
}
