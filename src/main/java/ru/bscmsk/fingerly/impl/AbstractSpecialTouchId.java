package ru.bscmsk.fingerly.impl;

import android.content.Context;

import ru.bscmsk.fingerly.cryptography.CipherInitializer;
import ru.bscmsk.fingerly.cryptography.HashChangedChecker;
import ru.bscmsk.fingerly.utils.IPrefsStore;

/**
 * Created by izashkalov on 12.01.2018.
 */

public abstract class AbstractSpecialTouchId extends AbstractTouchId {

    protected final int AES_KEY_LEN = 128;
    protected CipherInitializer cipherInitializer;
    protected HashChangedChecker hashChangedChecker;

    public AbstractSpecialTouchId(Context context, IPrefsStore prefs) {
        super(context, prefs);
    }

    @Override
    protected void initTouchId() {
        this.hashChangedChecker = new HashChangedChecker(prefsStore);
        this.cipherInitializer = new CipherInitializer(prefsStore);
    }

    @Override
    public void createKey() {
        String fingerHash = getHashedFingers();
        cipherInitializer.removeCipherKey();
        cipherInitializer.generateNewKey(fingerHash, AES_KEY_LEN);
        hashChangedChecker.removeHash();
        hashChangedChecker.saveHash(fingerHash);
    }

    @Override
    public void clearKeystore() {
        hashChangedChecker.removeHash();
        hashChangedChecker.removeHash();
    }

    @Override
    public boolean isKeyPermanentlyInvalidated() {
        return hashChangedChecker.hasSavedHash() &&
                !(hashChangedChecker.checkHash(getHashedFingers()) &&
                        cipherInitializer.hasCipherKey());
    }

    protected abstract String getHashedFingers();
}
