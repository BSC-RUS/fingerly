package ru.bscmsk.fingerly;

import android.content.Context;
import android.os.Build;

import ru.bscmsk.fingerly.impl.stub.TouchIdStub;
import ru.bscmsk.fingerly.impl.android.AndroidTouchId;
import ru.bscmsk.fingerly.impl.meizu.MeizuTouchId;
import ru.bscmsk.fingerly.impl.samsung.SamsungTouchId;
import ru.bscmsk.fingerly.interfaces.IUnionTouchId;
import ru.bscmsk.fingerly.utils.AppLogger;
import ru.bscmsk.fingerly.utils.IPrefsStore;

/**
 * Created by mlakatkin on 05.06.2017.
 */

public class TouchIdFactory {

    public static IUnionTouchId getTouchId(Context context, IPrefsStore preferences) {
        AppLogger.d(TouchIdFactory.class,"Android build code: " + Build.VERSION.SDK_INT + " version: " + Build.VERSION.RELEASE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AppLogger.d(TouchIdFactory.class,"Android version is supported");
            AndroidTouchId androidTouchId = new AndroidTouchId(context, preferences);
            if (androidTouchId.isApiSupported()) {
                AppLogger.d(TouchIdFactory.class, "Android api is supported.\n Use Android api");
                return androidTouchId;
            }
        }

        MeizuTouchId meizuTouchId = new MeizuTouchId(context, preferences);
        if (meizuTouchId.isApiSupported()) {
            AppLogger.d(TouchIdFactory.class, "Meizu api is supported.\n Use Meizu api");
            return meizuTouchId;
        }

        SamsungTouchId samsungTouchId = new SamsungTouchId(context, preferences);
        if (samsungTouchId.isApiSupported()) {
            AppLogger.d(TouchIdFactory.class, "Samsung api is supported.\n Use Samsung api");
            return samsungTouchId;
        }
        AppLogger.d(TouchIdFactory.class, "Use fingerprint stub");
        return new TouchIdStub(context, preferences);
    }
}
