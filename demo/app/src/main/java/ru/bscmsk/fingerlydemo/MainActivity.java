package ru.bscmsk.fingerlydemo;

import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import javax.crypto.Cipher;

import ru.bscmsk.fingerlydemo.service.FingerPrintService;
import ru.bscmsk.fingerlydemo.service.IFingerPrintCallback;
import ru.bscmsk.fingerlydemo.service.IFingerPrintService;

public class MainActivity extends AppCompatActivity {

	private static final int REQUEST_PERMISSION_CODE = 123;

//	private void fillUI() {
//		encryptedMessage.setText(getFpService().getEncryptedMessage());
//		actionBtn.setEnabled(checkTouch());
//		actionBtn.setText(getFpService().hasEncryptedSecret() ? "Decrypt message" : "Encrypt message");
//		cancelBtn.setEnabled(false);
//		cancelBtn.setOnClickListener(v -> getFpService().cancel());
//	}

	private boolean checkTouch() {
		if (!getFpService().checkPermission()) {
			requestPermission(REQUEST_PERMISSION_CODE, getFpService().getPermissionCode());
			return false;
		}
		if (!getFpService().isFingerPrintAvailable()) {
			Toast.makeText(MainActivity.this, "your phone is not support fingerprint ", Toast.LENGTH_SHORT).show();
			return false;
		}
		if (!getFpService().hasFingerPrints()) {
			Toast.makeText(MainActivity.this, "You should add fingerprint", Toast.LENGTH_SHORT).show();
			return false;
		}
		if (getFpService().isStorageCompromised()) {
			Toast.makeText(MainActivity.this, "keystore is compromised or not exist ", Toast.LENGTH_SHORT).show();
			getFpService().reset();
			return true;
		}
		return true;
	}

	private void requestPermission(int requestCode, String... permissions) {
		ActivityCompat.requestPermissions(this,
			permissions, requestCode);
	}

	private IFingerPrintService getFpService() {
		return FingerPrintService.getService(getApplication());
	}
}
