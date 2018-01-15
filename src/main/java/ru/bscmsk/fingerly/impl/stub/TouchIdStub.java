package ru.bscmsk.fingerly.impl.stub;

import android.content.Context;

import ru.bscmsk.fingerly.impl.AbstractTouchId;
import ru.bscmsk.fingerly.interfaces.UnionTouchIdCallback;
import ru.bscmsk.fingerly.utils.IPrefsStore;

import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ERROR_HW_UNAVAILABLE;

/**
 * Created by mlakatkin on 05.06.2017.
 */

public class TouchIdStub extends AbstractTouchId{

	public TouchIdStub(Context context, IPrefsStore prefs) {
		super(context, prefs);
	}

	@Override
	public void createKey() {}

	@Override
	public boolean isApiSupported() {
		return false;
	}

	@Override
	public boolean isPermissionGranted() {
		return true;
	}

	@Override
	public void requestFinger(CipherMode mode, UnionTouchIdCallback callback) {
		callback.onAuthenticationError(FINGERPRINT_ERROR_HW_UNAVAILABLE, "FINGERPRINT_ERROR_HW_UNAVAILABLE");
	}

	@Override
	public void cancel() {}

	@Override
	public int getLockoutTimeout() {
		return 0;
	}

	@Override
	public String permissionCode() {
		return "";
	}

	@Override
	public boolean isKeyPermanentlyInvalidated() {
		return false;
	}

	@Override
	public boolean hasFingerPrints() {
		return false;
	}

	@Override
	public void clearKeystore() {}

	@Override
	protected void initTouchId() {}
}
