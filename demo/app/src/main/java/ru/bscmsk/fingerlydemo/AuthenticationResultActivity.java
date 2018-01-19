package ru.bscmsk.fingerlydemo;

import android.app.Activity;
import android.content.Intent;

/**
 * Created by izashkalov on 18.01.2018.
 */

public class AuthenticationResultActivity extends AbstractBaseActivity {

	public static void start(Activity context) {
		Intent intent = new Intent(context, AuthenticationResultActivity.class);
		context.startActivity(intent);
	}

	@Override
	protected void initUI() {
		setContentView(R.layout.activity_authentication_result_layout);
	}

	@Override
	protected void fillUI() {

	}

}
