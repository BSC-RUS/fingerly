package ru.bscmsk.fingerly.impl;

import android.content.Context;
import android.os.Vibrator;

import ru.bscmsk.fingerly.interfaces.IUnionTouchId;
import ru.bscmsk.fingerly.utils.IPrefsStore;

import static android.content.Context.VIBRATOR_SERVICE;

/**
 * Created by izashkalov on 12.01.2018.
 */

public abstract class AbstractTouchId implements IUnionTouchId {

    protected final Context context;
    protected final IPrefsStore prefsStore;
    protected final String SALT;
    protected Vibrator vibrate;

    public AbstractTouchId(Context context, IPrefsStore prefs) {
        this.prefsStore = prefs;
        this.context = context;
        this.SALT = context.getApplicationContext().getPackageName();
        this.vibrate = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        initTouchId();
    }

    protected abstract void initTouchId();

    protected void makeVibrate() {
        if (vibrate != null)
            vibrate.vibrate(100);
    }
}
