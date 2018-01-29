package ru.bscmsk.fingerlydemo;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import ru.bscmsk.fingerlydemo.service.FingerPrintService;
import ru.bscmsk.fingerlydemo.service.IFingerPrintCallback;
import ru.bscmsk.fingerlydemo.service.IFingerPrintService;

import javax.crypto.Cipher;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static ru.bscmsk.fingerly.TouchIdCodes.FINGERPRINT_ACQUIRED_PARTIAL;

/**
 * Created by izashkalov on 17.01.2018.
 */

public class LoginActivity extends AbstractBaseActivity {

	private static final String RANDOM_UUID_KEY = "RANDOM_UUID_KEY";
	private Button btnAddNewTouch;
	private Button btnResetTouch;
	private String randomUUID;
	private TextView tvTitle;

	@Override
	protected void fillUI() {
		updateTitleViewState();
		updateEnableStatusBtn();
	}

	private void resetStorage() {
		getFpService().reset();
		getFpService().cancel();
		updateEnableStatusBtn();
		updateTitleViewState();
	}

	private void updateTitleViewState() {
		if (getFpService().hasEncryptedSecret()) {
			tvTitle.setText(getString(R.string.login_success_title));
		} else {
			tvTitle.setText(getString(R.string.login_error_title));
		}
	}

	@Override
	protected void initUI() {
		setContentView(R.layout.activity_login_layout);
		btnAddNewTouch = (Button) findViewById(R.id.btn_add_new_touch);
		btnAddNewTouch.setOnClickListener(v -> {
			if(!getFpService().hasFingerPrints()){
				showDialogSuccesButton(getString(R.string.login_emty_fingerprint_storage), (dialogInterface, i) -> {
					updateEnableStatusBtn();
					updateTitleViewState();
				});
				return;
			}

			showProgress(getString(R.string.login_add_new_touch_loader_text));
			randomUUID = UUID.randomUUID().toString();
			saveRandomUUID();
			initCreateFingerRequestCallback();
		});

		tvTitle = (TextView) findViewById(R.id.tv_title);

		btnResetTouch = (Button) findViewById(R.id.btn_reset_touch);
		btnResetTouch.setOnClickListener(v -> {
			getFpService().reset();
			updateEnableStatusBtn();
			updateTitleViewState();
			getFpService().cancel();
		});
	}

	private void updateEnableStatusBtn() {
		btnAddNewTouch.setEnabled(!getFpService().hasEncryptedSecret());
		btnResetTouch.setEnabled(getFpService().hasEncryptedSecret());
	}

	// This value you can send to the server for user authentication
	private void saveRandomUUID() {
		getFpService().getPrefs().add(RANDOM_UUID_KEY, randomUUID);
	}

	private void initCreateFingerRequestCallback() {
		getFpService().createFingerRequest(new IFingerPrintCallback() {
			@Override
			public void onFingerRecognized(Cipher cipher) {
				try {
					getFpService().setFingerSecret(cipher, randomUUID);
					showDialogSuccesButton("Add new touch", (dialogInterface, i) -> {
						dialogInterface.dismiss();
						initLoginFingerRequest();
						updateEnableStatusBtn();
						updateTitleViewState();
						hideProgress();
					});
				} catch (ExecutionException e) {
					showDialogSuccesButton("Erro edding touch", (dialogInterface, i) -> {
						dialogInterface.dismiss();
						hideProgress();
					});
				}
			}

			@Override
			public void onFingerIncorrect() {
				incorrecctFinger();
			}

			@Override
			public void onTicketFingerLocked(int epsTime) {
				ticketFingerLocked(epsTime);
			}

			@Override
			public void onFinishFingerLocked() {
				hideProgress();
			}

			@Override
			public void onCancelled() {
				hideProgress();
			}

			@Override
			public void onHelpResult(int helpMsgId) {
				checkHelpMessage(helpMsgId);
			}
		});
	}

	private void initLoginFingerRequest() {
		getFpService().authFingerRequest(new IFingerPrintCallback() {
			@Override
			public void onFingerRecognized(Cipher cipher) {
				String decryptMessage = getFpService().getFingerSecret(cipher);
				if (decryptMessage != null) {
					String touchKey = getFpService().getPrefs().get(RANDOM_UUID_KEY, String.class, "");
					if (touchKey.equals(decryptMessage)) {
						successLogin();
					}
				} else {
					Toast.makeText(LoginActivity.this, "Error while decrypt message", Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void onFingerIncorrect() {
				incorrecctFinger();
			}

			@Override
			public void onTicketFingerLocked(int epsTime) {
				ticketFingerLocked(epsTime);
			}

			@Override
			public void onFinishFingerLocked() {
				hideProgress();
			}

			@Override
			public void onCancelled() {
				hideProgress();
			}

			@Override
			public void onHelpResult(int helpMsgId) {
				checkHelpMessage(helpMsgId);
			}
		});
	}

	private void checkHelpMessage(int helpMsgId) {
		if(helpMsgId == FINGERPRINT_ACQUIRED_PARTIAL){
			showDialogSuccesButton("Try again. bad finger was read", (dialogInterface, i) -> {
				dialogInterface.dismiss();
				initLoginFingerRequest();
				updateEnableStatusBtn();
				updateTitleViewState();
				hideProgress();
			});
		}
	}

	private void incorrecctFinger() {
		Toast.makeText(LoginActivity.this, "Incorrect finger", Toast.LENGTH_SHORT).show();
	}

	private void successLogin() {
		AuthenticationResultActivity.start(this);
	}

	private void ticketFingerLocked(int epsTime) {
		runOnUiThread(() -> {
			showProgress("System has block fingerprint scanner wait " + epsTime / 1000);
		});
	}

	private IFingerPrintService getFpService() {
		return FingerPrintService.getService(getApplication());
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(getFpService().isStorageCompromised()){
			resetStorage();
			return;
		}

		if(getFpService().hasEncryptedSecret() && getFpService().hasFingerPrints())
			initLoginFingerRequest();
	}
}
