package ru.bscmsk.fingerly.interfaces;

import javax.crypto.Cipher;

/**
 * Created by mlakatkin on 01.06.2017.
 */

/**
 * Callback on result authentication by fingerprint
 */
public interface UnionTouchIdCallback {

    /**
     * Called when an unrecoverable error has been encountered and the operation is complete.
     *
     * @param errMsgId  message id
     * @param errString message value
     */
    void onAuthenticationError(int errMsgId, CharSequence errString);

    /**
     * Called when a recoverable error has been encountered during authentication.
     *
     * @param helpMsgId  message id
     * @param helpString message value
     */
    void onAuthenticationHelp(int helpMsgId, CharSequence helpString);

    /**
     * Called when a fingerprint is recognized.
     *
     * @param fingerCipher cipher can be used for encrypt/decrypt
     */
    void onAuthenticationSucceeded(Cipher fingerCipher);
}
